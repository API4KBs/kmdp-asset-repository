package edu.mayo.kmdp.repository.asset.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Disabled.*;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultArtifactId;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Terminology_Ontology_And_Assertional_KBs;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype._20210401.KnowledgeAssetType.Formal_Ontology;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;

import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.repository.asset.SemanticRepoAPITestBase;
import edu.mayo.kmdp.util.ws.JsonRestWSUtils.WithFHIR;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetRepositoryApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.client.ApiClientFactory;
import org.omg.spec.api4kp._20200801.id.IdentifierConstants;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder;

@Disabled("Disabling due to break when running on build server in main pipeline.")
class ParallelInteractionTest extends SemanticRepoAPITestBase {

  private KnowledgeAssetCatalogApi ckac;
  private KnowledgeAssetRepositoryApi repo;

  @BeforeEach
  protected void init() {
    super.init();
    String url = "http://localhost:" + port;
//    String url = "http://localhost:8080/kar";
    ApiClientFactory webClientFactory = new ApiClientFactory(url, WithFHIR.NONE);

    ckac = KnowledgeAssetCatalogApi.newInstance(webClientFactory);
    repo = KnowledgeAssetRepositoryApi.newInstance(webClientFactory);

    ckac.clearKnowledgeAssetCatalog();
  }

  @AfterEach
  void cleanup() {
    ckac.clearKnowledgeAssetCatalog();
  }

  @Test
  void testWriteParallelThenRead() {
    ExecutorService execs = Executors.newFixedThreadPool(10);
    List<Future<Answer<Void>>> futures = Stream.iterate(0, n -> n + 1)
        .limit(10)
        .flatMap(i -> Stream.<Callable<Answer<Void>>>of(this::publishAsset))
        .map(execs::submit)
        .collect(Collectors.toList());

    List<Answer<Void>> writeResults = futures.stream()
        .map(this::collectResult)
        .collect(Collectors.toList());
    writeResults.forEach(ans -> assertTrue(ans != null && ans.isSuccess()));

    List<Pointer> assets = ckac.listKnowledgeAssets().orElseGet(Assertions::fail);

    List<Answer<KnowledgeCarrier>> readResults = assets.stream()
        .map(this::doRead)
        .collect(Collectors.toList());
    readResults.forEach(ans -> assertTrue(ans != null && ans.isSuccess()));
  }


  @Test
  void testStressTestWithReadAndWrite() {
    ExecutorService execs = Executors.newFixedThreadPool(10);

    // concurrent writes
    List<Future<Answer<Void>>> futureWrites = Stream.iterate(0, n -> n + 1)
        .limit(10)
        .flatMap(i -> Stream.<Callable<Answer<Void>>>of(this::publishAsset))
        .map(execs::submit)
        .collect(Collectors.toList());

    List<Answer<Void>> writeResults = futureWrites.stream()
        .map(this::collectResult)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    writeResults.forEach(ans -> assertTrue(ans != null && ans.isSuccess()));

    // many more concurrent reads
    List<Future<Answer<?>>> futureReads = Stream.iterate(0, n -> n + 1)
        .limit(100)
        .flatMap(i -> Stream.<Callable<Answer<?>>>of(this::doRandomRead))
        .map(execs::submit)
        .collect(Collectors.toList());

    List<Answer<?>> readResults = futureReads.stream()
        .map(this::collectResult)
        .collect(Collectors.toList());
    readResults.forEach(ans -> assertTrue(ans != null && ans.isSuccess()));

  }

  @Test
  void testMixedReadsAndWrites() {
    // ensure there is at least one asset for readers to pick up
    publishAsset();

    ExecutorService execs = Executors.newFixedThreadPool(10);

    // concurrent writes
    List<Future<Answer<?>>> futures = Stream.iterate(0, n -> n + 1)
        .limit(100)
        .flatMap(i -> Stream.<Callable<Answer<?>>>of(this::readOrWrite))
        .map(execs::submit)
        .collect(Collectors.toList());

    List<Answer<?>> results = futures.stream()
        .map(this::collectResult)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    assertEquals(100, results.size());
    results.forEach(ans -> assertTrue(ans != null && ans.isSuccess()));

  }

  private Answer<?> readOrWrite() {
    boolean writer = Math.random() < 0.3;
    return writer
        ? publishAsset()
        : doRandomRead();
  }

  private Answer<KnowledgeCarrier> doRandomRead() {
    return ckac.listKnowledgeAssets()
        .map(ptrs -> ptrs.get(new Random().nextInt(ptrs.size())))
        .flatMap(this::doRead);
  }

  private Answer<KnowledgeCarrier> doRead(Pointer p) {
    return ckac.getKnowledgeAsset(p.getUuid(), p.getVersionTag())
        .flatMap(ka -> repo.getKnowledgeAssetCanonicalCarrier(ka.getAssetId().getUuid()));
  }

  private <X> X collectResult(Future<X> future) {
    try {
      return future.get();
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
      fail(e);
      return null;
    }
  }


  protected Answer<Void> publishAsset() {
    ResourceIdentifier assetId = SemanticIdentifier.newId(
        Registry.MAYO_ASSETS_BASE_URI_URI,
        UUID.randomUUID(),
        IdentifierConstants.VERSION_ZERO);
    ResourceIdentifier artifactId = defaultArtifactId(assetId, HTML);
    String name = Thread.currentThread().getName();
    String content = "Content " + name;

    KnowledgeAsset ax = SurrogateBuilder.newSurrogate(assetId)
        .withName("Asset " + name, "")
        .withFormalType(Terminology_Ontology_And_Assertional_KBs, Formal_Ontology)
        .get()
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(artifactId)
            .withRepresentation(rep(HTML, TXT, Charset.defaultCharset())));

    Answer<Void> a1 = ckac.setKnowledgeAssetVersion(assetId.getUuid(), assetId.getVersionTag(), ax);
    Answer<Void> a2 = repo.setKnowledgeAssetCarrierVersion(
        assetId.getUuid(), assetId.getVersionTag(),
        artifactId.getUuid(), artifactId.getVersionTag(),
        content.getBytes(StandardCharsets.UTF_8));

    return Answer.merge(a1, a2);
  }

}
