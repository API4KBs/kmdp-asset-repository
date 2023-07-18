package edu.mayo.kmdp.repository.asset;

import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Defines;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.In_Terms_Of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Imports;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Assessment_Predictive_And_Inferential_Models;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Rules_Policies_And_Guidelines;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Terminology_Ontology_And_Assertional_KBs;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Decision_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Service_Profile;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Value_Set;

import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder;

class LinkedRelationshipTest extends RepositoryTestBase {

  ResourceIdentifier a1 = SurrogateBuilder.randomAssetId();
  ResourceIdentifier a2 = SurrogateBuilder.randomAssetId();
  ResourceIdentifier a3 = SurrogateBuilder.randomAssetId();

  @Test
  void testLinks() {
    populateWithLinkedAssets();
    assertEquals(3,
        semanticRepository.listKnowledgeAssets().orElseGet(Assertions::fail).size());

    var s1 = semanticRepository.getKnowledgeAsset(a1.getUuid())
        .orElseGet(Assertions::fail);
    var s2 = semanticRepository.getKnowledgeAsset(a2.getUuid())
        .orElseGet(Assertions::fail);
    var s3 = semanticRepository.getKnowledgeAsset(a3.getUuid())
        .orElseGet(Assertions::fail);

    // defined/in-terms-of is only instantiated backwards
    assertTrue(s1.getLinks().isEmpty());

    assertEquals(2, s2.getLinks().size());
    assertTrue(s2.getLinks().stream()
        .anyMatch(l -> Objects.equals(l.getHref().asKey(), a1.asKey())));
    assertTrue(s2.getLinks().stream()
        .anyMatch(l -> Objects.equals(l.getHref().asKey(), a3.asKey())));

    assertEquals(1, s3.getLinks().size());
    assertTrue(s3.getLinks().stream()
        .anyMatch(l -> Objects.equals(l.getHref().asKey(), a2.asKey())));

    semanticRepository.clearKnowledgeAssetCatalog();
  }

  private void populateWithLinkedAssets() {
    var mockDm = new KnowledgeAsset()
        .withAssetId(a1)
        .withFormalCategory(Assessment_Predictive_And_Inferential_Models)
        .withFormalType(Decision_Model)
        .withName("Mock Decision")
        .withAnnotation(new Annotation()
            .withRef(Term.mock("c1", "1234-5").asConceptIdentifier())
            .withRel(In_Terms_Of.asConceptIdentifier()));
    var mockSP = new KnowledgeAsset()
        .withAssetId(a2)
        .withFormalCategory(Rules_Policies_And_Guidelines)
        .withFormalType(Service_Profile)
        .withName("Mock Query")
        .withAnnotation(new Annotation()
            .withRef(Term.mock("c1", "1234-5").asConceptIdentifier())
            .withRel(Defines.asConceptIdentifier()))
        .withLinks(new Dependency()
            .withHref(a3)
            .withRel(Imports));
    var mockVs = new KnowledgeAsset()
        .withAssetId(a3)
        .withFormalCategory(Terminology_Ontology_And_Assertional_KBs)
        .withFormalType(Value_Set)
        .withName("Mock VS");

    semanticRepository.setKnowledgeAssetVersion(a1.getUuid(), a1.getVersionTag(), mockDm);
    semanticRepository.setKnowledgeAssetVersion(a2.getUuid(), a2.getVersionTag(), mockSP);
    semanticRepository.setKnowledgeAssetVersion(a3.getUuid(), a3.getVersionTag(), mockVs);
  }

}
