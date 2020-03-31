package edu.mayo.kmdp.repository.asset.index;

import com.google.common.collect.Sets;
import edu.mayo.kmdp.terms.ConceptTerm;
import edu.mayo.kmdp.util.URIUtil;
import edu.mayo.ontology.taxonomies.kao.knowledgeassetrole.KnowledgeAssetRole;
import edu.mayo.ontology.taxonomies.kao.knowledgeassetrole.KnowledgeAssetRoleSeries;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetType;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries;
import edu.mayo.ontology.taxonomies.kmdo.annotationreltype.AnnotationRelTypeSeries;
import java.net.URI;
import java.util.Optional;
import java.util.Set;
import org.omg.spec.api4kp._1_0.id.ResourceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class that filters the repository content based on the Index and some known semantic
 * properties
 * <p>
 * Namely type and annotations
 *
 * Type is resolved to KnowledgeAsset::formalType or KnowledgeAsset::role,
 * and matched against the KnowledgeAssetType and KnowledgeAssetRole
 * terminologies implemented by this server
 *
 * Annotation properties are resolved to KnowledgeAsset::getAnnotation (getSubject in Surrogate v1),
 * and matched against the AnnotationRelType terminology implemented by this server
 * Annotation values are expected to be URIs that denote Concepts in some Concept Scheme
 */
public class StaticFilter {

  private static final Logger logger = LoggerFactory.getLogger(StaticFilter.class);

  private StaticFilter() {
    // nothing to do here
  }

  /**
   * Filters the content of the asset repository based on type/role and annotations
   *
   * If no filter is specified, returns all the known assets
   *
   * If a type or an annotation (property and/or value) is specified, it will filter
   * accordingly. If both type and annotation are present, it will return the the IDs
   * of the assets that match both criteria
   *
   * @param assetTypeTag
   * @param assetAnnotationTag
   * @param assetAnnotationConcept
   * @param index
   * @return
   */
  public static Set<ResourceIdentifier> filter(String assetTypeTag, String assetAnnotationTag,
      String assetAnnotationConcept, Index index) {

    Optional<Set<ResourceIdentifier>> assetsByType =
        filterByType(assetTypeTag, index);
    Optional<Set<ResourceIdentifier>> assetsByAnnotation =
        filterByAnnotation(assetAnnotationTag, assetAnnotationConcept, index);

    Set<ResourceIdentifier> all = null;
    if (assetsByType.isPresent()) {
      all = assetsByType.get();
    }
    if (assetsByAnnotation.isPresent()) {
      all = (all == null)
          ? assetsByAnnotation.get()
          : Sets.intersection(all, assetsByAnnotation.get());
    }
    if (all == null) {
      all = index.getAllAssetIds();
    }
    return all;
  }

  private static Optional<Set<ResourceIdentifier>> filterByAnnotation(String assetAnnotationTag,
      String assetAnnotationConcept, Index index) {
    Optional<URI> annotationURI = resolveAnnotationTag(assetAnnotationTag);
    Optional<URI> annotationValueURI = URIUtil.asUri(assetAnnotationConcept);

    Set<ResourceIdentifier> filteredResources;
    if (annotationURI.isPresent() && annotationValueURI.isPresent()) {
      filteredResources = index
          .getAssetIdsByAnnotation(annotationURI.get(), annotationValueURI.get());
    } else if (annotationURI.isPresent()) {
      filteredResources = index.getAssetIdsByAnnotation(annotationURI.get());
    } else {
      filteredResources = annotationValueURI
          .map(index::getAssetIdsByAnnotationValue)
          .orElse(null);
    }
    return Optional.ofNullable(filteredResources);
  }

  private static Optional<Set<ResourceIdentifier>> filterByType(String assetTypeTag, Index index) {
    return resolveTypeTag(assetTypeTag)
        .map(index::getAssetIdsByType);
  }

  private static Optional<URI> resolveTypeTag(String assetTypeTag) {
    Optional<KnowledgeAssetType> type = KnowledgeAssetTypeSeries.resolve(assetTypeTag);
    if (type.isPresent()) {
      return type.map(ConceptTerm::getRef);
    }
    Optional<KnowledgeAssetRole> role = KnowledgeAssetRoleSeries.resolve(assetTypeTag);
    if (role.isPresent()) {
      return role.map(ConceptTerm::getRef);
    }
    logger.warn("Unable to resolve {} to a known type or role", assetTypeTag);
    return Optional.empty();
  }

  private static Optional<URI> resolveAnnotationTag(String annotationTag) {
    return AnnotationRelTypeSeries.resolve(annotationTag)
        .map(ConceptTerm::getRef);
  }

}
