package edu.mayo.kmdp;

import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype._20210401.SemanticAnnotationRelType.Defines;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Service_Profile;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeprocessingtechnique.KnowledgeProcessingTechniqueSeries.Query_Technique;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.CQL_Essentials;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;

import edu.mayo.kmdp.kbase.query.sparql.v1_1.JenaQuery;
import edu.mayo.kmdp.knowledgebase.introspectors.fhir.stu3.StructureDefinitionMetadataIntrospector;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryService;
import edu.mayo.kmdp.repository.asset.glossary.KARSGraphCCGL;
import edu.mayo.kmdp.terms.TermsFHIRFacade;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetRepositoryApi;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries;

class GLTests {

  static KnowledgeAssetRepositoryService repo =
      KnowledgeAssetRepositoryService.mockTestRepository();

  static final String MOCK_EXPR = "This is a test";
  static final String MOCK_EXPR2 = "<p>This is another quasi-HTML test</p>";

  @BeforeAll
  static void init() {
    var struct = StructureDefinitionMetadataIntrospector.buildFhir3Resource(
        URI.create("urn:mock:assets:"),
        URI.create("urn:mock:artifacts:"),
        "Observation");

    var assetId = newId(Util.uuid("aaaa"), "1.0.0");
    var artifactIda = newId(Util.uuid("bbbb"), "2.0.0");
    var artifactIdb = newId(Util.uuid("cccc"), "3.0.0");
    var asset = new KnowledgeAsset()
        .withName("Test")
        .withAssetId(assetId)
        .withProcessingMethod(Query_Technique)
        .withAnnotation(new Annotation()
            .withRel(Defines.asConceptIdentifier())
            .withRef(Term.newTerm(URI.create("http://mock.term/foo")).asConceptIdentifier()))
        .withMemberOf(newId("MOCK-COLL"))
        .withFormalType(Service_Profile)
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(artifactIda)
            .withRepresentation(rep(HTML, TXT, Charset.defaultCharset()))
            .withInlinedExpression(MOCK_EXPR2))
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(artifactIdb)
            .withRepresentation(rep(CQL_Essentials, TXT, Charset.defaultCharset()))
            .withInlinedExpression(MOCK_EXPR))
        .withLinks(new Dependency()
            .withRel(DependencyTypeSeries.Effectuates)
            .withHref(struct.getAssetId()));

    var ans1 = repo.setKnowledgeAssetVersion(assetId.getUuid(), assetId.getVersionTag(), asset);
    assertTrue(ans1.isSuccess());

    var ans2 = repo.setKnowledgeAssetVersion
        (struct.getAssetId().getUuid(), struct.getAssetId().getVersionTag(), struct);
    assertTrue(ans2.isSuccess());
  }

  @Test
  void testLibrary() {
    var glossary = new KARSGraphCCGL(
        repo,
        repo.getInnerArtifactRepository(),
        new TermsFHIRFacade(KnowledgeAssetCatalogApi.newInstance(repo),
            KnowledgeAssetRepositoryApi.newInstance(repo)));

    var glossaries = glossary.listGlossaries()
        .orElseGet(Assertions::fail);
    assertEquals(1, glossaries.size());
    assertTrue(glossaries.stream().anyMatch(gl -> "MOCK-COLL".equals(gl.getGlossaryId())));
  }

  @Test
  void testEntries() {
    var glossary = new KARSGraphCCGL(
        repo,
        repo.getInnerArtifactRepository(),
        new TermsFHIRFacade(KnowledgeAssetCatalogApi.newInstance(repo),
            KnowledgeAssetRepositoryApi.newInstance(repo)));
    var entries = glossary.listGlossaryEntries("MOCK-COLL")
        .orElseGet(Assertions::fail);
    assertEquals(1, entries.size());
    var entry = entries.get(0);

    assertEquals("This is a test",
        entry.getDef().get(0).getComputableSpec().getInlinedExpr());

  }

  @Test
  void testGraphInlined() {
    var qry = JenaQuery.wholeGraph();
    var triples = repo.queryKnowledgeAssetGraph(qry)
        .orElseGet(Assertions::fail);
    assertTrue(triples.stream()
        .map(b -> b.get("o"))
        .filter(Objects::nonNull)
        .anyMatch(MOCK_EXPR::equals));
    assertTrue(triples.stream()
        .map(b -> b.get("o"))
        .filter(Objects::nonNull)
        .noneMatch(MOCK_EXPR2::equals));
  }



}
