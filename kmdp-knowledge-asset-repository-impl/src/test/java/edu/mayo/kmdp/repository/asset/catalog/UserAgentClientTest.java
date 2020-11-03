package edu.mayo.kmdp.repository.asset.catalog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.IdentifierConstants.VERSION_LATEST;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomArtifactId;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Terminology_Ontology_And_Assertional_KBs;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Factual_Knowledge;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;

import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerConfig;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerConfig.KnowledgeAssetRepositoryOptions;
import edu.mayo.kmdp.repository.asset.SemanticRepoAPITestBase;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.kmdp.util.XPathUtil;
import edu.mayo.kmdp.util.ws.JsonRestWSUtils.WithFHIR;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.client.ApiClientFactory;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class UserAgentClientTest extends SemanticRepoAPITestBase {


  private KnowledgeAssetCatalogApi rpo;

  @Autowired
  KnowledgeAssetRepositoryServerConfig cfg;

  @BeforeEach
  protected void init() {
    String host = "http://localhost:" + port;

    cfg.with(
        KnowledgeAssetRepositoryOptions.SERVER_HOST, host
    );

    ApiClientFactory apiClientFactory
        = new ApiClientFactory(host, WithFHIR.NONE);

    rpo = KnowledgeAssetCatalogApi.newInstance(apiClientFactory);
  }


  @Test
  public void testPointerXMLSerialization() {

    populateRepositoryWithRedirectables(UUID.randomUUID());

    String out = executeRequest("/cat/assets");
    System.out.println(out);
    Document dox = XMLUtil.loadXMLDocument(out)
        .orElseGet(Assertions::fail);

    Node n = new XPathUtil().xNode(dox, "//href");
    assertNotNull(n.getTextContent());
    assertFalse(Util.isEmpty(n.getTextContent()));
    assertTrue(n.getTextContent().contains("http://localhost:" + port));

    System.out.println(n);
  }


  private String executeRequest(String url) {
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      HttpGet request =
          new HttpGet("http://localhost:" + port + url);
      request.addHeader(HttpHeaders.ACCEPT,
          "application/xhtml+xml, application/xml;q=0.9");

      String out;
      try (CloseableHttpResponse resp = httpClient.execute(request)) {
        out = EntityUtils.toString(resp.getEntity());
      }
      return out;
    } catch (IOException e) {
      fail(e.getMessage());
      return "";
    }
  }

  private void populateRepositoryWithRedirectables(UUID pockId) {
    Answer<Void> ans1 = rpo.setKnowledgeAssetVersion(
        pockId,
        VERSION_LATEST,
        pocSurrogate(pockId, VERSION_LATEST));
    assertTrue(ans1.isSuccess());

  }

  private KnowledgeAsset pocSurrogate(UUID pockId, String version) {
    return new KnowledgeAsset()
        .withAssetId(SurrogateBuilder.assetId(pockId, version))
        .withFormalCategory(Terminology_Ontology_And_Assertional_KBs)
        .withFormalType(Factual_Knowledge)
        .withName("Test section of content")
        .withSurrogate(
            new KnowledgeArtifact()
                .withArtifactId(randomArtifactId())
                .withLocator(URI.create("http://localhost:" + port + "/cat"))
                .withRepresentation(rep(HTML, TXT))
        )
        .withCarriers(
            new KnowledgeArtifact()
                .withArtifactId(randomArtifactId())
                .withLocator(URI.create("http://www.myrepo/section0/carrier"))
                .withRepresentation(rep(HTML, TXT))
        );
  }

}
