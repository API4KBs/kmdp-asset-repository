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
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.assetId;
import static org.omg.spec.api4kp.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Equation;
import static org.omg.spec.api4kp.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.SPARQL_1_1;

import edu.mayo.kmdp.repository.asset.SemanticRepoAPITestBase;
import edu.mayo.kmdp.util.ws.JsonRestWSUtils.WithFHIR;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.client.ApiClientFactory;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetCatalogApiInternal;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;


class SemanticRepositoryRemoteQueryTest extends SemanticRepoAPITestBase {

  private KnowledgeAssetCatalogApi ckac;

  @BeforeEach
  void init() {
    ApiClientFactory webClientFactory = new ApiClientFactory("http://localhost:" + port,
        WithFHIR.NONE);

    ckac = KnowledgeAssetCatalogApi.newInstance(webClientFactory);

    ResourceIdentifier assetId = assetId(UUID.nameUUIDFromBytes("aaa000".getBytes()),"1.0.0");
    ckac.setKnowledgeAssetVersion(assetId.getUuid(),assetId.getVersionTag(),
        new KnowledgeAsset()
            .withAssetId(assetId)
            .withFormalType(Equation));
  }

  @Test
  void testQuery() {
    ApiClientFactory webClientFactory = new ApiClientFactory("http://localhost:" + port,
        WithFHIR.NONE);
    KnowledgeAssetCatalogApiInternal qryPoint = KnowledgeAssetCatalogApi.newInstance(webClientFactory);

    String query = "" +
        "select ?s where { ?s a <" + Equation.getReferentId() + "> }" +
        "";

    KnowledgeCarrier queryCarrier = AbstractCarrier.of(query)
        .withRepresentation(rep(SPARQL_1_1, TXT, Charset.defaultCharset()));

    List<Bindings> binds = qryPoint.queryKnowledgeAssetGraph(queryCarrier)
        .orElse(Collections.emptyList());

    assertEquals(1, binds.size());
    Object assetId = binds.get(0).get("s");
    assertEquals(
        assetId(UUID.nameUUIDFromBytes("aaa000".getBytes()),"1.0.0")
            .getVersionId().toString(),
        assetId);
  }

}
