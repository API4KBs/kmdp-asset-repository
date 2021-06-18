package edu.mayo.kmdp.repository.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.id.IdentifierConstants.VERSION_ZERO;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomAssetId;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder;

class KnowledgeGraphPersistenceTest extends RepositoryTestBase {

  private static final UUID GRAPH_UUID =
      kgHolder.getKnowledgeGraphAssetId().getUuid();
  private static final UUID GRAPH_CARR_UUID =
      kgHolder.getKnowledgeGraphArtifactId().getUuid();
  private static final UUID GRAPH_SURR_UUID =
      kgHolder.getKnowledgeGraphSurrogateId().getUuid();

  @Test
  void testPersistCarriersOnly() {
    Answer<List<Pointer>> ptrs1 =
        repos.getKnowledgeArtifactSeries("default", GRAPH_UUID);
    Answer<List<Pointer>> ptrs2 =
        repos.getKnowledgeArtifactSeries("default", GRAPH_CARR_UUID);
    Answer<List<Pointer>> ptrs3 =
        repos.getKnowledgeArtifactSeries("default", GRAPH_SURR_UUID);
    assertTrue(ptrs1.isNotFound());
    assertTrue(ptrs2.isSuccess());
    assertTrue(ptrs3.isNotFound());

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
    Model inMemory = kg.as(Model.class)
        .orElseGet(Assertions::fail);
    Model persisted = readGraphFromArtifactRepo();

    assertTrue(inMemory.containsAll(persisted));
    assertFalse(persisted.containsAll(inMemory));

    assertTrue(
        semanticRepository.setKnowledgeAssetCarrierVersion(
            GRAPH_UUID, null, null, null, null).isSuccess());
    Model persisted2 = readGraphFromArtifactRepo();

    assertTrue(inMemory.containsAll(persisted2));
    assertTrue(persisted2.containsAll(inMemory));
  }


  private Model readGraphFromArtifactRepo() {
    byte[] graphBinary =
        repos.getKnowledgeArtifactVersion("default", GRAPH_CARR_UUID, VERSION_ZERO)
            .orElseGet(Assertions::fail);
    Model graph = ModelFactory.createDefaultModel();
    graph.read(new ByteArrayInputStream(graphBinary), null);
    return graph;
  }


}
