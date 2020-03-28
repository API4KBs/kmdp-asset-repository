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
package edu.mayo.kmdp.repository.asset.bundle;


import edu.mayo.kmdp.metadata.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.surrogate.Representation;
import edu.mayo.kmdp.metadata.surrogate.resources.KnowledgeAsset;
import edu.mayo.kmdp.repository.asset.SemanticRepoAPITestBase;
import edu.mayo.kmdp.repository.asset.v4.KnowledgeAssetCatalogApi;
import edu.mayo.kmdp.repository.asset.v4.KnowledgeAssetRepositoryApi;
import edu.mayo.kmdp.repository.asset.v4.KnowledgeAssetRetrievalApi;
import edu.mayo.kmdp.repository.asset.v4.client.ApiClientFactory;
import edu.mayo.kmdp.util.ws.JsonRestWSUtils.WithFHIR;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static edu.mayo.ontology.taxonomies.kao.rel.dependencyreltype.DependencyTypeSeries.Depends_On;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.HL7_ELM;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BundlerIntegrationTest extends SemanticRepoAPITestBase {

  private KnowledgeAssetRepositoryApi repo;
  private KnowledgeAssetCatalogApi catalog;
  private KnowledgeAssetRetrievalApi lib;

  @BeforeEach
  void init() {
    ApiClientFactory apiClientFactory = new ApiClientFactory("http://localhost:" + port,
        WithFHIR.NONE);

    repo = KnowledgeAssetRepositoryApi.newInstance(apiClientFactory);
    catalog = KnowledgeAssetCatalogApi.newInstance(apiClientFactory);
    lib = KnowledgeAssetRetrievalApi.newInstance(apiClientFactory);
  }

  @Test
  void testBundleOnlyOne() {
    UUID u1 = UUID.nameUUIDFromBytes("1".getBytes());

    catalog.setVersionedKnowledgeAsset(u1, "2", new KnowledgeAsset().
        withCarriers(new ComputableKnowledgeArtifact().
            withRepresentation(new Representation().withLanguage(
                HL7_ELM))));
    repo.addKnowledgeAssetCarrier(u1, "2", "HI!".getBytes());

    List<KnowledgeCarrier> carriers = lib.getKnowledgeArtifactBundle(u1, "2",
        Depends_On.getTag(), -1, null).getOptionalValue()
        .orElseGet(Collections::emptyList);

    assertEquals(1, carriers.size());
  }

}
