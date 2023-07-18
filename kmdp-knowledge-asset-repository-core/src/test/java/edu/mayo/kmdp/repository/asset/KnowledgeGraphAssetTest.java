package edu.mayo.kmdp.repository.asset;

import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.Forbidden;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.id.IdentifierConstants.VERSION_ZERO;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Assertional_Knowledge;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.rdf.JenaRdfParser;
import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import java.util.List;
import java.util.UUID;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.repository.KnowledgeAssetCatalog;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

/**
 * This class tests the behavior of the Asset Repository implementation with respect to the 'well
 * known' Knowledge Asset Repository Graph Asset.
 * <p>
 * <p>
 * 17/06/2021: the behavior is specific to the implementation, not the APIs, . As/if the behavior
 * evolves, tests in this class may need to be adjusted
 */
class KnowledgeGraphAssetTest extends RepositoryTestBase {

  private static final UUID GRAPH_UUID = kgHolder.getInfo().knowledgeGraphAssetId().getUuid();

  private final JenaRdfParser parser = new JenaRdfParser();
  private final Surrogate2Parser metaParser = new Surrogate2Parser();

  private static final ResourceIdentifier graphArtifactId = kgHolder.getInfo()
      .knowledgeGraphArtifactId();
  private static final ResourceIdentifier graphSurrogateId = kgHolder.getInfo()
      .knowledgeGraphSurrogateId();

  @Test
  void testGraphAssetId() {
    KnowledgeAssetCatalog cat = semanticRepository.getKnowledgeAssetCatalog()
        .orElseGet(Assertions::fail);
    assertEquals(GRAPH_UUID, cat.getId().getUuid());
  }

  @Test
  void testGraphSurrogate() {
    KnowledgeAsset surr = semanticRepository.getKnowledgeAsset(GRAPH_UUID)
        .orElseGet(Assertions::fail);
    assertEquals(GRAPH_UUID, surr.getAssetId().getUuid());
    assertTrue(Assertional_Knowledge.isAnyOf(surr.getFormalType()));
  }

  @Test
  void testGraphSurrogateAfterClear() {
    semanticRepository.clearKnowledgeAssetCatalog();

    KnowledgeAsset surr = semanticRepository.getKnowledgeAsset(GRAPH_UUID)
        .orElseGet(Assertions::fail);
    assertEquals(GRAPH_UUID, surr.getAssetId().getUuid());
    assertTrue(Assertional_Knowledge.isAnyOf(surr.getFormalType()));
  }

  @Test
  void testGraphSurrogateAfterDeleteAll() {
    semanticRepository.deleteKnowledgeAssets();

    KnowledgeAsset surr = semanticRepository.getKnowledgeAsset(GRAPH_UUID)
        .orElseGet(Assertions::fail);
    assertEquals(GRAPH_UUID, surr.getAssetId().getUuid());
    assertTrue(Assertional_Knowledge.isAnyOf(surr.getFormalType()));
  }

  @Test
  void testGraphSurrogateVersion() {
    KnowledgeAsset surr = semanticRepository.getKnowledgeAssetVersion(GRAPH_UUID, VERSION_ZERO)
        .orElseGet(Assertions::fail);
    assertEquals(GRAPH_UUID, surr.getAssetId().getUuid());
    assertEquals(VERSION_ZERO, surr.getAssetId().getVersionTag());

    semanticRepository.getKnowledgeAssetVersion(GRAPH_UUID, VERSION_ZERO)
        .orElseGet(Assertions::fail);
  }

  @Test
  void testListAssetsNotIncludeGraph() {
    List<Pointer> ptrs = semanticRepository.listKnowledgeAssets()
        .orElseGet(Assertions::fail);
    assertTrue(ptrs.stream().noneMatch(ptr -> GRAPH_UUID.equals(ptr.getUuid())));
  }

  @Test
  void testCannotDeleteKnowledgeGraph() {
    Answer<Void> ans = semanticRepository.deleteKnowledgeAsset(GRAPH_UUID);
    checkForbidden(ans);

    Answer<Void> ans2 = semanticRepository.deleteKnowledgeAssetVersion(GRAPH_UUID, VERSION_ZERO);
    checkForbidden(ans2);
  }

  @Test
  void testGraphSurrogateVersions() {
    List<Pointer> surr = semanticRepository.listKnowledgeAssetVersions(GRAPH_UUID)
        .orElseGet(Assertions::fail);
    assertEquals(1, surr.size());
    assertEquals(GRAPH_UUID, surr.get(0).getUuid());
    assertEquals(VERSION_ZERO, surr.get(0).getVersionTag());
  }

  @Test
  void testCannotSetKnowledgeGraph() {
    Answer<Void> ans = semanticRepository.setKnowledgeAssetVersion(
        GRAPH_UUID, "na", new KnowledgeAsset());
    checkForbidden(ans);
  }

  @Test
  void testDefaultCarrier() {
    KnowledgeCarrier kc = semanticRepository.getKnowledgeAssetCanonicalCarrier(GRAPH_UUID)
        .flatMap(x -> parser.applyLift(x, Abstract_Knowledge_Expression, codedRep(OWL_2), null))
        .orElseGet(Assertions::fail);
    assertTrue(kc.as(Model.class).isPresent());
  }

  @Test
  void testGraphDefaultCarrierContent() {
    byte[] binary = semanticRepository.getKnowledgeAssetCanonicalCarrierContent(GRAPH_UUID)
        .orElseGet(Assertions::fail);
    String str = new String(binary);
    str = str.substring(str.indexOf('\n') + 1);
    assertTrue(str.startsWith("<rdf:RDF"));
  }

  @Test
  void testGraphDefaultCarrierContentVariant() {
    byte[] binary = semanticRepository.getKnowledgeGraphContent("text/turtle")
        .orElseGet(Assertions::fail);
    String str = new String(binary);
    System.out.println(str);
    assertFalse(str.contains("<rdf:RDF"));
  }

  @Test
  void testGraphDefaultSurrogate() {
    KnowledgeCarrier kc = semanticRepository.getKnowledgeAssetCanonicalSurrogate(GRAPH_UUID)
        .flatMap(x -> metaParser
            .applyLift(x, Abstract_Knowledge_Expression, codedRep(Knowledge_Asset_Surrogate_2_0),
                null))
        .orElseGet(Assertions::fail);
    assertTrue(kc.as(KnowledgeAsset.class).isPresent());
    assertEquals(GRAPH_UUID, kc.as(KnowledgeAsset.class).get().getAssetId().getUuid());
  }


  @Test
  void testGraphVersionCanonicalCarrier() {
    KnowledgeCarrier kc = semanticRepository
        .getKnowledgeAssetVersionCanonicalCarrier(GRAPH_UUID, VERSION_ZERO)
        .flatMap(x -> parser.applyLift(x, Abstract_Knowledge_Expression, codedRep(OWL_2), null))
        .orElseGet(Assertions::fail);
    assertTrue(kc.as(Model.class).isPresent());

    KnowledgeCarrier kc2 = semanticRepository
        .getKnowledgeAssetVersionCanonicalCarrier(GRAPH_UUID, "na")
        .flatMap(x -> parser.applyLift(x, Abstract_Knowledge_Expression, codedRep(OWL_2), null))
        .orElseGet(Assertions::fail);
    assertTrue(kc2.as(Model.class).isPresent());
  }

  @Test
  void testGraphVersionCarrierContent() {
    byte[] binary = semanticRepository
        .getKnowledgeAssetVersionCanonicalCarrierContent(GRAPH_UUID, VERSION_ZERO)
        .orElseGet(Assertions::fail);
    String str = new String(binary);
    str = str.substring(str.indexOf('\n') + 1);
    assertTrue(str.startsWith("<rdf:RDF"));
  }

  @Test
  void testListGraphAssetCarriers() {
    List<Pointer> ptrs = semanticRepository.listKnowledgeAssetCarriers(GRAPH_UUID, VERSION_ZERO)
        .orElseGet(Assertions::fail);
    assertEquals(1, ptrs.size());
    assertEquals(graphArtifactId.asKey(), ptrs.get(0).asKey());
  }

  @Test
  void testAddGraphAssetCarriers() {
    Answer<Void> ans = semanticRepository.addKnowledgeAssetCarrier(GRAPH_UUID, VERSION_ZERO, null);
    checkForbidden(ans);
  }

  @Test
  void testGraphVersionCarrierVersion() {
    KnowledgeCarrier kc = semanticRepository.getKnowledgeAssetCarrierVersion(
        GRAPH_UUID,
        VERSION_ZERO,
        graphArtifactId.getUuid(),
        graphArtifactId.getVersionTag())
        .flatMap(x -> parser.applyLift(x, Abstract_Knowledge_Expression, codedRep(OWL_2), null))
        .orElseGet(Assertions::fail);
    assertTrue(kc.as(Model.class).isPresent());
  }

  @Test
  void testGraphVersionCarrier() {
    KnowledgeCarrier kc = semanticRepository.getKnowledgeAssetCarrier(
        GRAPH_UUID,
        VERSION_ZERO,
        graphArtifactId.getUuid())
        .flatMap(x -> parser.applyLift(x, Abstract_Knowledge_Expression, codedRep(OWL_2), null))
        .orElseGet(Assertions::fail);
    assertTrue(kc.as(Model.class).isPresent());
  }

  @Test
  void testListGraphAssetCarrierVersions() {
    List<Pointer> ptrs = semanticRepository.listKnowledgeAssetCarrierVersions(
        GRAPH_UUID, VERSION_ZERO, graphArtifactId.getUuid())
        .orElseGet(Assertions::fail);
    assertEquals(1, ptrs.size());
    assertEquals(graphArtifactId.asKey(), ptrs.get(0).asKey());
  }


  @Test
  void testGraphAssetVersionCarrierVersionContent() {
    byte[] binary = semanticRepository.getKnowledgeAssetCarrierVersionContent(
        GRAPH_UUID, "na", graphArtifactId.getUuid(), graphArtifactId.getVersionTag())
        .orElseGet(Assertions::fail);
    String str = new String(binary);
    str = str.substring(str.indexOf('\n') + 1);
    assertTrue(str.startsWith("<rdf:RDF"));

    byte[] binary2 = semanticRepository.getKnowledgeAssetCarrierVersionContent(
        GRAPH_UUID, "na", graphArtifactId.getUuid(), VERSION_ZERO)
        .orElseGet(Assertions::fail);
    String str2 = new String(binary2);
    str2 = str2.substring(str2.indexOf('\n') + 1);
    assertTrue(str2.startsWith("<rdf:RDF"));
  }

  @Test
  void testCompositeOperations() {
    Answer<?> ans;
    ans = semanticRepository.getAnonymousCompositeKnowledgeAssetCarrier(GRAPH_UUID, null);
    checkForbidden(ans);

    ans = semanticRepository.getAnonymousCompositeKnowledgeAssetSurrogate(GRAPH_UUID, null);
    checkForbidden(ans);

    ans = semanticRepository.getAnonymousCompositeKnowledgeAssetStructure(GRAPH_UUID, null);
    checkForbidden(ans);

    ans = semanticRepository.getCompositeKnowledgeAssetCarrier(GRAPH_UUID, null);
    checkForbidden(ans);

    ans = semanticRepository.getCompositeKnowledgeAssetSurrogate(GRAPH_UUID, null);
    checkForbidden(ans);

    ans = semanticRepository.getCompositeKnowledgeAssetStructure(GRAPH_UUID, null);
    checkForbidden(ans);
  }

  @Test
  void testGraphVersionSurrogateVersion() {
    Answer<KnowledgeCarrier> ans = semanticRepository.getKnowledgeAssetSurrogateVersion(
        GRAPH_UUID,
        VERSION_ZERO,
        graphSurrogateId.getUuid(),
        graphSurrogateId.getVersionTag());
    assertTrue(ans.isSuccess());
    assertTrue(Encoded_Knowledge_Expression.sameAs(ans.get().getLevel()));

    KnowledgeCarrier kc = ans.flatMap(x -> metaParser
        .applyLift(x, Abstract_Knowledge_Expression, codedRep(Knowledge_Asset_Surrogate_2_0), null))
        .orElseGet(Assertions::fail);
    assertTrue(kc.as(KnowledgeAsset.class).isPresent());
  }

  @Test
  void testGraphVersionSurrogate() {
    KnowledgeCarrier kc = semanticRepository.getKnowledgeAssetVersionCanonicalSurrogate(
        GRAPH_UUID,
        VERSION_ZERO)
        .flatMap(x -> metaParser
            .applyLift(x, Abstract_Knowledge_Expression, codedRep(Knowledge_Asset_Surrogate_2_0),
                null))
        .orElseGet(Assertions::fail);
    assertTrue(kc.as(KnowledgeAsset.class).isPresent());
  }

  @Test
  void testListGraphAssetSurrogates() {
    List<Pointer> ptrs = semanticRepository.listKnowledgeAssetSurrogates(GRAPH_UUID, VERSION_ZERO)
        .orElseGet(Assertions::fail);
    assertEquals(1, ptrs.size());
    assertEquals(graphSurrogateId.asKey(), ptrs.get(0).asKey());
  }

  @Test
  void testListGraphAssetSurrogateVersions() {
    List<Pointer> ptrs = semanticRepository.listKnowledgeAssetSurrogateVersions(
        GRAPH_UUID, VERSION_ZERO, graphSurrogateId.getUuid())
        .orElseGet(Assertions::fail);
    assertEquals(1, ptrs.size());
    assertEquals(graphSurrogateId.asKey(), ptrs.get(0).asKey());
  }

  @Test
  void testAddSurrogate() {
    Answer<Void> ans = semanticRepository.addKnowledgeAssetSurrogate(
        GRAPH_UUID, VERSION_ZERO, new KnowledgeCarrier());
    checkForbidden(ans);
  }

  @Test
  void testAddCanonicalSurrogate() {
    Answer<Void> ans = semanticRepository.addCanonicalKnowledgeAssetSurrogate(
        GRAPH_UUID, VERSION_ZERO, new KnowledgeCarrier());
    checkForbidden(ans);
  }

  private void checkForbidden(Answer<?> ans) {
    assertTrue(Forbidden.sameAs(ans.getOutcomeType()));
  }


}
