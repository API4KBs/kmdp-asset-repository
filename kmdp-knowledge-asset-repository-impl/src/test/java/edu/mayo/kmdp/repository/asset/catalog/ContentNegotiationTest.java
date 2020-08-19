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
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.assetId;
import static org.omg.spec.api4kp.taxonomy.iso639_2_languagecode._20190201.Language.English;
import static org.omg.spec.api4kp.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Rules_Policies_And_Guidelines;
import static org.omg.spec.api4kp.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Terminology_Ontology_And_Assertional_KBs;
import static org.omg.spec.api4kp.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Clinical_Rule;
import static org.omg.spec.api4kp.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Factual_Knowledge;
import static org.omg.spec.api4kp.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.KNART_1_3;

import edu.mayo.kmdp.repository.asset.SemanticRepoAPITestBase;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.ws.JsonRestWSUtils.WithFHIR;
import java.net.URI;
import java.util.Collections;
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

public class ContentNegotiationTest extends SemanticRepoAPITestBase {

  private KnowledgeAssetRepositoryApi repo;
  private KnowledgeAssetCatalogApi ckac;

  @BeforeEach
  void init() {
    ApiClientFactory apiClientFactory = new ApiClientFactory("http://localhost:" + port,
        WithFHIR.NONE);

    repo = KnowledgeAssetRepositoryApi.newInstance(apiClientFactory);
    ckac = KnowledgeAssetCatalogApi.newInstance(apiClientFactory);
  }



  @Test
  public void testContentNegotiation() {
    UUID assetId = UUID.nameUUIDFromBytes("1".getBytes());
    String versionTag = "1";

    KnowledgeDocument knart = buildExampleArtifact();
    String knartXML = serializeExample(knart);
    assertNotEquals("N/A", knartXML);

    KnowledgeAsset asset = buildComputableAsset(assetId,versionTag,knartXML);

    ckac.setKnowledgeAssetVersion(assetId,versionTag,asset);

    Answer<KnowledgeCarrier> ans = repo.getCanonicalKnowledgeAssetCarrier(
        assetId,
        versionTag,
        ModelMIMECoder.encode(rep(KNART_1_3, XML_1_1)));

    assertTrue(ans.isSuccess());
    assertTrue(ans.getOptionalValue().isPresent());

    Answer<KnowledgeCarrier> ans2 = repo.getCanonicalKnowledgeAssetCarrier(
        assetId,
        versionTag,
        ModelMIMECoder.encode(rep(HTML, TXT)));

    assertTrue(ans2.isSuccess());
    String txEd = ans2.flatOpt(AbstractCarrier::asString).orElse("");
    assertTrue(txEd.startsWith("<html>"));
  }


  @Test
  public void testContentNegotiationWithHTML() {
    UUID assetId = UUID.nameUUIDFromBytes("2".getBytes());
    String versionTag = "2";

    KnowledgeAsset asset = buildAssetWithHTMLCarrier(assetId,versionTag);
    assertFalse(asset.getCarriers().isEmpty());
    URI redirect = asset.getCarriers().get(0).getLocator();
    assertNotNull(redirect);

    ckac.setKnowledgeAssetVersion(assetId,versionTag,asset);

    Answer<KnowledgeCarrier> ans = repo.getCanonicalKnowledgeAssetCarrier(
        assetId,
        versionTag,
        "text/html,application/xhtml+xml,application/xml;q=0.9,"
            + "image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");

    assertTrue(ans.isSuccess());
    assertTrue(HTML.sameAs(ans.get().getRepresentation().getLanguage()));
  }

  private KnowledgeAsset buildComputableAsset(UUID assetId, String versionTag, String inlined) {
    return new KnowledgeAsset()
        .withAssetId(assetId(assetId, versionTag))
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
        .withAssetId(assetId(assetId, versionTag))
        .withFormalCategory(Terminology_Ontology_And_Assertional_KBs)
        .withFormalType(Factual_Knowledge)
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
