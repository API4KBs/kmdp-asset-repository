/*
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.repository.asset.catalog;

import static edu.mayo.kmdp.registry.Registry.BASE_UUID_URN_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.assetId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultSurrogateUUID;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomArtifactId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomAssetId;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Care_Process_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Rule;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Decision_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Predictive_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeprocessingtechnique.KnowledgeProcessingTechniqueSeries.Qualitative_Technique;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;

import edu.mayo.kmdp.repository.asset.SemanticRepoAPITestBase;
import edu.mayo.kmdp.util.ws.JsonRestWSUtils.WithFHIR;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetRepositoryApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.client.ApiClientFactory;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.surrogate.Applicability;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder;

class SemanticRepositoryIntegrationTest extends SemanticRepoAPITestBase {

  private KnowledgeAssetCatalogApi ckac;
  private KnowledgeAssetRepositoryApi repo;

  @BeforeEach
  protected void init() {
    super.init();
    ApiClientFactory webClientFactory = new ApiClientFactory("http://localhost:" + port,
        WithFHIR.NONE);

    ckac = KnowledgeAssetCatalogApi.newInstance(webClientFactory);
    repo = KnowledgeAssetRepositoryApi.newInstance(webClientFactory);
  }

  @Test
  void testListKnowledgeAssetsType() {
    ResourceIdentifier assetId = randomAssetId();
    ckac.setKnowledgeAssetVersion(assetId.getUuid(), assetId.getVersionTag(),
        new KnowledgeAsset()
            .withAssetId(assetId)
            .withFormalType(Decision_Model));

    List<Pointer> pointers = ckac
        .listKnowledgeAssets(Decision_Model.getTag(), null, null, -1, -1)
        .orElse(Collections.emptyList());

    assertEquals(1, pointers.size());
  }

  @Test
  void testListKnowledgeAssetsBadType() {
    ResourceIdentifier assetId = randomAssetId();
    ckac.setKnowledgeAssetVersion(assetId.getUuid(), assetId.getVersionTag(),
        new KnowledgeAsset()
            .withAssetId(assetId)
            .withFormalType(Care_Process_Model));

    List<Pointer> pointers = ckac.listKnowledgeAssets(Predictive_Model.getTag(),
        null, null, -1, -1)
        .orElse(Collections.emptyList());

    assertEquals(0, pointers.size());
  }

  @Test
  void testListKnowledgeAssetsNoType() {
    Answer<List<Pointer>> zero = ckac.listKnowledgeAssets();
    assertTrue(zero.isSuccess());

    ResourceIdentifier rid1 = randomAssetId();
    ckac.setKnowledgeAssetVersion(rid1.getUuid(), rid1.getVersionTag(),
        new KnowledgeAsset()
            .withAssetId(rid1)
            .withFormalType(Care_Process_Model));

    ResourceIdentifier rid2 = randomAssetId();
    ckac.setKnowledgeAssetVersion(rid2.getUuid(), rid2.getVersionTag(),
        new KnowledgeAsset()
            .withAssetId(rid2)
            .withFormalType(Clinical_Rule));

    Answer<List<Pointer>> ans = ckac.listKnowledgeAssets();
    List<Pointer> pointers = ans
        .orElse(Collections.emptyList());

    assertTrue(pointers.stream().anyMatch(p -> p.getResourceId().toString()
        .contains(rid1.getUuid().toString())));
    assertTrue(pointers.stream().anyMatch(p -> p.getHref().toString()
        .contains(rid2.getUuid().toString())));
  }

  @Test
  void testGeKnowledgeAssetsVersions() {
    UUID uid = UUID.randomUUID();
    ResourceIdentifier rid1 = assetId(testAssetNS(), uid, "1.0.0");
    ckac.setKnowledgeAssetVersion(rid1.getUuid(), rid1.getVersionTag(),
        new KnowledgeAsset().withAssetId(rid1));

    ResourceIdentifier rid2 = assetId(testAssetNS(), uid, "2.0.0");
    ckac.setKnowledgeAssetVersion(rid2.getUuid(), rid2.getVersionTag(),
        new KnowledgeAsset().withAssetId(rid2));

    List<Pointer> pointers = ckac.listKnowledgeAssets()
        .orElse(Collections.emptyList())
        .stream()
        .filter(p -> p.getHref().toString()
            .contains("assets/" + uid))
        .collect(Collectors.toList());

    assertEquals(1, pointers.size());
  }

  @Test
  void testGetLatestKnowledgeAsset() {
    UUID uid = UUID.randomUUID();

    ResourceIdentifier rid2 = assetId(testAssetNS(), uid, "2.0.0");
    ckac.setKnowledgeAssetVersion(rid2.getUuid(), rid2.getVersionTag(),
        new KnowledgeAsset().withAssetId(rid2));

    ResourceIdentifier rid1 = assetId(testAssetNS(), uid, "1.0.0");
    ckac.setKnowledgeAssetVersion(rid1.getUuid(), rid1.getVersionTag(),
        new KnowledgeAsset().withAssetId(rid1));

    Answer<KnowledgeAsset> axx = ckac.getKnowledgeAsset(uid, null);
    assertTrue(axx.isSuccess());
    assertEquals("2.0.0", axx.get().getAssetId().getVersionTag());
  }

  @Test
  void testGetLatestKnowledgeAssetHasCorrectId() {
    UUID uid = UUID.randomUUID();
    ResourceIdentifier rid1 = newId(uid, "1.0.0");
    ckac.setKnowledgeAssetVersion(rid1.getUuid(), rid1.getVersionTag(),
        new KnowledgeAsset().withAssetId(rid1));

    ResourceIdentifier rid2 = newId(uid, "2.0.0");
    ckac.setKnowledgeAssetVersion(rid2.getUuid(), rid2.getVersionTag(),
        new KnowledgeAsset().withAssetId(rid2));

    String id = ckac.getKnowledgeAsset(uid)
        .map(KnowledgeAsset::getAssetId)
        .map(ResourceIdentifier::getTag)
        .orElseGet(Assertions::fail);

    assertEquals(uid.toString(), id);
  }

  @Test
  void testSurrogateWithApplicability() {
    ResourceIdentifier assetId = randomAssetId();
    ckac.setKnowledgeAssetVersion(assetId.getUuid(), assetId.getVersionTag(),
        new KnowledgeAsset()
            .withAssetId(assetId)
            .withApplicableIn(new Applicability()
                .withSituation(Qualitative_Technique.asConceptIdentifier()))
    );
    KnowledgeAsset ax = ckac.getKnowledgeAsset(assetId.getUuid())
        .orElseGet(Assertions::fail);

    assertNotNull(ax);
    assertNotNull(ax.getApplicableIn());

    Term t = ax.getApplicableIn().getSituation().get(0);
    assertNotNull(t);
    assertEquals(Qualitative_Technique.getUuid(), t.getUuid());
  }

  @Test
  void testIdempotentPublishWithImplicitSurrogate() {
    KnowledgeAsset asset = new KnowledgeAsset()
        .withAssetId(randomAssetId())
        .withName("Test");
    UUID uuid = asset.getAssetId().getUuid();
    String vTag = asset.getAssetId().getVersionTag();

    Answer<Void> ans1 = ckac.setKnowledgeAssetVersion(
        uuid,
        vTag,
        asset);
    assertTrue(ans1.isSuccess());

    Answer<Void> ans2 = ckac.setKnowledgeAssetVersion(
        uuid,
        vTag,
        asset);
    assertTrue(ans2.isSuccess());

    ResourceIdentifier surrId = SurrogateBuilder.defaultSurrogateId(
        BASE_UUID_URN_URI,
        asset.getAssetId(),
        Knowledge_Asset_Surrogate_2_0);

    KnowledgeAsset surrogate = ckac.getKnowledgeAssetVersion(uuid, vTag)
        .orElseGet(Assertions::fail);
    assertEquals(1, surrogate.getSurrogate().size());
    surrogate.getSurrogate()
        .forEach(
            surr -> assertTrue(surr.getArtifactId().sameAs(surrId))
        );
  }

  @Test
  void testIdempotentPublishWithKnownSurrogate() {
    KnowledgeAsset asset = SurrogateBuilder
        .newSurrogate(randomAssetId())
        .get();
    UUID uuid = asset.getAssetId().getUuid();
    String vTag = asset.getAssetId().getVersionTag();

    Answer<Void> ans1 = ckac.setKnowledgeAssetVersion(
        uuid,
        vTag,
        asset);
    assertTrue(ans1.isSuccess());

    Answer<Void> ans2 = ckac.setKnowledgeAssetVersion(
        uuid,
        vTag,
        asset);
    assertTrue(ans2.isSuccess());

    UUID suid = defaultSurrogateUUID(asset.getAssetId(),Knowledge_Asset_Surrogate_2_0);

    KnowledgeAsset surrogate = ckac.getKnowledgeAssetVersion(uuid, vTag)
        .orElseGet(Assertions::fail);
    assertEquals(1, surrogate.getSurrogate().size());
    surrogate.getSurrogate()
        .forEach(
            surr -> assertEquals(suid, surr.getArtifactId().getUuid())
        );
  }


  @Test
  void testRawContentEndpoints() {
    ResourceIdentifier assetId = randomAssetId();
    ResourceIdentifier artifactId = randomArtifactId();
    ckac.setKnowledgeAssetVersion(assetId.getUuid(), assetId.getVersionTag(),
        new KnowledgeAsset()
            .withAssetId(assetId)
            .withFormalType(Care_Process_Model));
    repo.setKnowledgeAssetCarrierVersion(
        assetId.getUuid(), assetId.getVersionTag(),
        artifactId.getUuid(), artifactId.getVersionTag(),
        "Foo".getBytes());


    Answer<byte[]> binary1 =
        repo.getKnowledgeAssetCanonicalCarrierContent(assetId.getUuid());
    Answer<byte[]> binary2 =
        repo.getKnowledgeAssetVersionCanonicalCarrierContent(
            assetId.getUuid(), assetId.getVersionTag());
    Answer<byte[]> binary3 =
        repo.getKnowledgeAssetCarrierVersionContent(
            assetId.getUuid(), assetId.getVersionTag(),
            artifactId.getUuid(), artifactId.getVersionTag());

    assertTrue(binary1.isSuccess());
    assertTrue(binary2.isSuccess());
    assertTrue(binary3.isSuccess());

    assertTrue(Arrays.equals(binary1.get(), binary2.get()));
    assertTrue(Arrays.equals(binary1.get(), binary3.get()));

    assertTrue(Arrays.equals("Foo".getBytes(), binary1.get()));

    assertEquals("Foo", new String(binary1.get()));
  }
}
