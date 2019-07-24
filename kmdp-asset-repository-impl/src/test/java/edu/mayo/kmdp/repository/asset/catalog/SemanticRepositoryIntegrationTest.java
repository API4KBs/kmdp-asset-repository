/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.mayo.kmdp.repository.asset.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mayo.kmdp.metadata.surrogate.resources.KnowledgeAsset;
import edu.mayo.kmdp.repository.asset.IntegrationTestBase;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetCatalogApi;
import edu.mayo.kmdp.repository.asset.client.ApiClientFactory;
import edu.mayo.kmdp.util.ws.JsonRestWSUtils.WithFHIR;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype._20190801.KnowledgeAssetType;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;

public class SemanticRepositoryIntegrationTest extends IntegrationTestBase {

  private ApiClientFactory webClientFactory = new ApiClientFactory("http://localhost:11111", WithFHIR.NONE);

  protected KnowledgeAssetCatalogApi ckac = KnowledgeAssetCatalogApi.newInstance(webClientFactory);


  @Test
  public void testListKnowledgeAssetsType() {
    ckac.setVersionedKnowledgeAsset(UUID.randomUUID(), "1",
        new KnowledgeAsset().withFormalType(KnowledgeAssetType.Decision_Model));

    List<Pointer> pointers = ckac
        .listKnowledgeAssets(KnowledgeAssetType.Decision_Model.getTag(),
            null, -1, -1).getOptionalValue().get();

    assertEquals(1, pointers.size());
  }

  @Test
  public void testListKnowledgeAssetsBadType() {

    ckac.setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("1".getBytes()), "1",
        new KnowledgeAsset().withFormalType(KnowledgeAssetType.Care_Process_Model));

    List<Pointer> pointers = ckac.listKnowledgeAssets(KnowledgeAssetType.Predictive_Model.getTag(),
        null, -1, -1).getOptionalValue().get();

    assertEquals(0, pointers.size());
  }

  @Test
  public void testListKnowledgeAssetsNoType() {
    Answer<List<Pointer>> zero = ckac.listKnowledgeAssets(null,null,-1,-1);
    assertTrue(zero.isSuccess());
    assertTrue(zero.getOptionalValue().get().isEmpty());

    ckac.setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("98".getBytes()), "1",
        new KnowledgeAsset().withFormalType(KnowledgeAssetType.Care_Process_Model));
    ckac.setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("89".getBytes()), "1",
        new KnowledgeAsset().withFormalType(KnowledgeAssetType.Clinical_Rule));

    Answer<List<Pointer>> ans = ckac.listKnowledgeAssets(null,
        null, -1, -1);
    List<Pointer> pointers = ans.getOptionalValue().get();

    assertEquals(2, pointers.size());
  }

  @Test
  public void testGeKnowledgeAssetsVersions() {

    ckac.setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("4".getBytes()), "1", new KnowledgeAsset());
    ckac.setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("4".getBytes()), "2", new KnowledgeAsset());

    List<Pointer> pointers = ckac.listKnowledgeAssets(null,
        null, -1, -1).getOptionalValue().get().stream()
        .filter((p) -> p.getHref().toString().contains("assets/" + UUID.nameUUIDFromBytes("4".getBytes())))
        .collect(Collectors.toList());

    assertEquals(1, pointers.size());
  }

  @Test
  public void testGetLatestKnowledgeAsset() {
    ckac.setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("3".getBytes()), "1", new KnowledgeAsset());
    ckac.setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("3".getBytes()), "2", new KnowledgeAsset());

    Assertions.assertTrue(ckac.getKnowledgeAsset(UUID.nameUUIDFromBytes("3".getBytes())).isSuccess());
  }

  @Test
  public void testGetLatestKnowledgeAssetHasCorrectId() {
    ckac.setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("1".getBytes()), "1", new KnowledgeAsset());
    ckac.setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("1".getBytes()), "2", new KnowledgeAsset());

    Assertions.assertEquals(UUID.nameUUIDFromBytes("1".getBytes()).toString(),
        ckac.getKnowledgeAsset(UUID.nameUUIDFromBytes("1".getBytes())).getOptionalValue().get().getAssetId()
            .getTag());
  }

}
