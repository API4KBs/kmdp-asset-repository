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
package edu.mayo.kmdp.repository.asset;

import static edu.mayo.kmdp.metadata.v2.surrogate.SurrogateBuilder.randomArtifactId;
import static edu.mayo.kmdp.repository.asset.negotiation.ContentNegotiationHelper.decodePreferences;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory.KnowledgeAssetCategorySeries.Rules_Policies_And_Guidelines;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory.KnowledgeAssetCategorySeries.Terminology_Ontology_And_Assertional_KBs;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Clinical_Rule;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Factual_Knowledge;
import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.TXT;
import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.XML_1_1;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.KNART_1_3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;

import edu.mayo.kmdp.metadata.v2.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.v2.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.v2.surrogate.SurrogateBuilder;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.repository.asset.negotiation.ContentNegotiationHelper;
import edu.mayo.ontology.taxonomies.api4kp.responsecodes.ResponseCodeSeries;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.id.SemanticIdentifier;
import org.omg.spec.api4kp._1_0.services.SyntacticRepresentation;

class InternalSurrogateNegotiationTest extends RepositoryTestBase {

  private static UUID pockId = UUID.randomUUID();
  private static UUID rulId = UUID.randomUUID();

  @Test
  void testPopulation() {
    populateRepositoryWithRedirectables();
    Answer<KnowledgeAsset> a1 = semanticRepository.getKnowledgeAsset(pockId);
    Answer<KnowledgeAsset> a2 = semanticRepository.getKnowledgeAsset(rulId);

    assertTrue(a1.isSuccess());
    assertTrue(a2.isSuccess());
  }


  @Test
  void testHeaders() {
    populateRepositoryWithRedirectables();

    Answer<KnowledgeAsset> a3 = semanticRepository.getKnowledgeAsset(pockId,
        "application/json, text/html;q=0.9");
    assertEquals(ResponseCodeSeries.OK,a3.getOutcomeType());

    Answer<KnowledgeAsset> a1 = semanticRepository.getKnowledgeAsset(pockId,
        "text/html, application/xhtml+xml, application/xml;q=0.9, image/webp, */*;q=0.8");
    assertEquals(ResponseCodeSeries.SeeOther,a1.getOutcomeType());

    Answer<KnowledgeAsset> a2 = semanticRepository.getKnowledgeAsset(pockId,
        "application/json");
    assertEquals(ResponseCodeSeries.OK,a2.getOutcomeType());

  }

  private void populateRepositoryWithRedirectables() {
    String version = "1.0.0";
    Answer<Void> r1 = semanticRepository.setKnowledgeAssetVersion(
        pockId,
        version,
        pocSurrogate(pockId, version));
    assertTrue(r1.isSuccess());

    Answer<Void> r2 = semanticRepository.setKnowledgeAssetVersion(
            rulId,
        version,
            rulSurrogate(rulId, version));
    assertTrue(r2.isSuccess());
  }

  private KnowledgeAsset rulSurrogate(UUID assetId, String version) {
    // Rules' catalog entry in KCMS
    return new KnowledgeAsset()
        .withAssetId(SemanticIdentifier.newId(Registry.BASE_UUID_URN_URI,assetId,version))
        .withFormalCategory(Rules_Policies_And_Guidelines)
        .withFormalType(Clinical_Rule)
        .withName("Test rule")
        .withSurrogate(
            new ComputableKnowledgeArtifact()
                .withArtifactId(randomArtifactId())
                .withLocator(URI.create("http://www.google.com"))
                .withRepresentation(rep(HTML,TXT))
        )
        .withCarriers(
            new ComputableKnowledgeArtifact()
                .withArtifactId(randomArtifactId())
                .withLocator(URI.create("http://www.myrepo/rule0/carrier?format=xml"))
                .withRepresentation(rep(KNART_1_3,XML_1_1)
                ),
            new ComputableKnowledgeArtifact()
                .withArtifactId(randomArtifactId())
                .withLocator(URI.create("http://www.myrepo/rule0/carrier"))
                .withRepresentation(rep(HTML,TXT)
                )
        );
  }

  private KnowledgeAsset pocSurrogate(UUID pockId, String version) {
    return new KnowledgeAsset()
        .withAssetId(SemanticIdentifier.newId(Registry.BASE_UUID_URN_URI,pockId,version))
        .withFormalCategory(Terminology_Ontology_And_Assertional_KBs)
        .withFormalType(Factual_Knowledge)
        .withName("Test section of content")
        .withSurrogate(
            new ComputableKnowledgeArtifact()
                .withArtifactId(randomArtifactId())
                .withLocator(URI.create("http://www.google.com"))
                .withRepresentation(rep(HTML,TXT)
                )
        )
        .withCarriers(
            new ComputableKnowledgeArtifact()
                .withArtifactId(randomArtifactId())
                .withLocator(URI.create("http://www.myrepo/section0/carrier"))
                .withRepresentation(rep(HTML,TXT)
                )
        );
  }


  @Test
  void testFormalMIMEDecoding() {
    List<SyntacticRepresentation> reps;

    reps = decodePreferences("");
    assertTrue(reps.isEmpty());

    reps = decodePreferences(null);
    assertTrue(reps.isEmpty());

    reps = decodePreferences("model/bpmn+xml;q=0.9,model/dmn-v12+xml,model/cmmn-v11+xml;q=0.6");
    assertEquals(3,reps.size());
    assertTrue(DMN_1_2.sameAs(reps.get(0).getLanguage()));
  }

}
