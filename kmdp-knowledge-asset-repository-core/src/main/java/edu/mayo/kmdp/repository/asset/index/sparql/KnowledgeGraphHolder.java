package edu.mayo.kmdp.repository.asset.index.sparql;

import static edu.mayo.kmdp.repository.asset.index.sparql.KnowledgeGraphInfo.ASSET_URI;
import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.Forbidden;
import static java.nio.charset.Charset.defaultCharset;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.rdf.model.ResourceFactory.createStatement;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings.DEFAULT;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.rdf.JenaRdfParser;
import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryService;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerProperties;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerProperties.KnowledgeAssetRepositoryOptions;
import edu.mayo.kmdp.util.NameUtils;
import edu.mayo.kmdp.util.concurrent.LatchedScheduleExecutor;
import edu.mayo.kmdp.util.concurrent.Once;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.shared.Lock;
import org.apache.jena.vocabulary.RDFS;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.artifact.v4.server.KnowledgeArtifactApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.artifact.v4.server.KnowledgeArtifactRepositoryApiInternal;
import org.omg.spec.api4kp._20200801.services.KnowledgeBase;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.terms.ConceptTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Wrapper class that mediates the access to a Knowledge Asset Repository
 * Knowledge Graph, implemented by means of a Jena RDF Graph (Model)
 *
 * The class has two primary functions:
 * - regulate access to the Graph as a shared read/write resource in a multi-threaded environment
 *  -- primarily using Jena native 'locking'
 * - manage the persistence of the Graph, as a 'well known' Knowledge Artifact
 *  -- the Graph is reloaded on startup
 *  -- reinitialized (and persisted) on reset
 *  -- persisted on an explicit command
 *  -- persisted after a period of time T from the latest Write to the graph
 *     (rescheduling if a new Write occurs before T, or canceling on an explicit write/reset)
 *  -- persisted on Shutdown
 */
@Component
public class KnowledgeGraphHolder implements KnowledgeBaseApiInternal._getKnowledgeBase {

  private static final Logger logger = LoggerFactory.getLogger(KnowledgeGraphHolder.class);

  /**
   * Parser used to read/write the Knowledge Graph into the underlying Artifact Repository
   */
  private static final JenaRdfParser parser = new JenaRdfParser();
  /**
   * Parser used to read/write the Knowledge Graph Surrogate into the underlying Artifact Repository
   */
  private static final Surrogate2Parser surrogateParser = new Surrogate2Parser();

  /**
   * Knowledge Graph metadata (Identifiers + canonical Surrogate)
   */
  @Autowired
  private KnowledgeGraphInfo kgi;

  /**
   * Artifact Repository client
   */
  @Autowired
  private KnowledgeArtifactApiInternal artifactApi;
  /**
   * Artifact Repository client
   */
  @Autowired
  private KnowledgeArtifactRepositoryApiInternal repoApi;
  /**
   * Artifact Repository federated repository ID
   */
  @Value("${edu.mayo.kmdp.repository.artifact.identifier:default}")
  private String defaultRepositoryId = "default";

  /**
   * The Knowledge Graph
   */
  private Model knowledgeGraph;

  /**
   * Knowledge Base API wrapper for the Knowledge Graph
   */
  private KnowledgeBase kBase;

  /**
   * {@link Once} operator used to load the Graph on startup
   */
  private final Once<Boolean> graphLoaded =
      new Once<>("GraphLoader", this::ensureGraphLoaded);
  /**
   * {@link Once} operator used to persist the Graph on shutdown
   */
  private final Once<Answer<Void>> graphClosed =
      new Once<>("GraphSealer", this::persistEncodedGraphIntoArtifactRepository);

  /**
   * Atomic flag that marks Graphs that have been shut down (no more writes allowed)
   */
  private AtomicBoolean shutdown = new AtomicBoolean(false);

  /**
   * Delay between the last Graph Write Operation and its persistence
   */
  @Value("${edu.mayo.kmdp.repository.asset.graph.autoSaveDelay:10}")
  private int autoSaveDelay;
  /**
   * Latched Scheduler that persists the Graph after the 'last' Write operation
   */
  private LatchedScheduleExecutor<Answer<Void>> saver;


  /**
   * Default constructor used with Component injection
   */
  public KnowledgeGraphHolder() {
    // default constructor for auto-wiring
  }

  /**
   * Static factory method
   * @param artifactRepo the Artifact Repository used for persistence
   * @param kgi the Graph Metadata
   * @return a new {@link KnowledgeGraphHolder}
   */
  public static KnowledgeGraphHolder newKnowledgeGraphHolder(
      KnowledgeArtifactRepositoryService artifactRepo,
      KnowledgeGraphInfo kgi) {
    return newKnowledgeGraphHolder(
        artifactRepo,
        kgi,
        KnowledgeAssetRepositoryServerProperties.emptyProperties());
  }

  /**
   * Static factory method
   * @param artifactRepo the Artifact Repository used for persistence
   * @param kgi the Graph Metadata
   * @param cfg additional configuration
   * @return a new {@link KnowledgeGraphHolder}
   */
  public static KnowledgeGraphHolder newKnowledgeGraphHolder(
      KnowledgeArtifactRepositoryService artifactRepo,
      KnowledgeGraphInfo kgi,
      KnowledgeAssetRepositoryServerProperties cfg) {
    var kgh = new KnowledgeGraphHolder();
    kgh.repoApi = artifactRepo;
    kgh.artifactApi = artifactRepo;
    kgh.kgi = kgi;
    kgh.autoSaveDelay = cfg.getTyped(KnowledgeAssetRepositoryOptions.AUTOSAVE_DELAY);
    kgh.init();
    return kgh;
  }

  /**
   * Initialization method
   * Loads the Graph from the Artifact Repository, if exists, or creates and persists a new one
   */
  @PostConstruct
  public void init() {
    logger.info("INITIALIZE Knowledge Graph {}", kgi.getKnowledgeGraphLabel());
    this.defaultRepositoryId = validateArtifactRepositoryId();
    this.saver =
        new LatchedScheduleExecutor<>(autoSaveDelay,
            this::persistEncodedGraphIntoArtifactRepository);
    graphLoaded.executeIfNotDone();
  }

  /**
   * Shutdown method
   * Tries to close any pending tasks, before finally persisting the Graph one last time
   */
  @PreDestroy
  protected void shutdown() {
    logger.info("SHUT DOWN Knowledge Graph {}", kgi.getKnowledgeGraphLabel());
    try {
      if (!shutdown.getAndSet(true)) {
        saver.shutdown();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      if (graphLoaded.isDone()) {
        graphClosed.executeIfNotDone();
      }
    }
  }

  /**
   * Persists the Graph directly (with no delay) in the Artifact Repository
   * Cancels any pending scheduled writes
   * @return the Answer resulting from the persistence, or FORBIDDEN if the Graph has been shut down
   */
  public Answer<Void> saveGraph() {
    logger.info("EXPLICIT PERSIST the Knowledge Graph {}", kgi.getKnowledgeGraphLabel());
    saver.cancelExecution(false);
    return persistEncodedGraphIntoArtifactRepository();
  }

  /**
   * @return The graph metadata
   */
  public KnowledgeGraphInfo getInfo() {
    return kgi;
  }

  /**
   * Resets the Knowledge Graph
   * Cancels any pending write, and re-creates a new empty Graph, persisting it in the process
   */
  public synchronized void resetGraph() {
    logger.info("RESET Knowledge Graph {}", kgi.getKnowledgeGraphLabel());
    saver.cancelExecution(true);
    boolean success = reinitialize().isSuccess();
    if (! success) {
      var msg = "Unable to RESET a Knowledge Graph Successfully";
      logger.error(msg);
      throw new IllegalStateException(msg);
    }
  }

  /**
   * Applies a client-provided function to write to the Graph,
   * wrapping it in a Graph Write Lock
   * @param graphMutator Callback function to the client's write operations
   * @param <T> the return type of the write operation
   * @return T
   */
  public <T> T writeContentToGraph(Function<Model, T> graphMutator) {
    if (this.shutdown.get()) {
      var msg = "Unable to WRITE to a shut-down Knowledge Graph";
      logger.error(msg);
      throw new IllegalStateException(msg);
    }
    var kg = getModel();
    kg.enterCriticalSection(Lock.WRITE);
    try {
      return graphMutator.apply(kg);
    } finally {
      logger.info("SCHEDULE persistence of the Knowledge Graph");
      saver.scheduleExecution();
      kg.leaveCriticalSection();
    }
  }

  /**
   * Applies a client-provided function to read to the Graph,
   * wrapping it in a Graph Read Lock
   * @param graphReader Callback function to the client's read operations
   * @param <T> the return type of the read operation
   * @return T
   */
  public <T> T readGraphContent(Function<Model, T> graphReader) {
    var kg = getModel();
    kg.enterCriticalSection(Lock.READ);
    try {
      return graphReader.apply(kg);
    } finally {
      kg.leaveCriticalSection();
    }
  }

  /**
   * Applies a client-provided function to read to the Graph,
   * wrapping it in a Graph Read Lock
   * @param graphReader Callback function to the client's read operations
   */
  public void processGraphContent(Consumer<Model> graphReader) {
    var kg = getModel();
    kg.enterCriticalSection(Lock.READ);
    try {
      graphReader.accept(kg);
    } finally {
      kg.leaveCriticalSection();
    }
  }

  /*
   ********************************************************************************
   */

  /**
   * Returns the Jena native Graph, ensuring that it is loaded
   * @return the Jena Knowledge Graph
   */
  protected Model getModel() {
    if (graphClosed.isDone()) {
      throw new IllegalStateException("Access Denied: Graph is closed");
    }
    graphLoaded.executeIfNotDone();
    return knowledgeGraph;
  }

  /**
   * Ensures that the Graph is loaded, creating a new one if
   * the Graph is not present in the underlying Artifact Repository
   * (usually because of a new/clean DB)
   * @return true if the Graph is already loaded, or has been successfully loaded; false otherwise
   */
  protected boolean ensureGraphLoaded() {
    Answer<KnowledgeCarrier> existing = reloadGraph();
    if (existing.isSuccess()) {
      return true;
    }
    Answer<Void> brandNew = reinitialize();
    return brandNew.isSuccess()
        && retrieveEncodedGraphFromArtifactRepository().isSuccess();
  }

  /**
   * Creates a new empty Graph:
   * - creates a new Jena Model
   * - adds the T-box system triples
   * - wraps it in a KnowledgeCarrier
   *
   * @return a Knowledge Carrier wrapping the newly created Graph
   */
  protected KnowledgeCarrier newEmptyGraph() {
    var kgModel = ModelFactory.createDefaultModel();
    kgModel.add(getTBoxTriples());
    return wrapGraph(kgModel);
  }

  /**
   * Resets the Knowledge Graph, creating a new empty graph,
   * and persisting it in the Artifact Repository
   * @return an Answer describing the outcome of the persistence operations
   */
  protected Answer<Void> reinitialize() {
    KnowledgeCarrier newGraph = newEmptyGraph();
    initializeKnowledgeResources(newGraph);
    Answer<Void> a1 = persistEncodedSurrogateIntoArtifactRepository();
    Answer<Void> a2 = persistEncodedGraphIntoArtifactRepository();
    return Answer.merge(a1, a2);
  }

  /**
   * Reloads the Knowledge Graph from the Artifact repository, if present
   * @return the Graph wrapped in a KnowledgeCarrier if successful,
   * 'not found' or a more appropriate error otherwise
   * (as determined by the call to the Artifact repository API)
   */
  protected Answer<KnowledgeCarrier> reloadGraph() {
    Answer<KnowledgeCarrier> graphCarrier =
        retrieveEncodedGraphFromArtifactRepository()
            .map(this::wrapBinary)
            .flatMap(
                bin -> parser.applyLift(bin, Abstract_Knowledge_Expression, codedRep(OWL_2), null));
    if (graphCarrier.isSuccess()) {
      initializeKnowledgeResources(graphCarrier.get());
    }
    return graphCarrier;
  }

  /**
   * Reads the encoded graph from the Artifact Repository
   * @return a Knowledge Carrier at the Encoded expression Level, wrapping the Graph
   */
  protected Answer<byte[]> retrieveEncodedGraphFromArtifactRepository() {
    logger.info("Load Knowledge Graph from the Artifact Repository");
    return artifactApi.getKnowledgeArtifactVersion(
        defaultRepositoryId,
        kgi.knowledgeGraphArtifactId().getUuid(),
        kgi.knowledgeGraphArtifactId().getVersionTag());
  }

  /**
   * Serializes the Knowledge Graph, lowering from AST to Encoded level
   * @return the binary serialization+encoding of the Knowledge Graph
   */
  protected Answer<byte[]> encodeGraph() {
    return parser.applyLower(
        kBase.getManifestation(), Encoded_Knowledge_Expression,
        codedRep(OWL_2, XML_1_1, defaultCharset(), DEFAULT), null)
        .flatOpt(AbstractCarrier::asBinary);
  }

  /**
   * Serializes the Knowledge Graph Surrogate, lowering from AST to Encoded level
   * @return the binary serialization+encoding of the Graph's canonical Knowledge Asset
   */
  protected Answer<byte[]> encodeGraphSurrogate() {
    return surrogateParser.applyLower(
        AbstractCarrier.ofAst(kgi.getKnowledgeGraphSurrogate())
            .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0))
            .withAssetId(kgi.knowledgeGraphAssetId())
            .withArtifactId(kgi.knowledgeGraphSurrogateId()),
        Encoded_Knowledge_Expression,
        codedRep(Knowledge_Asset_Surrogate_2_0, JSON, defaultCharset(), DEFAULT), null)
        .flatOpt(AbstractCarrier::asBinary);
  }

  /**
   * Persists the Knowledge Surrogate in the Artifact Repository
   * @return the result of the setKnowledgeArtifactVersion operation
   */
  protected Answer<Void> persistEncodedSurrogateIntoArtifactRepository() {
    logger.info("Writing Knowledge Graph Metadata to the Artifact Repository");
    return encodeGraphSurrogate()
        .flatMap(binary ->
            artifactApi.setKnowledgeArtifactVersion(
                this.defaultRepositoryId,
                kgi.knowledgeGraphSurrogateId().getUuid(),
                kgi.knowledgeGraphSurrogateId().getVersionTag(),
                binary));
  }

  /**
   * Persists the Knowledge Graph in the Artifact Repository
   * The operation is 'Forbidden' if the Graph has been shut down
   * @return the result of the setKnowledgeArtifactVersion operation
   */
  protected synchronized Answer<Void> persistEncodedGraphIntoArtifactRepository() {
    logger.info("Writing Knowledge Graph {} to the Artifact Repository",
        kgi.getKnowledgeGraphLabel());
    if (shutdown.get()) {
      return Answer.failed(Forbidden);
    }
    return encodeGraph()
        .flatMap(binary -> artifactApi.setKnowledgeArtifactVersion(
            this.defaultRepositoryId,
            kgi.knowledgeGraphArtifactId().getUuid(),
            kgi.knowledgeGraphArtifactId().getVersionTag(),
            binary));
  }

  /*
   ********************************************************************************
   */

  /**
   * @return the Knowledge Graph wrapped in a Knowledge Carrier
   */
  public KnowledgeCarrier getKnowledgeGraph() {
    return getKnowledgeBase().getManifestation();
  }

  /**
   * @return the Knowledge Graph as a Knowledge Base
   */
  public KnowledgeBase getKnowledgeBase() {
    graphLoaded.executeIfNotDone();
    return kBase;
  }

  /**
   * Implementation of the API4KP getKnowledgeBase operation
   * @param kbaseId the ID of the requested KB, which should match the ID of the Graph's KB
   * @param versionTag the version tag (ignored, should match 0.0.0)
   * @param params additional configuration (not used)
   * @return the Knowledge Graph, as Knowledge Base
   */
  @Override
  public Answer<KnowledgeBase> getKnowledgeBase(UUID kbaseId, String versionTag, String params) {
    return kBase != null && kgi.graphKnowledgeBaseId().getUuid().equals(kbaseId)
        ? Answer.of(getKnowledgeBase())
        : Answer.notFound();
  }

  /**
   * (Re)initializes the internal data structures on a successful reinitialization of the Graph
   * @param graph the Knowledge Graph data, wrapped in a {@link KnowledgeCarrier}
   */
  protected void initializeKnowledgeResources(KnowledgeCarrier graph) {
    this.knowledgeGraph = graph.as(Model.class).orElse(null);

    this.kBase = new KnowledgeBase()
        .withKbaseId(kgi.graphKnowledgeBaseId().toPointer())
        .withManifestation(graph);
  }

  /**
   * Wraps a binary Graph in a KnowledgeCarrier
   * @param bytes the binary encoding of a Knowledge Graph
   * @return the Graph in a {@link KnowledgeCarrier}
   */
  protected KnowledgeCarrier wrapBinary(byte[] bytes) {
    return AbstractCarrier.of(bytes)
        .withAssetId(kgi.knowledgeGraphAssetId())
        .withArtifactId(kgi.knowledgeGraphArtifactId())
        .withRepresentation(KnowledgeGraphInfo.graphCodedRepresentation)
        .withLabel(kgi.getKnowledgeGraphLabel());
  }

  /**
   * Wraps a Graph AST in a KnowledgeCarrier
   * @param knowledgeGraph the Abstract_Expression (AST) representation of a Knowledge Graph
   * @return the Graph in a {@link KnowledgeCarrier}
   */
  protected KnowledgeCarrier wrapGraph(Model knowledgeGraph) {
    return AbstractCarrier.ofAst(knowledgeGraph)
        .withAssetId(kgi.knowledgeGraphAssetId())
        .withArtifactId(kgi.knowledgeGraphArtifactId())
        .withRepresentation(KnowledgeGraphInfo.graphAbstractRepresentation)
        .withLabel(kgi.getKnowledgeGraphLabel());
  }

  /**
   * Validates the ID of the Artifact Repository
   *
   * @return the id of the artifact repository
   */
  private String validateArtifactRepositoryId() {
    return repoApi.listKnowledgeArtifactRepositories()
        .filter(l -> !l.isEmpty())
        .map(l -> l.get(0))
        .map(descr -> NameUtils.getTrailingPart(descr.getId().getResourceId().toString()))
        .orElse(defaultRepositoryId);
  }


  /**
   * List of T-box Statements adding semantics to the index graph, supporting query/inference
   *
   * @return the knowledge T-box triples
   */
  public List<Statement> getTBoxTriples() {
    List<Statement> statements = new ArrayList<>();
    addKnowledgeAssetTriples(KnowledgeAssetTypeSeries.values(), statements);
    addKnowledgeAssetTriples(ClinicalKnowledgeAssetTypeSeries.values(), statements);
    addRelationshipTriples(DependencyTypeSeries.values(), statements);
    return statements;
  }

  /**
   * Creates graph T-box triples from a controlled terminology, adding them to an accumulator
   * @param values the Terms (denoting types) in the terminology
   * @param statements the list of statements to be augmented
   */
  protected void addKnowledgeAssetTriples(ConceptTerm[] values, List<Statement> statements) {
    Arrays.stream(values)
        .forEach(ax -> {
          statements.add(createStatement(
              createResource(ax.getReferentId().toString()),
              RDFS.subClassOf,
              createResource(ASSET_URI.toString())));
          addHierarchy(ax, RDFS.subClassOf, statements);
        });
  }

  /**
   * Creates graph R-box triples from a controlled terminology, adding them to an accumulator
   * @param values the Terms (denoting relationships) in the terminology
   * @param statements the list of statements to be augmented
   */
  protected void addRelationshipTriples(ConceptTerm[] values, List<Statement> statements) {
    Arrays.stream(values)
        .forEach(ax ->
            addHierarchy(ax, RDFS.subPropertyOf, statements));
  }

  /**
   * Creates graph T-box 'isA' triples from a controlled terminology, adding them to an accumulator
   * For a given child concept, determines the ancestors and asserts a given parent/child relation
   * @param ax the child term
   * @param type the parent/child relationship to be asserted
   * @param statements the list of statements to be augmented
   */
  protected void addHierarchy(ConceptTerm ax, Property type, List<Statement> statements) {
    Arrays.stream(ax.getAncestors()).forEach(parent ->
        statements.add(
            createStatement(createResource(ax.getReferentId().toString()),
                type,
                createResource(parent.getReferentId().toString()))));
  }


  /**
   * @return the Graph as a 'raw' Jena Model
   * (used for testing only)
   */
  public Model testGetModel() {
    return knowledgeGraph;
  }

}
