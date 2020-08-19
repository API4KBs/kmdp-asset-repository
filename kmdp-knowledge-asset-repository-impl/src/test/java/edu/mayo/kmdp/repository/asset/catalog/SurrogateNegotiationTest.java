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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.IdentifierConstants.VERSION_LATEST;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomArtifactId;
import static org.omg.spec.api4kp.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Rules_Policies_And_Guidelines;
import static org.omg.spec.api4kp.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Terminology_Ontology_And_Assertional_KBs;
import static org.omg.spec.api4kp.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Clinical_Rule;
import static org.omg.spec.api4kp.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Factual_Knowledge;
import static org.omg.spec.api4kp.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.KNART_1_3;
import static org.omg.spec.api4kp.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;

import edu.mayo.kmdp.repository.asset.SemanticRepoAPITestBase;
import edu.mayo.kmdp.util.ws.JsonRestWSUtils.WithFHIR;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.client.ApiClientFactory;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

public class SurrogateNegotiationTest extends SemanticRepoAPITestBase {

  private static Logger logger = LoggerFactory.getLogger(SurrogateNegotiationTest.class);

  private static final URI REDIRECT_URL = URI.create("https://httpstat.us/306");

  private KnowledgeAssetCatalogApi rpo;

  @BeforeEach
  void init() {
    ApiClientFactory apiClientFactory = new ApiClientFactory("http://localhost:" + port,
        WithFHIR.NONE);

    rpo = KnowledgeAssetCatalogApi.newInstance(apiClientFactory);
  }

  @Test
  public void testPopulation() {
    UUID pockId = UUID.randomUUID();
    UUID rulId = UUID.randomUUID();

    populateRepositoryWithRedirectables(pockId, rulId);

    assertTrue(rpo.getKnowledgeAsset(pockId, null).isSuccess());
    assertTrue(rpo.getKnowledgeAsset(rulId, null).isSuccess());
  }


  @Test
  public void testHeaders() {
    UUID pockId = UUID.randomUUID();
    UUID rulId = UUID.randomUUID();

    populateRepositoryWithRedirectables(pockId, rulId);

    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      HttpGet request =
          new HttpGet("http://localhost:" + port + "/cat/assets/" + pockId);
      request.addHeader(HttpHeaders.USER_AGENT, "Colorless/42");
      request.addHeader(HttpHeaders.ACCEPT,
          "text/html, application/xhtml+xml, application/xml;q=0.9, image/webp, */*;q=0.8");
      request.addHeader(HttpHeaders.ACCEPT_LANGUAGE,
          "fr; q=1.0, en; q=0.5");

      try (CloseableHttpResponse resp = httpClient.execute(request)) {
        String out = EntityUtils.toString(resp.getEntity());
        assertEquals("306 Unused", out);
      }
    } catch (IOException e) {
      assertTrue(e.getMessage().contains(REDIRECT_URL.getPath()));
    }

  }

  private void populateRepositoryWithRedirectables(UUID pockId, UUID rulId) {
    Answer<Void> ans1 = rpo.setKnowledgeAssetVersion(
        pockId,
        VERSION_LATEST,
        pocSurrogate(pockId, VERSION_LATEST));
    assertTrue(ans1.isSuccess());

    Answer<Void> ans2 = rpo.setKnowledgeAssetVersion(
        rulId,
        VERSION_LATEST,
        rulSurrogate(rulId, VERSION_LATEST));
    assertTrue(ans2.isSuccess());
  }

  private KnowledgeAsset rulSurrogate(UUID assetId, String version) {
    // Rules' catalog entry in KCMS
    return new KnowledgeAsset()
        .withAssetId(SurrogateBuilder.assetId(assetId,version))
        .withFormalCategory(Rules_Policies_And_Guidelines)
        .withFormalType(Clinical_Rule)
        .withName("Test rule")
        .withSurrogate(
            new KnowledgeArtifact()
                .withArtifactId(randomArtifactId())
                .withLocator(REDIRECT_URL)
                .withRepresentation(rep(HTML,TXT)),
            new KnowledgeArtifact()
                .withArtifactId(randomArtifactId())
                .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0,JSON))
        )
        .withCarriers(
            new KnowledgeArtifact()
                .withArtifactId(randomArtifactId())
                .withLocator(URI.create("http://www.myrepo/rule0/carrier?format=xml"))
                .withRepresentation(rep(KNART_1_3,XML_1_1)),
            new KnowledgeArtifact()
                .withArtifactId(randomArtifactId())
                .withLocator(URI.create("http://www.myrepo/rule0/carrier"))
                .withRepresentation(rep(HTML))
        );
  }

  private KnowledgeAsset pocSurrogate(UUID pockId, String version) {
    return new KnowledgeAsset()
        .withAssetId(SurrogateBuilder.assetId(pockId,version))
        .withFormalCategory(Terminology_Ontology_And_Assertional_KBs)
        .withFormalType(Factual_Knowledge)
        .withName("Test section of content")
        .withSurrogate(
            new KnowledgeArtifact()
                .withArtifactId(randomArtifactId())
                .withLocator(REDIRECT_URL)
                .withRepresentation(rep(HTML,TXT))
        )
        .withCarriers(
            new KnowledgeArtifact()
                .withArtifactId(randomArtifactId())
                .withLocator(URI.create("http://www.myrepo/section0/carrier"))
                .withRepresentation(rep(HTML,TXT))
        );
  }


}
