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
package edu.mayo.kmdp.repository.asset;

import static edu.mayo.kmdp.metadata.v2.surrogate.SurrogateBuilder.assetId;
import static edu.mayo.kmdp.util.Util.uuid;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Care_Process_Model;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;
import static org.omg.spec.api4kp._1_0.services.tranx.ModelMIMECoder.encode;

import edu.mayo.kmdp.metadata.v2.surrogate.KnowledgeAsset;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.id.ResourceIdentifier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;


class SurrogatesTest extends RepositoryTestBase {

  @Test
  void testNegotiateLegacySurrogate() {
    UUID uuid = uuid("foo");
    ResourceIdentifier assetId1 = assetId(uuid,"1.0.0");

    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(assetId1.getUuid(), assetId1.getVersionTag(),
            new KnowledgeAsset()
                .withAssetId(assetId1)
                .withFormalType(Care_Process_Model)));

    Answer<KnowledgeCarrier> surrogateAns = semanticRepository.getCanonicalKnowledgeAssetSurrogate(
        assetId1.getUuid(),assetId1.getVersionTag(),
        encode(rep(Knowledge_Asset_Surrogate)));
    assertTrue(surrogateAns.isSuccess());

    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset legacySurrogate =
        surrogateAns
            .flatOpt(kc -> kc.as(edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset.class))
            .orElseGet(Assertions::fail);

    assertEquals(assetId1.getResourceId(),legacySurrogate.getAssetId().getUri());
  }

}