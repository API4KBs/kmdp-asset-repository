package edu.mayo.kmdp.repository.asset.index.sparql.impl;

import static edu.mayo.kmdp.repository.asset.index.sparql.impl.SparqlIndex.InternalQueryManager.TRANSITIVE_CLOSURE_SELECT;
import static edu.mayo.kmdp.util.JenaUtil.objA;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.jena.rdf.model.ResourceFactory.createStatement;
import static org.omg.spec.api4kp._20200801.id.IdentifierConstants.SNAPSHOT;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Depends_On;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Imports;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Includes_By_Reference;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Draft;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.mayo.kmdp.repository.asset.index.Index;
import edu.mayo.kmdp.repository.asset.index.sparql.KnowledgeGraphInfo;
import edu.mayo.kmdp.util.DateTimeUtil;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.omg.spec.api4kp._20200801.aspects.LogLevel;
import org.omg.spec.api4kp._20200801.aspects.Loggable;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.Derivative;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.Link;
import org.omg.spec.api4kp._20200801.surrogate.Publication;
import org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRole;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRoleSeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetType;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeprocessingtechnique.KnowledgeProcessingTechnique;
import org.omg.spec.api4kp._20200801.terms.ConceptTerm;
import org.omg.spec.api4kp._20200801.terms.model.ConceptDescriptor;
import org.semanticweb.owlapi.vocab.DublinCoreVocabulary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * An implementation of the Asset {@link Index} interface that uses an RDF/SPARQL backend.
 */
@Component
public class SparqlIndex implements Index {

  private static final Set<DependencyTypeSeries> TRAVERSE_DEPS =
      Util.newEnumSet(Arrays.asList(Imports, Includes_By_Reference, Depends_On),
          DependencyTypeSeries.class);

  private static final String TRAVERSE_DEPS_SPARQL;


  // compute the SPARQL query string for all related predicates
  static {
    TRAVERSE_DEPS_SPARQL =
        "(" + TRAVERSE_DEPS.stream().map(c -> "<" + c.getReferentId().toString() + ">")
            .collect(Collectors.joining("|")) + ")";
  }

  // All below are likely wrong until changed.
  public static final String LCC = "https://www.omg.org/spec/LCC/Languages/LanguageRepresentation/";
  public static final String API4KP = "https://www.omg.org/spec/API4KP/api4kp/";
  public static final String API4KP_SERIES = "https://www.omg.org/spec/API4KP/api4kp-series/";
  public static final String KMD = "http://ontology.mayo.edu/ontologies/kmdp/";
  public static final String DC = DublinCoreVocabulary.NAME_SPACE;


  public static final String ASSET = "KnowledgeAsset";
  public static final URI ASSET_URI = URI.create(API4KP + ASSET);

  public static final String IDENTIFIED_BY = "identifiedBy";
  public static final URI IDENTIFIED_BY_URI = URI.create(LCC + IDENTIFIED_BY);
  public static final String HAS_TAG = "hasTag";
  public static final URI HAS_TAG_URI = URI.create(LCC + HAS_TAG);
  // (Asset|Artifact) lcc:identified_by o lcc:hasTag
  public static final String TAG_ID = "tag";
  public static final URI TAG_ID_URI = URI.create(KMD + TAG_ID);

  public static final String HAS_VERSION = "hasVersion";
  public static final URI HAS_VERSION_URI = URI.create(API4KP_SERIES + HAS_VERSION);
  // (Version) lcc:identified_by o lcc:hasTag
  public static final String HAS_VERSION_TAG = "hasVersionTag";
  public static final URI HAS_VERSION_TAG_URI = URI.create(KMD + HAS_VERSION_TAG);
  public static final String ESTABLISHED = "hasObservedDateTime";
  public static final URI ESTABLISHED_URI = URI.create(API4KP_SERIES + ESTABLISHED);

  public static final String HAS_CARRIER = "isCarriedBy";
  public static final URI HAS_CARRIER_URI = URI.create(API4KP + HAS_CARRIER);
  public static final String HAS_SURROGATE = "hasAssetSurrogate";
  public static final URI HAS_SURROGATE_URI = URI.create(API4KP + HAS_SURROGATE);
  // subProperty of hasAssetSurrogate
  public static final String HAS_CANONICAL_SURROGATE = "hasCanonicalSurrogate";
  public static final URI HAS_CANONICAL_SURROGATE_URI = URI.create(KMD + HAS_CANONICAL_SURROGATE);

  public static final String FORMAT = DublinCoreVocabulary.FORMAT.getShortForm();
  public static final URI FORMAT_URI = URI.create(DC + FORMAT);
  public static final URI LOCATOR_URI = URI.create(API4KP + "accessURL");

  public static final String MEMBER_OF = "isMemberOf";
  public static final URI MEMBER_OF_URI = URI.create(LCC + MEMBER_OF);

  public static final String USES_METHOD = "uses-method";
  public static final URI USES_METHOD_URI = URI.create(API4KP + USES_METHOD);

  public static final String HAS_EXPRESSION = "hasExpression";
  public static final URI HAS_EXPRESSION_URI = URI.create(API4KP + HAS_EXPRESSION);


  @Autowired
  protected JenaSparqlDAO jenaSparqlDao;

  @Autowired
  protected KnowledgeGraphInfo kgi;

  public SparqlIndex() {
    // empty constructor
  }

  public static SparqlIndex newSparqlIndex(JenaSparqlDAO jenaSparqlDao, KnowledgeGraphInfo kgi) {
    var sparqlIndex = new SparqlIndex();
    sparqlIndex.jenaSparqlDao = jenaSparqlDao;
    sparqlIndex.kgi = kgi;
    return sparqlIndex;
  }

  @Override
  public void reset() {
    this.jenaSparqlDao.reinitialize();
  }

  @Override
  @Loggable
  public void registerAssetByCanonicalSurrogate(KnowledgeAsset assetSurrogate,
      ResourceIdentifier surrogateId, String surrogateMimeType) {
    registerAsset(
        assetSurrogate,
        surrogateId,
        surrogateMimeType);
    assetSurrogate.getCarriers()
        .forEach(ka -> registerArtifactToAsset(
            assetSurrogate.getAssetId(),
            ka,
            Util.coalesce(ModelMIMECoder.encode(ka.getRepresentation()), ka.getMimeType())));
    // exclude the canonical surrogate, which is processed by 'registerAsset'
    assetSurrogate.getSurrogate().stream()
        .filter(surr -> !surr.getArtifactId().sameAs(surrogateId))
        .forEach(surr -> registerSurrogateToAsset(
            assetSurrogate.getAssetId(),
            surr,
            ModelMIMECoder.encode(surr.getRepresentation())));
  }

  /**
   * Deconstruct an Asset into RDF statements.
   *
   * @param asset
   * @param aliases
   * @param surrogate
   * @param types
   * @param roles
   * @param annotations
   * @param lifecycle
   * @param processingMethod
   * @param memberOf
   * @return
   */
  public List<Statement> toRdf(
      ResourceIdentifier asset, List<ResourceIdentifier> aliases,
      String assetName,
      ResourceIdentifier surrogate, String surrogateMimeType,
      List<KnowledgeAssetType> types, List<KnowledgeAssetRole> roles, List<Annotation> annotations,
      List<Link> related, Publication lifecycle,
      List<KnowledgeProcessingTechnique> processingMethod, List<ResourceIdentifier> memberOf) {
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
        .flatMap(dependency -> toDependencyStatements(assetVersionId, dependency))
        .collect(Collectors.toList()));

    // composites
    statements.addAll(related.stream()
        .flatMap(StreamUtil.filterAs(org.omg.spec.api4kp._20200801.surrogate.Component.class))
        .flatMap(part -> toParthoodStatements(assetVersionId, part))
        .collect(Collectors.toList()));

    // derivation
    statements.addAll(related.stream()
        .flatMap(StreamUtil.filterAs(Derivative.class))
        .flatMap(derivative -> toDerivationStatements(assetVersionId, derivative))
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
        getEstablishedOn(lifecycle, asset)));

    // Surrogate link
    statements.add(this.toStatement(
        assetVersionId,
        HAS_CANONICAL_SURROGATE_URI,
        surrogateId));
    statements.add(this.toStatement(
        assetVersionId,
        HAS_SURROGATE_URI,
        surrogateId));
    statements.add(this.toStringValueStatement(
        surrogateId,
        FORMAT_URI,
        surrogateMimeType));
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

    // Asset techniques
    statements.addAll(processingMethod.stream().map(method ->
        this.toStatement(
            assetVersionId,
            USES_METHOD_URI,
            method.getConceptId())
    ).collect(Collectors.toList()));

    statements.addAll(memberOf.stream().map(coll ->
        this.toStatement(
            assetVersionId,
            MEMBER_OF_URI,
            coll.getResourceId())
    ).collect(Collectors.toList()));

    // Asset name
    if (!Util.isEmpty(assetName)) {
      statements.add(
          toStringValueStatement(assetVersionId, URI.create(RDFS.label.getURI()), assetName));
    }

    aliases.stream()
        .filter(alias -> alias.getVersionId() != null)
        .forEach(alias -> statements.add(
            toStatement(assetVersionId, URI.create(OWL.SAMEAS.toString()), alias.getVersionId())));

    return statements;
  }

  private Stream<Statement> toDependencyStatements(URI subj, Dependency dependency) {
    var dependencyType = ConceptDescriptor.toConceptDescriptor(dependency.getRel());
    URI tgt = dependency.getHref().getVersionId();

    return toRelatedStatements(subj, dependencyType, tgt);
  }

  private Stream<Statement> toDerivationStatements(URI subj, Derivative derivative) {
    var derivationType = ConceptDescriptor.toConceptDescriptor(derivative.getRel());
    URI tgt = derivative.getHref().getVersionId();

    return toRelatedStatements(subj, derivationType, tgt);
  }

  private Stream<Statement> toParthoodStatements(URI subj,
      org.omg.spec.api4kp._20200801.surrogate.Component part) {
    var partType = ConceptDescriptor.toConceptDescriptor(part.getRel());
    URI tgt = part.getHref().getVersionId();

    return toRelatedStatements(subj, partType, tgt);
  }

  private Stream<Statement> toRelatedStatements(URI subj, ConceptDescriptor rel, URI tgt) {
    return Stream.concat(
        Arrays.stream(rel.getClosure())
            .map(anc -> toStatement(subj, anc.getReferentId(), tgt)),
        Stream.of(this.toStatement(subj, rel.getReferentId(), tgt))
    );
  }

  public Statement toStatement(URI subject, URI predicate, URI object) {
    return createStatement(
        ResourceFactory.createResource(subject.toString()),
        ResourceFactory.createProperty(predicate.toString()),
        ResourceFactory.createResource(object.toString()));
  }

  public Statement toStringValueStatement(URI subject, URI predicate, String object) {
    return createStatement(
        ResourceFactory.createResource(subject.toString()),
        ResourceFactory.createProperty(predicate.toString()),
        ResourceFactory.createStringLiteral(object));
  }

  public Statement toLongValueStatement(URI subject, URI predicate, Long object) {
    return createStatement(
        ResourceFactory.createResource(subject.toString()),
        ResourceFactory.createProperty(predicate.toString()),
        ResourceFactory.createTypedLiteral(object));
  }

  @Override
  public Set<ResourceIdentifier> getRelatedAssets(ResourceIdentifier assetPointer, URI relation) {

    Map<String, URI> params = Maps.newHashMap();
    params.put("?s", assetPointer.getVersionId());
    params.put("?p", relation);

    Set<ResourceIdentifier> related = Sets.newHashSet();

    this.jenaSparqlDao.runSparql(
        new ParameterizedSparqlString(TRANSITIVE_CLOSURE_SELECT),
        params,
        Collections.emptyMap(), (
            querySolution -> related.add(
                this.resourceToResourceIdentifier(querySolution.getResource("?o")))));

    return related;
  }

  @Override
  public Set<ResourceIdentifier> getRelatedAssets(ResourceIdentifier assetPointer) {
    Map<String, URI> params = Maps.newHashMap();
    params.put("?s", assetPointer.getVersionId());

    Set<ResourceIdentifier> related = Sets.newHashSet();

    this.jenaSparqlDao.runSparql(
        new ParameterizedSparqlString(InternalQueryManager.DEPENDENCY_CLOSURE_SELECT),
        params, Collections.emptyMap(), (
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

  private void registerAsset(KnowledgeAsset asset,
      ResourceIdentifier surrogate, String surrogateMimeType) {
    if (asset.getAssetId().getVersionId().toString().startsWith("urn:uuid")) {
      throw new IllegalStateException("KR URIs should be DID now");
    }

    this.jenaSparqlDao
        .store(this.toRdf(asset.getAssetId(), asset.getSecondaryId(),
            asset.getName(), surrogate, surrogateMimeType,
            asset.getFormalType(), asset.getRole(), asset.getAnnotation(),
            asset.getLinks(), asset.getLifecycle(), asset.getProcessingMethod(),
            asset.getMemberOf()));
  }


  @Override
  @Loggable(level = LogLevel.INFO)
  public void unregisterAsset(ResourceIdentifier asset) {
    if (kgi.isKnowledgeGraphAsset(asset.getUuid())) {
      throw new IllegalArgumentException("Unable remove the Knowledge Graph from the Index");
    }
    var assetId = asset.getResourceId().toString();
    jenaSparqlDao.removeBySubject(assetId);
  }

  @Override
  @Loggable(level = LogLevel.INFO)
  public void unregisterAssetVersion(ResourceIdentifier asset) {
    if (kgi.isKnowledgeGraphAsset(asset.getUuid())) {
      throw new IllegalArgumentException("Unable remove the Knowledge Graph from the Index");
    }
    Set<ResourceIdentifier> surrs = getSurrogatesForAsset(asset);
    Set<ResourceIdentifier> carrs = getArtifactsForAsset(asset);

    Set<String> ids = new HashSet<>();
    ids.add(asset.getVersionId().toString());

    surrs.forEach(surr -> {
      ids.add(surr.getResourceId().toString());
      getSurrogateVersions(surr.getUuid())
          .forEach(surrV ->
              ids.add(surrV.getVersionId().toString()));
    });

    carrs.forEach(carr -> {
      ids.add(carr.getResourceId().toString());
      getCarrierVersions(carr.getUuid())
          .forEach(carrV ->
              ids.add(carrV.getVersionId().toString()));
    });

    jenaSparqlDao.removeBySubjects(ids);
    jenaSparqlDao.remove(singletonList(objA(
        asset.getResourceId().toString(),
        HAS_VERSION_URI.toString(),
        asset.getVersionId().toString())));
  }

  @Override
  public void registerArtifactToAsset(ResourceIdentifier assetPointer,
      KnowledgeArtifact artifact, String mimeType) {
    if (kgi.isKnowledgeGraphAsset(assetPointer.getUuid())) {
      throw new IllegalArgumentException("Unable to register a Carrier for the Knowledge Graph");
    }

    ResourceIdentifier artifactId = artifact.getArtifactId();
    if (artifactId.getVersionId().toString().startsWith("urn:uuid")) {
      throw new IllegalStateException("KR URIs should be DID now");
    }
    List<Statement> statements = new ArrayList<>();
    Stream.concat(
            Stream.of(
                toStatement(assetPointer.getVersionId(), HAS_CARRIER_URI, artifactId.getResourceId()),
                toStatement(artifactId.getResourceId(), HAS_VERSION_URI, artifactId.getVersionId()),
                toStringValueStatement(artifactId.getResourceId(), TAG_ID_URI,
                    artifactId.getUuid().toString()),
                toStringValueStatement(artifactId.getVersionId(), HAS_VERSION_TAG_URI,
                    artifactId.getVersionTag()),
                toStringValueStatement(artifactId.getResourceId(), FORMAT_URI,
                    mimeType != null ? mimeType : "*/*"),
                toLongValueStatement(artifactId.getVersionId(),
                    ESTABLISHED_URI, getEstablishedOn(artifact.getLifecycle(), artifactId))),
            artifact.getLinks().stream()
                .flatMap(StreamUtil.filterAs(Dependency.class))
                .flatMap(dependency -> toDependencyStatements(artifactId.getVersionId(), dependency)))
        .forEach(statements::add);

    var accessURL = artifact.getLocator() != null
        ? artifact.getLocator()
        : URI.create(
            assetPointer.getVersionId() + "/carriers/" +
                artifactId.getUuid() + "/versions/" + artifactId.getVersionTag());
    statements.add(toStatement(artifactId.getVersionId(), LOCATOR_URI, accessURL));

    if (artifact.getInlinedExpression() != null
        && TXT.sameAs(artifact.getRepresentation().getFormat())
        && !HTML.sameAs(artifact.getRepresentation().getLanguage())) {
      statements.add(toStringValueStatement(
          artifact.getArtifactId().getVersionId(),
          HAS_EXPRESSION_URI,
          artifact.getInlinedExpression()));
    }
    this.jenaSparqlDao.store(statements);
  }

  private Long getEstablishedOn(Publication lifecycle, ResourceIdentifier resourceId) {
    return Optional.ofNullable(lifecycle)
        .flatMap(pub -> getSurrogateEstablished(pub, resourceId.getVersionTag()))
        .orElseGet(resourceId::getEstablishedOn)
        .getTime();
  }

  /**
   * Tries to determine when a certain version of a Resource was effectively 'created'.
   * <p>
   * This method is a placeholder, because the correlation between the supported metadata events
   * (creation, last modification and issuance) is not explicitly defined in its relationship to
   * series and versioning, assets and artifacts. As a consequence, its implementation in the source
   * systems is not consistent.
   *
   * @param pub
   * @param versionTag
   * @return
   */
  @Deprecated
  private Optional<Date> getSurrogateEstablished(Publication pub, String versionTag) {
    if (Draft.sameAs(pub.getPublicationStatus()) || versionTag != null && versionTag.contains(
        SNAPSHOT)) {
      return Optional.ofNullable(pub.getLastReviewedOn())
          .or(() -> Optional.ofNullable(pub.getCreatedOn()));
    } else {
      return Optional.ofNullable(pub.getCreatedOn())
          .or(() -> Optional.ofNullable(pub.getLastReviewedOn()));
    }
  }

  @Override
  public void registerSurrogateToAsset(ResourceIdentifier assetPointer,
      KnowledgeArtifact surrogate, String mimeType) {
    if (kgi.isKnowledgeGraphAsset(assetPointer.getUuid())) {
      throw new IllegalArgumentException("Unable to register a Surrogate for the Knowledge Graph");
    }
    ResourceIdentifier surrogateId = surrogate.getArtifactId();
    if (surrogateId.getVersionId().toString().startsWith("urn:uuid")) {
      throw new IllegalStateException("KR URIs should be DID now");
    }

    List<Statement> statements = Arrays.asList(
        toStatement(assetPointer.getVersionId(), HAS_SURROGATE_URI, surrogateId.getResourceId()),
        toStatement(surrogateId.getResourceId(), HAS_VERSION_URI, surrogateId.getVersionId()),
        toStringValueStatement(surrogateId.getResourceId(), TAG_ID_URI,
            surrogateId.getUuid().toString()),
        toStringValueStatement(surrogateId.getResourceId(), FORMAT_URI,
            Util.isNotEmpty(mimeType) ? mimeType : null),
        toStringValueStatement(surrogateId.getVersionId(), HAS_VERSION_TAG_URI,
            surrogateId.getVersionTag()),
        toLongValueStatement(surrogateId.getVersionId(),
            ESTABLISHED_URI, getEstablishedOn(surrogate.getLifecycle(), surrogateId))
    );
    this.jenaSparqlDao.store(statements);
  }

  @Override
  public Optional<ResourceIdentifier> getCanonicalSurrogateForAsset(
      ResourceIdentifier assetPointer) {
    if (kgi.isKnowledgeGraphAsset(assetPointer.getUuid())) {
      return Optional.ofNullable(kgi.knowledgeGraphSurrogateId());
    }
    return this.jenaSparqlDao
        .readObjectBySubjectAndPredicate(assetPointer.getVersionId(), HAS_CANONICAL_SURROGATE_URI)
        .stream()
        .map(this::resourceSeriesToResourceIdentifier)
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
    if (kgi.isKnowledgeGraphAsset(assetId.getUuid())) {
      return true;
    }
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
    if (kgi.isKnowledgeGraphAsset(assetId.getUuid())) {
      return singleton(kgi.knowledgeGraphArtifactId());
    }
    List<Resource> resources =
        this.jenaSparqlDao
            .readObjectBySubjectAndPredicate(assetId.getVersionId(), HAS_CARRIER_URI);

    return resources.stream()
        .map(this::resourceSeriesToResourceIdentifier)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<ResourceIdentifier> getSurrogatesForAsset(ResourceIdentifier assetId) {
    if (kgi.isKnowledgeGraphAsset(assetId.getUuid())) {
      return singleton(kgi.knowledgeGraphSurrogateId());
    }
    List<Resource> resources =
        this.jenaSparqlDao
            .readObjectBySubjectAndPredicate(assetId.getVersionId(), HAS_SURROGATE_URI);

    return resources.stream()
        .map(this::resourceSeriesToResourceIdentifier)
        .collect(Collectors.toSet());
  }

  /**
   * Retrieves the name of an Asset
   *
   * @param assetId the ID of the Asset
   * @return the name of the Asset, according to the Surrogate
   */
  @Override
  public Optional<String> getAssetName(ResourceIdentifier assetId) {
    if (kgi.isKnowledgeGraphAsset(assetId.getUuid())) {
      return Optional.ofNullable(kgi.getKnowledgeGraphLabel());
    }
    List<Literal> labels = this.jenaSparqlDao.readValueBySubjectAndPredicate(
        assetId.getVersionId(), URI.create(RDFS.label.getURI()));

    return labels.isEmpty()
        ? Optional.empty()
        : Optional.of(labels.get(0).getString());
  }

  /**
   * Retrieves the types and roles of an Asset
   *
   * @param assetId the ID of the Asset
   * @return the types and roles, according to the Surrogate, as ConceptIdentifiers
   */
  @Override
  public List<ConceptIdentifier> getAssetTypes(ResourceIdentifier assetId) {
    List<Resource> types = this.jenaSparqlDao.readObjectBySubjectAndPredicate(
        assetId.getVersionId(), URI.create(RDF.type.getURI()));

    List<ConceptIdentifier> typeConcepts = new ArrayList<>();

    types.stream()
        .map(type -> URI.create(type.getURI()))
        .map(uri -> KnowledgeAssetTypeSeries.resolveRef(uri)
            .or(() -> ClinicalKnowledgeAssetTypeSeries.resolveRef(uri)))
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

  @Override
  public Optional<ResourceIdentifier>  resolveAsset(UUID assetId, String versionTag) {
    if (kgi.isKnowledgeGraphAsset(assetId)) {
      return Optional.of(kgi.knowledgeGraphAssetId());
    }
    return resolve(assetId, versionTag);
  }

  @Override
  public Optional<ResourceIdentifier> resolveArtifact(UUID artifactId, String versionTag) {
    if (kgi.isKnowledgeGraphCarrier(artifactId)) {
      return Optional.of(kgi.knowledgeGraphArtifactId());
    }
    if (kgi.isKnowledgeGraphSurrogate(artifactId)) {
      return Optional.of(kgi.knowledgeGraphSurrogateId());
    }
    return resolve(artifactId, versionTag);
  }

  protected Optional<ResourceIdentifier> resolve(UUID assetId, String versionTag) {
    if (assetId == null || versionTag == null) {
      return Optional.empty();
    }
    List<Resource> versions = new ArrayList<>();
    Map<String, Literal> literalParams = new HashMap<>();
    literalParams.put("?tag",
        ResourceFactory.createPlainLiteral(assetId.toString()));
    literalParams.put("?vTag",
        ResourceFactory.createPlainLiteral(versionTag));

    this.jenaSparqlDao.runSparql(
        new ParameterizedSparqlString(InternalQueryManager.RESOLVE_TAG_VERSION_SELECT),
        Collections.emptyMap(),
        literalParams, (
            querySolution -> versions.add(querySolution.getResource("?version"))));

    return versions.isEmpty()
        ? Optional.empty()
        : Optional.of(resourceToResourceIdentifier(versions.get(0)));
  }

  @Override
  public Optional<ResourceIdentifier> resolveAsset(UUID assetId) {
    if (kgi.isKnowledgeGraphAsset(assetId)) {
      return Optional.of(kgi.knowledgeGraphAssetId());
    }
    return resolve(assetId);
  }

  @Override
  public Optional<ResourceIdentifier> resolveArtifact(UUID artifactId) {
    if (kgi.isKnowledgeGraphAsset(artifactId)) {
      return Optional.of(kgi.knowledgeGraphArtifactId());
    }
    return resolve(artifactId);
  }

  @Override
  public Optional<Date> getEstablishmentDate(ResourceIdentifier resourceId) {
    List<Instant> dates = new ArrayList<>();
    Map<String, URI> params = Maps.newHashMap();
    params.put("?s", resourceId.getVersionId());

    this.jenaSparqlDao.runSparql(
        new ParameterizedSparqlString(InternalQueryManager.ESTABLISHED_DATE_SELECT),
        params,
        Collections.emptyMap(),
        qs -> dates.add(Instant.ofEpochMilli(qs.getLiteral("?o").getLong()))
    );
    return dates.isEmpty()
        ? Optional.empty()
        : Optional.of(Date.from(dates.get(0)));
  }

  protected Optional<ResourceIdentifier> resolve(UUID resourceId) {
    List<Resource> versions = new ArrayList<>();
    Map<String, Literal> literalParams = new HashMap<>();
    literalParams.put("?tag",
        ResourceFactory.createPlainLiteral(resourceId.toString()));

    this.jenaSparqlDao.runSparql(
        new ParameterizedSparqlString(InternalQueryManager.RESOLVE_TAG_SELECT),
        Collections.emptyMap(),
        literalParams, (
            querySolution -> versions.add(querySolution.getResource("?asset"))));

    return versions.isEmpty()
        ? Optional.empty()
        : Optional.of(resourceSeriesToResourceIdentifier(versions.get(0)));
  }

  /**
   * Returns the known Versions of a given KnowledgeAsset, sorted by (asset) version Tag and,
   * subsequently, by timestamp
   *
   * @param assetSeriesId the UUID of the Asset series, common to all versions
   */
  @Override
  public List<ResourceIdentifier> getAssetVersions(UUID assetSeriesId) {
    if (kgi.isKnowledgeGraphAsset(assetSeriesId)) {
      return singletonList(kgi.knowledgeGraphAssetId());
    }
    List<ResourceIdentifier> versions = new ArrayList<>();
    Map<String, Literal> literalParams = new HashMap<>();
    literalParams.put("?tag",
        ResourceFactory.createPlainLiteral(assetSeriesId.toString()));

    this.jenaSparqlDao.runSparql(
        new ParameterizedSparqlString(InternalQueryManager.ASSET_VERSIONS_SELECT),
        Collections.emptyMap(),
        literalParams, (
            querySolution -> versions.add(
                this.versionInfoToPointer(
                    querySolution.getResource("?asset"),
                    querySolution.getResource("?version"),
                    querySolution.getLiteral("?vTag"),
                    querySolution.getLiteral("?vTimestamp"),
                    null))));
    return versions;
  }


  /**
   * Returns the known Versions of a Canonical Surrogate sorted by timestamp
   *
   * @param surrogateSeriesId the identifier of a Surrogate series
   */
  @Override
  public List<Pointer> getSurrogateVersions(UUID surrogateSeriesId) {
    if (kgi.isKnowledgeGraphSurrogate(surrogateSeriesId)) {
      return singletonList(kgi.knowledgeGraphSurrogateId().toPointer());
    }
    List<Pointer> versions = new ArrayList<>();
    Map<String, Literal> literalParams = new HashMap<>();
    literalParams.put("?tag",
        ResourceFactory.createPlainLiteral(surrogateSeriesId.toString()));

    this.jenaSparqlDao.runSparql(
        new ParameterizedSparqlString(InternalQueryManager.SURROGATE_VERSIONS_SELECT),
        Collections.emptyMap(),
        literalParams, (
            querySolution -> versions.add(
                this.versionInfoToPointer(
                    querySolution.getResource("?surrogate"),
                    querySolution.getResource("?version"),
                    querySolution.getLiteral("?vTag"),
                    querySolution.getLiteral("?vTimestamp"),
                    querySolution.getLiteral("?format")
                ))));
    return versions;
  }


  /**
   * Returns the known Versions of a Knowledge Artifact sorted by timestamp
   *
   * @param carrierSeriesId the identifier of a Surrogate series
   */
  @Override
  public List<Pointer> getCarrierVersions(UUID carrierSeriesId) {
    if (kgi.isKnowledgeGraphCarrier(carrierSeriesId)) {
      return singletonList(kgi.knowledgeGraphArtifactId().toPointer());
    }
    List<Pointer> versions = new ArrayList<>();
    Map<String, Literal> literalParams = new HashMap<>();
    literalParams.put("?tag",
        ResourceFactory.createPlainLiteral(carrierSeriesId.toString()));

    this.jenaSparqlDao.runSparql(
        new ParameterizedSparqlString(InternalQueryManager.CARRIER_VERSIONS_SELECT),
        Collections.emptyMap(),
        literalParams, (
            querySolution -> versions.add(
                this.versionInfoToPointer(
                    querySolution.getResource("?carrier"),
                    querySolution.getResource("?version"),
                    querySolution.getLiteral("?vTag"),
                    querySolution.getLiteral("?vTimestamp"),
                    querySolution.getLiteral("?format")
                ))));
    return versions;
  }


  protected ResourceIdentifier resourceToResourceIdentifier(Resource resource) {
    return newVersionId(URI.create(resource.getURI()));
  }

  protected ResourceIdentifier resourceSeriesToResourceIdentifier(Resource resource) {
    return newId(URI.create(resource.getURI()));
  }

  protected Pointer versionInfoToPointer(
      Resource resourceId, Resource resourceVersionId,
      Literal versionTag, Literal versionTimestamp, Literal mimeType) {
    var rid = SemanticIdentifier
        .newVersionIdAsPointer(URI.create(resourceVersionId.getURI()));
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
    if (mimeType != null) {
      rid.setMimeType(mimeType.getString());
    }
    return rid;
  }

  static class InternalQueryManager {

    private static final String PREAMBLE = ""
        + " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
        + " PREFIX lcc: <https://www.omg.org/spec/LCC/Languages/LanguageRepresentation/> \n"
        + " PREFIX api4kp: <https://www.omg.org/spec/API4KP/api4kp/> \n"
        + " PREFIX dc: <" + DublinCoreVocabulary.NAME_SPACE + "> \n"
        + " PREFIX api4kp-series: <https://www.omg.org/spec/API4KP/api4kp-series/> \n"
        + " PREFIX kmd: <http://ontology.mayo.edu/ontologies/kmdp/> \n";


    static final String RESOLVE_TAG_SELECT =
        PREAMBLE
            + "SELECT ?asset \n"
            + "WHERE { \n"
            + "  ?asset kmd:" + TAG_ID + " ?tag . \n"
            + "}";

    static final String RESOLVE_TAG_VERSION_SELECT =
        PREAMBLE
            + "SELECT ?asset ?version \n"
            + "WHERE { \n"
            + "  ?asset kmd:" + TAG_ID + " ?tag ; \n"
            + "     api4kp-series:" + HAS_VERSION + " ?version . \n"
            + "  ?version kmd:" + HAS_VERSION_TAG + " ?vTag . \n"
            + "}";

    static final String CARRIER_VERSIONS_SELECT =
        PREAMBLE
            + "SELECT ?carrier ?version ?vTag ?vTimestamp ?format \n"
            + "WHERE { \n"
            + "  ?asset api4kp:" + HAS_CARRIER + " ?carrier . \n"
            + "  ?carrier kmd:" + TAG_ID + " ?tag ; \n"
            + "     api4kp-series:" + HAS_VERSION + " ?version . \n"
            + "     OPTIONAL { ?carrier dc:" + FORMAT + " ?format } \n"
            + "  ?version  \n"
            + "     kmd:" + HAS_VERSION_TAG + " ?vTag ; \n"
            + "     api4kp-series:" + ESTABLISHED + " ?vTimestamp . \n"
            + "} \n"
            + "ORDER BY DESC(?vTimestamp)";

    static final String SURROGATE_VERSIONS_SELECT =
        PREAMBLE
            + "SELECT ?surrogate ?version ?vTag ?vTimestamp ?format \n"
            + "WHERE { \n"
            + "  ?asset api4kp:" + HAS_SURROGATE + " ?surrogate . \n"
            + "  ?surrogate kmd:" + TAG_ID + " ?tag ; \n"
            + "     api4kp-series:" + HAS_VERSION + " ?version . \n"
            + "     OPTIONAL { ?surrogate dc:" + FORMAT + " ?format } \n"
            + "  ?version  \n"
            + "     kmd:" + HAS_VERSION_TAG + " ?vTag ; \n"
            + "     api4kp-series:" + ESTABLISHED + " ?vTimestamp . \n"
            + "} \n"
            + "ORDER BY DESC(?vTimestamp)";

    static final String ASSET_VERSIONS_SELECT =
        PREAMBLE
            + "SELECT ?asset ?version ?vTag ?vTimestamp \n"
            + "WHERE { \n"
            + "  ?asset kmd:" + TAG_ID + " ?tag ; \n"
            + "     api4kp-series:" + HAS_VERSION + " ?version . \n"
            + "  ?version rdf:type api4kp:" + ASSET + " ; \n"
            + "     kmd:" + HAS_VERSION_TAG + " ?vTag ; \n"
            + "     api4kp-series:" + ESTABLISHED + " ?vTimestamp . \n"
            + "} \n"
            + "ORDER BY DESC(?vTimestamp)";

    static final String TRANSITIVE_CLOSURE_SELECT =
        "SELECT ?o \n" +
            "WHERE { \n" +
            "    ?s ?p* ?o \n" +
            "}";

    static final String DEPENDENCY_CLOSURE_SELECT =
        "SELECT ?o \n" +
            "WHERE { \n" +
            "    ?s " + TRAVERSE_DEPS_SPARQL + " ?o\n" +
            "}";

    static final String ESTABLISHED_DATE_SELECT =
        PREAMBLE +
            "SELECT ?o \n" +
            "WHERE { \n" +
            "    ?s api4kp-series:" + ESTABLISHED + " ?o\n" +
            "}";

    static final String TRIPLE_OBJECT_SELECT =
        "SELECT ?o WHERE { ?s ?p ?o . }";

    static final String TRIPLE_SUBJECT_SELECT =
        "SELECT ?s WHERE { ?s ?p ?o . }";

    private InternalQueryManager() {
      // constants only
    }

  }
}
