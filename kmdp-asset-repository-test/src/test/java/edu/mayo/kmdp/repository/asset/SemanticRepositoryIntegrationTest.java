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
package edu.mayo.kmdp.repository.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import edu.mayo.kmdp.metadata.surrogate.resources.KnowledgeAsset;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype._1_0.KnowledgeAssetType;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;

public class SemanticRepositoryIntegrationTest extends IntegrationTestBase {

  private ApiClient webClient = new ApiClient()
      .setBasePath("http://localhost:11111");

  protected KnowledgeAssetRepositoryApi client = KnowledgeAssetRepositoryApi.newInstance(webClient);
  protected KnowledgeAssetCatalogApi ckac = KnowledgeAssetCatalogApi.newInstance(webClient);


  @Test
  public void testListKnowledgeAssetsType() {
    client.initKnowledgeAsset(new KnowledgeAsset().withFormalType(KnowledgeAssetType.Decision_Model));

    List<Pointer> pointers = ckac
        .listKnowledgeAssets(KnowledgeAssetType.Decision_Model.getTag(),
            null, -1, -1);

    assertEquals(1, pointers.size());
  }

  @Test
  public void testListKnowledgeAssetsBadType() {

    client.initKnowledgeAsset(new KnowledgeAsset().withFormalType(KnowledgeAssetType.Care_Process_Model));

    List<Pointer> pointers = ckac.listKnowledgeAssets(KnowledgeAssetType.Predictive_Model.getTag(),
        null, -1, -1);

    assertEquals(0, pointers.size());
  }

  @Test
  public void testListKnowledgeAssetsNoType() {

    client.initKnowledgeAsset(new KnowledgeAsset().withFormalType(KnowledgeAssetType.Care_Process_Model));
    client.initKnowledgeAsset(new KnowledgeAsset().withFormalType(KnowledgeAssetType.Clinical_Rule));

    List<Pointer> pointers = ckac.listKnowledgeAssets(null,
        null, -1, -1);

    assertEquals(2, pointers.size());
  }

  @Test
  public void testGeKnowledgeAssetsVersions() {

    client.setVersionedKnowledgeAsset("4", "1", new KnowledgeAsset());
    client.setVersionedKnowledgeAsset("4", "2", new KnowledgeAsset());

    List<Pointer> pointers = ckac.listKnowledgeAssets(null,
        null, -1, -1).stream()
        .filter((p)->p.getHref().toString().contains("assets/4"))
        .collect(Collectors.toList());

    assertEquals(1, pointers.size());
  }

  @Test
  public void testGetLatestKnowledgeAsset() {
    client.setVersionedKnowledgeAsset("3", "1", new KnowledgeAsset());
    client.setVersionedKnowledgeAsset("3", "2", new KnowledgeAsset());

    assertNotNull(ckac.getKnowledgeAsset("3"));
  }

  @Test
  public void testGetLatestKnowledgeAssetHasCorrectId() {
    client.setVersionedKnowledgeAsset("1", "1", new KnowledgeAsset());
    client.setVersionedKnowledgeAsset("1", "2", new KnowledgeAsset());

    assertEquals("1", ckac.getKnowledgeAsset("1").getAssetId().getTag());
  }

}
