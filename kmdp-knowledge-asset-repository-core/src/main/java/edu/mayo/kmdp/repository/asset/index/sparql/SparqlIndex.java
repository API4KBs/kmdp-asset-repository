package edu.mayo.kmdp.repository.asset.index.sparql;

import com.google.common.collect.Lists;
import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.metadata.annotations.Annotation;
import edu.mayo.kmdp.metadata.annotations.BasicAnnotation;
import edu.mayo.kmdp.metadata.annotations.SimpleAnnotation;
import edu.mayo.kmdp.repository.asset.index.Index;
import edu.mayo.kmdp.repository.asset.index.IndexPointer;
import edu.mayo.ontology.taxonomies.kao.knowledgeassetrole.KnowledgeAssetRole;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetType;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An implementation of the Asset {@link Index} interface that uses an RDF/SPARQL backend.
 */
@Component
public class SparqlIndex implements Index {

  //TODO: Change this to the official URIs as published in the ontology.
  // All below are likely wrong until changed.
  private static String URI_BASE = "https://ontology.mayo.edu/";

  private static URI ASSET_URI = URI.create(URI_BASE + "asset");
  private static URI HAS_VERSION_URI = URI.create(URI_BASE + "hasVersion");
  private static URI HAS_CARRIER_URI = URI.create(URI_BASE + "hasCarrier");
  private static URI HAS_SURROGATE_URI = URI.create(URI_BASE + "hasSurrogate");

  @Autowired
  private JenaSparqlDao jenaSparqlDao;

  public SparqlIndex(JenaSparqlDao jenaSparqlDao) {
    this.jenaSparqlDao = jenaSparqlDao;
  }

  @Override
  public void registerAsset(IndexPointer asset, IndexPointer surrogate, List<KnowledgeAssetType> types, List<KnowledgeAssetRole> roles, List<Annotation> annotations, String name, String description) {
    this.jenaSparqlDao.store(this.toRdf(asset, surrogate, types, roles, annotations, name, description));
  }

  /**
   * Deconstruct an Asset into RDF statements.
   *
   * @param asset
   * @param surrogate
   * @param types
   * @param roles
   * @param annotations
   * @param name
   * @param description
   * @return
   */
  public List<Statement> toRdf(IndexPointer asset, IndexPointer surrogate, List<KnowledgeAssetType> types, List<KnowledgeAssetRole> roles, List<Annotation> annotations, String name, String description) {
    List<Statement> statements = Lists.newArrayList();

    // annotations
    statements.addAll(annotations.stream().map(annotation -> {
      if (annotation instanceof BasicAnnotation) {
        return this.toStatement(
                asset.getVersionId(), annotation.getRel().getConceptId(), ((BasicAnnotation) annotation).getExpr());
      } else if (annotation instanceof SimpleAnnotation) {
        return this.toStatement(asset.getVersionId(), annotation.getRel().getConceptId(), ((SimpleAnnotation) annotation).getExpr().getConceptId());
      } else {
        throw new UnsupportedOperationException("Cannot store Annotation of class: " + annotation.getClass().getName());
      }
    }).collect(Collectors.toList()));

    // type of Asset
    statements.add(this.toStatement(asset.getVersionId(), URI.create(RDF.type.getURI()), ASSET_URI));

    // version
    statements.add(this.toStatement(asset.getUri(), HAS_VERSION_URI, asset.getVersionId()));

    // Surrogate link
    statements.add(this.toStatement(asset.getVersionId(), HAS_SURROGATE_URI, surrogate.getVersionId()));

    // Asset types
    statements.addAll(types.stream().map(type ->
            this.toStatement(asset.getVersionId(), URI.create(RDF.type.getURI()), type.getConceptId())
    ).collect(Collectors.toList()));

    // Asset roles
    statements.addAll(roles.stream().map(role ->
            this.toStatement(asset.getVersionId(), URI.create(RDF.type.getURI()), role.getConceptId())
    ).collect(Collectors.toList()));

    return statements;
  }

  public Statement toStatement(URI subject, URI predicate, URI object) {
    Statement s = ResourceFactory.createStatement(
            ResourceFactory.createResource(subject.toString()),
            ResourceFactory.createProperty(predicate.toString()),
            ResourceFactory.createResource(object.toString()));

    return s;
  }

  @Override
  public Set<IndexPointer> getAllAssetIds() {
    return this.jenaSparqlDao.readSubjectByPredicateAndObject(URI.create(RDF.type.getURI()), ASSET_URI)
            .stream()
            .map(this::resourceToIndexPointer).collect(Collectors.toSet());
  }

  @Override
  public void registerArtifactToAsset(IndexPointer assetPointer, IndexPointer artifact) {
    this.jenaSparqlDao.store(assetPointer.getVersionId(), HAS_CARRIER_URI, artifact.getVersionId());
  }

  @Override
  public void registerSurrogateToAsset(IndexPointer assetPointer, IndexPointer surrogate) {
    this.jenaSparqlDao.store(assetPointer.getVersionId(), HAS_SURROGATE_URI, surrogate.getVersionId());
  }

  @Override
  public IndexPointer getSurrogateForAsset(IndexPointer assetPointer) {
    return this.jenaSparqlDao.readObjectBySubjectAndPredicate(assetPointer.getVersionId(), HAS_SURROGATE_URI)
            .stream()
            .map(this::resourceToIndexPointer)
            .findFirst().orElse(null);
  }

  @Override
  public String getLocation(IndexPointer pointer) {
    return pointer.getVersionId().toString();
  }

  @Override
  public Set<IndexPointer> getAssetIdsByType(URI assetType) {
    List<Resource> resources =
            this.jenaSparqlDao.readSubjectByPredicateAndObject(URI.create(RDF.type.getURI()), assetType);

    return resources.stream().map(this::resourceToIndexPointer).collect(Collectors.toSet());
  }

  protected IndexPointer resourceToIndexPointer(Resource resource) {
    IndexPointer indexPointer = new IndexPointer(DatatypeHelper.toURIIDentifier(resource.getURI()));
    return indexPointer;
  }

  @Override
  public Set<IndexPointer> getAssetIdsByAnnotation(URI annotation) {
    List<Resource> resources =
            this.jenaSparqlDao.readSubjectByPredicate(annotation);

    return resources.stream().map(this::resourceToIndexPointer).collect(Collectors.toSet());
  }

  @Override
  public Set<IndexPointer> getArtifactsForAsset(IndexPointer artifact) {
    List<Resource> resources =
            this.jenaSparqlDao.readObjectBySubjectAndPredicate(artifact.getVersionId(), HAS_CARRIER_URI);

    return resources.stream().map(this::resourceToIndexPointer).collect(Collectors.toSet());
  }

  @Override
  public void reset() {
    throw new UnsupportedOperationException("`reset` is now disabled.");
  }

}
