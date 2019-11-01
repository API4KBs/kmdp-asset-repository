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

import static edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory.KnowledgeAssetCategorySeries.Rules_Policies_And_Guidelines;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory.KnowledgeAssetCategorySeries.Terminology_Ontology_And_Assertional_KBs;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Clinical_Rule;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Factual_Knowledge;
import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.TXT;
import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.XML_1_1;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.KNART_1_3;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.metadata.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.surrogate.Representation;
import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class InternalSurrogateNegotiationTest extends RepositoryTestBase {

  private static UUID pockId = UUID.randomUUID();
  private static UUID rulId = UUID.randomUUID();

  @Test
  void testPopulation() {
    populateRepositoryWithRedirectables();
    ResponseEntity<KnowledgeAsset> a1 = semanticRepository.getKnowledgeAsset(pockId,null);
    ResponseEntity<KnowledgeAsset> a2 = semanticRepository.getKnowledgeAsset(rulId,null);

    assertTrue(a1.getStatusCode().is2xxSuccessful());
    assertTrue(a2.getStatusCode().is2xxSuccessful());
  }


  @Test
  void testHeaders() {
    populateRepositoryWithRedirectables();

    ResponseEntity<KnowledgeAsset> a3 = semanticRepository.getKnowledgeAsset(pockId,
        "application/json, text/html;q=0.9");
    assertSame(HttpStatus.OK,a3.getStatusCode());

    ResponseEntity<KnowledgeAsset> a1 = semanticRepository.getKnowledgeAsset(pockId,
        "text/html, application/xhtml+xml, application/xml;q=0.9, image/webp, */*;q=0.8");
    assertSame(HttpStatus.SEE_OTHER,a1.getStatusCode());

    ResponseEntity<KnowledgeAsset> a2 = semanticRepository.getKnowledgeAsset(pockId,
        "application/json");
    assertSame(HttpStatus.OK,a2.getStatusCode());

  }

  private void populateRepositoryWithRedirectables() {
    String version = "LATEST";
    ResponseEntity<Void> r1 = semanticRepository.setVersionedKnowledgeAsset(
        pockId,
        version,
        pocSurrogate(pockId, version));
    assertTrue(r1.getStatusCode().is2xxSuccessful());

    ResponseEntity<Void> r2 = semanticRepository.setVersionedKnowledgeAsset(
            rulId,
        version,
            rulSurrogate(rulId, version));
    assertTrue(r2.getStatusCode().is2xxSuccessful());
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
                .withLocator(URI.create("http://www.google.com"))
                .withRepresentation(new Representation()
                    .withLanguage(HTML)
                    .withFormat(TXT))
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
                .withLocator(URI.create("http://www.google.com"))
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
