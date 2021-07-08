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
package edu.mayo.kmdp.repository.asset.catalog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.assetId;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Rule;
import static org.omg.spec.api4kp._20200801.taxonomy.iso639_2_languagecode._20190201.Language.English;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Rules_Policies_And_Guidelines;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Terminology_Ontology_And_Assertional_KBs;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Grounded_Knowledge;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.KNART_1_3;

import edu.mayo.kmdp.repository.asset.SemanticRepoAPITestBase;
import edu.mayo.kmdp.util.FileUtil;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.kmdp.util.ws.JsonRestWSUtils.WithFHIR;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.hl7.cdsdt.r2.ST;
import org.hl7.elm.r1.And;
import org.hl7.elm.r1.ExpressionRef;
import org.hl7.knowledgeartifact.r1.Condition;
import org.hl7.knowledgeartifact.r1.Condition.ConditionRole;
import org.hl7.knowledgeartifact.r1.ConditionRoleType;
import org.hl7.knowledgeartifact.r1.Conditions;
import org.hl7.knowledgeartifact.r1.KnowledgeDocument;
import org.hl7.knowledgeartifact.r1.Metadata;
import org.hl7.knowledgeartifact.r1.Metadata.ArtifactType;
import org.hl7.knowledgeartifact.r1.ObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetRepositoryApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.client.ApiClientFactory;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder;
import org.w3c.dom.Document;

class ContentNegotiationTest extends SemanticRepoAPITestBase {

  private KnowledgeAssetRepositoryApi repo;
  private KnowledgeAssetCatalogApi ckac;

  @BeforeEach
  protected void init() {
    super.init();
    ApiClientFactory apiClientFactory = new ApiClientFactory("http://localhost:" + port,
        WithFHIR.NONE);

    repo = KnowledgeAssetRepositoryApi.newInstance(apiClientFactory);
    ckac = KnowledgeAssetCatalogApi.newInstance(apiClientFactory);
  }


  @Test
  void testContentNegotiation() {
    UUID assetId = UUID.nameUUIDFromBytes("1".getBytes());
    String versionTag = "1";

    KnowledgeDocument knart = buildExampleArtifact();
    String knartXML = serializeExample(knart);
    assertNotEquals("N/A", knartXML);

    KnowledgeAsset asset = buildComputableAsset(assetId,versionTag,knartXML);


    Answer<Void> set = ckac.setKnowledgeAssetVersion(assetId,versionTag,asset);
    assertTrue(set.isSuccess());

    Answer<KnowledgeCarrier> ans = repo.getKnowledgeAssetVersionCanonicalCarrier(
        assetId,
        versionTag,
        ModelMIMECoder.encode(rep(KNART_1_3, XML_1_1)));

    assertTrue(ans.isSuccess());
    assertTrue(ans.getOptionalValue().isPresent());

    Answer<KnowledgeCarrier> ans2 = repo.getKnowledgeAssetVersionCanonicalCarrier(
        assetId,
        versionTag,
        ModelMIMECoder.encode(rep(HTML, TXT)));

    assertTrue(ans2.isSuccess());
    String txEd = ans2.flatOpt(AbstractCarrier::asString).orElse("");
    assertTrue(txEd.startsWith("<html>"));


    Answer<KnowledgeCarrier> ans3 = repo.getKnowledgeAssetVersionCanonicalCarrier(
        assetId,
        versionTag);

    assertTrue(ans3.isSuccess());
    assertTrue(ans3.getOptionalValue().isPresent());
    assertTrue(KNART_1_3.sameAs(ans3.get().getRepresentation().getLanguage()));
  }

  @Test
  void testContentNegotiationWithXMLBuilder() throws MalformedURLException {
    UUID assetId = UUID.nameUUIDFromBytes("xml".getBytes());
    String versionTag = "1.0.0";

    // Asset primary manifestation is XML based
    KnowledgeDocument knart = buildExampleArtifact();
    KnowledgeAsset asset = buildComputableAsset(assetId, versionTag, serializeExample(knart));

    Answer<Void> set = ckac.setKnowledgeAssetVersion(assetId, versionTag, asset);
    assertTrue(set.isSuccess());

    String srcUrl = "http://localhost:" + port + "/cat/assets/" + assetId + "/carrier/content";

    // load XML
    Optional<Document> dox = XMLUtil.loadXMLDocument(new URL(srcUrl));
    assertTrue(dox.isPresent());

    // openStream triggers the content negotiation, returning HTML
    URL scrUrl2 = new URL(srcUrl);
    try {
      Optional<String> str = FileUtil.read(scrUrl2.openStream());
      assertTrue(str.isPresent());
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }


  @Test
  void testContentNegotiationWithHTML() {
    UUID assetId = UUID.nameUUIDFromBytes("2".getBytes());
    String versionTag = "2";

    KnowledgeAsset asset = buildAssetWithHTMLCarrier(assetId,versionTag);
    assertFalse(asset.getCarriers().isEmpty());
    URI redirect = asset.getCarriers().get(0).getLocator();
    assertNotNull(redirect);

    ckac.setKnowledgeAssetVersion(assetId,versionTag,asset);

    Answer<KnowledgeCarrier> ans = repo.getKnowledgeAssetVersionCanonicalCarrier(
        assetId,
        versionTag,
        "text/html,application/xhtml+xml,application/xml;q=0.9,"
            + "image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");

    assertTrue(ans.isSuccess());
    assertTrue(HTML.sameAs(ans.get().getRepresentation().getLanguage()));
  }

  private KnowledgeAsset buildComputableAsset(UUID assetId, String versionTag, String inlined) {
    return new KnowledgeAsset()
        .withAssetId(assetId(testAssetNS(), assetId, versionTag))
        .withFormalCategory(Rules_Policies_And_Guidelines)
        .withFormalType(Clinical_Rule)
        .withName("Mock Rule")
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(SurrogateBuilder.randomArtifactId())
            .withLocalization(English)
            .withName("Mock Rule - KNART version")
            .withRepresentation(rep(KNART_1_3,XML_1_1))
            .withInlinedExpression(inlined)
        );
  }

  private KnowledgeAsset buildAssetWithHTMLCarrier(UUID assetId, String versionTag) {
    return new KnowledgeAsset()
        .withAssetId(assetId(testAssetNS(), assetId, versionTag))
        .withFormalCategory(Terminology_Ontology_And_Assertional_KBs)
        .withFormalType(Grounded_Knowledge)
        .withName("Stuff")
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(SurrogateBuilder.randomArtifactId())
            .withLocalization(English)
            .withName("Some Text")
            .withRepresentation(rep(HTML,TXT))
            .withLocator(URI.create("http://localhost:" + port))
        );
  }

  private KnowledgeDocument buildExampleArtifact() {
    return new KnowledgeDocument()
        .withMetadata(new Metadata()
            .withTitle(new ST().withValue("Hello World Rule"))
            .withArtifactType(new ArtifactType().withValue(
                org.hl7.knowledgeartifact.r1.ArtifactType.RULE)))
        .withConditions(new Conditions()
            .withCondition(new Condition()
                .withConditionRole(
                    new ConditionRole().withValue(ConditionRoleType.APPLICABLE_SCENARIO))
                .withLogic(new And()
                    .withOperand(new ExpressionRef().withName("A"))
                    .withOperand(new ExpressionRef().withName("B"))
                )
            )
        );
  }


  private String serializeExample(KnowledgeDocument knart) {
    ObjectFactory of = new ObjectFactory();
    return JaxbUtil.marshall(
        Collections.singleton(of.getClass()),
        knart,
        of::createKnowledgeDocument,
        JaxbUtil.defaultProperties())
        .flatMap(Util::asString)
        .orElse("N/A");
  }

}
