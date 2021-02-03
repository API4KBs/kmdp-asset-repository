package edu.mayo.kmdp.repository.asset.catalog;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.IdentifierConstants.VERSION_ZERO;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.assetId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomArtifactId;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Terminology_Ontology_And_Assertional_KBs;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Factual_Knowledge;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.Turtle;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
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
import java.util.Collections;
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
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.client.ApiClientFactory;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.repository.KnowledgeAssetCatalog;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
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
  public void testGetKnowledgeAssetWithHTMLViaRedirect() {
    UUID assetId = UUID.randomUUID();
    populateRepository(assetId);

    String out = executeRequest(
        "/cat/assets/" + assetId + "/versions/" + VERSION_ZERO,
        "text/html");

    org.jsoup.nodes.Document doh = Jsoup.parse(out);
    assertNotNull(doh);
  }

  @Test
  public void testCanonicalSurrogate() {
    UUID assetId = UUID.randomUUID();
    populateRepositoryWithRedirectables(assetId);

    String out = executeRequest(
        "/cat/assets/" + assetId + "/versions/" + VERSION_ZERO + "/surrogate",
        "application/xml");

    XMLUtil.loadXMLDocument(out)
        .orElseGet(Assertions::fail);
  }

  @Test
  public void testCanonicalSurrogateJSON() {
    UUID assetId = UUID.randomUUID();
    populateRepositoryWithRedirectables(assetId);

    String out = executeRequest(
        "/cat/assets/" + assetId + "/versions/" + VERSION_ZERO + "/surrogate",
        "application/json");

    KnowledgeCarrier kc = JSonUtil.readJson(out, KnowledgeCarrier.class)
        .orElseGet(Assertions::fail);
    Answer<KnowledgeCarrier> ans = new Surrogate2Parser()
        .applyLift(
            kc,
            Abstract_Knowledge_Expression,
            codedRep(Knowledge_Asset_Surrogate_2_0),
            null);
    assertTrue(ans.isSuccess());
  }

  @Test
  public void testCanonicalSurrogateWithWrappedNegotiation() {
    UUID assetId = UUID.randomUUID();
    populateRepositoryWithRedirectables(assetId);

    // the KC will return as JSON (default format)
    // the KC will contain a HTML document
    String out = executeModelRequest(
        "/cat/assets/" + assetId + "/versions/" + VERSION_ZERO + "/surrogate",
        "text/html");

    String s = JSonUtil.parseJson(out, KnowledgeCarrier.class)
        .flatMap(AbstractCarrier::asString)
        .orElseGet(Assertions::fail);
    org.jsoup.nodes.Document doh = Jsoup.parse(s);
    assertNotNull(doh);
  }


  @Test
  public void testCanonicalSurrogateWithHTMLOut() {
    UUID assetId = UUID.randomUUID();
    populateRepositoryWithRedirectables(assetId);

    // the KC will return as JSON (default format)
    // the KC will contain a HTML document
    String out = executeRequest(
        "/cat/assets/" + assetId + "/versions/" + VERSION_ZERO + "/surrogate",
        "text/html");

    org.jsoup.nodes.Document doh = Jsoup.parse(out);
    assertNotNull(doh);
  }


  @Test
  public void testPointerXMLSerialization() {
    populateRepositoryWithRedirectables(UUID.randomUUID());

    String out = executeRequest("/cat/assets", "application/xhtml+xml, application/xml;q=0.9");
    Document dox = XMLUtil.loadXMLDocument(out)
        .orElseGet(Assertions::fail);

    Node n = new XPathUtil().xNode(dox, "//href");
    assertNotNull(n.getTextContent());
    assertFalse(Util.isEmpty(n.getTextContent()));
    assertTrue(n.getTextContent().contains("http://localhost:" + port));

  }

  @Test
  void testGraphXMLSerialization() {
    UUID assetId = UUID.randomUUID();
    populateRepositoryWithRedirectables(assetId);

    String out = executeRequest("/cat/graph", "application/xml");
    XMLUtil.loadXMLDocument(out)
        .orElseGet(Assertions::fail);
  }


  @Test
  void testGraphJSONSerialization() {
    UUID assetId = UUID.randomUUID();
    populateRepositoryWithRedirectables(assetId);

    String out = executeRequest("/cat/graph", "application/json");
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

    KnowledgeCarrier kc = JSonUtil.parseJson(out, KnowledgeCarrier.class)
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
    List<Resource> assets = m.listResourcesWithProperty(RDF.type,
        ResourceFactory.createResource(SparqlIndex.ASSET_URI.toString()))
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


  @Test
  void testSurrogateWithCarrierLatestVersion() {
    UUID assetId = UUID.randomUUID();
    populateRepositoryWithRedirectables(assetId);

    String ptrStr = executeRequest(
        "/cat/assets/" + assetId + "/versions/" + VERSION_ZERO + "/carriers",
        "application/json");
    List<Pointer> ptrs = JSonUtil.parseJsonList(
        new ByteArrayInputStream(ptrStr.getBytes()),
        Pointer.class)
        .orElse(Collections.emptyList());
    assertFalse(ptrs.isEmpty());
    Pointer ptr = ptrs.get(0);

    String carrierId = ptr.getUuid().toString();

    String out = executeModelRequest(
        "/cat/assets/" + assetId + "/versions/" + VERSION_ZERO + "/carriers/" + carrierId,
        "text/html", "text/html");
    assertTrue(out.trim().startsWith("<html>"));

    String outJ = executeModelRequest(
        "/cat/assets/" + assetId + "/versions/" + VERSION_ZERO + "/carriers/" + carrierId,
        "application/json", "text/html");
    JSonUtil.parseJson(outJ,KnowledgeCarrier.class)
        .orElseGet(Assertions::fail);
  }


  @Test
  void testSurrogateWithDefaultCarrier() {
    UUID assetId = UUID.randomUUID();
    populateRepositoryWithRedirectables(assetId);

    String str = executeRequest(
        "/cat/assets/" + assetId + "/versions/" + VERSION_ZERO + "/carrier",
        "text/html");
    assertTrue(str.startsWith("<html>"));

    // the application/json applies to the KnowledgeCarrier, not to the actual content
    // the available carrier, in HTML, will be wrapped in JSON and returned
    assertDoesNotThrow(() -> tryExecuteRequest(
        "/cat/assets/" + assetId + "/versions/" + VERSION_ZERO + "/carrier",
        "application/json"));

  }


  @Test
  void testSpecificCarrierVersion() {
    UUID assetId = UUID.randomUUID();
    populateRepositoryWithRedirectables(assetId);

    String ptrStr = executeRequest(
        "/cat/assets/" + assetId + "/versions/" + VERSION_ZERO + "/carriers",
        "application/json");
    List<Pointer> ptrs = JSonUtil.parseJsonList(
        new ByteArrayInputStream(ptrStr.getBytes()),
        Pointer.class)
        .orElse(Collections.emptyList());
    assertFalse(ptrs.isEmpty());
    Pointer ptr = ptrs.get(0);

    String str = executeRequest(
        "/cat/assets/" + assetId + "/versions/" + VERSION_ZERO
            + "/carriers/" + ptr.getUuid() + "/versions/" + ptr.getVersionTag(),
        "text/html");
    assertTrue(str.startsWith("<html>"));
  }

  @Test
  void testSpecificSurrogateVersion() {
    UUID assetId = UUID.randomUUID();
    populateRepositoryWithRedirectables(assetId);

    String ptrStr = executeRequest(
        "/cat/assets/" + assetId + "/versions/" + VERSION_ZERO + "/surrogates",
        "application/json");
    List<Pointer> ptrs = JSonUtil.parseJsonList(
        new ByteArrayInputStream(ptrStr.getBytes()),
        Pointer.class)
        .orElse(Collections.emptyList());
    assertFalse(ptrs.isEmpty());

    Pointer ptr = ptrs.stream()
        .filter(p -> ModelMIMECoder.decode(p.getMimeType())
            .map(rep -> rep.getLanguage().sameAs(HTML)).orElse(false))
        .findFirst()
        .orElseGet(Assertions::fail);

    String str = executeModelRequest(
        "/cat/assets/" + assetId + "/versions/" + VERSION_ZERO
            + "/surrogates/" + ptr.getUuid() + "/versions/" + ptr.getVersionTag(),
        "text/html", ptr.getMimeType());

    // Despite claiming HTML form, the surrogate metadata actually redirects to /cat
    // which returns a JSON payload. This inconsistency is only due to the test configuration
    // and the need to redirect to a predictable URL distinct from the one used for the carriers...
    JSonUtil.parseJson(str, KnowledgeAssetCatalog.class)
        .orElseGet(Assertions::fail);
  }



  @Test
  void testSpecificSurrogateVersionWithNoRedirect() {
    UUID assetId = UUID.randomUUID();
    populateRepository(assetId);

    String ptrStr = executeRequest(
        "/cat/assets/" + assetId + "/versions/" + VERSION_ZERO + "/surrogates",
        "application/json");
    List<Pointer> ptrs = JSonUtil.parseJsonList(
        new ByteArrayInputStream(ptrStr.getBytes()),
        Pointer.class)
        .orElse(Collections.emptyList());
    assertFalse(ptrs.isEmpty());

    Pointer ptr = ptrs.stream()
        .filter(p -> ModelMIMECoder.decode(p.getMimeType())
            .map(rep -> rep.getLanguage().sameAs(Knowledge_Asset_Surrogate_2_0))
            .orElse(false))
        .findFirst()
        .orElseGet(Assertions::fail);

    String str = executeRequest(
        "/cat/assets/" + assetId + "/versions/" + VERSION_ZERO
            + "/surrogates/" + ptr.getUuid() + "/versions/" + ptr.getVersionTag(),
        "text/html");

    assertTrue(str.startsWith("<html>"));
  }

  @Test
  void testSpecificSurrogateVersionWithNoRedirectWrapped() {
    UUID assetId = UUID.randomUUID();
    populateRepository(assetId);

    String ptrStr = executeRequest(
        "/cat/assets/" + assetId + "/versions/" + VERSION_ZERO + "/surrogates",
        "application/json");
    List<Pointer> ptrs = JSonUtil.parseJsonList(
        new ByteArrayInputStream(ptrStr.getBytes()),
        Pointer.class)
        .orElse(Collections.emptyList());
    assertFalse(ptrs.isEmpty());

    Pointer ptr = ptrs.stream()
        .filter(p -> ModelMIMECoder.decode(p.getMimeType())
            .map(rep -> rep.getLanguage().sameAs(Knowledge_Asset_Surrogate_2_0))
            .orElse(false))
        .findFirst()
        .orElseGet(Assertions::fail);

    String str = executeModelRequest(
        "/cat/assets/" + assetId + "/versions/" + VERSION_ZERO
            + "/surrogates/" + ptr.getUuid() + "/versions/" + ptr.getVersionTag(),
        "application/json", "text/html");

    KnowledgeCarrier kc = JSonUtil.parseJson(str, KnowledgeCarrier.class)
        .orElseGet(Assertions::fail);
    assertTrue(kc.asString().orElse("").startsWith("<html>"));
  }


  private String executeRequest(String url, String accept) {
    try {
      return tryExecuteRequest(url, accept);
    } catch (StatusCodeException e) {
      fail(e);
    }
    return "";
  }

  private String tryExecuteRequest(String url, String accept)
      throws StatusCodeException {
    HttpGet request =
        new HttpGet("http://localhost:" + port + url);
    request.addHeader(HttpHeaders.ACCEPT, accept);
      return executeModelRequest(request);
  }

  private String executeModelRequest(String url, String xAccept) {
    HttpGet request =
        new HttpGet("http://localhost:" + port + url);
    request.addHeader("X-Accept", xAccept);
    try {
      return executeModelRequest(request);
    } catch (StatusCodeException e) {
      fail(e);
    }
    return "";
  }

  private String executeModelRequest(String url, String accept, String xAccept) {
    HttpGet request =
        new HttpGet("http://localhost:" + port + url);
    request.addHeader(HttpHeaders.ACCEPT, accept);
    request.addHeader("X-Accept", xAccept);
    try {
      return executeModelRequest(request);
    } catch (StatusCodeException e) {
      fail(e);
    }
    return "";
  }

  private String executeModelRequest(HttpGet request) throws StatusCodeException {
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      try (CloseableHttpResponse resp = httpClient.execute(request)) {
        if (resp.getStatusLine().getStatusCode() / 100 == 5) {
          fail("Internal Server error");
        }
        if (resp.getStatusLine().getStatusCode() / 100 != 2) {
          throw new StatusCodeException(
              resp.getStatusLine().getStatusCode(),
              resp.getStatusLine().getReasonPhrase());
        }
        return EntityUtils.toString(resp.getEntity());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "";
  }

  private void populateRepositoryWithRedirectables(UUID pockId) {
    Answer<Void> ans1 = rpo.setKnowledgeAssetVersion(
        pockId,
        VERSION_ZERO,
        pocSurrogate(pockId, VERSION_ZERO, true));
    assertTrue(ans1.isSuccess());
  }

  private void populateRepository(UUID pockId) {
    Answer<Void> ans1 = rpo.setKnowledgeAssetVersion(
        pockId,
        VERSION_ZERO,
        pocSurrogate(pockId, VERSION_ZERO, false));
    assertTrue(ans1.isSuccess());
  }

  private KnowledgeAsset pocSurrogate(
      UUID pockId,
      String version,
      boolean withHTMLSurrogateRedirect) {
    KnowledgeAsset ka = new KnowledgeAsset()
        .withAssetId(assetId(testAssetNS(), pockId, version))
        .withFormalCategory(Terminology_Ontology_And_Assertional_KBs)
        .withFormalType(Factual_Knowledge)
        .withName("Test section of content")
        .withSurrogate(
            new KnowledgeArtifact()
                .withArtifactId(randomArtifactId())
                .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0, JSON))
        )
        .withCarriers(
            new KnowledgeArtifact()
                .withArtifactId(randomArtifactId())
                .withLocator(URI.create("http://localhost:" + port + "/cat/assets/"
                    + pockId + "/versions/" + version + "/surrogate"))
                .withRepresentation(rep(HTML, TXT))
        );
    if (withHTMLSurrogateRedirect) {
      ka.withSurrogate(
          new KnowledgeArtifact()
              .withArtifactId(randomArtifactId())
              .withLocator(URI.create("http://localhost:" + port + "/cat"))
              .withRepresentation(rep(HTML, TXT)));
    }
    return ka;
  }

  private static class StatusCodeException extends Exception {

    private int code;
    private String msg;

    public StatusCodeException(int statusCode, String reasonPhrase) {
      this.code = statusCode;
      this.msg = reasonPhrase;
    }

    public int getCode() {
      return code;
    }

    public String getMsg() {
      return msg;
    }

    @Override
    public String toString() {
      return "StatusCodeException{" +
          "code=" + code +
          ", msg='" + msg + '\'' +
          '}';
    }
  }
}
