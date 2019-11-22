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

import static edu.mayo.ontology.taxonomies.kao.knowledgeassetrole._20190801.KnowledgeAssetRole.Operational_Concept_Definition;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype._20190801.KnowledgeAssetType.Care_Process_Model;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype._20190801.KnowledgeAssetType.Predictive_Model;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mayo.kmdp.language.LanguageDeSerializer;
import edu.mayo.kmdp.language.parsers.SurrogateParser;
import edu.mayo.kmdp.metadata.annotations.resources.SimpleAnnotation;
import edu.mayo.kmdp.metadata.surrogate.resources.KnowledgeAsset;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactApi;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryApi;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactSeriesApi;
import edu.mayo.kmdp.repository.artifact.ResourceNotFoundException;
import edu.mayo.kmdp.repository.artifact.jcr.JcrKnowledgeArtifactRepository;
import edu.mayo.kmdp.repository.asset.index.MapDbIndex;
import edu.mayo.kmdp.tranx.DeserializeApi;
import edu.mayo.ontology.taxonomies.kmdo.annotationreltype._20190801.AnnotationRelType;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.identifiers.ConceptIdentifier;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


public class SemanticRepositoryTest {

  @Inject
  private SemanticKnowledgeAssetRepository semanticRepository;

  private JcrKnowledgeArtifactRepository repos;

  private final String BASE_URI = Registry.MAYO_ASSETS_BASE_URI;

  @BeforeEach
  void setUpRepos() {
    repos = new JcrKnowledgeArtifactRepository(
        new Jcr(new Oak()).createRepository(), new KnowledgeArtifactRepositoryServerConfig());

    MapDbIndex index = new MapDbIndex();

    KnowledgeArtifactRepositoryApi knowledgeArtifactRepositoryApi =
        KnowledgeArtifactRepositoryApi.newInstance(repos);
    KnowledgeArtifactApi knowledgeArtifactApi =
        KnowledgeArtifactApi.newInstance(repos);
    KnowledgeArtifactSeriesApi knowledgeArtifactSeriesApi =
        KnowledgeArtifactSeriesApi.newInstance(repos);
    DeserializeApi parserApi = DeserializeApi.newInstance(new LanguageDeSerializer(
        Collections.singletonList(new SurrogateParser())));

    semanticRepository = new SemanticKnowledgeAssetRepository(knowledgeArtifactRepositoryApi,
        knowledgeArtifactApi,
        knowledgeArtifactSeriesApi,
        parserApi,
        index,
        new KnowledgeAssetRepositoryServerConfig());
  }

  @AfterEach
  void shutdown() {
    if (repos != null) {
      repos.shutdown();
    }
  }

  @Test
  void testInit() {
    assertNotNull(semanticRepository);
  }

  // listKnowledgeAssets
  @Test
  void testListAll() {
    assertNotNull(semanticRepository
      .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "1",
        new KnowledgeAsset().withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
      .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo2".getBytes()), "1",
        new KnowledgeAsset().withFormalType(Care_Process_Model)));
    List<Pointer> assets = semanticRepository
      .listKnowledgeAssets(null, null, null, null)
      .getBody();

    assertNotNull(assets);
    assertEquals(2, assets.size());
  }

  @Test
  void testListAllMultipleVersions() {
    assertNotNull(semanticRepository
      .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "1",
        new KnowledgeAsset().withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
      .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "2",
        new KnowledgeAsset().withFormalType(Care_Process_Model)));
    List<Pointer> assets = semanticRepository
      .listKnowledgeAssets(null, null, null, null)
      .getBody();

    assertNotNull(assets);
    assertEquals(1, assets.size());
  }

  @Test
  void testListAllEmptyList() {
    List<Pointer> assets = semanticRepository
      .listKnowledgeAssets(null, null, null, null)
      .getBody();

    assertNotNull(assets);
    assertEquals(0, assets.size());
  }

  @Test
  void testPointersHaveType() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    List<Pointer> assets = semanticRepository
        .listKnowledgeAssets(Care_Process_Model.getTag(), null, -1, -1)
        .getBody();

    assertNotNull(assets);
    assertEquals(1, assets.size());

    assertEquals(Care_Process_Model.getRef(), assets.get(0).getType());
  }

  // initKnowledgeAsset
  @Test
  void testInitAssetReturnsUUIDAndIsCreated() {
    ResponseEntity<UUID> responseEntity = semanticRepository
      .initKnowledgeAsset();
    assertEquals(responseEntity.getStatusCode(), HttpStatus.CREATED);
    assertNotNull(responseEntity.getBody());

    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset = semanticRepository
      .getKnowledgeAsset(responseEntity.getBody())
      .getBody();
    String expected = BASE_URI + responseEntity.getBody();
    //GUID returned should be id of asset
    assertEquals(expected, asset.getAssetId().getUri().toString());
    String versionId = asset.getAssetId().getVersionId().toString();
    //Version should be 0.0.0
    assertEquals("/0.0.0", versionId.substring(versionId.lastIndexOf("/")));

  }

  // getKnowledgeAsset
  @Test
  void testGetLatest() {
    assertNotNull(semanticRepository
      .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "1",
        new KnowledgeAsset().withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
      .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "2",
        new KnowledgeAsset().withFormalType(Care_Process_Model)));
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset = semanticRepository
      .getKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()))
      .getBody();

    assertNotNull(asset);
    assertEquals("2", asset.getAssetId().getVersion());
  }

  @Test
  void testGetLatestOutOfOrder() {
    assertNotNull(semanticRepository
      .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "5",
        new KnowledgeAsset().withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
      .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "2",
        new KnowledgeAsset().withFormalType(Care_Process_Model)));
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset = semanticRepository
      .getKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()))
      .getBody();

    assertNotNull(asset);
    assertEquals("2", asset.getAssetId().getVersion());
  }

  @Test
  void testGetLatestAssetNotFound() {
    // 404 status returned if attempting to retrieve the latest version of an asset that doesn't exist
    assertNotNull(semanticRepository
      .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "5",
        new KnowledgeAsset().withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
      .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "2",
        new KnowledgeAsset().withFormalType(Care_Process_Model)));
    ResponseEntity<edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset> response = semanticRepository
      .getKnowledgeAsset(UUID.nameUUIDFromBytes("fooDoesNotExist".getBytes()));

    assertEquals(response.getStatusCode(), HttpStatus.NOT_FOUND);
  }

  // getKnowledgeAssetVersions

  @Test
  void getVersions() {
    assertNotNull(semanticRepository
      .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "1",
        new KnowledgeAsset().withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
      .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "2",
        new KnowledgeAsset().withFormalType(Care_Process_Model)));

    List<Pointer> versions = semanticRepository
      .getKnowledgeAssetVersions(UUID.nameUUIDFromBytes("foo".getBytes()), null, null, null, null, null).getBody();

    assertNotNull(versions);
    assertEquals(2, versions.size());
  }

  @Test
  void getVersionsAssetNotFound() {
    assertNotNull(semanticRepository
      .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "1",
        new KnowledgeAsset().withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
      .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "2",
        new KnowledgeAsset().withFormalType(Care_Process_Model)));

    assertThrows(
      ResourceNotFoundException.class,
      () -> semanticRepository
        .getKnowledgeAssetVersions(UUID.nameUUIDFromBytes("fooNotFound".getBytes()), null, null, null, null, null));
  }

  //getVersionedKnowledgeAsset

  @Test
  void testSpecificVersionSuccess() {
    assertNotNull(semanticRepository
      .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "5",
        new KnowledgeAsset().withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
      .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "2",
        new KnowledgeAsset().withFormalType(Care_Process_Model)));
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset = semanticRepository
      .getVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "5")
      .getBody();

    assertNotNull(asset);
    assertEquals("5", asset.getAssetId().getVersion());
  }

  @Test
  void testSpecificVersionAssetNotFound() {
    assertNotNull(semanticRepository
      .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "5",
        new KnowledgeAsset().withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
      .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "2",
        new KnowledgeAsset().withFormalType(Care_Process_Model)));
    ResponseEntity <edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset> response = semanticRepository
      .getVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("fooDoeNotExist".getBytes()), "5");
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void testSpecificVersionAssetExistsVersionNotFound() {
    assertNotNull(semanticRepository
      .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "5",
        new KnowledgeAsset().withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
      .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "2",
        new KnowledgeAsset().withFormalType(Care_Process_Model)));
    ResponseEntity <edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset> response = semanticRepository
      .getVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "12345");
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    ObjectMapper Obj = new ObjectMapper();

    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset = new edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset().withFormalType(Care_Process_Model);
  }

  // setVersionedKnowledgeAsset
  @Test
  void testSetVersionedAssetAssetDoesNotExist() {
    assertNotNull(semanticRepository
      .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "1",
        new KnowledgeAsset().withFormalType(Care_Process_Model)));
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset = new KnowledgeAsset().withFormalType(Care_Process_Model)
      .withAssetId(new URIIdentifier()
        .withUri(URI.create(BASE_URI + "45a81582-1b1d-3439-9400-6e2fee0c3f52"))
        .withVersionId(URI.create(BASE_URI + "b9a26917-0a79-483d-b0e8-6610ba9aad5b/versions/1")));
    ResponseEntity <Void> response = semanticRepository
      .setVersionedKnowledgeAsset(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "1", asset);
    //expect 204 status code
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

  }

  @Test
  void testSetVersionedAssetVersionDoesNotExist() {
    assertNotNull(semanticRepository
      .setVersionedKnowledgeAsset(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "1",
        new KnowledgeAsset().withFormalType(Care_Process_Model)));
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset = new KnowledgeAsset().withFormalType(Care_Process_Model)
      .withAssetId(new URIIdentifier()
        .withUri(URI.create(BASE_URI + "45a81582-1b1d-3439-9400-6e2fee0c3f52"))
        .withVersionId(URI.create(BASE_URI + "45a81582-1b1d-3439-9400-6e2fee0c3f52/versions/2")));
    ResponseEntity <Void> response = semanticRepository
      .setVersionedKnowledgeAsset(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "2", asset);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    List<Pointer> versions = semanticRepository
      .getKnowledgeAssetVersions(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), null, null, null, null, null).getBody();

    assertEquals(2, versions.size());
  }

  @Test
  void testSetVersionedAssetVersionAlreadyExists_IsReplaced() {
    assertNotNull(semanticRepository
      .setVersionedKnowledgeAsset(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "1",
        new KnowledgeAsset().withFormalType(Care_Process_Model)));
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset = new KnowledgeAsset().withFormalType(Predictive_Model)
      .withAssetId(new URIIdentifier()
        .withUri(URI.create(BASE_URI + "45a81582-1b1d-3439-9400-6e2fee0c3f52"))
        .withVersionId(URI.create(BASE_URI + "45a81582-1b1d-3439-9400-6e2fee0c3f52/versions/1")));
    ResponseEntity <Void> response = semanticRepository
      .setVersionedKnowledgeAsset(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "1", asset);
    //expect 204 status code
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset assetResult = semanticRepository
      .getVersionedKnowledgeAsset(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "1").getBody();

    assertEquals(Predictive_Model, assetResult.getFormalType().get(0));
  }


  @Test
  void testInconsistentVersionId_shouldFail() {
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset = new KnowledgeAsset().withFormalType(Predictive_Model)
      .withAssetId(new URIIdentifier()
        .withUri(URI.create(BASE_URI + "45a81582-1b1d-3439-9400-6e2fee0c3f52"))
        .withVersionId(URI.create(BASE_URI + "45a81582-1b1d-3439-9400-6e2fee0c3f52/versions/1")));
    ResponseEntity <Void> response = semanticRepository
      .setVersionedKnowledgeAsset(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "2", asset);
    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
  }


  @Test
  void testInconsistentAssetId_shouldFail() {
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset = new KnowledgeAsset().withFormalType(Predictive_Model)
      .withAssetId(new URIIdentifier()
        .withUri(URI.create(BASE_URI +"45a81582-1b1d-3439-9400-6e2fee0c3f52"))
        .withVersionId(URI.create(BASE_URI + "b9a26917-0a79-483d-b0e8-6610ba9aad5b/versions/1")));
    ResponseEntity <Void> response = semanticRepository
      .setVersionedKnowledgeAsset(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"), "1", asset);
    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
  }

  @Test
  void testMissingAssetId_getsSet() {
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset = new KnowledgeAsset().withFormalType(Predictive_Model);
    ResponseEntity <Void> response = semanticRepository
      .setVersionedKnowledgeAsset(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"), "1", asset);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    String expectedAssetId = BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52";
    String expectedVersionId = BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52/versions/1";
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset ka = semanticRepository.getVersionedKnowledgeAsset(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"),"1").getBody();

    assertEquals(ka.getAssetId().getUri().toString(), expectedAssetId);
    assertEquals(ka.getAssetId().getVersionId().toString(), expectedVersionId);
  }


  @Test
  void initAndGetAssetByType() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    List<Pointer> assets = semanticRepository
        .listKnowledgeAssets(Care_Process_Model.getTag(), null, -1, -1)
        .getBody();

    assertNotNull(assets);
    assertEquals(1, assets.size());
  }

  @Test
  void addAndGetAssetByType() {
    KnowledgeAsset axx = new KnowledgeAsset().withFormalType(Care_Process_Model);
    assertNotNull(
        semanticRepository.setVersionedKnowledgeAsset(UUID.randomUUID(),
            "1",
            axx));
    List<Pointer> assets = semanticRepository
        .listKnowledgeAssets(Care_Process_Model.getTag(), null, -1, -1)
        .getBody();

    assertNotNull(assets);
    assertEquals(1, assets.size());
  }

  @Test
  void listKnowledgeAssetsMultipleVersions() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "2",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));

    List<Pointer> versions = semanticRepository
        .listKnowledgeAssets(Care_Process_Model.getTag(), null, -1, -1).getBody();

    assertNotNull(versions);
    assertEquals(1, versions.size());
  }

  @Test
  void listKnowledgeAssetsMultipleVersionsCorrectHrefAndId() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "2",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));

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
    List<Pointer> assets = semanticRepository
        .listKnowledgeAssets(Care_Process_Model.getTag(), null, -1, -1)
        .getBody();

    assertNotNull(assets);
    assertEquals(0, assets.size());
  }

  @Test
  void addAndGetAssetByNoType() {
    KnowledgeAsset axx = new KnowledgeAsset().withFormalType(Care_Process_Model);
    assertNotNull(
        semanticRepository.setVersionedKnowledgeAsset(UUID.randomUUID(),
            "1",
            axx));
    List<Pointer> assets = semanticRepository.listKnowledgeAssets(null, null, -1, -1).getBody();

    assertNotNull(assets);
    assertEquals(1, assets.size());
  }

  @Test
  void initAndGetAssetByAnnotation() {
    assertNotNull(semanticRepository.setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("1".getBytes()), "1",
        new KnowledgeAsset().withSubject(
            new SimpleAnnotation()
                .withExpr(new ConceptIdentifier()
                    .withConceptId(URI.create("http://something"))))));

    List<Pointer> pointers = semanticRepository
        .listKnowledgeAssets(null, "http://something", -1, -1).getBody();
    assertNotNull(pointers);
    assertEquals(1, pointers.size());
  }

  @Test
  void initAndGetAssetByAnnotationAndRel() {
    assertNotNull(semanticRepository.setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("1".getBytes()), "1",
        new KnowledgeAsset().withSubject(
            new SimpleAnnotation()
                .withExpr(new ConceptIdentifier().withConceptId(URI.create("http://something")))
                .withRel(new ConceptIdentifier().withRef(URI.create("http://somerel"))))
        )
    );

    List<Pointer> pointers = semanticRepository
        .listKnowledgeAssets(null, "http://somerel:http://something", -1, -1).getBody();
    assertNotNull(pointers);
    assertEquals(1, pointers.size());

  }

  @Test
  void addKnowledgeAssetCarrier() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));

    semanticRepository
        .setKnowledgeAssetCarrierVersion(UUID.nameUUIDFromBytes("foo".getBytes()), "1", UUID.nameUUIDFromBytes("q".getBytes()), "z", "there".getBytes());
    ResponseEntity<KnowledgeCarrier> artifact = semanticRepository
        .getCanonicalKnowledgeAssetCarrier(UUID.nameUUIDFromBytes("foo".getBytes()), "1", "");

    assertNotNull(artifact.getBody());
    assertEquals("there", new String(((BinaryCarrier) artifact.getBody()).getEncodedExpression()));
  }

  @Test
  void addKnowledgeAssetCarriers() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));

    semanticRepository
        .setKnowledgeAssetCarrierVersion(UUID.nameUUIDFromBytes("foo".getBytes()), "1", UUID.nameUUIDFromBytes("q".getBytes()), "z", "there".getBytes());
    ResponseEntity<List<Pointer>> artifacts = semanticRepository
        .getKnowledgeAssetCarriers(UUID.nameUUIDFromBytes("foo".getBytes()), "1");

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
        .setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("foo".getBytes()), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));

    semanticRepository
        .setKnowledgeAssetCarrierVersion(UUID.nameUUIDFromBytes("foo".getBytes()), "1", UUID.nameUUIDFromBytes("q".getBytes()), "z", "there".getBytes());
    semanticRepository
        .setKnowledgeAssetCarrierVersion(UUID.nameUUIDFromBytes("foo".getBytes()), "1", UUID.nameUUIDFromBytes("q".getBytes()), "x", "there".getBytes());
    ResponseEntity<List<Pointer>> artifacts = semanticRepository
        .getKnowledgeAssetCarriers(UUID.nameUUIDFromBytes("foo".getBytes()), "1");

    assertNotNull(artifacts.getBody());
    assertEquals(2, artifacts.getBody().size());
  }

  @Test
  void initAndGetAllDefinitions() {
    assertNotNull(semanticRepository.setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("1".getBytes()), "1",
        new KnowledgeAsset().withRole(Operational_Concept_Definition)
            .withSubject(new SimpleAnnotation()
                .withRel(AnnotationRelType.Defines.asConcept())
                .withExpr(dizziness))));

    List<Pointer> pointers = semanticRepository
        .listKnowledgeAssets(Operational_Concept_Definition.getTag(), null, -1, -1)
        .getBody();
    assertNotNull(pointers);
    assertEquals(1, pointers.size());
  }

  @Test
  void initAndGetAllDefinitionsWithMultiple() {
    assertNotNull(semanticRepository.setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("1".getBytes()), "1",
        new KnowledgeAsset().withRole(Operational_Concept_Definition)
            .withSubject(new SimpleAnnotation()
                .withRel(AnnotationRelType.Defines.asConcept())
                .withExpr(dizziness))));

    assertNotNull(semanticRepository.setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("2".getBytes()), "1",
        new KnowledgeAsset().withRole(Operational_Concept_Definition)
            .withSubject(new SimpleAnnotation()
                .withRel(AnnotationRelType.Defines.asConcept())
                .withExpr(sleep_apnea))));

    List<Pointer> pointers = semanticRepository
        .listKnowledgeAssets(Operational_Concept_Definition.getTag(), null, -1, -1)
        .getBody();
    assertNotNull(pointers);
    assertEquals(2, pointers.size());
  }

  @Test
  void initAndGetAllDefinitionsWithMultipleVersions() {
    assertNotNull(semanticRepository.setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("1".getBytes()), "1",
        new KnowledgeAsset().withRole(Operational_Concept_Definition)
            .withSubject(new SimpleAnnotation()
                .withRel(AnnotationRelType.Defines.asConcept())
                .withExpr(dizziness))));

    assertNotNull(semanticRepository.setVersionedKnowledgeAsset(UUID.nameUUIDFromBytes("1".getBytes()), "2",
        new KnowledgeAsset().withRole(Operational_Concept_Definition)
            .withSubject(new SimpleAnnotation()
                .withRel(AnnotationRelType.Defines.asConcept())
                .withExpr(dizziness))));

    List<Pointer> pointers = semanticRepository
        .listKnowledgeAssets(Operational_Concept_Definition.getTag(), null, -1, -1)
        .getBody();
    assertNotNull(pointers);
    assertEquals(1, pointers.size());
  }

}
