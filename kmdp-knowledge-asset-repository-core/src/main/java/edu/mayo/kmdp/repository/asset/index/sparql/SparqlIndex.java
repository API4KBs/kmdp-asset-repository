package edu.mayo.kmdp.repository.asset.index.sparql;

import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Depends_On;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Imports;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Includes_By_Reference;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.mayo.kmdp.repository.asset.index.Index;
import edu.mayo.kmdp.util.DateTimeUtil;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeBase;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.Derivative;
import org.omg.spec.api4kp._20200801.surrogate.Link;
import org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRole;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRoleSeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetType;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.terms.ConceptTerm;
import org.omg.spec.api4kp._20200801.terms.model.ConceptDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * An implementation of the Asset {@link Index} interface that uses an RDF/SPARQL backend.
 */
@Component
public class SparqlIndex implements Index {

  private static final Set<DependencyTypeSeries> TRAVERSE_DEPS =
      Util.newEnumSet(Arrays.asList(Imports, Includes_By_Reference, Depends_On), DependencyTypeSeries.class);

  private static final String TRAVERSE_DEPS_SPARQL;


  // compute the SPARQL query string for all related predicates
  static {
    TRAVERSE_DEPS_SPARQL =
        "(" + TRAVERSE_DEPS.stream().map(c -> "<" + c.getReferentId().toString() + ">")
            .collect(Collectors.joining("|")) + ")";
  }

  //TODO: Change this to the official URIs as published in the ontology.
  // All below are likely wrong until changed.
  private static String LCC = "https://www.omg.org/spec/LCC/Languages/LanguageRepresentation/";
  private static String API4KP = "https://www.omg.org/spec/API4KP/api4kp/";
  private static String API4KP_SERIES = "https://www.omg.org/spec/API4KP/api4kp-series/";
  private static String KMD = "http://ontology.mayo.edu/ontologies/kmdp/";


  private static final String ASSET = "KnowledgeAsset";
  private static final URI ASSET_URI = URI.create(API4KP + ASSET);

  private static final String IDENTIFIED_BY = "identifiedBy";
  private static final URI IDENTIFIED_BY_URI = URI.create(LCC + IDENTIFIED_BY);
  private static final String HAS_TAG = "hasTag";
  private static final URI HAS_TAG_URI = URI.create(LCC + HAS_TAG);
  // (Asset|Artifact) lcc:identified_by o lcc:hasTag
  private static final String TAG_ID = "tag";
  private static final URI TAG_ID_URI = URI.create(KMD + TAG_ID);

  private static final String HAS_VERSION = "hasVersion";
  private static final URI HAS_VERSION_URI = URI.create(API4KP_SERIES + HAS_VERSION);
  // (Version) lcc:identified_by o lcc:hasTag
  private static final String HAS_VERSION_TAG = "hasVersionTag";
  private static final URI HAS_VERSION_TAG_URI = URI.create(KMD + HAS_VERSION_TAG);
  private static final String ESTABLISHED = "hasObservedDateTime";
  private static final URI ESTABLISHED_URI = URI.create(API4KP_SERIES + ESTABLISHED);

  private static final String HAS_CARRIER = "isCarriedBy";
  private static final URI HAS_CARRIER_URI = URI.create(API4KP + HAS_CARRIER);
  private static final String HAS_SURROGATE = "hasAssetSurrogate";
  private static final URI HAS_SURROGATE_URI = URI.create(API4KP + HAS_SURROGATE);
  // subProperty of hasAssetSurrogate
  private static final String HAS_CANONICAL_SURROGATE = "hasCanonicalSurrogate";
  private static final URI HAS_CANONICAL_SURROGATE_URI = URI.create(KMD + HAS_CANONICAL_SURROGATE);

  @Autowired
  protected JenaSparqlDao jenaSparqlDao;

  public SparqlIndex(JenaSparqlDao jenaSparqlDao) {
    this.jenaSparqlDao = jenaSparqlDao;
  }


  /**
   * Deconstruct an Asset into RDF statements.
   *
   * @param asset
   * @param surrogate
   * @param types
   * @param roles
   * @param annotations
   * @return
   */
  public List<Statement> toRdf(ResourceIdentifier asset, String assetName,
      ResourceIdentifier surrogate,
      List<KnowledgeAssetType> types, List<KnowledgeAssetRole> roles, List<Annotation> annotations,
      List<Link> related) {
    List<Statement> statements = Lists.newArrayList();

    URI assetId = asset.getResourceId();
    URI assetVersionId = asset.getVersionId();
    URI surrogateId = surrogate.getResourceId();
    URI surrogateVersionId = surrogate.getVersionId();

    // annotations
    statements.addAll(annotations.stream().map(
        annotation -> this.toStatement(
            assetVersionId,
            annotation.getRel().getReferentId(),
            annotation.getRef().getEvokes())
    ).collect(Collectors.toList()));

    // related
    statements.addAll(related.stream()
        .flatMap(StreamUtil.filterAs(Dependency.class))
        .flatMap(dependency -> toDependencyStatements(assetVersionId,dependency))
        .collect(Collectors.toList()));

    // composites
    statements.addAll(related.stream()
        .flatMap(StreamUtil.filterAs(org.omg.spec.api4kp._20200801.surrogate.Component.class))
        .flatMap(part -> toParthoodStatements(assetVersionId,part))
        .collect(Collectors.toList()));

    // derivation
    statements.addAll(related.stream()
        .flatMap(StreamUtil.filterAs(Derivative.class))
        .flatMap(derivative -> toDerivationStatements(assetVersionId,derivative))
        .collect(Collectors.toList()));

    // type of Asset
    statements
        .add(this.toStatement(
            assetVersionId,
            URI.create(RDF.type.getURI()),
            ASSET_URI));

    // version
    statements.add(this.toStringValueStatement(
        assetId,
        TAG_ID_URI,
        asset.getTag()));
    statements.add(this.toStatement(
        assetId,
        HAS_VERSION_URI,
        assetVersionId));
    statements.add(this.toStringValueStatement(
        assetVersionId,
        HAS_VERSION_TAG_URI,
        asset.getVersionTag()));
    statements.add(this.toLongValueStatement(
        assetVersionId,
        ESTABLISHED_URI,
        asset.getEstablishedOn().toInstant().toEpochMilli()));

    // Surrogate link
    statements.add(this.toStatement(
        assetVersionId,
        HAS_CANONICAL_SURROGATE_URI,
        surrogateId));
    statements.add(this.toStatement(
        assetVersionId,
        HAS_SURROGATE_URI,
        surrogateId));
    statements.add(this.toStatement(
        surrogateId,
        HAS_VERSION_URI,
        surrogateVersionId));
    statements.add(this.toStringValueStatement(
        surrogateId,
        TAG_ID_URI,
        surrogate.getUuid().toString()));
    statements.add(this.toStringValueStatement(
        surrogateVersionId,
        HAS_VERSION_TAG_URI,
        surrogate.getVersionTag()));
    statements.add(this.toLongValueStatement(
        surrogateVersionId,
        ESTABLISHED_URI,
        surrogate.getEstablishedOn().toInstant().toEpochMilli()));

    // Asset types
    statements.addAll(types.stream().map(type ->
        this.toStatement(
            assetVersionId,
            URI.create(RDF.type.getURI()),
            type.getReferentId())
    ).collect(Collectors.toList()));

    // Asset roles
    statements.addAll(roles.stream().map(role ->
        this.toStatement(
            assetVersionId,
            URI.create(RDF.type.getURI()),
            role.getReferentId())
    ).collect(Collectors.toList()));

    // Asset name
    if (!Util.isEmpty(assetName)) {
      statements.add(
          toStringValueStatement(assetVersionId, URI.create(RDFS.label.getURI()), assetName));
    }

    return statements;
  }

  private Stream<Statement> toDependencyStatements(URI subj, Dependency dependency) {
    ConceptDescriptor dependencyType = ConceptDescriptor.toConceptDescriptor(dependency.getRel());
    URI tgt = dependency.getHref().getVersionId();

    return toRelatedStatements(subj,dependencyType,tgt);
  }

  private Stream<Statement> toDerivationStatements(URI subj, Derivative derivative) {
    ConceptDescriptor derivationType = ConceptDescriptor.toConceptDescriptor(derivative.getRel());
    URI tgt = derivative.getHref().getVersionId();

    return toRelatedStatements(subj,derivationType,tgt);
  }

  private Stream<Statement> toParthoodStatements(URI subj, org.omg.spec.api4kp._20200801.surrogate.Component part) {
    ConceptDescriptor partType = ConceptDescriptor.toConceptDescriptor(part.getRel());
    URI tgt = part.getHref().getVersionId();

    return toRelatedStatements(subj,partType,tgt);
  }

  private Stream<Statement> toRelatedStatements(URI subj, ConceptDescriptor rel, URI tgt) {
    return Stream.concat(
        Arrays.stream(rel.getClosure())
            .map(anc -> toStatement(subj, anc.getReferentId(), tgt)),
        Stream.of(this.toStatement(subj, rel.getReferentId(), tgt))
    );
  }

  public Statement toStatement(URI subject, URI predicate, URI object) {
    return ResourceFactory.createStatement(
        ResourceFactory.createResource(subject.toString()),
        ResourceFactory.createProperty(predicate.toString()),
        ResourceFactory.createResource(object.toString()));
  }

  public Statement toStringValueStatement(URI subject, URI predicate, String object) {
    return ResourceFactory.createStatement(
        ResourceFactory.createResource(subject.toString()),
        ResourceFactory.createProperty(predicate.toString()),
        ResourceFactory.createStringLiteral(object));
  }

  public Statement toLongValueStatement(URI subject, URI predicate, Long object) {
    return ResourceFactory.createStatement(
        ResourceFactory.createResource(subject.toString()),
        ResourceFactory.createProperty(predicate.toString()),
        ResourceFactory.createTypedLiteral(object));
  }

  @Override
  public Set<ResourceIdentifier> getRelatedAssets(ResourceIdentifier assetPointer, URI relation) {
    String sparql = "" +
        "SELECT ?o\n" +
        "WHERE {\n" +
        "    ?s ?p* ?o\n" +
        "}";

    Map<String, URI> params = Maps.newHashMap();
    params.put("?s", assetPointer.getVersionId());
    params.put("?p", relation);

    Set<ResourceIdentifier> related = Sets.newHashSet();

    this.jenaSparqlDao.runSparql(sparql, params, Collections.emptyMap(), (
        querySolution -> related.add(
            this.resourceToResourceIdentifier(querySolution.getResource("?o")))));

    return related;
  }

  @Override
  public Set<ResourceIdentifier> getRelatedAssets(ResourceIdentifier assetPointer) {
    String sparql = "" +
        "SELECT ?o\n" +
        "WHERE {\n" +
        "    ?s " + TRAVERSE_DEPS_SPARQL + " ?o\n" +
        "}";

    Map<String, URI> params = Maps.newHashMap();
    params.put("?s", assetPointer.getVersionId());

    Set<ResourceIdentifier> related = Sets.newHashSet();

    this.jenaSparqlDao.runSparql(sparql, params, Collections.emptyMap(), (
        querySolution -> related.add(
            this.resourceToResourceIdentifier(querySolution.getResource("?o")))));

    Set<ResourceIdentifier> downstream = Sets.union(related,
        related.stream()
            .map(this::getRelatedAssets)
            .flatMap(Set::stream)
            .collect(Collectors.toSet()));

    return Sets.union(Sets.newHashSet(assetPointer), downstream);
  }

  @Override
  public Set<ResourceIdentifier> getAllAssetIds() {
    return this.jenaSparqlDao
        .readSubjectByPredicateAndObject(URI.create(RDF.type.getURI()), ASSET_URI)
        .stream()
        .map(this::resourceToResourceIdentifier)
        .collect(Collectors.toSet());
  }

  @Override
  public void registerAsset(ResourceIdentifier asset, String assetName,
      ResourceIdentifier surrogate,
      List<KnowledgeAssetType> types, List<KnowledgeAssetRole> roles, List<Annotation> annotations,
      List<Link> related) {
    this.jenaSparqlDao
        .store(this.toRdf(asset, assetName, surrogate, types, roles, annotations, related));
  }

  @Override
  public void registerArtifactToAsset(ResourceIdentifier assetPointer,
      ResourceIdentifier artifact) {
    List<Statement> statements = Arrays.asList(
        toStatement(assetPointer.getVersionId(), HAS_CARRIER_URI, artifact.getResourceId()),
        toStatement(artifact.getResourceId(), HAS_VERSION_URI, artifact.getVersionId()),
        toStringValueStatement(artifact.getResourceId(), TAG_ID_URI, artifact.getUuid().toString()),
        toStringValueStatement(artifact.getVersionId(), HAS_VERSION_TAG_URI, artifact.getVersionTag()),
        toLongValueStatement(artifact.getVersionId(),
            ESTABLISHED_URI, artifact.getEstablishedOn().toInstant().toEpochMilli())
        );
    this.jenaSparqlDao.store(statements);
  }

  @Override
  public void registerSurrogateToAsset(ResourceIdentifier assetPointer,
      ResourceIdentifier surrogate) {
    List<Statement> statements = Arrays.asList(
        toStatement(assetPointer.getVersionId(), HAS_SURROGATE_URI, surrogate.getResourceId()),
        toStatement(surrogate.getResourceId(), HAS_VERSION_URI, surrogate.getVersionId()),
        toStringValueStatement(surrogate.getResourceId(), TAG_ID_URI, surrogate.getUuid().toString()),
        toStringValueStatement(surrogate.getVersionId(), HAS_VERSION_TAG_URI, surrogate.getVersionTag()),
        toLongValueStatement(surrogate.getVersionId(),
            ESTABLISHED_URI, surrogate.getEstablishedOn().toInstant().toEpochMilli())
    );
    this.jenaSparqlDao.store(statements);
  }

  @Override
  public Optional<ResourceIdentifier> getCanonicalSurrogateForAsset(
      ResourceIdentifier assetPointer) {
    return this.jenaSparqlDao
        .readObjectBySubjectAndPredicate(assetPointer.getVersionId(), HAS_CANONICAL_SURROGATE_URI)
        .stream()
        .map(this::resourceToResourceIdentifier)
        .findFirst();
  }

  @Override
  public URI getLocation(ResourceIdentifier pointer) {
    if (pointer instanceof Pointer) {
      return ((Pointer) pointer).getHref();
    }
    return pointer.getVersionId();
  }

  /**
   * Returns true if the given UUID is associated to an Asset
   *
   * @param assetId The identifier of the asset
   * @return true if the URI maps to an Asset
   */
  @Override
  public boolean isKnownAsset(ResourceIdentifier assetId) {
    return jenaSparqlDao.checkStatementExists(
        toStringValueStatement(
            assetId.getResourceId(),
            TAG_ID_URI,
            assetId.getUuid().toString()));
  }

  @Override
  public Set<ResourceIdentifier> getAssetIdsByType(URI assetType) {
    List<Resource> resources =
        this.jenaSparqlDao
            .readSubjectByPredicateAndObject(URI.create(RDF.type.getURI()), assetType);

    return resources.stream()
        .map(this::resourceToResourceIdentifier)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<ResourceIdentifier> getAssetIdsByAnnotation(URI annotation, URI value) {
    List<Resource> resources =
        this.jenaSparqlDao.readSubjectByPredicateAndObject(annotation, value);

    return resources.stream()
        .map(this::resourceToResourceIdentifier)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<ResourceIdentifier> getAssetIdsByAnnotation(URI annotation) {
    List<Resource> resources =
        this.jenaSparqlDao.readSubjectByPredicate(annotation);

    return resources.stream()
        .map(this::resourceToResourceIdentifier)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<ResourceIdentifier> getAssetIdsByAnnotationValue(URI annotation) {
    List<Resource> resources =
        this.jenaSparqlDao.readSubjectByObject(annotation);

    return resources.stream()
        .map(this::resourceToResourceIdentifier)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<ResourceIdentifier> getArtifactsForAsset(ResourceIdentifier assetId) {
    List<Resource> resources =
        this.jenaSparqlDao
            .readObjectBySubjectAndPredicate(assetId.getVersionId(), HAS_CARRIER_URI);

    return resources.stream()
        .map(this::resourceToResourceIdentifier)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<ResourceIdentifier> getSurrogatesForAsset(ResourceIdentifier assetId) {
    List<Resource> resources =
        this.jenaSparqlDao
            .readObjectBySubjectAndPredicate(assetId.getVersionId(), HAS_SURROGATE_URI);

    return resources.stream()
        .map(this::resourceToResourceIdentifier)
        .collect(Collectors.toSet());
  }

  /**
   * Retrieves the name of an Asset
   *
   * @param assetId
   * @return
   */
  @Override
  public Optional<String> getAssetName(ResourceIdentifier assetId) {
    List<Literal> labels = this.jenaSparqlDao.readValueBySubjectAndPredicate(
        assetId.getVersionId(), URI.create(RDFS.label.getURI()));

    return labels.isEmpty()
        ? Optional.empty()
        : Optional.of(labels.get(0).getString());
  }

  /**
   * Retrieves the types and roles of an Asset
   *
   * @param assetId
   * @return
   */
  @Override
  public List<ConceptIdentifier> getAssetTypes(ResourceIdentifier assetId) {
    List<Resource> types = this.jenaSparqlDao.readObjectBySubjectAndPredicate(
        assetId.getVersionId(), URI.create(RDF.type.getURI()));

    List<ConceptIdentifier> typeConcepts = new ArrayList<>();

    types.stream()
        .map(type -> URI.create(type.getURI()))
        .map(KnowledgeAssetTypeSeries::resolveRef)
        .flatMap(StreamUtil::trimStream)
        .map(ConceptTerm::asConceptIdentifier)
        .forEach(typeConcepts::add);

    types.stream()
        .map(type -> URI.create(type.getURI()))
        .map(KnowledgeAssetRoleSeries::resolveRef)
        .flatMap(StreamUtil::trimStream)
        .map(ConceptTerm::asConceptIdentifier)
        .forEach(typeConcepts::add);

    return typeConcepts;
  }

  /**
   * Returns the known Versions of a given KnowledgeAsset, sorted by (asset) version Tag and,
   * subsequently, by timestamp
   *
   * @param assetSeriesId the UUID of the Asset series, common to all versions
   */
  @Override
  public List<ResourceIdentifier> getAssetVersions(UUID assetSeriesId) {
    String sparql = ""
        + " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
        + " PREFIX lcc: <https://www.omg.org/spec/LCC/Languages/LanguageRepresentation/> \n"
        + " PREFIX api4kp: <https://www.omg.org/spec/API4KP/api4kp/> \n"
        + " PREFIX api4kp-series: <https://www.omg.org/spec/API4KP/api4kp-series/> \n"
        + " PREFIX kmd: <http://ontology.mayo.edu/ontologies/kmdp/> \n"

        + "SELECT ?asset ?version ?vTag ?vTimestamp \n"
        + "WHERE { \n"
        + "  ?asset kmd:" + TAG_ID + " ?tag ; \n"
        + "     api4kp-series:" + HAS_VERSION + " ?version . \n"
        + "  ?version rdf:type api4kp:" + ASSET + " ; \n"
        + "     kmd:" + HAS_VERSION_TAG + " ?vTag ; \n"
        + "     api4kp-series:" + ESTABLISHED + " ?vTimestamp . \n"
        + "} \n"
        + "ORDER BY DESC(?vTimestamp)";

    List<ResourceIdentifier> versions = new ArrayList<>();
    Map<String, Literal> literalParams = new HashMap<>();
    literalParams.put("?tag",
        ResourceFactory.createPlainLiteral(assetSeriesId.toString()));

    this.jenaSparqlDao.runSparql(sparql,
        Collections.emptyMap(),
        literalParams, (
            querySolution -> versions.add(
                this.versionInfoToIdentifier(
                    querySolution.getResource("?asset"),
                    querySolution.getResource("?version"),
                    querySolution.getLiteral("?vTag"),
                    querySolution.getLiteral("?vTimestamp")))));
    return versions;
  }


  /**
   * Returns the known Versions of a Canonical Surrogate sorted by timestamp
   *
   * @param surrogateSeriesId the identifier of a Surrogate series
   */
  @Override
  public List<ResourceIdentifier> getSurrogateVersions(UUID surrogateSeriesId) {
    String sparql = ""
        + " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
        + " PREFIX lcc: <https://www.omg.org/spec/LCC/Languages/LanguageRepresentation/> \n"
        + " PREFIX api4kp: <https://www.omg.org/spec/API4KP/api4kp/> \n"
        + " PREFIX api4kp-series: <https://www.omg.org/spec/API4KP/api4kp-series/> \n"
        + " PREFIX kmd: <http://ontology.mayo.edu/ontologies/kmdp/> \n"

        + "SELECT ?surrogate ?version ?vTag ?vTimestamp \n"
        + "WHERE { \n"
        + "  ?asset api4kp:" + HAS_SURROGATE + " ?surrogate . \n"
        + "  ?surrogate kmd:" + TAG_ID + " ?tag ; \n"
        + "     api4kp-series:" + HAS_VERSION + " ?version . \n"
        + "  ?version  \n"
        + "     kmd:" + HAS_VERSION_TAG + " ?vTag ; \n"
        + "     api4kp-series:" + ESTABLISHED + " ?vTimestamp . \n"
        + "} \n"
        + "ORDER BY DESC(?vTimestamp)";

    List<ResourceIdentifier> versions = new ArrayList<>();
    Map<String, Literal> literalParams = new HashMap<>();
    literalParams.put("?tag",
        ResourceFactory.createPlainLiteral(surrogateSeriesId.toString()));

    this.jenaSparqlDao.runSparql(sparql,
        Collections.emptyMap(),
        literalParams, (
            querySolution -> versions.add(
                this.versionInfoToIdentifier(
                    querySolution.getResource("?surrogate"),
                    querySolution.getResource("?version"),
                    querySolution.getLiteral("?vTag"),
                    querySolution.getLiteral("?vTimestamp")))));
    return versions;
  }



  /**
   * Returns the known Versions of a Knowledge Artifact sorted by timestamp
   *
   * @param carrierSeriesId the identifier of a Surrogate series
   */
  @Override
  public List<ResourceIdentifier> getCarrierVersions(UUID carrierSeriesId) {
    String sparql = ""
        + " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
        + " PREFIX lcc: <https://www.omg.org/spec/LCC/Languages/LanguageRepresentation/> \n"
        + " PREFIX api4kp: <https://www.omg.org/spec/API4KP/api4kp/> \n"
        + " PREFIX api4kp-series: <https://www.omg.org/spec/API4KP/api4kp-series/> \n"
        + " PREFIX kmd: <http://ontology.mayo.edu/ontologies/kmdp/> \n"

        + "SELECT ?carrier ?version ?vTag ?vTimestamp \n"
        + "WHERE { \n"
        + "  ?asset api4kp:" + HAS_CARRIER + " ?carrier . \n"
        + "  ?carrier kmd:" + TAG_ID + " ?tag ; \n"
        + "     api4kp-series:" + HAS_VERSION + " ?version . \n"
        + "  ?version  \n"
        + "     kmd:" + HAS_VERSION_TAG + " ?vTag ; \n"
        + "     api4kp-series:" + ESTABLISHED + " ?vTimestamp . \n"
        + "} \n"
        + "ORDER BY DESC(?vTimestamp)";

    List<ResourceIdentifier> versions = new ArrayList<>();
    Map<String, Literal> literalParams = new HashMap<>();
    literalParams.put("?tag",
        ResourceFactory.createPlainLiteral(carrierSeriesId.toString()));

    this.jenaSparqlDao.runSparql(sparql,
        Collections.emptyMap(),
        literalParams, (
            querySolution -> versions.add(
                this.versionInfoToIdentifier(
                    querySolution.getResource("?carrier"),
                    querySolution.getResource("?version"),
                    querySolution.getLiteral("?vTag"),
                    querySolution.getLiteral("?vTimestamp")))));
    return versions;
  }

  @Override
  public void reset() {
    this.jenaSparqlDao.truncate();
  }

  /**
   * Returns the ID of the KnowledgeGraph underlying this index
   *
   * @return
   */
  @Override
  public Pointer getKnowledgeBaseId() {
    return jenaSparqlDao.getKnowledgeBase().getKbaseId();
  }

  /**
   * Returns the KnowledgeGraph underlying this index
   *
   * @return
   */
  @Override
  public KnowledgeBase asKnowledgeBase() {
    return jenaSparqlDao.getKnowledgeBase();
  }


  protected ResourceIdentifier resourceToResourceIdentifier(Resource resource) {
    return SemanticIdentifier
        .newVersionId(URI.create(resource.getURI()));
  }

  protected ResourceIdentifier versionInfoToIdentifier(
      Resource resourceId, Resource resourceVersionId,
      Literal versionTag, Literal versionTimestamp) {
    ResourceIdentifier rid = SemanticIdentifier
        .newVersionId(URI.create(resourceVersionId.getURI()));
    if (!rid.getResourceId().equals(URI.create(resourceId.getURI()))) {
      throw new IllegalStateException("Mismatch between entity " + resourceId.getURI()
          + "  and its version " + resourceVersionId.getURI());
    }
    if (!rid.getVersionTag().equals(versionTag.getString())) {
      throw new IllegalStateException(
          "Unable to reconstruct indexed version Tag: found " + rid.getVersionTag()
              + " expected " + versionTag.getString());
    }
    rid.setEstablishedOn(DateTimeUtil.fromEpochTimestamp(versionTimestamp.getLong()));
    return rid;
  }

}
