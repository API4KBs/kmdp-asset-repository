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
package edu.mayo.mea3.repository.semantic;

import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype._1_0.KnowledgeAssetType.Care_Process_Model;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import edu.mayo.kmdp.metadata.annotations.resources.SimpleAnnotation;
import edu.mayo.kmdp.metadata.surrogate.resources.KnowledgeAsset;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactApi;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryApi;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactSeriesApi;
import edu.mayo.kmdp.repository.artifact.jcr.JcrKnowledgeArtifactRepository;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerConfig;
import edu.mayo.kmdp.repository.asset.SemanticKnowledgeAssetRepository;
import edu.mayo.kmdp.repository.asset.index.MapDbIndex;
import edu.mayo.kmdp.terms.AssetVocabulary;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype._1_0.KnowledgeAssetType;
import java.net.URI;
import java.util.List;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.identifiers.ConceptIdentifier;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.springframework.http.ResponseEntity;


public class SemanticRepositoryTest {

  private SemanticKnowledgeAssetRepository semanticRepository;

  @BeforeEach
  void setUpRepos() {
    JcrKnowledgeArtifactRepository repos = new JcrKnowledgeArtifactRepository(
        new Jcr(new Oak()).createRepository(), new KnowledgeArtifactRepositoryServerConfig());

    MapDbIndex index = new MapDbIndex();

    KnowledgeArtifactRepositoryApi knowledgeArtifactRepositoryApi = KnowledgeArtifactRepositoryApi.newInstance(repos);
    KnowledgeArtifactApi knowledgeArtifactApi = KnowledgeArtifactApi.newInstance(repos);
    KnowledgeArtifactSeriesApi knowledgeArtifactSeriesApi = KnowledgeArtifactSeriesApi
        .newInstance(repos);

    semanticRepository = new SemanticKnowledgeAssetRepository(knowledgeArtifactRepositoryApi,
        knowledgeArtifactApi,
        knowledgeArtifactSeriesApi,
        index,
        new KnowledgeAssetRepositoryServerConfig());
  }

  @Test
  void testInit() {
    assertNotNull(semanticRepository);
  }

  @Test
  void testPointersHaveType() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset("foo", "1", new KnowledgeAsset().withType(Care_Process_Model)));
    List<Pointer> assets = semanticRepository.listKnowledgeAssets(Care_Process_Model.getTag(), null, -1, -1)
        .getBody();

    assertNotNull(assets);
    assertEquals(1, assets.size());

    assertEquals(Care_Process_Model.getRef(), assets.get(0).getType());
  }

  @Test
  void initAndGetAssetByType() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset("foo", "1", new KnowledgeAsset().withType(Care_Process_Model)));
    List<Pointer> assets = semanticRepository.listKnowledgeAssets(Care_Process_Model.getTag(), null, -1, - 1)
        .getBody();

    assertNotNull(assets);
    assertEquals(1, assets.size());
  }

  @Test
  void getVersions() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset("foo", "1", new KnowledgeAsset().withType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset("foo", "2", new KnowledgeAsset().withType(Care_Process_Model)));

    List<Pointer> versions = semanticRepository.getKnowledgeAssetVersions("foo",-1, - 1, null, null, null).getBody();

    assertNotNull(versions);
    assertEquals(2, versions.size());
  }

  @Test
  void addAndGetAssetByType() {
    assertNotNull(
        semanticRepository.addKnowledgeAsset(new KnowledgeAsset().withType(Care_Process_Model)));
    List<Pointer> assets = semanticRepository.listKnowledgeAssets(Care_Process_Model.getTag(), null, -1, -1)
        .getBody();

    assertNotNull(assets);
    assertEquals(1, assets.size());
  }

  @Test
  void listKnowledgeAssetsMultipleVersions() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset("foo", "1", new KnowledgeAsset().withType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset("foo", "2", new KnowledgeAsset().withType(Care_Process_Model)));

    List<Pointer> versions = semanticRepository
        .listKnowledgeAssets(Care_Process_Model.getTag(), null, -1, -1).getBody();

    assertNotNull(versions);
    assertEquals(1, versions.size());
  }

  @Test
  void listKnowledgeAssetsMultipleVersionsCorrectHrefAndId() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset("foo", "1", new KnowledgeAsset().withType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset("foo", "2", new KnowledgeAsset().withType(Care_Process_Model)));

    List<Pointer> versions = semanticRepository
        .listKnowledgeAssets(Care_Process_Model.getTag(), null, -1, -1).getBody();

    assertNotNull(versions);
    assertEquals(1, versions.size());

    assertNotNull(versions.get(0).getEntityRef().getVersionId());
    assertNotNull(versions.get(0).getEntityRef().getVersion());

    assertFalse(versions.get(0).getHref().toString().contains("versions"));

  }

  @Test
  void addAndGetAssetByTypeWithNone() {
    List<Pointer> assets = semanticRepository.listKnowledgeAssets(Care_Process_Model.getTag(), null, -1, -1)
        .getBody();

    assertNotNull(assets);
    assertEquals(0, assets.size());
  }

  @Test
  void addAndGetAssetByNoType() {
    assertNotNull(
        semanticRepository.addKnowledgeAsset(new KnowledgeAsset().withType(Care_Process_Model)));
    List<Pointer> assets = semanticRepository.listKnowledgeAssets(null, null, -1, -1).getBody();

    assertNotNull(assets);
    assertEquals(1, assets.size());
  }

  @Test
  void initAndGetAssetByAnnotation() {
    assertNotNull(semanticRepository.setVersionedKnowledgeAsset("1", "1",
        new KnowledgeAsset().withSubject(
            new SimpleAnnotation().withExpr(
                new ConceptIdentifier().withRef(
                    URI.create("http://something"))))));

    List<Pointer> pointers = semanticRepository.listKnowledgeAssets(null, "http://something", -1, -1).getBody();
    assertNotNull(pointers);
    assertEquals(1,pointers.size());
  }

  @Test
  void initAndGetAssetByAnnotationAndRel() {
    assertNotNull(semanticRepository.setVersionedKnowledgeAsset("1", "1",
        new KnowledgeAsset().withSubject(
            new SimpleAnnotation().withExpr(
                new ConceptIdentifier().withRef(
                    URI.create("http://something"))).withRel(new ConceptIdentifier()
                .withRef(URI.create("http://somerel"))))
        )
    );

    List<Pointer> pointers = semanticRepository.listKnowledgeAssets(null, "http://somerel:http://something", -1, -1).getBody();
    assertNotNull(pointers);
    assertEquals(1,pointers.size());

  }

  @Test
  void addKnowledgeAssetCarrier() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset("foo", "1", new KnowledgeAsset().withType(Care_Process_Model)));

    semanticRepository.setKnowledgeAssetCarrierVersion("foo", "1", "q", "z", "there".getBytes());
    ResponseEntity<KnowledgeCarrier> artifact = semanticRepository
        .getCanonicalKnowledgeAssetCarrier("foo", "1", "q");

    assertNotNull(artifact.getBody());
    assertEquals("there", new String(((BinaryCarrier) artifact.getBody()).getEncodedExpression()));
  }

  @Test
  void addKnowledgeAssetCarriers() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset("foo", "1", new KnowledgeAsset().withType(Care_Process_Model)));

    semanticRepository.setKnowledgeAssetCarrierVersion("foo", "1", "q", "z", "there".getBytes());
    ResponseEntity<List<Pointer>> artifacts = semanticRepository
        .getKnowledgeAssetCarriers("foo", "1");

    assertNotNull(artifacts.getBody());
    assertEquals(1, artifacts.getBody().size());
  }


  private ConceptIdentifier dizziness = new ConceptIdentifier()
      .withConceptId(URI.create("http://foo/term/Dizzy"))
      .withTag("Dizzy")
      .withLabel("Dizzy")
      .withRef(URI.create("uri:urn:Dizzy"));

  private ConceptIdentifier sleep_apnea = new ConceptIdentifier()
      .withConceptId(URI.create("http://foo/term/SleepApnea"))
      .withTag("SleepApnea")
      .withLabel("Sleep Apnea")
      .withRef(URI.create("uri:urn:SleepApnea"));
  
  @Test
  void addKnowledgeAssetCarriersMultiple() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset("foo", "1", new KnowledgeAsset().withType(Care_Process_Model)));

    semanticRepository.setKnowledgeAssetCarrierVersion("foo", "1", "q", "z", "there".getBytes());
    semanticRepository.setKnowledgeAssetCarrierVersion("foo", "1", "q", "x", "there".getBytes());
    ResponseEntity<List<Pointer>> artifacts = semanticRepository
        .getKnowledgeAssetCarriers("foo", "1");

    assertNotNull(artifacts.getBody());
    assertEquals(2, artifacts.getBody().size());
  }

  @Test
  void initAndGetAllDefinitions() {
    assertNotNull(semanticRepository.setVersionedKnowledgeAsset("1", "1",
        new KnowledgeAsset().withType(KnowledgeAssetType.Operational_Concept_Defintion)
            .withSubject(new SimpleAnnotation()
                .withRel(AssetVocabulary.DEFINES.asConcept())
                .withExpr(dizziness))));

    List<Pointer> pointers = semanticRepository
        .listKnowledgeAssets(KnowledgeAssetType.Operational_Concept_Defintion.getTag(), null, -1, -1)
        .getBody();
    assertNotNull(pointers);
    assertEquals(1, pointers.size());
  }

  @Test
  void initAndGetAllDefinitionsWithMultiple() {
    assertNotNull(semanticRepository.setVersionedKnowledgeAsset("1", "1",
        new KnowledgeAsset().withType(KnowledgeAssetType.Operational_Concept_Defintion)
            .withSubject(new SimpleAnnotation()
                .withRel(AssetVocabulary.DEFINES.asConcept())
                .withExpr(dizziness))));

    assertNotNull(semanticRepository.setVersionedKnowledgeAsset("2", "1",
        new KnowledgeAsset().withType(KnowledgeAssetType.Operational_Concept_Defintion)
            .withSubject(new SimpleAnnotation()
                .withRel(AssetVocabulary.DEFINES.asConcept())
                .withExpr(sleep_apnea))));

    List<Pointer> pointers = semanticRepository
        .listKnowledgeAssets(KnowledgeAssetType.Operational_Concept_Defintion.getTag(), null, -1, -1)
        .getBody();
    assertNotNull(pointers);
    assertEquals(2, pointers.size());
  }

  @Test
  void initAndGetAllDefinitionsWithMultipleVersions() {
    assertNotNull(semanticRepository.setVersionedKnowledgeAsset("1", "1",
        new KnowledgeAsset().withType(KnowledgeAssetType.Operational_Concept_Defintion)
            .withSubject(new SimpleAnnotation()
                .withRel(AssetVocabulary.DEFINES.asConcept())
                .withExpr(dizziness))));

    assertNotNull(semanticRepository.setVersionedKnowledgeAsset("1", "2",
        new KnowledgeAsset().withType(KnowledgeAssetType.Operational_Concept_Defintion)
            .withSubject(new SimpleAnnotation()
                .withRel(AssetVocabulary.DEFINES.asConcept())
                .withExpr(dizziness))));

    List<Pointer> pointers = semanticRepository
        .listKnowledgeAssets(KnowledgeAssetType.Operational_Concept_Defintion.getTag(), null, -1, -1)
        .getBody();
    assertNotNull(pointers);
    assertEquals(1, pointers.size());
  }

}
