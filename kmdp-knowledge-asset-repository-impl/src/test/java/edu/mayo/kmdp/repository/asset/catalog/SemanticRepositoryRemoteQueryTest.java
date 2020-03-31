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

import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Decision_Model;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Equation;
import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.TXT;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.SPARQL_1_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;

import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.v2.surrogate.SurrogateBuilder;
import edu.mayo.kmdp.repository.asset.SemanticRepoAPITestBase;
import edu.mayo.kmdp.repository.asset.v4.KnowledgeAssetCatalogApi;
import edu.mayo.kmdp.repository.asset.v4.KnowledgeAssetRetrievalApi;
import edu.mayo.kmdp.repository.asset.v4.client.ApiClientFactory;
import edu.mayo.kmdp.util.ws.JsonRestWSUtils.WithFHIR;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.AbstractCarrier;
import org.omg.spec.api4kp._1_0.datatypes.Bindings;
import org.omg.spec.api4kp._1_0.id.SemanticIdentifier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;


class SemanticRepositoryRemoteQueryTest extends SemanticRepoAPITestBase {

  private KnowledgeAssetCatalogApi ckac;

  @BeforeEach
  void init() {
    ApiClientFactory webClientFactory = new ApiClientFactory("http://localhost:" + port,
        WithFHIR.NONE);

    ckac = KnowledgeAssetCatalogApi.newInstance(webClientFactory);

    ckac.setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("aaa000".getBytes()), "1",
        new KnowledgeAsset().withFormalType(Equation));
  }

  @Test
  void testQuery() {
    ApiClientFactory webClientFactory = new ApiClientFactory("http://localhost:" + port,
        WithFHIR.NONE);
    KnowledgeAssetRetrievalApi qryPoint = KnowledgeAssetRetrievalApi.newInstance(webClientFactory);

    String query = "" +
        "select ?s where { ?s a <" + KnowledgeAssetTypeSeries.Equation.getRef() + "> }" +
        "";

    KnowledgeCarrier queryCarrier = AbstractCarrier.of(query)
        .withRepresentation(rep(SPARQL_1_1, TXT));

    List<Bindings> binds = qryPoint.queryKnowledgeAssets(queryCarrier)
        .orElse(Collections.emptyList());

    assertEquals(1, binds.size());
    Object assetId = binds.get(0).get("s");
    assertEquals(
        SurrogateBuilder.assetId(UUID.nameUUIDFromBytes("aaa000".getBytes()),"1")
            .getVersionId().toString(),
        assetId);
  }

}