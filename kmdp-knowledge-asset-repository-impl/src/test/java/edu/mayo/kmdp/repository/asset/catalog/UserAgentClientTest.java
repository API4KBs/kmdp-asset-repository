package edu.mayo.kmdp.repository.asset.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.IdentifierConstants.VERSION_LATEST;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomArtifactId;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Terminology_Ontology_And_Assertional_KBs;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Factual_Knowledge;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.Turtle;

import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerConfig;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerConfig.KnowledgeAssetRepositoryOptions;
import edu.mayo.kmdp.repository.asset.SemanticRepoAPITestBase;
import edu.mayo.kmdp.repository.asset.index.sparql.SparqlIndex;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.JenaUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.kmdp.util.XPathUtil;
import edu.mayo.kmdp.util.ws.JsonRestWSUtils.WithFHIR;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.client.ApiClientFactory;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
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

    String out = executeRequest("/cat/assets", "application/xhtml+xml, application/xml;q=0.9");
    System.out.println(out);
    Document dox = XMLUtil.loadXMLDocument(out)
        .orElseGet(Assertions::fail);

    Node n = new XPathUtil().xNode(dox, "//href");
    assertNotNull(n.getTextContent());
    assertFalse(Util.isEmpty(n.getTextContent()));
    assertTrue(n.getTextContent().contains("http://localhost:" + port));

    System.out.println(n);
  }

  @Test
  void testGraphXMLSerialization() {
    UUID assetId = UUID.randomUUID();
    populateRepositoryWithRedirectables(assetId);

    String out = executeRequest("/cat/graph", "application/xml");
    System.out.println(out);
    XMLUtil.loadXMLDocument(out)
        .orElseGet(Assertions::fail);
  }


  @Test
  void testGraphJSONSerialization() {
    UUID assetId = UUID.randomUUID();
    populateRepositoryWithRedirectables(assetId);

    String out = executeRequest("/cat/graph", "application/json");
    System.out.println(out);
    JSonUtil.readJson(out, KnowledgeCarrier.class)
        .orElseGet(Assertions::fail);
  }

  @Test
  void testGraphRDFSerialization() {
    UUID assetId = UUID.randomUUID();
    populateRepositoryWithRedirectables(assetId);

    // X-Accept controls the serialized artifact
    // Accept defaults to JSON
    String out = executeModelRequest("/cat/graph", codedRep(OWL_2, Turtle, TXT));

    KnowledgeCarrier kc = JSonUtil.parseJson(out,KnowledgeCarrier.class)
        .orElseGet(Assertions::fail);

    String ttlStr = kc.asString()
        .orElseGet(Assertions::fail);
    assertTrue(Util.isNotEmpty(ttlStr));

    Model m = ModelFactory.createDefaultModel().read(
        new ByteArrayInputStream(ttlStr.getBytes()),
        null,
        "TTL"
    );
    JenaUtil.asString(m);
    List<Resource> assets = m.listResourcesWithProperty(RDF.type,ResourceFactory.createResource(SparqlIndex.ASSET_URI.toString()))
        .filterKeep(r -> r.getURI().contains(assetId.toString()))
        .toList();
    assertEquals(1, assets.size());
  }

  @Test
  void testSurrogateXMLSerialization() {
    UUID assetId = UUID.randomUUID();
    populateRepositoryWithRedirectables(assetId);

    String out = executeRequest("/cat/assets/" + assetId, "application/xml");
    XMLUtil.loadXMLDocument(out)
        .orElseGet(Assertions::fail);
  }

  @Test
  void testSurrogateJSONSerialization() {
    UUID assetId = UUID.randomUUID();
    populateRepositoryWithRedirectables(assetId);

    String out = executeRequest("/cat/assets/" + assetId, "application/json");
    JSonUtil.readJson(out, KnowledgeAsset.class)
        .orElseGet(Assertions::fail);
  }


  private String executeRequest(String url, String accept) {
    HttpGet request =
        new HttpGet("http://localhost:" + port + url);
    request.addHeader(HttpHeaders.ACCEPT, accept);
    return executeModelRequest(request);
  }

  private String executeModelRequest(String url, String xAccept) {
    HttpGet request =
        new HttpGet("http://localhost:" + port + url);
    request.addHeader("X-Accept", xAccept);
    return executeModelRequest(request);
  }

  private String executeModelRequest(HttpGet request) {
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
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
