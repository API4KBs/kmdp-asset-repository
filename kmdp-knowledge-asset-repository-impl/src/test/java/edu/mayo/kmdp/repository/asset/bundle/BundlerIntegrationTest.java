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
package edu.mayo.kmdp.repository.asset.bundle;


import static edu.mayo.ontology.taxonomies.kao.rel.dependencyreltype.DependencyTypeSeries.Depends_On;
import static edu.mayo.ontology.taxonomies.kao.rel.dependencyreltype.DependencyTypeSeries.Imports;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.HL7_ELM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.metadata.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.surrogate.Dependency;
import edu.mayo.kmdp.metadata.surrogate.Representation;
import edu.mayo.kmdp.metadata.surrogate.resources.KnowledgeAsset;
import edu.mayo.kmdp.repository.asset.SemanticRepoAPITestBase;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetCatalogApi;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryApi;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRetrievalApi;
import edu.mayo.kmdp.repository.asset.client.ApiClientFactory;
import edu.mayo.kmdp.util.ws.JsonRestWSUtils.WithFHIR;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;

class BundlerIntegrationTest extends SemanticRepoAPITestBase {

  private ApiClientFactory apiClientFactory = new ApiClientFactory("http://localhost:11111", WithFHIR.NONE);
  private KnowledgeAssetRepositoryApi repo = KnowledgeAssetRepositoryApi.newInstance(apiClientFactory);
  private KnowledgeAssetCatalogApi catalog = KnowledgeAssetCatalogApi.newInstance(apiClientFactory);
  private KnowledgeAssetRetrievalApi lib = KnowledgeAssetRetrievalApi.newInstance(apiClientFactory);


  @Test
  void testBundleOnlyOne() {
    UUID u1 = UUID.nameUUIDFromBytes("1".getBytes());

    catalog.setVersionedKnowledgeAsset(u1, "2", new KnowledgeAsset().
        withCarriers(new ComputableKnowledgeArtifact().
            withRepresentation(new Representation().withLanguage(
                HL7_ELM))));
    repo.addKnowledgeAssetCarrier(u1, "2", "HI!".getBytes());

    List<KnowledgeCarrier> carriers = lib.getKnowledgeArtifactBundle(u1, "2",
        Depends_On.getTag(), -1, null).getOptionalValue()
        .orElseGet(Collections::emptyList);

    assertEquals(1, carriers.size());
  }

  @Test
  void testBundleWithDependency() {
    UUID ua = UUID.nameUUIDFromBytes("a".getBytes());
    UUID u1 = UUID.nameUUIDFromBytes("1".getBytes());
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset ka = new KnowledgeAsset().
        withCarriers(new ComputableKnowledgeArtifact().
            withRepresentation(
                new Representation().withLanguage(HL7_ELM))).
        withAssetId(DatatypeHelper.uri("http:/some/uri/", ua.toString(), "b"));

    catalog.setVersionedKnowledgeAsset(ua, "b", ka);

    catalog.setVersionedKnowledgeAsset(u1, "2", new KnowledgeAsset().
        withCarriers(new ComputableKnowledgeArtifact().
            withRepresentation(
                new Representation().withLanguage(HL7_ELM))).
        withAssetId(DatatypeHelper.uri("http:/some/uri/", u1.toString(), "2")).
        withRelated(new Dependency().withRel(Imports).withTgt(ka)));

    repo.addKnowledgeAssetCarrier(ua, "b", "Hi!".getBytes());
    repo.addKnowledgeAssetCarrier(u1, "2", "There!".getBytes());

    List<KnowledgeCarrier> carriers = lib
        .getKnowledgeArtifactBundle(u1, "2", Imports.getTag(), -1, "")
        .getOptionalValue()
        .orElseGet(Collections::emptyList);

    assertEquals(2, carriers.size());
    List<String> strings = carriers.stream().map(BinaryCarrier.class::cast)
        .map(BinaryCarrier::getEncodedExpression).map(String::new).collect(Collectors.toList());

    assertTrue(strings.contains("Hi!"));
    assertTrue(strings.contains("There!"));
  }

  @Test
  void testBundleWithDependencyThreeDeep() {
    UUID ua = UUID.nameUUIDFromBytes("a".getBytes());
    UUID u1 = UUID.nameUUIDFromBytes("1".getBytes());
    UUID uq = UUID.nameUUIDFromBytes("q".getBytes());

    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset ka1 = new KnowledgeAsset().
        withCarriers(new ComputableKnowledgeArtifact().
            withRepresentation(
                new Representation().withLanguage(HL7_ELM)))
        .withAssetId(DatatypeHelper.uri("http:/some/uri/", ua.toString(), "b"));

    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset ka2 = new KnowledgeAsset().
        withCarriers(new ComputableKnowledgeArtifact().
            withRepresentation(
                new Representation().withLanguage(HL7_ELM)))
        .withAssetId(DatatypeHelper.uri("http:/some/uri/", uq.toString(), "r"))
        .withRelated(new Dependency().withRel(Imports).withTgt(ka1));

    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset ka3 = new KnowledgeAsset().
        withCarriers(new ComputableKnowledgeArtifact().
            withRepresentation(
                new Representation().withLanguage(HL7_ELM)))
        .withAssetId(DatatypeHelper.uri("http:/some/uri/", u1.toString(), "2"))
        .withRelated(
            new Dependency().withRel(Imports).withTgt(ka2));

    catalog.setVersionedKnowledgeAsset(ua, "b", ka1);
    catalog.setVersionedKnowledgeAsset(uq, "r", ka2);
    catalog.setVersionedKnowledgeAsset(u1, "2", ka3);

    repo.addKnowledgeAssetCarrier(ua, "b", "Hi!".getBytes());
    repo.addKnowledgeAssetCarrier(u1, "2", "There!".getBytes());
    repo.addKnowledgeAssetCarrier(uq, "r", "Zebra!".getBytes());

    List<KnowledgeCarrier> carriers = lib
        .getKnowledgeArtifactBundle(u1, "2", Imports.getTag(), -1, "")
        .getOptionalValue()
        .orElseGet(Collections::emptyList);

    assertEquals(3, carriers.size());
    List<String> strings = carriers.stream().map(BinaryCarrier.class::cast)
        .map(BinaryCarrier::getEncodedExpression).map(String::new).collect(Collectors.toList());

    assertTrue(strings.contains("Hi!"));
    assertTrue(strings.contains("There!"));
    assertTrue(strings.contains("Zebra!"));
  }

}
