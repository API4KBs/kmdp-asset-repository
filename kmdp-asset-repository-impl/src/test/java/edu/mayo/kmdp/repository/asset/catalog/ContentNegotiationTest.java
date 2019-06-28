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

import static edu.mayo.kmdp.SurrogateBuilder.id;
import static edu.mayo.ontology.taxonomies.krformat._2018._08.SerializationFormat.XML_1_1;
import static edu.mayo.ontology.taxonomies.krlanguage._2018._08.KnowledgeRepresentationLanguage.HTML;
import static edu.mayo.ontology.taxonomies.krlanguage._2018._08.KnowledgeRepresentationLanguage.KNART_1_3;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;

import edu.mayo.kmdp.metadata.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.surrogate.InlinedRepresentation;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.surrogate.Representation;
import edu.mayo.kmdp.repository.asset.IntegrationTestBase;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetCatalogApi;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryApi;
import edu.mayo.kmdp.repository.asset.client.ApiClientFactory;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.ws.JsonRestWSUtils.WithFHIR;
import edu.mayo.ontology.taxonomies.iso639_1_languagecodes._20170801.Language;
import edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory._1_0.KnowledgeAssetCategory;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype._1_0.KnowledgeAssetType;
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
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.omg.spec.api4kp._1_0.services.tranx.ModelMIMECoder;

public class ContentNegotiationTest extends IntegrationTestBase {


  private ApiClientFactory webClientFactory = new ApiClientFactory("http://localhost:11111",
      WithFHIR.NONE);

  protected KnowledgeAssetCatalogApi ckac = KnowledgeAssetCatalogApi
      .newInstance(webClientFactory);
  protected KnowledgeAssetRepositoryApi repo = KnowledgeAssetRepositoryApi
      .newInstance(webClientFactory);


  @Test
  public void testContentNegotiation() {
    UUID assetId = UUID.nameUUIDFromBytes("1".getBytes());
    String versionTag = "1";

    KnowledgeDocument knart = buildExampleArtifact();
    String knartXML = serializeExample(knart);
    assertNotEquals("N/A", knartXML);

    KnowledgeAsset asset = buildAsset(assetId,versionTag,knartXML);

    assertTrue(ckac.listKnowledgeAssets(null,null,null,null)
        .getOptionalValue().get().isEmpty());
    ckac.setVersionedKnowledgeAsset(assetId,versionTag,asset);

    Answer<KnowledgeCarrier> ans = repo.getCanonicalKnowledgeAssetCarrier(
        assetId,
        versionTag,
        ModelMIMECoder.encode(rep(KNART_1_3,
            XML_1_1)));

    assertTrue(ans.isSuccess());
    assertTrue(ans.getOptionalValue().isPresent());

    BinaryCarrier bc = (BinaryCarrier) ans.getOptionalValue().get();

    String x = new String(bc.getEncodedExpression());

    Answer<KnowledgeCarrier> ans2 = repo.getCanonicalKnowledgeAssetCarrier(
        assetId,
        versionTag,
        ModelMIMECoder.encode(rep(HTML)));

    assertTrue(ans2.isSuccess());

  }

  private KnowledgeAsset buildAsset(UUID assetId, String versionTag, String inlined) {
    return new edu.mayo.kmdp.metadata.surrogate.resources.KnowledgeAsset()
        .withAssetId(id(assetId, versionTag))
        .withFormalCategory(KnowledgeAssetCategory.Rules_Policies_And_Guidelines)
        .withFormalType(KnowledgeAssetType.Clinical_Rule)
        .withName("Mock Rule")
        .withCarriers(new ComputableKnowledgeArtifact()
            .withArtifactId(id(UUID.randomUUID(), versionTag))
            .withLocalization(Language.English)
            .withName("Mock Rule - KNAR version")
            .withRepresentation(new Representation()
                .withLanguage(KNART_1_3)
                .withFormat(XML_1_1))
            .withInlined(new InlinedRepresentation().withExpr(inlined))
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
