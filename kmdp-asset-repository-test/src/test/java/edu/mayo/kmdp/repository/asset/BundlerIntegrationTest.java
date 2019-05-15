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
package edu.mayo.kmdp.repository.asset;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.metadata.surrogate.Dependency;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeExpression;
import edu.mayo.kmdp.metadata.surrogate.Representation;
import edu.mayo.kmdp.metadata.surrogate.resources.KnowledgeAsset;
import edu.mayo.ontology.taxonomies.kao.rel.dependencyreltype._20190801.DependencyType;
import edu.mayo.ontology.taxonomies.krlanguage._2018._08.KnowledgeRepresentationLanguage;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;

public class BundlerIntegrationTest extends IntegrationTestBase {

  private ApiClient apiClient = new ApiClient().setBasePath("http://localhost:11111");
  private KnowledgeAssetRepositoryApi repo = KnowledgeAssetRepositoryApi.newInstance(apiClient);


  @Test
  public void testBundleOnlyOne() {
    apiClient.selectHeaderAccept(new String[] {});

    repo.setVersionedKnowledgeAsset("1", "2", new KnowledgeAsset().
        withExpression(new KnowledgeExpression().
            withRepresentation(new Representation().withLanguage(
                KnowledgeRepresentationLanguage.HL7_ELM))));
    repo.addKnowledgeAssetCarrier("1", "2", "HI!".getBytes());

    List<KnowledgeCarrier> carriers = repo.getKnowledgeAssetBundle("1", "2",
        DependencyType.Depends_On.getTag(), -1);

    assertEquals(1, carriers.size());
  }

  @Test
  public void testBundleWithDependency() {
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset ka = new KnowledgeAsset().
        withExpression(new KnowledgeExpression().
            withRepresentation(
                new Representation().withLanguage(KnowledgeRepresentationLanguage.HL7_ELM))).
        withResourceId(DatatypeHelper.uri("http:/some/uri/", "a", "b"));

    repo.setVersionedKnowledgeAsset("a", "b", ka);

    repo.setVersionedKnowledgeAsset("1", "2", new KnowledgeAsset().
        withExpression(new KnowledgeExpression().
            withRepresentation(
                new Representation().withLanguage(KnowledgeRepresentationLanguage.HL7_ELM))).
        withResourceId(DatatypeHelper.uri("http:/some/uri/", "1", "2")).
        withRelated(new Dependency().withRel(DependencyType.Imports).withTgt(ka)));

    repo.addKnowledgeAssetCarrier("a", "b", "Hi!".getBytes());
    repo.addKnowledgeAssetCarrier("1", "2", "There!".getBytes());

    List<KnowledgeCarrier> carriers = repo
        .getKnowledgeAssetBundle("1", "2", DependencyType.Imports.getTag(), -1);

    assertEquals(2, carriers.size());
    List<String> strings = carriers.stream().map(BinaryCarrier.class::cast)
        .map(BinaryCarrier::getEncodedExpression).map(String::new).collect(Collectors.toList());

    assertTrue(strings.contains("Hi!"));
    assertTrue(strings.contains("There!"));
  }

  @Test
  public void testBundleWithDependencyThreeDeep() {
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset ka1 = new KnowledgeAsset().
        withExpression(new KnowledgeExpression().
            withRepresentation(
                new Representation().withLanguage(KnowledgeRepresentationLanguage.HL7_ELM))).
        withResourceId(DatatypeHelper.uri("http:/some/uri/", "a", "b"));

    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset ka2 = new KnowledgeAsset().
        withExpression(new KnowledgeExpression().
            withRepresentation(
                new Representation().withLanguage(KnowledgeRepresentationLanguage.HL7_ELM))).
        withResourceId(DatatypeHelper.uri("http:/some/uri/", "q", "r"))
        .withRelated(new Dependency().withRel(DependencyType.Imports).withTgt(ka1));

    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset ka3 = new KnowledgeAsset().
        withExpression(new KnowledgeExpression().
            withRepresentation(
                new Representation().withLanguage(KnowledgeRepresentationLanguage.HL7_ELM))).
        withResourceId(DatatypeHelper.uri("http:/some/uri/", "1", "2")).
        withRelated(
            new Dependency().withRel(DependencyType.Imports).withTgt(ka2));

    repo.setVersionedKnowledgeAsset("a", "b", ka1);
    repo.setVersionedKnowledgeAsset("q", "r", ka2);
    repo.setVersionedKnowledgeAsset("1", "2", ka3);

    repo.addKnowledgeAssetCarrier("a", "b", "Hi!".getBytes());
    repo.addKnowledgeAssetCarrier("1", "2", "There!".getBytes());
    repo.addKnowledgeAssetCarrier("q", "r", "Zebra!".getBytes());

    List<KnowledgeCarrier> carriers = repo
        .getKnowledgeAssetBundle("1", "2", DependencyType.Imports.getTag(), -1);

    assertEquals(3, carriers.size());
    List<String> strings = carriers.stream().map(BinaryCarrier.class::cast)
        .map(BinaryCarrier::getEncodedExpression).map(String::new).collect(Collectors.toList());

    assertTrue(strings.contains("Hi!"));
    assertTrue(strings.contains("There!"));
    assertTrue(strings.contains("Zebra!"));
  }

}
