/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.repository.asset.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.assetId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomAssetId;
import static org.omg.spec.api4kp.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Care_Process_Model;
import static org.omg.spec.api4kp.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Clinical_Rule;
import static org.omg.spec.api4kp.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Decision_Model;
import static org.omg.spec.api4kp.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Predictive_Model;
import static org.omg.spec.api4kp.taxonomy.knowledgeprocessingtechnique.KnowledgeProcessingTechniqueSeries.Qualitative_Technique;

import edu.mayo.kmdp.repository.asset.SemanticRepoAPITestBase;
import edu.mayo.kmdp.util.ws.JsonRestWSUtils.WithFHIR;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.client.ApiClientFactory;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.surrogate.Applicability;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

class SemanticRepositoryIntegrationTest extends SemanticRepoAPITestBase {

  private KnowledgeAssetCatalogApi ckac;

  @BeforeEach
  void init() {
    ApiClientFactory webClientFactory = new ApiClientFactory("http://localhost:" + port,
        WithFHIR.NONE);

    ckac = KnowledgeAssetCatalogApi.newInstance(webClientFactory);
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
    ckac.setKnowledgeAssetVersion(assetId.getUuid(),assetId.getVersionTag(),
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
    ckac.setKnowledgeAssetVersion(rid1.getUuid(),rid1.getVersionTag(),
        new KnowledgeAsset()
            .withAssetId(rid1)
            .withFormalType(Care_Process_Model));

    ResourceIdentifier rid2 = randomAssetId();
    ckac.setKnowledgeAssetVersion(rid2.getUuid(),rid2.getVersionTag(),
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
    ResourceIdentifier rid1 = assetId(uid,"1.0.0");
    ckac.setKnowledgeAssetVersion(rid1.getUuid(),rid1.getVersionTag(),
        new KnowledgeAsset().withAssetId(rid1));

    ResourceIdentifier rid2 = assetId(uid, "2.0.0");
    ckac.setKnowledgeAssetVersion(rid2.getUuid(),rid2.getVersionTag(),
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

    ResourceIdentifier rid2 = assetId(uid, "2.0.0");
    ckac.setKnowledgeAssetVersion(rid2.getUuid(),rid2.getVersionTag(),
        new KnowledgeAsset().withAssetId(rid2));

    ResourceIdentifier rid1 = assetId(uid, "1.0.0");
    ckac.setKnowledgeAssetVersion(rid1.getUuid(),rid1.getVersionTag(),
        new KnowledgeAsset().withAssetId(rid1));

    Answer<KnowledgeAsset> axx = ckac.getKnowledgeAsset(uid, null);
    assertTrue(axx.isSuccess());
    assertEquals("2.0.0", axx.get().getAssetId().getVersionTag());
  }

  @Test
  void testGetLatestKnowledgeAssetHasCorrectId() {
    UUID uid = UUID.randomUUID();
    ResourceIdentifier rid1 = newId(uid, "1.0.0");
    ckac.setKnowledgeAssetVersion(rid1.getUuid(),rid1.getVersionTag(),
        new KnowledgeAsset().withAssetId(rid1));

    ResourceIdentifier rid2 = newId(uid,"2.0.0");
    ckac.setKnowledgeAssetVersion(rid2.getUuid(),rid2.getVersionTag(),
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
    ckac.setKnowledgeAssetVersion(assetId.getUuid(),assetId.getVersionTag(),
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
    assertEquals(Qualitative_Technique.getUuid(),t.getUuid());
  }

}
