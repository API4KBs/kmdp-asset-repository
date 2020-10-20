/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.repository.asset;

import static edu.mayo.kmdp.util.Util.isEmpty;
import static edu.mayo.kmdp.util.Util.uuid;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.In_Terms_Of;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.snapshot.SemanticAnnotationRelType.Defines;
import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.Conflict;
import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.Created;
import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.NoContent;
import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.NotFound;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.artifactId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.assetId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomArtifactId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomAssetId;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Depends_On;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRoleSeries.Operational_Concept_Definition;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Care_Process_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Formal_Ontology;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Predictive_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HL7_ELM;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;

import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.util.DateTimeUtil;
import edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.id.IdentifierConstants;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.repository.KnowledgeAssetCatalog;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder;


class SemanticRepositoryTest extends RepositoryTestBase {

  private static final String BASE_URI = Registry.MAYO_ASSETS_BASE_URI;

  @Test
  void testInit() {
    assertNotNull(semanticRepository);
  }

  @Test
  void testMetadata() {
    KnowledgeAssetCatalog cat = semanticRepository.getKnowledgeAssetCatalog()
        .orElse(null);
    assertNotNull(cat);
    assertFalse(cat.getSupportedAssetTypes().isEmpty());
    assertFalse(cat.getSurrogateModels().isEmpty());
    assertFalse(isEmpty(cat.getSupportedAnnotations()));
    assertNotNull(cat.getId());
  }

  // listKnowledgeAssets
  @Test
  void testListAll() {
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "1",
            new KnowledgeAsset()
                .withName("Example A")
                .withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo2"), "1",
            new KnowledgeAsset()
                .withName("Example B")
                .withFormalType(Care_Process_Model)));
    List<Pointer> assets = semanticRepository
        .listKnowledgeAssets().orElse(emptyList());

    assertNotNull(assets);
    assertEquals(2, assets.size());

    assertTrue(assets.stream().allMatch(p -> Care_Process_Model.getReferentId().equals(p.getType())));
    assertTrue(assets.stream().anyMatch(p -> uuid("foo2").equals(p.getUuid())));
    assertTrue(assets.stream().anyMatch(p -> "Example A".equals(p.getName())));
  }

  @Test
  void testListAllInMemoryHelperMethod() {
    KnowledgeAssetRepositoryService repo = KnowledgeAssetRepositoryService
        .selfContainedRepository();

    assertTrue(repo
        .setKnowledgeAssetVersion(uuid("foo"), "1",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model))
        .isSuccess());

    assertTrue(repo
        .setKnowledgeAssetVersion(uuid("foo2"), "1",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model))
        .isSuccess());

    List<Pointer> assets = repo
        .listKnowledgeAssets().orElse(emptyList());

    assertNotNull(assets);
    assertEquals(2, assets.size());
  }

  @Test
  void testListAllMultipleVersions() {
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "2",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    List<Pointer> assets = semanticRepository
        .listKnowledgeAssets()
        .orElse(emptyList());

    assertNotNull(assets);
    assertEquals(1, assets.size());
  }

  @Test
  void testListAllMultipleVersionsIsLatest() {
    UUID guid = uuid("foo");

    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(guid, "1.0.0",
            new KnowledgeAsset()
                .withAssetId(assetId(guid,"1.0.0"))));
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(guid, "2.0.0",
            new KnowledgeAsset()
                .withAssetId(assetId(guid,"2.0.0"))));
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(guid, "0.1.0",
            new KnowledgeAsset()
                .withAssetId(assetId(guid,"0.1.0"))));

    List<Pointer> assets = semanticRepository
        .listKnowledgeAssets()
        .orElse(emptyList());

    assertNotNull(assets);
    assertEquals(1, assets.size());

    assertEquals("2.0.0", assets.get(0).getVersionTag());
  }

  @Test
  void testListAllEmptyList() {
    List<Pointer> assets = semanticRepository
        .listKnowledgeAssets()
        .orElse(emptyList());

    assertNotNull(assets);
    assertEquals(0, assets.size());
  }

  @Test
  void testPointersHaveType() {
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    List<Pointer> assets = semanticRepository
        .listKnowledgeAssets(Care_Process_Model.getTag(), null, null, -1, -1)
        .orElse(emptyList());

    assertNotNull(assets);
    assertEquals(1, assets.size());

    assertEquals(Care_Process_Model.getReferentId(), assets.get(0).getType());
  }

  // initKnowledgeAsset
  @Test
  void testInitAssetReturnsUUIDAndIsCreated() {
    Answer<UUID> responseEntity =
        semanticRepository.initKnowledgeAsset();
    assertEquals(Created, responseEntity.getOutcomeType());
    assertTrue(responseEntity.isSuccess());
    UUID newAssetId = responseEntity.get();
    assertNotNull(newAssetId);

    KnowledgeAsset asset =
        semanticRepository.getKnowledgeAsset(newAssetId).orElse(null);

    assertNotNull(asset);
    String expected = BASE_URI + newAssetId;
    //GUID returned should be id of asset
    assertEquals(expected, asset.getAssetId().getResourceId().toString());
    String versionId = asset.getAssetId().getVersionId().toString();
    //Version should be 0.0.0
    assertEquals("/0.0.0", versionId.substring(versionId.lastIndexOf("/")));

  }

  // getKnowledgeAsset
  @Test
  void testGetLatest() {
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "1",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "2",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    KnowledgeAsset asset =
        semanticRepository
            .getKnowledgeAsset(uuid("foo"))
            .orElse(null);

    assertNotNull(asset);
    assertEquals("2.0.0", asset.getAssetId().getVersionTag());
  }

  @Test
  void testGetLatestOutOfOrder() {
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "5.0.0",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "2.0.0",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    KnowledgeAsset asset =
        semanticRepository
            .getKnowledgeAsset(uuid("foo"))
            .orElse(null);

    assertNotNull(asset);
    assertEquals("5.0.0", asset.getAssetId().getVersionTag());
  }

  @Test
  void testGetLatestAssetNotFound() {
    // 404 status returned if attempting to retrieve the latest version of an asset that doesn't exist
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "5",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "2",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    Answer<KnowledgeAsset> response =
        semanticRepository.getKnowledgeAsset(uuid("fooDoesNotExist"));

    assertEquals(NotFound, response.getOutcomeType());
  }

  // listKnowledgeAssetVersions

  @Test
  void getVersions() {
    UUID uuid = uuid("foo");
    ResourceIdentifier assetId1 = assetId(uuid,"1.0.0");
    ResourceIdentifier assetId2 = assetId(uuid,"2.0.0");

    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(assetId1.getUuid(), assetId1.getVersionTag(),
            new KnowledgeAsset()
                .withAssetId(assetId1)
                .withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(assetId2.getUuid(), assetId2.getVersionTag(),
            new KnowledgeAsset()
                .withAssetId(assetId2)
                .withFormalType(Care_Process_Model)));

    List<Pointer> versions = semanticRepository
        .listKnowledgeAssetVersions(uuid)
        .orElse(emptyList());

    assertNotNull(versions);
    assertEquals(2, versions.size());
  }

  @Test
  void getVersionsAssetNotFound() {
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "1",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "2",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));

    Answer<List<Pointer>> assetVersions = semanticRepository
            .listKnowledgeAssetVersions(uuid("fooNotFound"));
    assertFalse(assetVersions.isSuccess());
    assertEquals(NotFound,assetVersions.getOutcomeType());
  }

  //getVersionedKnowledgeAsset

  @Test
  void testSpecificVersionSuccess() {
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "5",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "2",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    KnowledgeAsset asset = semanticRepository
        .getKnowledgeAssetVersion(uuid("foo"), "5")
        .orElse(null);

    assertNotNull(asset);
    assertEquals("5.0.0", asset.getAssetId().getVersionTag());
  }

  @Test
  void testSpecificVersionAssetNotFound() {
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "5",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "2",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    Answer<KnowledgeAsset> response =
        semanticRepository.getKnowledgeAssetVersion(uuid("fooDoeNotExist"), "5");
    assertEquals(NotFound, response.getOutcomeType());
  }

  @Test
  void testSpecificVersionAssetExistsVersionNotFound() {
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "5",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "2",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    Answer<KnowledgeAsset> response =
        semanticRepository.getKnowledgeAssetVersion(uuid("foo"), "12345");
    assertEquals(NotFound, response.getOutcomeType());
  }

  // setKnowledgeAssetVersion
  @Test
  void testSetVersionedAssetAssetDoesNotExist() {
    ResourceIdentifier assetId = randomAssetId();

    KnowledgeAsset asset = new KnowledgeAsset()
        .withFormalType(Care_Process_Model)
        .withAssetId(assetId);

    Answer<Void> response = semanticRepository
        .setKnowledgeAssetVersion(assetId.getUuid(), assetId.getVersionTag(), asset);

    assertEquals(NoContent, response.getOutcomeType());
  }

  @Test
  void testSetVersionedAssetVersionDoesNotExist() {
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "1",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    KnowledgeAsset asset = new KnowledgeAsset()
        .withFormalType(Care_Process_Model)
        .withAssetId(
            assetId(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "2"));
    Answer<Void> response = semanticRepository
        .setKnowledgeAssetVersion(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "2",
            asset);
    assertEquals(NoContent, response.getOutcomeType());

    List<Pointer> versions = semanticRepository
        .listKnowledgeAssetVersions(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"))
        .orElse(emptyList());

    assertEquals(2, versions.size());
  }

  @Test
  void testSetVersionedAssetVersionAlreadyExistsIsReplacedWithVersion() {
    ResourceIdentifier assetId = randomAssetId();
    semanticRepository
        .setKnowledgeAssetVersion(assetId.getUuid(), assetId.getVersionTag(),
            new KnowledgeAsset()
                .withAssetId(assetId)
                .withFormalType(Care_Process_Model));

    KnowledgeAsset changedAsset = new KnowledgeAsset()
        .withFormalType(Predictive_Model)
        .withAssetId(assetId);

    Answer<Void> response = semanticRepository
        .setKnowledgeAssetVersion(assetId.getUuid(),assetId.getVersionTag(),changedAsset);
    assertEquals(Conflict, response.getOutcomeType());

    ResourceIdentifier newAssetId =
        assetId(assetId.getUuid(),assetId.getSemanticVersionTag().incrementMinorVersion().toString());
    changedAsset.withAssetId(newAssetId);

    Answer<Void> response2 = semanticRepository
        .setKnowledgeAssetVersion(newAssetId.getUuid(),newAssetId.getVersionTag(),changedAsset);
    assertEquals(NoContent, response2.getOutcomeType());

    KnowledgeAsset assetResult = semanticRepository
        .getKnowledgeAssetVersion(newAssetId.getUuid(),newAssetId.getVersionTag())
        .orElse(null);
    assertNotNull(assetResult);

    assertTrue(Predictive_Model.sameAs(assetResult.getFormalType().get(0)));
  }


  @Test
  void testInconsistentVersionIdShouldFail() {
    KnowledgeAsset asset = new KnowledgeAsset()
        .withFormalType(Predictive_Model)
        .withAssetId(
            assetId(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "1"));
    Answer<Void> response = semanticRepository
        .setKnowledgeAssetVersion(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "2",
            asset);
    assertEquals(Conflict, response.getOutcomeType());
  }


  @Test
  void testInconsistentAssetId_shouldFail() {
    KnowledgeAsset asset = new KnowledgeAsset()
        .withFormalType(Predictive_Model)
        .withAssetId(
            assetId(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "1"));
    Answer<Void> response = semanticRepository
        .setKnowledgeAssetVersion(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"), "1",
            asset);
    assertEquals(Conflict, response.getOutcomeType());
  }

  @Test
  void testMissingAssetId_getsSet() {
    KnowledgeAsset asset = new KnowledgeAsset()
        .withFormalType(Predictive_Model);
    Answer<Void> response = semanticRepository
        .setKnowledgeAssetVersion(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"), "1",
            asset);
    assertEquals(NoContent, response.getOutcomeType());
    String expectedAssetId = BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52";
    String expectedVersionId = BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52/versions/1.0.0";
    KnowledgeAsset ka = semanticRepository
        .getKnowledgeAssetVersion(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"), "1")
        .orElse(null);
    assertNotNull(ka);

    assertEquals(URI.create(expectedAssetId), ka.getAssetId().getResourceId());
    assertEquals(URI.create(expectedVersionId), ka.getAssetId().getVersionId());
  }

  @Test
  void testMissingVersionOnly_getsSet() {
    KnowledgeAsset asset = new KnowledgeAsset()
        .withFormalType(Predictive_Model)
        .withAssetId(SemanticIdentifier.newId(URI.create(BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52")));

    Answer<Void> response = semanticRepository
        .setKnowledgeAssetVersion(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"), "1.0.0",
            asset);
    assertEquals(NoContent, response.getOutcomeType());

    String expectedAssetId = BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52";
    String expectedVersionId = BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52/versions/1.0.0";
    KnowledgeAsset ka = semanticRepository
        .getKnowledgeAssetVersion(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"), "1.0.0")
        .orElse(null);
    assertNotNull(ka);

    assertEquals(ka.getAssetId().getResourceId().toString(), expectedAssetId);
    assertEquals(ka.getAssetId().getVersionId().toString(), expectedVersionId);
  }

  @Test
  void testMissingAssetIdOnly_getsSet() {
    KnowledgeAsset asset = new KnowledgeAsset()
        .withFormalType(Predictive_Model)
        .withAssetId(
            SemanticIdentifier.newVersionId(URI.create(BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52/versions/1.0.0")));

    Answer<Void> response = semanticRepository
        .setKnowledgeAssetVersion(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"), "1.0.0",
            asset);
    assertEquals(NoContent, response.getOutcomeType());
    String expectedAssetId = BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52";
    String expectedVersionId = BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52/versions/1.0.0";
    KnowledgeAsset ka = semanticRepository
        .getKnowledgeAssetVersion(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"), "1.0.0")
        .orElse(null);
    assertNotNull(ka);

    assertEquals(ka.getAssetId().getResourceId().toString(), expectedAssetId);
    assertEquals(ka.getAssetId().getVersionId().toString(), expectedVersionId);
  }


  @Test
  void initAndGetAssetByType() {
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    List<Pointer> assets = semanticRepository
        .listKnowledgeAssets(Care_Process_Model.getTag(), null, null, -1, -1)
        .orElse(emptyList());

    assertEquals(1, assets.size());
  }

  @Test
  void initAndGetAssetByRole() {
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "1",
            new KnowledgeAsset().withRole(Operational_Concept_Definition)));
    List<Pointer> assets = semanticRepository
        .listKnowledgeAssets(Operational_Concept_Definition.getTag(), null, null, -1, -1)
        .orElse(emptyList());

    assertEquals(1, assets.size());
  }

  @Test
  void initAndGetAssetByWrongType() {
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    List<Pointer> assets = semanticRepository
        .listKnowledgeAssets(Formal_Ontology.getTag(), null, null, -1, -1)
        .orElse(emptyList());

    assertEquals(0, assets.size());
  }

  @Test
  void addAndGetAssetByType() {
    KnowledgeAsset axx = new KnowledgeAsset().withFormalType(Care_Process_Model);
    assertNotNull(
        semanticRepository.setKnowledgeAssetVersion(UUID.randomUUID(),
            "1",
            axx));
    List<Pointer> assets = semanticRepository
        .listKnowledgeAssets(Care_Process_Model.getTag(), null, null, -1, -1)
        .orElse(emptyList());

    assertEquals(1, assets.size());
  }

  @Test
  void listKnowledgeAssetsMultipleVersions() {
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "2",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));

    List<Pointer> versions = semanticRepository
        .listKnowledgeAssets(Care_Process_Model.getTag(), null, null, -1, -1)
        .orElse(emptyList());

    assertNotNull(versions);
    assertEquals(1, versions.size());
  }

  @Test
  void listKnowledgeAssetsMultipleVersionsCorrectHrefAndId() {
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "2",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));

    List<Pointer> versions = semanticRepository
        .listKnowledgeAssets(Care_Process_Model.getTag(), null, null, -1, -1)
        .orElse(emptyList());

    assertNotNull(versions);
    assertEquals(1, versions.size());

    assertNotNull(versions.get(0).getVersionTag());
    assertNotNull(versions.get(0).getVersionId());

    assertFalse(versions.get(0).getHref().toString().contains("versions"));

  }

  @Test
  void addAndGetAssetByTypeWithNone() {
    List<Pointer> assets = semanticRepository
        .listKnowledgeAssets(Care_Process_Model.getTag(), null, null, -1, -1)
        .orElse(emptyList());

    assertNotNull(assets);
    assertEquals(0, assets.size());
  }

  @Test
  void addAndGetAssetByNoType() {
    KnowledgeAsset axx = new KnowledgeAsset().withFormalType(Care_Process_Model);
    assertNotNull(
        semanticRepository.setKnowledgeAssetVersion(UUID.randomUUID(),
            "1",
            axx));
    List<Pointer> assets = semanticRepository.listKnowledgeAssets()
        .orElse(emptyList());

    assertNotNull(assets);
    assertEquals(1, assets.size());
  }

  @Test
  void initAndGetAssetByAnnotation() {
    assertNotNull(semanticRepository.setKnowledgeAssetVersion(uuid("1"), "1",
        new KnowledgeAsset().withAnnotation(
            new Annotation()
                .withRel(Defines.asConceptIdentifier())
                .withRef(Term.newTerm("http://something").asConceptIdentifier()))));

    List<Pointer> pointers = semanticRepository
        .listKnowledgeAssets(null, Defines.getTag(), null, -1, -1)
        .orElse(emptyList());
    assertNotNull(pointers);
    assertEquals(1, pointers.size());
  }

  @Test
  void initAndGetAssetByWrongAnnotation() {
    assertNotNull(semanticRepository.setKnowledgeAssetVersion(uuid("1"), "1",
        new KnowledgeAsset().withAnnotation(
            new Annotation()
                .withRel(In_Terms_Of.asConceptIdentifier())
                .withRef(Term.newTerm("http://something").asConceptIdentifier()))));

    List<Pointer> pointers = semanticRepository
        .listKnowledgeAssets(null, Defines.getTag(), null, -1, -1)
        .orElse(emptyList());
    assertNotNull(pointers);
    assertEquals(0, pointers.size());
  }

  @Test
  void addKnowledgeAssetCarrier() {
    ResourceIdentifier assetId = assetId(uuid("foo"), "1.0.0");
    ResourceIdentifier artifactId = artifactId(uuid("q"), "1.0.0");

    semanticRepository
        .setKnowledgeAssetVersion(assetId.getUuid(), assetId.getVersionTag(),
            new KnowledgeAsset()
                .withAssetId(assetId)
                .withFormalType(Care_Process_Model));

    semanticRepository
        .setKnowledgeAssetCarrierVersion(
            assetId.getUuid(), assetId.getVersionTag(),
            artifactId.getUuid(), artifactId.getVersionTag(),
            "test".getBytes());

    KnowledgeCarrier namedArtifact = semanticRepository
        .getKnowledgeAssetCarrierVersion(assetId.getUuid(),assetId.getVersionTag(),
            artifactId.getUuid(),artifactId.getVersionTag())
        .orElse(null);
    assertNotNull(namedArtifact);
    assertEquals("test", namedArtifact.asString().orElse(""));

    KnowledgeCarrier artifact = semanticRepository
        .getCanonicalKnowledgeAssetCarrier(assetId.getUuid(),assetId.getVersionTag())
        .orElse(null);

    assertNotNull(artifact);
    assertEquals("test", artifact.asString().orElse(""));
  }

  @Test
  void addKnowledgeAssetCarriers() {
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));

    semanticRepository
        .setKnowledgeAssetCarrierVersion(uuid("foo"), "1", uuid("q"), "z", "there".getBytes());
    List<Pointer> artifacts = semanticRepository
        .listKnowledgeAssetCarriers(uuid("foo"), "1")
        .orElse(emptyList());

    assertEquals(1, artifacts.size());
  }


  private ConceptIdentifier dizziness = Term.newTerm(
      URI.create("urn:foo:dizzy"),
      "dizzy",
      UUID.randomUUID(),
      URI.create("urn:foo"),
      URI.create("urn:foo:dizzy"),
      IdentifierConstants.VERSION_LATEST,
      "Dizzy",
      DateTimeUtil.today()).asConceptIdentifier();

  private ConceptIdentifier sleep_apnea = Term.newTerm(
      URI.create("urn:foo:sleepApnea"),
      "sleepApnea",
      UUID.randomUUID(),
      URI.create("urn:foo"),
      URI.create("urn:foo:sleepApnea"),
      IdentifierConstants.VERSION_LATEST,
      "Sleep Apnea",
      DateTimeUtil.today()).asConceptIdentifier();

  @Test
  void addKnowledgeAssetCarriersMultiple() {
    ResourceIdentifier assetId = assetId(uuid("foo"), "1.0.0");
    ResourceIdentifier artIdv1 = assetId(uuid("q"), "1.0.0");
    ResourceIdentifier artIdv2 = assetId(uuid("q"), "2.0.0");

    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(assetId.getUuid(), assetId.getVersionTag(),
            new KnowledgeAsset().withFormalType(Care_Process_Model)));

    semanticRepository.setKnowledgeAssetCarrierVersion(
        assetId.getUuid(), assetId.getVersionTag(),
        artIdv1.getUuid(), artIdv1.getVersionTag(), "there".getBytes());
    semanticRepository.setKnowledgeAssetCarrierVersion(
        assetId.getUuid(), assetId.getVersionTag(),
        artIdv2.getUuid(), artIdv2.getVersionTag(), "there".getBytes());

    List<Pointer> artifacts = semanticRepository
        .listKnowledgeAssetCarriers(assetId.getUuid(),assetId.getVersionTag())
        .orElse(emptyList());

    assertNotNull(artifacts);
    assertEquals(1, artifacts.size());
    Pointer ptr = artifacts.get(0);

    assertEquals(artIdv2.getUuid(),ptr.getUuid());
    assertEquals(artIdv2.getVersionTag(),ptr.getVersionTag());
  }

  @Test
  void initAndGetAllDefinitions() {
    assertNotNull(semanticRepository.setKnowledgeAssetVersion(uuid("1"), "1",
        new KnowledgeAsset().withRole(Operational_Concept_Definition)
            .withAnnotation(new Annotation()
                .withRel(Defines.asConceptIdentifier())
                .withRef(dizziness))));

    List<Pointer> pointers = semanticRepository
        .listKnowledgeAssets(Operational_Concept_Definition.getTag(), null, null, -1, -1)
        .orElse(emptyList());
    assertEquals(1, pointers.size());
  }

  @Test
  void initAndGetAllDefinitionsWithMultiple() {
    assertNotNull(semanticRepository.setKnowledgeAssetVersion(uuid("1"), "1",
        new KnowledgeAsset().withRole(Operational_Concept_Definition)
            .withAnnotation(new Annotation()
                .withRel(Defines.asConceptIdentifier())
                .withRef(dizziness))));

    assertNotNull(semanticRepository.setKnowledgeAssetVersion(uuid("2"), "1",
        new KnowledgeAsset().withRole(Operational_Concept_Definition)
            .withAnnotation(new Annotation()
                .withRel(Defines.asConceptIdentifier())
                .withRef(sleep_apnea))));

    List<Pointer> pointers = semanticRepository
        .listKnowledgeAssets(Operational_Concept_Definition.getTag(), null, null, -1, -1)
        .orElse(emptyList());
    assertEquals(2, pointers.size());
  }

  @Test
  void initAndGetAllDefinitionsWithMultipleVersions() {
    assertNotNull(semanticRepository.setKnowledgeAssetVersion(uuid("1"), "1",
        new KnowledgeAsset().withRole(Operational_Concept_Definition)
            .withAnnotation(new Annotation()
                .withRel(Defines.asConceptIdentifier())
                .withRef(dizziness))));

    assertNotNull(semanticRepository.setKnowledgeAssetVersion(uuid("1"), "2",
        new KnowledgeAsset().withRole(Operational_Concept_Definition)
            .withAnnotation(new Annotation()
                .withRel(Defines.asConceptIdentifier())
                .withRef(dizziness))));

    List<Pointer> pointers = semanticRepository
        .listKnowledgeAssets(Operational_Concept_Definition.getTag(), null, null, -1, -1)
        .orElse(emptyList());

    assertEquals(1, pointers.size());
  }

  @Test
  void testResultCarrierHasMinimalMetadata() {
    ResourceIdentifier assetId = randomAssetId();
    ResourceIdentifier artifactId = randomArtifactId();

    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(assetId.getUuid(), assetId.getVersionTag(),
            new KnowledgeAsset()
                .withAssetId(assetId)
                .withName("Test")
                .withCarriers(new KnowledgeArtifact()
                    .withArtifactId(artifactId)
                    .withRepresentation(rep(HTML))
                )
        ));

    semanticRepository
        .setKnowledgeAssetCarrierVersion(
            assetId.getUuid(), assetId.getVersionTag(),
            artifactId.getUuid(), artifactId.getVersionTag(),
            "there".getBytes());

    Answer<KnowledgeCarrier> kc = semanticRepository.getKnowledgeAssetCarrierVersion(
        assetId.getUuid(), assetId.getVersionTag(),
        artifactId.getUuid(), artifactId.getVersionTag()
    );

    assertTrue(kc.isSuccess());

    KnowledgeCarrier ck = kc.orElse(AbstractCarrier.of(""));
    assertNotNull(ck.getArtifactId());
    assertNotNull(ck.getAssetId());
    assertEquals("Test", ck.getLabel());
    assertTrue(HTML.sameAs(ck.getRepresentation().getLanguage()));
  }

  @Test
  void getRelatedTransitive() {
    UUID u1 = UUID.nameUUIDFromBytes("1".getBytes());
    UUID u2 = UUID.nameUUIDFromBytes("2".getBytes());

    semanticRepository.setKnowledgeAssetVersion(u1, "1", new KnowledgeAsset()
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(SurrogateBuilder.randomArtifactId())
            .withRepresentation(rep(HL7_ELM))));
    semanticRepository.setKnowledgeAssetCarrierVersion(
        u1, "1", UUID.randomUUID(), "0", "HI1!".getBytes());

    KnowledgeAsset asset1 = semanticRepository.getKnowledgeAssetVersion(u1, "1")
        .orElse(new KnowledgeAsset());

    semanticRepository.setKnowledgeAssetVersion(u2, "1", new KnowledgeAsset()
        .withLinks(new Dependency().withRel(Depends_On).withHref(asset1.getAssetId()))
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(SurrogateBuilder.randomArtifactId())
            .withRepresentation(rep(HL7_ELM))));
    semanticRepository.setKnowledgeAssetCarrierVersion(
        u2, "1", UUID.randomUUID(), "0", "HI2!".getBytes());

//    List<KnowledgeCarrier> carriers = semanticRepository.getKnowledgeArtifactBundle(u2, "1",
//        Depends_On.getTag(), -1, null).getOptionalValue()
//        .orElseGet(Collections::emptyList);
//
//    assertEquals(2, carriers.size());
  }


  @Test
  void testGetSurrogates() {
    ResourceIdentifier assetId = assetId(uuid("1"),"1.0.0");
    ResourceIdentifier extraSurrogateId = SurrogateBuilder.randomArtifactId();

    KnowledgeAsset surrogate = new KnowledgeAsset()
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(randomArtifactId())
            .withRepresentation(rep(HL7_ELM)))
        .withSurrogate(
            new KnowledgeArtifact()
                .withArtifactId(extraSurrogateId)
                .withRepresentation(rep(HTML,TXT))
                .withInlinedExpression("<p>Metadata</p>"));

    semanticRepository.setKnowledgeAssetVersion(
        assetId.getUuid(),assetId.getVersionTag(), surrogate);

    List<Pointer> surrPointers =
        semanticRepository.listKnowledgeAssetSurrogates(assetId.getUuid(),assetId.getVersionTag())
            .orElse(Collections.emptyList());
    assertEquals(2,surrPointers.size());

    Answer<KnowledgeCarrier> retrievedSurr =
        semanticRepository.getKnowledgeAssetSurrogateVersion(assetId.getUuid(),assetId.getVersionTag(),
          extraSurrogateId.getUuid(),extraSurrogateId.getVersionTag());
    assertTrue(retrievedSurr.isSuccess());
    assertEquals("<p>Metadata</p>",
        retrievedSurr.get().asString().orElse(""));
  }


  @Test
  void testDetectConflictOnOverrideSurrogate() {
    ResourceIdentifier assetId = randomAssetId();
    ResourceIdentifier surrogateId = randomArtifactId();

    KnowledgeAsset a1 = new KnowledgeAsset()
        .withAssetId(assetId)
        .withName("AAAA")
        .withSurrogate(new KnowledgeArtifact()
            .withArtifactId(surrogateId)
            .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0,JSON)));

    Answer<Void> ans1 = semanticRepository
        .setKnowledgeAssetVersion(assetId.getUuid(),assetId.getVersionTag(),a1);
    assertTrue(ans1.isSuccess());
    assertEquals(1, semanticRepository.listKnowledgeAssets()
        .orElse(Collections.emptyList()).size());

    Answer<Void> ans2 = semanticRepository
        .setKnowledgeAssetVersion(assetId.getUuid(),assetId.getVersionTag(),a1);
    assertTrue(ans2.isSuccess());
    assertEquals(1, semanticRepository.listKnowledgeAssets()
        .orElse(Collections.emptyList()).size());

    KnowledgeAsset a2 = new KnowledgeAsset()
        .withAssetId(assetId)
        .withName("BBBB")
        .withSurrogate(new KnowledgeArtifact()
            .withArtifactId(surrogateId)
            .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0,JSON)));

    Answer<Void> ans3 = semanticRepository
        .setKnowledgeAssetVersion(assetId.getUuid(),assetId.getVersionTag(),a2);
    assertTrue(ans3.isFailure());

    KnowledgeAsset a3 = new KnowledgeAsset()
        .withAssetId(assetId)
        .withName("BBBB")
        .withSurrogate(new KnowledgeArtifact()
            .withArtifactId(artifactId(surrogateId.getUuid(),"0.0.1"))
            .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0,JSON)));

    Answer<Void> ans4 = semanticRepository
        .setKnowledgeAssetVersion(assetId.getUuid(),assetId.getVersionTag(),a3);
    assertTrue(ans4.isSuccess());
    assertEquals(1, semanticRepository.listKnowledgeAssets()
        .orElse(Collections.emptyList()).size());
  }

  @Test
  public void testContentNegotiationWithHTMLExpectRedirect() {
    UUID assetId = UUID.nameUUIDFromBytes("2".getBytes());
    String versionTag = "2";
    String mockRedirectURL = "http://localhost:123/foo";

    KnowledgeAsset asset = new KnowledgeAsset()
        .withAssetId(assetId(assetId, versionTag))
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(SurrogateBuilder.randomArtifactId())
            .withRepresentation(rep(HTML,TXT))
            .withLocator(URI.create(mockRedirectURL))
        );

    semanticRepository.setKnowledgeAssetVersion(assetId,versionTag,asset);

    Answer<KnowledgeCarrier> ans = semanticRepository.getCanonicalKnowledgeAssetCarrier(
        assetId,
        versionTag,
        "text/html");

    assertTrue(ResponseCodeSeries.SeeOther.sameAs(ans.getOutcomeType()));
    assertEquals(mockRedirectURL, ans.getMeta("Location").orElse(""));
  }

}
