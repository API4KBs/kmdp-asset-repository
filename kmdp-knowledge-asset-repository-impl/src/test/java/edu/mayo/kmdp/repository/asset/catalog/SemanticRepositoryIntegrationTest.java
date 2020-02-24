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

import edu.mayo.kmdp.id.Term;
import edu.mayo.kmdp.id.adapter.URIId;
import edu.mayo.kmdp.metadata.annotations.SimpleApplicability;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.repository.asset.SemanticRepoAPITestBase;
import edu.mayo.kmdp.repository.asset.v3.KnowledgeAssetCatalogApi;
import edu.mayo.kmdp.repository.asset.v3.client.ApiClientFactory;
import edu.mayo.kmdp.util.ws.JsonRestWSUtils.WithFHIR;
import edu.mayo.ontology.taxonomies.kao.knowledgeprocessingtechnique.KnowledgeProcessingTechniqueSeries;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.*;
import static org.junit.jupiter.api.Assertions.*;

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
    ckac.setVersionedKnowledgeAsset(UUID.randomUUID(), "1",
        new KnowledgeAsset().withFormalType(Decision_Model));

    List<Pointer> pointers = ckac
        .listKnowledgeAssets(Decision_Model.getTag(),
            null, -1, -1)
        .orElse(Collections.emptyList());

    assertEquals(1, pointers.size());
  }

  @Test
  void testListKnowledgeAssetsBadType() {

    ckac.setVersionedKnowledgeAsset(strToUUID("1"), "1",
        new KnowledgeAsset().withFormalType(Care_Process_Model));

    List<Pointer> pointers = ckac.listKnowledgeAssets(Predictive_Model.getTag(),
        null, -1, -1)
        .orElse(Collections.emptyList());

    assertEquals(0, pointers.size());
  }

  @Test
  void testListKnowledgeAssetsNoType() {
    Answer<List<Pointer>> zero = ckac.listKnowledgeAssets(null, null, -1, -1);
    assertTrue(zero.isSuccess());

    ckac.setVersionedKnowledgeAsset(strToUUID("98"), "1",
        new KnowledgeAsset().withFormalType(Care_Process_Model));
    ckac.setVersionedKnowledgeAsset(strToUUID("89"), "1",
        new KnowledgeAsset().withFormalType(Clinical_Rule));

    Answer<List<Pointer>> ans = ckac.listKnowledgeAssets(null,
        null, -1, -1);
    List<Pointer> pointers = ans
        .orElse(Collections.emptyList());

    assertTrue(pointers.stream().anyMatch(p -> p.getEntityRef().getUri().toString()
        .contains(strToUUID("98").toString())));
    assertTrue(pointers.stream().anyMatch(p -> p.getEntityRef().getUri().toString()
        .contains(strToUUID("89").toString())));
  }

  @Test
  @Ignore
  void testGeKnowledgeAssetsVersions() {

    ckac.setVersionedKnowledgeAsset(strToUUID("4"), "1",
        new KnowledgeAsset());
    ckac.setVersionedKnowledgeAsset(strToUUID("4"), "2",
        new KnowledgeAsset());

    List<Pointer> pointers = ckac.listKnowledgeAssets(null,
        null, -1, -1)
        .orElse(Collections.emptyList())
        .stream()
        .filter((p) -> p.getHref().toString()
            .contains("assets/" + strToUUID("4")))
        .collect(Collectors.toList());

    assertEquals(1, pointers.size());
  }

  @Test
  void testGetLatestKnowledgeAsset() {
    ckac.setVersionedKnowledgeAsset(strToUUID("3"), "1",
        new KnowledgeAsset());
    ckac.setVersionedKnowledgeAsset(strToUUID("3"), "2",
        new KnowledgeAsset());

    Assertions.assertTrue(
        ckac.getKnowledgeAsset(strToUUID("3"), null).isSuccess());
  }

  @Test
  void testGetLatestKnowledgeAssetHasCorrectId() {
    ckac.setVersionedKnowledgeAsset(strToUUID("1"), "1",
        new KnowledgeAsset());
    ckac.setVersionedKnowledgeAsset(strToUUID("1"), "2",
        new KnowledgeAsset());

    String id = ckac.getKnowledgeAsset(strToUUID("1"), null)
        .map(KnowledgeAsset::getAssetId)
        .map(URIId::getTag)
        .orElse(null);

    assertEquals(strToUUID("1").toString(), id);
  }

  @Test
  void testSurrogateWithApplicability() {
    ckac.setVersionedKnowledgeAsset(strToUUID("0099"), "1",
        new KnowledgeAsset()
            .withApplicableIn(new SimpleApplicability()
                .withSituation(KnowledgeProcessingTechniqueSeries.Logic_Based_Technique.asConcept()))
    );
    KnowledgeAsset ax = ckac.getKnowledgeAsset(strToUUID("0099"), null).orElse(null);

    assertNotNull(ax);
    assertNotNull(ax.getApplicableIn());
    assertTrue(ax.getApplicableIn() instanceof SimpleApplicability);

    Term t = ((SimpleApplicability) ax.getApplicableIn()).getSituation();
    assertNotNull(t);
    assertEquals(KnowledgeProcessingTechniqueSeries.Logic_Based_Technique.getConceptUUID(),t.getConceptUUID());
  }


  private UUID strToUUID(String s) {
    return UUID.nameUUIDFromBytes(s.getBytes());
  }
}
