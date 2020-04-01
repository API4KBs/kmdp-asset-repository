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

import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Care_Process_Model;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Clinical_Rule;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Decision_Model;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Predictive_Model;
import static edu.mayo.ontology.taxonomies.kao.knowledgeprocessingtechnique.KnowledgeProcessingTechniqueSeries.Logic_Based_Technique;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mayo.kmdp.metadata.v2.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.v2.surrogate.annotations.Applicability;
import edu.mayo.kmdp.repository.asset.SemanticRepoAPITestBase;
import edu.mayo.kmdp.repository.asset.v4.KnowledgeAssetCatalogApi;
import edu.mayo.kmdp.repository.asset.v4.client.ApiClientFactory;
import edu.mayo.kmdp.util.ws.JsonRestWSUtils.WithFHIR;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.id.Pointer;
import org.omg.spec.api4kp._1_0.id.ResourceIdentifier;
import org.omg.spec.api4kp._1_0.id.SemanticIdentifier;
import org.omg.spec.api4kp._1_0.id.Term;

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
    ResourceIdentifier assetId = SemanticIdentifier.newId(UUID.randomUUID(),"1");
    ckac.setVersionedKnowledgeAsset(assetId.getUuid(), assetId.getVersionTag(),
        new KnowledgeAsset()
            .withAssetId(assetId)
            .withFormalType(Decision_Model));

    List<Pointer> pointers = ckac
        .listKnowledgeAssets(Decision_Model.getTag(),
            null, null, -1, -1)
        .orElse(Collections.emptyList());

    assertEquals(1, pointers.size());
  }

  @Test
  void testListKnowledgeAssetsBadType() {
    ResourceIdentifier assetId = SemanticIdentifier.newId(strToUUID("1"),"1");
    ckac.setVersionedKnowledgeAsset(assetId.getUuid(),assetId.getVersionTag(),
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
    Answer<List<Pointer>> zero = ckac.listKnowledgeAssets(null, null, null, -1, -1);
    assertTrue(zero.isSuccess());

    ResourceIdentifier rid1 = SemanticIdentifier.newId(strToUUID("98"), "1");
    ckac.setVersionedKnowledgeAsset(rid1.getUuid(),rid1.getVersionTag(),
        new KnowledgeAsset()
            .withAssetId(rid1)
            .withFormalType(Care_Process_Model));

    ResourceIdentifier rid2 = SemanticIdentifier.newId(strToUUID("89"), "1");
    ckac.setVersionedKnowledgeAsset(rid2.getUuid(),rid2.getVersionTag(),
        new KnowledgeAsset()
            .withAssetId(rid2)
            .withFormalType(Clinical_Rule));

    Answer<List<Pointer>> ans = ckac.listKnowledgeAssets(null,
        null, null, -1, -1);
    List<Pointer> pointers = ans
        .orElse(Collections.emptyList());

    assertTrue(pointers.stream().anyMatch(p -> p.getResourceId().toString()
        .contains(strToUUID("98").toString())));
    assertTrue(pointers.stream().anyMatch(p -> p.getHref().toString()
        .contains(strToUUID("89").toString())));
  }

  @Test
  void testGeKnowledgeAssetsVersions() {

    ResourceIdentifier rid1 = SemanticIdentifier.newId(strToUUID("4"), "1");
    ckac.setVersionedKnowledgeAsset(rid1.getUuid(),rid1.getVersionTag(),
        new KnowledgeAsset().withAssetId(rid1));

    ResourceIdentifier rid2 = SemanticIdentifier.newId(strToUUID("4"), "2");
    ckac.setVersionedKnowledgeAsset(rid2.getUuid(),rid2.getVersionTag(),
        new KnowledgeAsset().withAssetId(rid2));

    List<Pointer> pointers = ckac.listKnowledgeAssets(null,
        null, null, -1, -1)
        .orElse(Collections.emptyList())
        .stream()
        .filter((p) -> p.getHref().toString()
            .contains("assets/" + strToUUID("4")))
        .collect(Collectors.toList());

    assertEquals(1, pointers.size());
  }

  @Test
  void testGetLatestKnowledgeAsset() {
    ResourceIdentifier rid1 = SemanticIdentifier.newId(strToUUID("3"), "1");
    ckac.setVersionedKnowledgeAsset(rid1.getUuid(),rid1.getVersionTag(),
        new KnowledgeAsset().withAssetId(rid1));

    ResourceIdentifier rid2 = SemanticIdentifier.newId(strToUUID("3"), "2");
    ckac.setVersionedKnowledgeAsset(rid2.getUuid(),rid2.getVersionTag(),
        new KnowledgeAsset().withAssetId(rid2));

    Assertions.assertTrue(
        ckac.getKnowledgeAsset(strToUUID("3"), null).isSuccess());
  }

  @Test
  void testGetLatestKnowledgeAssetHasCorrectId() {
    ResourceIdentifier rid1 = SemanticIdentifier.newId(strToUUID("1"), "1");
    ckac.setVersionedKnowledgeAsset(rid1.getUuid(),rid1.getVersionTag(),
        new KnowledgeAsset().withAssetId(rid1));

    ResourceIdentifier rid2 = SemanticIdentifier.newId(strToUUID("1"), "2");
    ckac.setVersionedKnowledgeAsset(rid2.getUuid(),rid2.getVersionTag(),
        new KnowledgeAsset().withAssetId(rid2));

    String id = ckac.getKnowledgeAsset(strToUUID("1"), null)
        .map(KnowledgeAsset::getAssetId)
        .map(ResourceIdentifier::getTag)
        .orElse(null);

    assertEquals(strToUUID("1").toString(), id);
  }

  @Test
  void testSurrogateWithApplicability() {
    ckac.setVersionedKnowledgeAsset(strToUUID("0099"), "1",
        new KnowledgeAsset()
            .withApplicableIn(new Applicability()
                .withSituation(Logic_Based_Technique.asConceptIdentifier()))
    );
    KnowledgeAsset ax = ckac.getKnowledgeAsset(strToUUID("0099"), null).orElse(null);

    assertNotNull(ax);
    assertNotNull(ax.getApplicableIn());

    Term t = ax.getApplicableIn().getSituation().get(0);
    assertNotNull(t);
    assertEquals(Logic_Based_Technique.getConceptUUID(),t.getUuid());
  }


  private UUID strToUUID(String s) {
    return UUID.nameUUIDFromBytes(s.getBytes());
  }
}
