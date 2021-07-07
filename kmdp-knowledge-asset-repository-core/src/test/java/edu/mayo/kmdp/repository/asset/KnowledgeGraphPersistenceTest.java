package edu.mayo.kmdp.repository.asset;

import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.id.IdentifierConstants.VERSION_ZERO;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomAssetId;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.rdf.JenaRdfParser;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder;

class KnowledgeGraphPersistenceTest extends RepositoryTestBase {

  private static final UUID GRAPH_UUID =
      kgHolder.getInfo().knowledgeGraphAssetId().getUuid();
  private static final UUID GRAPH_CARR_UUID =
      kgHolder.getInfo().knowledgeGraphArtifactId().getUuid();
  private static final UUID GRAPH_SURR_UUID =
      kgHolder.getInfo().knowledgeGraphSurrogateId().getUuid();

  @Test
  void testPersistCarriersOnly() {
    Answer<List<Pointer>> ptrs1 =
        artifactRepository.getKnowledgeArtifactSeries("default", GRAPH_UUID);
    Answer<List<Pointer>> ptrs2 =
        artifactRepository.getKnowledgeArtifactSeries("default", GRAPH_CARR_UUID);
    Answer<List<Pointer>> ptrs3 =
        artifactRepository.getKnowledgeArtifactSeries("default", GRAPH_SURR_UUID);
    assertTrue(ptrs1.isNotFound());
    assertTrue(ptrs2.isSuccess());
    assertTrue(ptrs3.isSuccess());

    assertEquals(1, ptrs2.get().size());
  }

  @Test
  void testEmptyOnStart() {
    semanticRepository.clearKnowledgeAssetCatalog();
    Model graph = readGraphFromArtifactRepo();
    assertEquals(kgHolder.getTBoxTriples().size(), graph.size());
  }

  @Test
  void testAddAssets() {
    ResourceIdentifier axId = randomAssetId();
    KnowledgeAsset asset = SurrogateBuilder.newSurrogate(axId).get();

    assertTrue(
        semanticRepository.clearKnowledgeAssetCatalog().isSuccess());
    assertTrue(
        semanticRepository.setKnowledgeAssetVersion(
            axId.getUuid(), axId.getVersionTag(), asset).isSuccess());

    KnowledgeCarrier kg = semanticRepository.getKnowledgeAssetCanonicalCarrier(GRAPH_UUID)
        .orElseGet(Assertions::fail);
    Model inMemory = new JenaRdfParser()
        .applyLift(kg, Abstract_Knowledge_Expression, codedRep(OWL_2), null)
        .flatOpt(x -> x.as(Model.class))
        .orElseGet(Assertions::fail);
    Model persisted = readGraphFromArtifactRepo();

    // test that the Graph obtained from the DB is identical to the one obtained from the API
    assertTrue(inMemory.containsAll(persisted));
    assertTrue(persisted.containsAll(inMemory));
    assertFalse(persisted.contains(createResource(axId.getVersionId().toString()), RDF.type));

    assertTrue(
        semanticRepository.setKnowledgeAssetCarrierVersion(
            GRAPH_UUID, null, null, null, null).isSuccess());
    Model persisted2 = readGraphFromArtifactRepo();

    //assertTrue(inMemory.containsAll(persisted2));
    assertTrue(persisted2.containsAll(inMemory));
    assertTrue(persisted2.contains(createResource(axId.getVersionId().toString()), RDF.type));
  }

  @Test
  void testAddAssetsWithDelayedWrite() {
    ResourceIdentifier axId = randomAssetId();
    KnowledgeAsset asset = SurrogateBuilder.newSurrogate(axId).get();

    assertTrue(
        semanticRepository.clearKnowledgeAssetCatalog().isSuccess());
    assertTrue(
        semanticRepository.setKnowledgeAssetVersion(
            axId.getUuid(), axId.getVersionTag(), asset).isSuccess());

    await()
        .atMost(10, TimeUnit.SECONDS)
        .pollDelay(1, TimeUnit.SECONDS)
        .until(() -> readGraphFromArtifactRepo()
            .contains(createResource(axId.getVersionId().toString()), RDF.type));
  }

  @Test
  void testWriteThenClear() {
    ResourceIdentifier axId =
        newId(UUID.fromString("b1493fcc-2cff-4402-98e6-75059feb0ed1"), VERSION_ZERO);
    KnowledgeAsset asset = SurrogateBuilder.newSurrogate(axId).get();
    assertTrue(
        semanticRepository.setKnowledgeAssetVersion(
            axId.getUuid(), axId.getVersionTag(), asset).isSuccess());

    semanticRepository.clearKnowledgeAssetCatalog();

    ResourceIdentifier axId2 =
        newId(UUID.fromString("397f7045-ca12-4611-89aa-87387b2d8940"), VERSION_ZERO);
    KnowledgeAsset asset2 = SurrogateBuilder.newSurrogate(axId2).get();
    assertTrue(
        semanticRepository.setKnowledgeAssetVersion(
            axId2.getUuid(), axId2.getVersionTag(), asset2).isSuccess());

    await()
        .atMost(10, TimeUnit.SECONDS)
        .pollDelay(1, TimeUnit.SECONDS)
        .until(() -> readGraphFromArtifactRepo()
            .contains(createResource(axId2.getVersionId().toString()), RDF.type));

    assertFalse(readGraphFromArtifactRepo()
        .contains(createResource(axId.getVersionId().toString()), RDF.type));
  }

  private Model readGraphFromArtifactRepo() {
    byte[] graphBinary =
        artifactRepository.getKnowledgeArtifactVersion("default", GRAPH_CARR_UUID, VERSION_ZERO)
            .orElseGet(Assertions::fail);
    Model graph = ModelFactory.createDefaultModel();
    graph.read(new ByteArrayInputStream(graphBinary), null);
    return graph;
  }


}
