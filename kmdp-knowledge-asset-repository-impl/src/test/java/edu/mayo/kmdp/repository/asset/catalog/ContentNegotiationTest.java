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

import static edu.mayo.kmdp.SurrogateBuilder.assetId;
import static edu.mayo.ontology.taxonomies.iso639_2_languagecodes.LanguageSeries.English;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory.KnowledgeAssetCategorySeries.Rules_Policies_And_Guidelines;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory.KnowledgeAssetCategorySeries.Terminology_Ontology_And_Assertional_KBs;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Clinical_Rule;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Factual_Knowledge;
import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.TXT;
import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.XML_1_1;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.KNART_1_3;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;

import edu.mayo.kmdp.metadata.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.surrogate.InlinedRepresentation;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.surrogate.Representation;
import edu.mayo.kmdp.repository.asset.SemanticRepoAPITestBase;
import edu.mayo.kmdp.repository.asset.v3.KnowledgeAssetCatalogApi;
import edu.mayo.kmdp.repository.asset.v3.KnowledgeAssetRepositoryApi;
import edu.mayo.kmdp.repository.asset.v3.client.ApiClientFactory;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.ws.JsonRestWSUtils.WithFHIR;
import java.net.URI;
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
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.omg.spec.api4kp._1_0.services.tranx.ModelMIMECoder;

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

    ckac.setVersionedKnowledgeAsset(assetId,versionTag,asset);

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
    Optional<String> txEd = ans2.get().asString();
    assertTrue(txEd.isPresent());
    assertTrue(txEd.get().startsWith("<html>"));
  }


  @Test
  public void testContentNegotiationWithHTML() {
    UUID assetId = UUID.nameUUIDFromBytes("2".getBytes());
    String versionTag = "2";

    KnowledgeAsset asset = buildAssetWithHTMLCarrier(assetId,versionTag);
    assertFalse(asset.getCarriers().isEmpty());
    URI redirect = asset.getCarriers().get(0).getLocator();
    assertNotNull(redirect);

    ckac.setVersionedKnowledgeAsset(assetId,versionTag,asset);

    Answer<KnowledgeCarrier> ans = repo.getCanonicalKnowledgeAssetCarrier(
        assetId,
        versionTag,
        "text/html,application/xhtml+xml,application/xml;q=0.9,"
            + "image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");

    assertTrue(ans.isSuccess());
    assertTrue(HTML.sameAs(ans.get().getRepresentation().getLanguage()));
  }

  private KnowledgeAsset buildComputableAsset(UUID assetId, String versionTag, String inlined) {
    return new edu.mayo.kmdp.metadata.surrogate.resources.KnowledgeAsset()
        .withAssetId(assetId(assetId, versionTag))
        .withFormalCategory(Rules_Policies_And_Guidelines)
        .withFormalType(Clinical_Rule)
        .withName("Mock Rule")
        .withCarriers(new ComputableKnowledgeArtifact()
            .withArtifactId(assetId(UUID.randomUUID(), versionTag))
            .withLocalization(English)
            .withName("Mock Rule - KNAR version")
            .withRepresentation(new Representation()
                .withLanguage(KNART_1_3)
                .withFormat(XML_1_1))
            .withInlined(new InlinedRepresentation().withExpr(inlined))
        );
  }

  private KnowledgeAsset buildAssetWithHTMLCarrier(UUID assetId, String versionTag) {
    return new edu.mayo.kmdp.metadata.surrogate.resources.KnowledgeAsset()
        .withAssetId(assetId(assetId, versionTag))
        .withFormalCategory(Terminology_Ontology_And_Assertional_KBs)
        .withFormalType(Factual_Knowledge)
        .withName("Stuff")
        .withCarriers(new ComputableKnowledgeArtifact()
            .withArtifactId(assetId(UUID.randomUUID(), versionTag))
            .withLocalization(English)
            .withName("Some Text")
            .withRepresentation(new Representation()
                .withLanguage(HTML)
                .withFormat(TXT))
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
