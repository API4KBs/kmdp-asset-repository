package edu.mayo.kmdp.repository.asset.index;

import static edu.mayo.kmdp.util.Util.isEmpty;
import static edu.mayo.kmdp.util.Util.isNotEmpty;

import com.google.common.collect.Sets;
import edu.mayo.kmdp.util.URIUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRole;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRoleSeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetType;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.terms.ConceptTerm;
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
   * @param assetTypeTag the tag of an Asset type or role
   * @param assetAnnotationTag the tag of a semantic Asset / Concept relationship type
   * @param assetAnnotationConcept the tag of a related Concept
   * @param index the Index (built on top of a queryable Knowledge Graph)
   * @return the Identifiers of the Assets that match the filtering criteria
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


  /**
   * Sub-filter by annotation
   *
   * @param assetAnnotationTag the tag of a semantic Asset / Concept relationship type
   * @param assetAnnotationConcept the tag of a related Concept
   * @param index the Index (built on top of a queryable Knowledge Graph)
   * @return the Identifiers of the Assets that match the filtering criteria
   */
  private static Optional<Set<ResourceIdentifier>> filterByAnnotation(String assetAnnotationTag,
      String assetAnnotationConcept, Index index) {
    if (Util.isEmpty(assetAnnotationConcept) && Util.isEmpty(assetAnnotationTag)) {
      // no annotation filter -> empty
      return Optional.empty();
    }

    Optional<URI> annotationURI = resolveAnnotationTag(assetAnnotationTag);
    Optional<URI> annotationValueURI = URIUtil.asURI(assetAnnotationConcept);

    Set<ResourceIdentifier> filteredResources;
    if (annotationURI.isPresent() && annotationValueURI.isPresent()) {
      // rel/ann filter -> restricted set
      filteredResources = index
          .getAssetIdsByAnnotation(annotationURI.get(), annotationValueURI.get());
    } else if (annotationURI.isPresent()) {
      // rel filter -> restricted set
      filteredResources = index.getAssetIdsByAnnotation(annotationURI.get());
    } else if (annotationValueURI.isPresent()) {
      // ann filter -> restricted set
      filteredResources = index.getAssetIdsByAnnotationValue(annotationValueURI.get());
    } else {
      // unknown filter -> empty set
      filteredResources = Collections.emptySet();
    }
    return Optional.ofNullable(filteredResources);
  }


  /**
   * Sub-filter by asset type/role
   *
   * @param assetTypeTag the tag of an Asset type or role
   * @param index the Index (built on top of a queryable Knowledge Graph)
   * @return the Identifiers of the Assets that match the filtering criteria
   */
  private static Optional<Set<ResourceIdentifier>> filterByType(String assetTypeTag, Index index) {
    return isNotEmpty(assetTypeTag)
        ? Optional.of(
            // type filter -> restricted set
            resolveTypeOrRoleTag(assetTypeTag)
                .map(index::getAssetIdsByType)
                // unknown type filter -> empty set
                .orElse(Collections.emptySet()))
        // no type filter -> empty
        : Optional.empty();
  }

  /**
   * Tries to resolve an Asset type/role tag using the supported Ontologies
   * @param assetTypeTag the Tag to be resolved
   * @return the URI of the concept's referent, as defined in an Asset Type ontology
   */
  private static Optional<URI> resolveTypeOrRoleTag(String assetTypeTag) {
    if (Util.isEmpty(assetTypeTag)) {
      return Optional.empty();
    }
    Optional<KnowledgeAssetType> type = resolveAssetTypeTag(assetTypeTag);
    if (type.isPresent()) {
      return type.map(ConceptTerm::getReferentId);
    }
    Optional<KnowledgeAssetRole> role = KnowledgeAssetRoleSeries.resolve(assetTypeTag);
    if (role.isPresent()) {
      return role.map(ConceptTerm::getConceptId);
    }
    logger.warn("Unable to resolve {} to a known type or role", assetTypeTag);
    return Optional.empty();
  }

  /**
   * Tries to resolve an Annotation Tag using the supported Ontologies
   * @param annotationTag the annotation Tag to be resolved
   * @return the URI of the concept's referent, as defined in an annotation ontology
   */
  private static Optional<URI> resolveAnnotationTag(String annotationTag) {
    return SemanticAnnotationRelTypeSeries.resolve(annotationTag)
        .map(ConceptTerm::getReferentId);
  }

  /**
   * Determines the 'primary' asset type for an Asset with multiple types/roles,
   * to match a client's request.
   * </p>
   * If the client's provided type (tag) matches one of the asset types, that type is
   * returned.
   * Asset types are preferred over Asset Roles
   *
   * @param assetTypes the types/roles of an Asset
   * @param assetTypeTag a client-provided type
   * @return the primary Asset type
   */
  public static Optional<ConceptIdentifier> choosePrimaryType(
      List<ConceptIdentifier> assetTypes, String assetTypeTag) {
    return resolveAssetTypes(assetTypes, assetTypeTag)
          .max(Comparator.comparing(c -> c.getAncestors().length))
          .map(ConceptTerm::asConceptIdentifier)
        .or(() -> assetTypes.stream().findFirst());
  }

  private static Stream<KnowledgeAssetType> resolveAssetTypes(List<ConceptIdentifier> assetTypes,
      String assetTypeTag) {
    return assetTypes.stream()
        .flatMap(cid -> resolveAssetTypeTag(cid.getTag()).stream())
        .filter(c -> isEmpty(assetTypeTag) || assetTypeTag.equals(c.getTag()));
  }

  private static Optional<KnowledgeAssetType> resolveAssetTypeTag(String assetTypeTag) {
    return KnowledgeAssetTypeSeries.resolve(assetTypeTag)
        // the taxonomy is bugged - OCD should not be in KAT. This will be fixed in API4KP1.0
        .filter(k -> !KnowledgeAssetTypeSeries.Operational_Concept_Definition.sameAs(k))
        .or(() -> ClinicalKnowledgeAssetTypeSeries.resolve(assetTypeTag));
  }
}
