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

import static edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory.KnowledgeAssetCategorySeries.Rules_Policies_And_Guidelines;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory.KnowledgeAssetCategorySeries.Terminology_Ontology_And_Assertional_KBs;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Clinical_Rule;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Factual_Knowledge;
import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.JSON;
import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.TXT;
import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.XML_1_1;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.KNART_1_3;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.metadata.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.surrogate.Representation;
import edu.mayo.kmdp.repository.asset.SemanticRepoAPITestBase;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetCatalogApi;
import edu.mayo.kmdp.repository.asset.client.ApiClientFactory;
import edu.mayo.kmdp.util.ws.JsonRestWSUtils.WithFHIR;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;


public class SurrogateNegotiationTest extends SemanticRepoAPITestBase {

  private static Logger logger = LoggerFactory.getLogger(SurrogateNegotiationTest.class);

  private ApiClientFactory webClientFactory = new ApiClientFactory("http://localhost:11111",
      WithFHIR.NONE);

  protected KnowledgeAssetCatalogApi rpo = KnowledgeAssetCatalogApi.newInstance(webClientFactory);

  private static final URI REDIRECT_URL = URI.create("https://httpstat.us/306");

  private static String version = "LATEST";

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

    CloseableHttpClient httpClient = HttpClients.createDefault();

    HttpGet request =
        new HttpGet("http://localhost:11111/cat/assets/" + pockId);
    request.addHeader(HttpHeaders.USER_AGENT, "Colorless/42");
    request.addHeader(HttpHeaders.ACCEPT,
        "text/html, application/xhtml+xml, application/xml;q=0.9, image/webp, */*;q=0.8");
    request.addHeader(HttpHeaders.ACCEPT_LANGUAGE,
        "fr; q=1.0, en; q=0.5");

    try {
      CloseableHttpResponse resp = httpClient.execute(request);
      String out = EntityUtils.toString(resp.getEntity());
      assertEquals("306 Unused", out);
    } catch (IOException e) {
      assertTrue(e.getMessage().contains(REDIRECT_URL.getPath()));
    }

  }

  private void populateRepositoryWithRedirectables(UUID pockId, UUID rulId) {
    Answer<Void> ans1 = rpo.setVersionedKnowledgeAsset(
        pockId,
        version,
        pocSurrogate(pockId, version));
    assertTrue(ans1.isSuccess());

    Answer<Void> ans2 = rpo.setVersionedKnowledgeAsset(
        rulId,
        version,
        rulSurrogate(rulId, version));
    assertTrue(ans2.isSuccess());
  }

  private KnowledgeAsset rulSurrogate(UUID assetId, String version) {
    // Rules' catalog entry in KCMS
    return new KnowledgeAsset()
        .withAssetId(DatatypeHelper.vuri(
            "urn:uuid:" + assetId,
            "urn:uuid:" + assetId + ":" + version))
        .withFormalCategory(Rules_Policies_And_Guidelines)
        .withFormalType(Clinical_Rule)
        .withName("Test rule")
        .withSurrogate(
            new ComputableKnowledgeArtifact()
                .withLocator(REDIRECT_URL)
                .withRepresentation(new Representation()
                    .withLanguage(HTML)
                    .withFormat(TXT)
                ),
            new ComputableKnowledgeArtifact()
                .withRepresentation(new Representation()
                    .withLanguage(Knowledge_Asset_Surrogate)
                    .withFormat(JSON)
                )
        )
        .withCarriers(
            new ComputableKnowledgeArtifact()
                .withLocator(URI.create("http://www.myrepo/rule0/carrier?format=xml"))
                .withRepresentation(new Representation()
                    .withLanguage(KNART_1_3)
                    .withFormat(XML_1_1)
                ),
            new ComputableKnowledgeArtifact()
                .withLocator(URI.create("http://www.myrepo/rule0/carrier"))
                .withRepresentation(new Representation()
                    .withLanguage(HTML)
                )
        );
  }

  private KnowledgeAsset pocSurrogate(UUID pockId, String version) {
    return new KnowledgeAsset()
        .withAssetId(DatatypeHelper.vuri(
            "urn:uuid:" + pockId,
            "urn:uuid:" + pockId + ":" + version))
        .withFormalCategory(Terminology_Ontology_And_Assertional_KBs)
        .withFormalType(Factual_Knowledge)
        .withName("Test section of content")
        .withSurrogate(
            new ComputableKnowledgeArtifact()
                .withLocator(REDIRECT_URL)
                .withRepresentation(new Representation()
                    .withLanguage(HTML)
                    .withFormat(TXT)
                )
        )
        .withCarriers(
            new ComputableKnowledgeArtifact()
                .withLocator(URI.create("http://www.myrepo/section0/carrier"))
                .withRepresentation(new Representation()
                    .withLanguage(HTML)
                    .withFormat(TXT)
                )
        );
  }


}
