package edu.mayo.kmdp.repository.asset.index.sparql;

import static edu.mayo.ontology.taxonomies.kao.rel.dependencyreltype.DependencyTypeSeries.Depends_On;
import static edu.mayo.ontology.taxonomies.kao.rel.dependencyreltype.DependencyTypeSeries.Imports;
import static edu.mayo.ontology.taxonomies.kao.rel.dependencyreltype.DependencyTypeSeries.Includes;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.mayo.kmdp.metadata.annotations.Annotation;
import edu.mayo.kmdp.metadata.annotations.BasicAnnotation;
import edu.mayo.kmdp.metadata.annotations.SimpleAnnotation;
import edu.mayo.kmdp.metadata.surrogate.Association;
import edu.mayo.kmdp.metadata.surrogate.Dependency;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.repository.asset.index.Index;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.ontology.taxonomies.kao.knowledgeassetrole.KnowledgeAssetRole;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetType;
import edu.mayo.ontology.taxonomies.kao.rel.dependencyreltype.DependencyTypeSeries;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.omg.spec.api4kp._1_0.id.Pointer;
import org.omg.spec.api4kp._1_0.id.ResourceIdentifier;
import org.omg.spec.api4kp._1_0.id.SemanticIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * An implementation of the Asset {@link Index} interface that uses an RDF/SPARQL backend.
 */
@Component
public class SparqlIndex implements Index {

  private static final Set<DependencyTypeSeries> TRAVERSE_DEPS =
      Util.newEnumSet(Arrays.asList(Imports, Includes, Depends_On), DependencyTypeSeries.class);

  private static final String TRAVERSE_DEPS_SPARQL;

  // compute the SPARQL query string for all related predicates
  static {
    TRAVERSE_DEPS_SPARQL =
        "(" + TRAVERSE_DEPS.stream().map(c -> "<" + c.getRef().toString() + ">")
            .collect(Collectors.joining("|")) + ")";
  }

  //TODO: Change this to the official URIs as published in the ontology.
  // All below are likely wrong until changed.
  private static String URI_BASE = "https://ontology.mayo.edu/";

  private static final URI ASSET_URI = URI.create(URI_BASE + "asset");
  private static final URI HAS_VERSION_URI = URI.create(URI_BASE + "hasVersion");
  private static final URI HAS_CARRIER_URI = URI.create(URI_BASE + "hasCarrier");
  private static final URI HAS_SURROGATE_URI = URI.create(URI_BASE + "hasSurrogate");

  @Autowired
  private JenaSparqlDao jenaSparqlDao;

  public SparqlIndex(JenaSparqlDao jenaSparqlDao) {
    this.jenaSparqlDao = jenaSparqlDao;
  }

  @Override
  public void registerAsset(ResourceIdentifier asset, ResourceIdentifier surrogate,
      List<KnowledgeAssetType> types, List<KnowledgeAssetRole> roles, List<Annotation> annotations,
      List<Association> related) {
    this.jenaSparqlDao.store(this.toRdf(asset, surrogate, types, roles, annotations, related));
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
  public List<Statement> toRdf(ResourceIdentifier asset, ResourceIdentifier surrogate,
      List<KnowledgeAssetType> types, List<KnowledgeAssetRole> roles, List<Annotation> annotations,
      List<Association> related) {
    List<Statement> statements = Lists.newArrayList();
    URI versionedAssetId = asset.getVersionId();

    // annotations
    statements.addAll(annotations.stream().map(annotation -> {
      if (annotation instanceof SimpleAnnotation) {
        return this.toStatement(
            versionedAssetId,
            annotation.getRel().getRef(),
            ((SimpleAnnotation) annotation).getExpr().getConceptId());
      } else {
        throw new UnsupportedOperationException(
            "Cannot store Annotation of class: " + annotation.getClass().getName());
      }
    }).collect(Collectors.toList()));

    // related
    statements.addAll(related.stream()
        .flatMap(StreamUtil.filterAs(Dependency.class))
        .map(dependency -> this.toStatement(
            versionedAssetId,
            dependency.getRel().asConcept().getRef(),
            ((KnowledgeAsset) dependency.getTgt()).getAssetId().getVersionId()))
        .collect(Collectors.toList()));

    // type of Asset
    statements
        .add(this.toStatement(
            versionedAssetId,
            URI.create(RDF.type.getURI()),
            ASSET_URI));

    // version
    statements.add(this.toStatement(
        asset.getResourceId(),
        HAS_VERSION_URI,
        asset.getVersionId()));

    // Surrogate link
    statements
        .add(this.toStatement(
            versionedAssetId,
            HAS_SURROGATE_URI,
            surrogate.getVersionId()));

    // Asset types
    statements.addAll(types.stream().map(type ->
        this.toStatement(
            versionedAssetId,
            URI.create(RDF.type.getURI()),
            type.getRef())
    ).collect(Collectors.toList()));

    // Asset roles
    statements.addAll(roles.stream().map(role ->
        this.toStatement(
            versionedAssetId,
            URI.create(RDF.type.getURI()),
            role.getRef())
    ).collect(Collectors.toList()));

    return statements;
  }

  public Statement toStatement(URI subject, URI predicate, URI object) {
    return ResourceFactory.createStatement(
        ResourceFactory.createResource(subject.toString()),
        ResourceFactory.createProperty(predicate.toString()),
        ResourceFactory.createResource(object.toString()));
  }

  @Override
  public Set<ResourceIdentifier> getRelatedAssets(ResourceIdentifier assetPointer, URI relation) {
    String sparql = "" +
        "SELECT ?o\n" +
        "WHERE {\n" +
        "    ?s ?p* ?o\n" +
        "}";

    Map<String, String> params = Maps.newHashMap();
    params.put("?s", assetPointer.getVersionId().toString());
    params.put("?p", relation.toString());

    Set<ResourceIdentifier> related = Sets.newHashSet();

    this.jenaSparqlDao.runSparql(sparql, params, (
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

    Map<String, String> params = Maps.newHashMap();
    params.put("?s", assetPointer.getVersionId().toString());

    Set<ResourceIdentifier> related = Sets.newHashSet();

    this.jenaSparqlDao.runSparql(sparql, params, (
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
  public void registerArtifactToAsset(ResourceIdentifier assetPointer,
      ResourceIdentifier artifact) {
    this.jenaSparqlDao.store(assetPointer.getVersionId(), HAS_CARRIER_URI, artifact.getVersionId());
  }

  @Override
  public void registerSurrogateToAsset(ResourceIdentifier assetPointer,
      ResourceIdentifier surrogate) {
    this.jenaSparqlDao
        .store(assetPointer.getVersionId(), HAS_SURROGATE_URI, surrogate.getVersionId());
  }

  @Override
  public ResourceIdentifier getSurrogateForAsset(ResourceIdentifier assetPointer) {
    return this.jenaSparqlDao
        .readObjectBySubjectAndPredicate(assetPointer.getVersionId(), HAS_SURROGATE_URI)
        .stream()
        .map(this::resourceToResourceIdentifier)
        .findFirst().orElse(null);
  }

  @Override
  public URI getLocation(ResourceIdentifier pointer) {
    if (pointer instanceof Pointer) {
      return ((Pointer) pointer).getHref();
    };
    return pointer.getVersionId();
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

  protected ResourceIdentifier resourceToResourceIdentifier(Resource resource) {
    return SemanticIdentifier
        .newVersionId(URI.create(resource.getURI()));
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
  public Set<ResourceIdentifier> getArtifactsForAsset(ResourceIdentifier artifact) {
    List<Resource> resources =
        this.jenaSparqlDao
            .readObjectBySubjectAndPredicate(artifact.getVersionId(), HAS_CARRIER_URI);

    return resources.stream()
        .map(this::resourceToResourceIdentifier)
        .collect(Collectors.toSet());
  }

  @Override
  public void reset() {
    throw new UnsupportedOperationException("\"reset\" is now disabled.");
  }

}
