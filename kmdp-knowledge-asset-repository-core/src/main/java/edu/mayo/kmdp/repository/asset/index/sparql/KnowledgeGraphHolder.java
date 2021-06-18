package edu.mayo.kmdp.repository.asset.index.sparql;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.rdf.model.ResourceFactory.createStatement;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.IdentifierConstants.VERSION_ZERO;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newIdAsPointer;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultArtifactId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultSurrogateId;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Terminology_Ontology_And_Assertional_KBs;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Assertional_Knowledge;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.rdf.JenaRdfParser;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryService;
import edu.mayo.kmdp.util.NameUtils;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.apache.jena.graph.GraphEvents;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDFS;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.artifact.v4.server.KnowledgeArtifactApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.artifact.v4.server.KnowledgeArtifactRepositoryApiInternal;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeBase;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder;
import org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.terms.ConceptTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeGraphHolder implements KnowledgeBaseApiInternal._getKnowledgeBase {

  private static final Logger logger = LoggerFactory.getLogger(KnowledgeGraphHolder.class);

  public static final String API4KP = "https://www.omg.org/spec/API4KP/api4kp/";
  public static final String ASSET = "KnowledgeAsset";
  public static final URI ASSET_URI = URI.create(API4KP + ASSET);

  private static final String LABEL = "Knowledge Asset Repository Knowledge Graph";

  private static final SyntacticRepresentation encodedRep =
      rep(OWL_2, XML_1_1, defaultCharset(), Encodings.DEFAULT);
  private static final JenaRdfParser parser = new JenaRdfParser();

  @Autowired
  private KnowledgeArtifactApiInternal artifactApi;
  @Autowired
  private KnowledgeArtifactRepositoryApiInternal repoApi;
  @Value("${edu.mayo.kmdp.repository.artifact.identifier:default}")
  private String defaultRepositoryId = "default";

  @Value("${edu.mayo.kmdp.repository.graph.identifier:4bea6c68-25a8-4c9b-9b5e-41b4cd1fe29b}")
  private final UUID graphUUID = UUID.fromString("4bea6c68-25a8-4c9b-9b5e-41b4cd1fe29b");
  private ResourceIdentifier graphAssetId;
  private ResourceIdentifier graphArtifactId;
  private ResourceIdentifier graphSurrogateId;

  private Model knowledgeGraph;

  private KnowledgeBase kBase;
  private KnowledgeAsset surrogate;

  public KnowledgeGraphHolder() {
    // default constructor for auto-wiring
  }

  public KnowledgeGraphHolder(
      KnowledgeArtifactRepositoryService artifactRepo) {
    this.repoApi = artifactRepo;
    this.artifactApi = artifactRepo;
    init();
  }

  @PostConstruct
  private void init() {
    this.defaultRepositoryId = validateArtifactRepositoryId();

    this.graphAssetId = newId(graphUUID, VERSION_ZERO);
    this.graphArtifactId = defaultArtifactId(graphAssetId, OWL_2);
    this.graphSurrogateId = defaultSurrogateId(graphAssetId, Knowledge_Asset_Surrogate_2_0);

    reloadGraph();
  }

  public void reloadGraph() {
    logger.info("Writing Knowledge Graph to the Artifact Repository");
    Answer<KnowledgeCarrier> graphCarrier =
        artifactApi.getKnowledgeArtifactVersion(
            defaultRepositoryId, graphArtifactId.getUuid(), graphArtifactId.getVersionTag())
            .map(this::wrapBinary)
            .flatMap(
                bin -> parser.applyLift(bin, Abstract_Knowledge_Expression, codedRep(OWL_2), null));
    if (graphCarrier.isSuccess()) {
      initializeKnowledgeResources(graphCarrier.get());
    } else {
      resetGraph();
    }
  }

  public Answer<Void> resetGraph() {
    KnowledgeCarrier newGraph = newEmptyGraph();
    initializeKnowledgeResources(newGraph);
    return persistGraph();
  }

  protected KnowledgeCarrier newEmptyGraph() {
    Model kgModel = ModelFactory.createDefaultModel();
    kgModel.notifyEvent(GraphEvents.startRead);
    try {
      kgModel.add(getTBoxTriples());
    } finally {
      kgModel.notifyEvent(GraphEvents.finishRead);
    }
    return wrapGraph(kgModel);
  }

  public Answer<Void> persistGraph() {
    return getEncodedGraph()
        .flatMap(binary ->
            artifactApi.setKnowledgeArtifactVersion(
                this.defaultRepositoryId,
                graphArtifactId.getUuid(),
                graphArtifactId.getVersionTag(),
                binary));
  }

  public Answer<byte[]> getEncodedGraph() {
    return parser.applyLower(
        kBase.getManifestation(), Encoded_Knowledge_Expression, codedRep(OWL_2), null)
        .flatOpt(AbstractCarrier::asBinary);
  }

  public Model getJenaModel() {
    return knowledgeGraph;
  }

  public ResourceIdentifier getKnowledgeBaseId() {
    return kBase.getKbaseId();
  }

  public ResourceIdentifier getKnowledgeGraphAssetId() {
    return graphAssetId;
  }

  public ResourceIdentifier getKnowledgeGraphArtifactId() {
    return graphArtifactId;
  }
  public ResourceIdentifier getKnowledgeGraphSurrogateId() {
    return graphSurrogateId;
  }

  public KnowledgeCarrier getKnowledgeGraph() {
    return getKnowledgeBase().getManifestation();
  }

  public KnowledgeAsset getSurrogate() {
    return this.surrogate;
  }

  public KnowledgeBase getKnowledgeBase() {
    return kBase;
  }

  @Override
  public Answer<KnowledgeBase> getKnowledgeBase(UUID kbaseId, String versionTag, String params) {
    return kBase != null && kBase.getKbaseId().getUuid().equals(kbaseId)
        ? Answer.of(getKnowledgeBase())
        : Answer.notFound();
  }

  public boolean isKnowledgeGraphAsset(UUID assetId) {
    return this.graphAssetId.getUuid().equals(assetId);
  }

  public boolean isGraphSurrogate(UUID surrogateId) {
    return this.graphSurrogateId.getUuid().equals(surrogateId);
  }

  public boolean isGraphCarrier(UUID carrierId) {
    return this.graphArtifactId.getUuid().equals(carrierId);
  }

  private void initializeKnowledgeResources(KnowledgeCarrier graph) {
    this.knowledgeGraph = graph.as(Model.class).orElse(null);

    this.kBase = new KnowledgeBase()
        .withManifestation(graph)
        .withKbaseId(newIdAsPointer(randomUUID(), VERSION_ZERO));

    this.surrogate = SurrogateBuilder.newSurrogate(graphAssetId)
        .withFormalType(Terminology_Ontology_And_Assertional_KBs, Assertional_Knowledge)
        .withName(LABEL, null)
        .withCarriers(graphAssetId, null)
        .withCarriers(graphArtifactId, null)
        .get();
  }

  private KnowledgeCarrier wrapBinary(byte[] bytes) {
    return AbstractCarrier.of(bytes)
        .withAssetId(graphAssetId)
        .withArtifactId(graphArtifactId)
        .withRepresentation(encodedRep)
        .withLabel(LABEL);
  }

  private KnowledgeCarrier wrapGraph(Model knowledgeGraph) {
    return AbstractCarrier.ofAst(knowledgeGraph)
        .withAssetId(graphAssetId)
        .withArtifactId(graphArtifactId)
        .withRepresentation(rep(OWL_2))
        .withLabel(LABEL);
  }

  /**
   * Validates
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

  private void addKnowledgeAssetTriples(ConceptTerm[] values, List<Statement> statements) {
    Arrays.stream(values)
        .forEach(ax -> {
          statements.add(createStatement(
              createResource(ax.getReferentId().toString()),
              RDFS.subClassOf,
              createResource(ASSET_URI.toString())));
          addHierarchy(ax, RDFS.subClassOf, statements);
        });
  }

  private void addRelationshipTriples(ConceptTerm[] values, List<Statement> statements) {
    Arrays.stream(values)
        .forEach(ax ->
            addHierarchy(ax, RDFS.subPropertyOf, statements));
  }

  private void addHierarchy(ConceptTerm ax, Property type, List<Statement> statements) {
    Arrays.stream(ax.getAncestors()).forEach(parent ->
        statements.add(
            createStatement(createResource(ax.getReferentId().toString()),
                type,
                createResource(parent.getReferentId().toString()))));
  }

}
