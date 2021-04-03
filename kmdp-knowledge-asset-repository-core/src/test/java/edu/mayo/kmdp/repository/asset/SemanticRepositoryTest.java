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

import static edu.mayo.kmdp.registry.Registry.BASE_UUID_URN_URI;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.IdentifierConstants.VERSION_ZERO;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.artifactId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.assetId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomArtifactId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomAssetId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper.getCanonicalSurrogateId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper.getComputableSurrogateMetadata;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Guidance_Rule;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Depends_On;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRoleSeries.Operational_Concept_Definition;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Care_Process_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Cognitive_Care_Process_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Formal_Ontology;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Predictive_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HL7_ELM;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import com.github.zafarkhaja.semver.Version;
import edu.mayo.kmdp.comparator.Contrastor.Comparison;
import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.util.DateTimeUtil;
import edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
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
import org.omg.spec.api4kp._20200801.surrogate.SurrogateDiffer;


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

    assertTrue(
        assets.stream().allMatch(p -> Care_Process_Model.getReferentId().equals(p.getType())));
    assertTrue(assets.stream().anyMatch(p -> uuid("foo2").equals(p.getUuid())));
    assertTrue(assets.stream().anyMatch(p -> "Example A".equals(p.getName())));
  }

  @Test
  void testClear() {
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

    assertTrue(
        assets.stream().allMatch(p -> Care_Process_Model.getReferentId().equals(p.getType())));
    assertTrue(assets.stream().anyMatch(p -> uuid("foo2").equals(p.getUuid())));
    assertTrue(assets.stream().anyMatch(p -> "Example A".equals(p.getName())));

    semanticRepository.deleteKnowledgeAssets();

    assets = semanticRepository
        .listKnowledgeAssets().orElse(emptyList());
    assertNotNull(assets);
    assertEquals(0, assets.size());
  }

  @Test
  void testPopulateClearAndPopulate() {
    // make sure we can repeat this process and re-populate
    this.testClear();
    this.testClear();
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
                .withAssetId(assetId(BASE_UUID_URN_URI, guid, "1.0.0"))));
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(guid, "2.0.0",
            new KnowledgeAsset()
                .withAssetId(assetId(BASE_UUID_URN_URI, guid, "2.0.0"))));
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(guid, "0.1.0",
            new KnowledgeAsset()
                .withAssetId(assetId(BASE_UUID_URN_URI, guid, "0.1.0"))));

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
    ResourceIdentifier assetId1 = assetId(BASE_UUID_URN_URI, uuid, "1.0.0");
    ResourceIdentifier assetId2 = assetId(BASE_UUID_URN_URI, uuid, "2.0.0");

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
    assertEquals(NotFound, assetVersions.getOutcomeType());
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
            assetId(BASE_UUID_URN_URI, UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"),
                "2"));
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
    ResourceIdentifier assetId = randomAssetId(testAssetNS());
    semanticRepository
        .setKnowledgeAssetVersion(assetId.getUuid(), assetId.getVersionTag(),
            new KnowledgeAsset()
                .withAssetId(assetId)
                .withFormalType(Care_Process_Model));

    KnowledgeAsset changedAsset = new KnowledgeAsset()
        .withFormalType(Predictive_Model)
        .withAssetId(assetId);

    Answer<Void> response = semanticRepository
        .setKnowledgeAssetVersion(assetId.getUuid(), assetId.getVersionTag(), changedAsset);
    assertEquals(Conflict, response.getOutcomeType());

    ResourceIdentifier newAssetId =
        assetId(BASE_UUID_URN_URI, assetId.getUuid(),
            assetId.getSemanticVersionTag().incrementMinorVersion().toString());
    changedAsset.withAssetId(newAssetId);

    Answer<Void> response2 = semanticRepository
        .setKnowledgeAssetVersion(newAssetId.getUuid(), newAssetId.getVersionTag(), changedAsset);
    assertEquals(NoContent, response2.getOutcomeType());

    KnowledgeAsset assetResult = semanticRepository
        .getKnowledgeAssetVersion(newAssetId.getUuid(), newAssetId.getVersionTag())
        .orElse(null);
    assertNotNull(assetResult);

    assertTrue(Predictive_Model.sameAs(assetResult.getFormalType().get(0)));
  }


  @Test
  void testInconsistentVersionIdShouldFail() {
    KnowledgeAsset asset = new KnowledgeAsset()
        .withFormalType(Predictive_Model)
        .withAssetId(
            assetId(BASE_UUID_URN_URI, UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"),
                "1"));
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
            assetId(BASE_UUID_URN_URI, UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"),
                "1"));
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
        .withAssetId(newId(URI.create(BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52")));

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
            SemanticIdentifier.newVersionId(
                URI.create(BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52/versions/1.0.0")));

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
  void initAndGetAssetByUnknownType() {
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    List<Pointer> assets = semanticRepository
        .listKnowledgeAssets(UUID.randomUUID().toString(), null, null, -1, -1)
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
  void initAndGetAssetByUnknownAnnotation() {
    assertNotNull(semanticRepository.setKnowledgeAssetVersion(uuid("1"), "1",
        new KnowledgeAsset().withAnnotation(
            new Annotation()
                .withRel(In_Terms_Of.asConceptIdentifier())
                .withRef(Term.newTerm("http://something").asConceptIdentifier()))));

    List<Pointer> pointers = semanticRepository
        .listKnowledgeAssets(null, UUID.randomUUID().toString(), null, 0, -1)
        .orElseGet(Assertions::fail);
    assertEquals(0, pointers.size());

    List<Pointer> pointers2 = semanticRepository
        .listKnowledgeAssets(null, null, UUID.randomUUID().toString(), 0, -1)
        .orElseGet(Assertions::fail);
    assertEquals(0, pointers2.size());
  }

  @Test
  void addKnowledgeAssetCarrier() {
    ResourceIdentifier assetId = assetId(BASE_UUID_URN_URI, uuid("foo"), "1.0.0");
    ResourceIdentifier artifactId = artifactId(BASE_UUID_URN_URI, uuid("q"), "1.0.0");

    Answer<Void> ans = semanticRepository
        .setKnowledgeAssetVersion(assetId.getUuid(), assetId.getVersionTag(),
            new KnowledgeAsset()
                .withAssetId(assetId)
                .withFormalType(Care_Process_Model));
    assertTrue(ans.isSuccess());

    semanticRepository
        .setKnowledgeAssetCarrierVersion(
            assetId.getUuid(), assetId.getVersionTag(),
            artifactId.getUuid(), artifactId.getVersionTag(),
            "test".getBytes());

    KnowledgeCarrier namedArtifact = semanticRepository
        .getKnowledgeAssetCarrierVersion(assetId.getUuid(), assetId.getVersionTag(),
            artifactId.getUuid(), artifactId.getVersionTag())
        .orElse(null);
    assertNotNull(namedArtifact);
    assertEquals("test", namedArtifact.asString().orElse(""));

    KnowledgeCarrier artifact = semanticRepository
        .getKnowledgeAssetVersionCanonicalCarrier(assetId.getUuid(), assetId.getVersionTag())
        .orElse(null);

    assertNotNull(artifact);
    assertEquals("test", artifact.asString().orElse(""));
  }

  @Test
  void addKnowledgeAssetCarriers() {
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));

    Answer<Void> ans = semanticRepository
        .setKnowledgeAssetCarrierVersion(uuid("foo"), "1", uuid("q"), "z", "there".getBytes());
    assertTrue(ans.isSuccess());

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
    ResourceIdentifier assetId = assetId(BASE_UUID_URN_URI, uuid("foo"), "1.0.0");
    ResourceIdentifier artIdv1 = assetId(BASE_UUID_URN_URI, uuid("q"), "1.0.0");
    ResourceIdentifier artIdv2 = assetId(BASE_UUID_URN_URI, uuid("q"), "2.0.0");

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
        .listKnowledgeAssetCarriers(assetId.getUuid(), assetId.getVersionTag())
        .orElse(emptyList());

    assertNotNull(artifacts);
    assertEquals(1, artifacts.size());
    Pointer ptr = artifacts.get(0);

    assertEquals(artIdv2.getUuid(), ptr.getUuid());
    assertEquals(artIdv2.getVersionTag(), ptr.getVersionTag());
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
    ResourceIdentifier assetId = assetId(BASE_UUID_URN_URI, uuid("1"), "1.0.0");
    ResourceIdentifier extraSurrogateId = SurrogateBuilder.randomArtifactId();

    KnowledgeAsset surrogate = new KnowledgeAsset()
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(randomArtifactId())
            .withRepresentation(rep(HL7_ELM)))
        .withSurrogate(
            new KnowledgeArtifact()
                .withArtifactId(extraSurrogateId)
                .withRepresentation(rep(HTML, TXT))
                .withInlinedExpression("<p>Metadata</p>"));

    semanticRepository.setKnowledgeAssetVersion(
        assetId.getUuid(), assetId.getVersionTag(), surrogate);

    List<Pointer> surrPointers =
        semanticRepository.listKnowledgeAssetSurrogates(assetId.getUuid(), assetId.getVersionTag())
            .orElse(Collections.emptyList());
    assertEquals(2, surrPointers.size());

    Answer<KnowledgeCarrier> retrievedSurr =
        semanticRepository
            .getKnowledgeAssetSurrogateVersion(assetId.getUuid(), assetId.getVersionTag(),
                extraSurrogateId.getUuid(), extraSurrogateId.getVersionTag());
    assertTrue(retrievedSurr.isSuccess());
    assertEquals("<p>Metadata</p>",
        retrievedSurr.get().asString().orElse(""));
  }


  @Test
  void testDetectConflictOnOverrideSurrogate() {
    ResourceIdentifier assetId = randomAssetId(testAssetNS());
    ResourceIdentifier surrogateId = randomArtifactId(testArtifactNS());

    KnowledgeAsset a1 = new KnowledgeAsset()
        .withAssetId(assetId)
        .withName("AAAA")
        .withSurrogate(new KnowledgeArtifact()
            .withArtifactId(surrogateId)
            .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0, JSON)));

    Answer<Void> ans1 = semanticRepository
        .setKnowledgeAssetVersion(assetId.getUuid(), assetId.getVersionTag(), a1);
    assertTrue(ans1.isSuccess());
    assertEquals(1, semanticRepository.listKnowledgeAssets()
        .orElse(Collections.emptyList()).size());

    Answer<Void> ans2 = semanticRepository
        .setKnowledgeAssetVersion(assetId.getUuid(), assetId.getVersionTag(), a1);
    assertTrue(ans2.isSuccess());
    assertEquals(1, semanticRepository.listKnowledgeAssets()
        .orElse(Collections.emptyList()).size());

    KnowledgeAsset a2 = new KnowledgeAsset()
        .withAssetId(assetId)
        .withName("BBBB")
        .withSurrogate(new KnowledgeArtifact()
            .withArtifactId(surrogateId)
            .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0, JSON)));

    Answer<Void> ans3 = semanticRepository
        .setKnowledgeAssetVersion(assetId.getUuid(), assetId.getVersionTag(), a2);
    assertTrue(ans3.isFailure());

    KnowledgeAsset a3 = new KnowledgeAsset()
        .withAssetId(assetId)
        .withName("BBBB")
        .withSurrogate(new KnowledgeArtifact()
            .withArtifactId(artifactId(BASE_UUID_URN_URI, surrogateId.getUuid(), "0.0.1"))
            .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0, JSON)));

    Answer<Void> ans4 = semanticRepository
        .setKnowledgeAssetVersion(assetId.getUuid(), assetId.getVersionTag(), a3);
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
        .withAssetId(assetId(BASE_UUID_URN_URI, assetId, versionTag))
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(SurrogateBuilder.randomArtifactId())
            .withRepresentation(rep(HTML, TXT))
            .withLocator(URI.create(mockRedirectURL))
        );

    semanticRepository.setKnowledgeAssetVersion(assetId, versionTag, asset);

    Answer<KnowledgeCarrier> ans =
        semanticRepository.getKnowledgeAssetVersionCanonicalCarrier(
            assetId,
            versionTag,
            "text/html");

    assertTrue(ResponseCodeSeries.SeeOther.sameAs(ans.getOutcomeType()));
    assertEquals(mockRedirectURL, ans.getMeta("Location").orElse(""));
  }

  @Test
  public void testNegotiateCarrierForLatestVersion() {
    UUID assetId = UUID.nameUUIDFromBytes("2".getBytes());
    String versionTag = "2";
    String mockRedirectURL = "http://localhost:123/foo";

    KnowledgeAsset asset = new KnowledgeAsset()
        .withAssetId(assetId(BASE_UUID_URN_URI, assetId, versionTag))
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(SurrogateBuilder.randomArtifactId())
            .withRepresentation(rep(HTML, TXT))
            .withLocator(URI.create(mockRedirectURL))
        );

    semanticRepository.setKnowledgeAssetVersion(assetId, versionTag, asset);

    Answer<KnowledgeCarrier> ans =
        semanticRepository.getKnowledgeAssetCanonicalCarrier(
            assetId,
            "text/html");

    assertTrue(ResponseCodeSeries.SeeOther.sameAs(ans.getOutcomeType()));
    assertEquals(mockRedirectURL, ans.getMeta("Location").orElse(""));
  }

  @Test
  public void testContentNegotiationSurrogateWithMetaFormats() {
    UUID assetId = UUID.nameUUIDFromBytes("2".getBytes());
    String versionTag = "2";

    KnowledgeAsset asset =
        SurrogateBuilder.newSurrogate(assetId(BASE_UUID_URN_URI, assetId, versionTag)).get();

    semanticRepository.setKnowledgeAssetVersion(assetId, versionTag, asset);

    Answer<KnowledgeAsset> ans = semanticRepository.getKnowledgeAssetVersion(
        assetId,
        versionTag,
        "application/xml");

    assertTrue(ans.isSuccess());
  }

  @Test
  void testGetSurrogateVersions() {
    ResourceIdentifier assetId = assetId(BASE_UUID_URN_URI, uuid("1"), "1.0.0");
    ResourceIdentifier extraSurrogateIdV1 = SurrogateBuilder.randomArtifactId();
    ResourceIdentifier extraSurrogateIdV2 = newId(extraSurrogateIdV1.getNamespaceUri(),
        extraSurrogateIdV1.getUuid(), "1.0.0");
    ResourceIdentifier extraSurrogateId2 = SurrogateBuilder.randomArtifactId();

    KnowledgeAsset surrogate = new KnowledgeAsset()
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(randomArtifactId())
            .withRepresentation(rep(HL7_ELM)))
        .withSurrogate(
            new KnowledgeArtifact()
                .withArtifactId(extraSurrogateIdV1)
                .withRepresentation(rep(HTML, TXT))
                .withInlinedExpression("<p>Metadata</p>"))
        .withSurrogate(
            new KnowledgeArtifact()
                .withArtifactId(extraSurrogateIdV2)
                .withRepresentation(rep(HTML, TXT))
                .withInlinedExpression("<p>Metadata 2</p>"))
        .withSurrogate(
            new KnowledgeArtifact()
                .withArtifactId(extraSurrogateId2)
                .withRepresentation(rep(HTML, TXT))
                .withInlinedExpression("<p>Another Metadata</p>"));

    semanticRepository.setKnowledgeAssetVersion(
        assetId.getUuid(), assetId.getVersionTag(), surrogate);

    List<Pointer> surrPointers =
        semanticRepository.listKnowledgeAssetSurrogateVersions(
            assetId.getUuid(), assetId.getVersionTag(), extraSurrogateIdV1.getUuid())
            .orElse(Collections.emptyList());
    assertEquals(2, surrPointers.size());

    List<Pointer> surrPointers2 =
        semanticRepository.listKnowledgeAssetSurrogateVersions(
            assetId.getUuid(), assetId.getVersionTag(), extraSurrogateId2.getUuid())
            .orElse(Collections.emptyList());
    assertEquals(1, surrPointers2.size());

    Answer<List<Pointer>> ans = semanticRepository.listKnowledgeAssetSurrogateVersions(
        assetId.getUuid(), assetId.getVersionTag(), randomArtifactId().getUuid());
    assertTrue(ans.isFailure());
    assertTrue(NotFound.sameAs(ans.getOutcomeType()));

  }


  @Test
  void testGetCarrierVersions() {
    ResourceIdentifier assetId = assetId(BASE_UUID_URN_URI, uuid("1"), "1.0.0");
    ResourceIdentifier extraCarrierIdV1 = randomArtifactId();
    ResourceIdentifier extraCarrierIdV2 =
        newId(extraCarrierIdV1.getNamespaceUri(), extraCarrierIdV1.getUuid(), "1.0.0");
    ResourceIdentifier extraCarrierId2 = randomArtifactId();

    KnowledgeAsset surrogate = new KnowledgeAsset()
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(randomArtifactId())
            .withRepresentation(rep(HL7_ELM)))
        .withSurrogate(
            new KnowledgeArtifact()
                .withArtifactId(extraCarrierIdV1)
                .withRepresentation(rep(HTML, TXT))
                .withInlinedExpression("<p>How to</p>"))
        .withSurrogate(
            new KnowledgeArtifact()
                .withArtifactId(extraCarrierIdV2)
                .withRepresentation(rep(HTML, TXT))
                .withInlinedExpression("<p>How to 2</p>"))
        .withSurrogate(
            new KnowledgeArtifact()
                .withArtifactId(extraCarrierId2)
                .withRepresentation(rep(HTML, TXT))
                .withInlinedExpression("<p>This or that</p>"));

    semanticRepository.setKnowledgeAssetVersion(
        assetId.getUuid(), assetId.getVersionTag(), surrogate);

    List<Pointer> carrPointers =
        semanticRepository.listKnowledgeAssetSurrogateVersions(
            assetId.getUuid(), assetId.getVersionTag(), extraCarrierIdV1.getUuid())
            .orElse(Collections.emptyList());
    assertEquals(2, carrPointers.size());

    List<Pointer> carrPointers2 =
        semanticRepository.listKnowledgeAssetSurrogateVersions(
            assetId.getUuid(), assetId.getVersionTag(), extraCarrierId2.getUuid())
            .orElse(Collections.emptyList());
    assertEquals(1, carrPointers2.size());

  }


  @Test
  void testInitKnowledgeAssetWithNonSemVerAssetID() {
    // the use of non-SemVer IDs is discouraged, but should behave consistently
    String vTag = "0.0.0-LATEST";
    String aTag = "0.0.0-CURRENT";
    UUID assetUUID = uuid("1165");
    ResourceIdentifier assetId = newId(assetUUID, "LATEST");

    KnowledgeAsset surrogate = new KnowledgeAsset()
        .withAssetId(assetId)
        .withName("Test")
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(newId(UUID.randomUUID(), "CURRENT")));

    semanticRepository.setKnowledgeAssetVersion(
        assetId.getUuid(), assetId.getVersionTag(), surrogate);

    List<Pointer> ptrs = semanticRepository.listKnowledgeAssets()
        .orElseGet(Assertions::fail);
    assertEquals(1, ptrs.size());
    Pointer ptr = ptrs.get(0);

    KnowledgeAsset ans = semanticRepository.getKnowledgeAsset(assetUUID)
        .orElseGet(Assertions::fail);

    System.out.println(ans.getAssetId().getVersionTag());

    ResourceIdentifier artId = ans.getCarriers().get(0).getArtifactId();
    ResourceIdentifier latestArtId = semanticRepository.getLatestCarrierVersion(artId.getUuid())
        .orElseGet(Assertions::fail);
    List<Pointer> artPtrs = semanticRepository.listKnowledgeAssetCarrierVersions(
        ans.getAssetId().getUuid(),
        ans.getAssetId().getVersionTag(),
        artId.getUuid())
        .orElseGet(Assertions::fail);
    assertEquals(1, artPtrs.size());

    assertEquals(vTag, ptr.getVersionTag());
    assertTrue(ptr.getVersionId().toString().endsWith(vTag));

    assertEquals(vTag, ans.getAssetId().getVersionTag());
    assertTrue(ans.getAssetId().getVersionId().toString().endsWith(vTag));

    assertEquals(aTag, artId.getVersionTag());
    assertTrue(artId.getVersionId().toString().endsWith(aTag));

    assertEquals(artId.getVersionId(), latestArtId.getVersionId());
  }

  @Test
  void testSurrogateXML() {
    // the use of non-SemVer IDs is discouraged, but should behave consistently
    UUID assetUUID = uuid("942124");
    String versionTag = "0.0.0";
    ResourceIdentifier assetId = SurrogateBuilder.assetId(BASE_UUID_URN_URI, assetUUID, versionTag);

    KnowledgeAsset surrogate = SurrogateBuilder
        .newSurrogate(assetId)
        .withName("Test", "Test Asset")
        .get();

    Answer<Void> ans = semanticRepository.setKnowledgeAssetVersion(
        assetId.getUuid(), assetId.getVersionTag(), surrogate);
    assertTrue(ans.isSuccess());

    List<Pointer> ptrs = semanticRepository.listKnowledgeAssetSurrogates(assetUUID, versionTag)
        .orElseGet(Assertions::fail);
    assertEquals(1, ptrs.size());

    Pointer surrPtr = ptrs.stream()
        .findFirst()
        .orElseGet(Assertions::fail);

    Answer<KnowledgeCarrier> xmlCarrier =
        semanticRepository.getKnowledgeAssetSurrogateVersion(
            assetUUID,
            versionTag,
            surrPtr.getUuid(),
            surrPtr.getVersionTag(),
            codedRep(Knowledge_Asset_Surrogate_2_0, XML_1_1))
            .flatMap(kc -> new Surrogate2Parser()
                .applyLift(
                    kc,
                    Abstract_Knowledge_Expression,
                    codedRep(Knowledge_Asset_Surrogate_2_0),
                    null));
    assertTrue(xmlCarrier.isSuccess());

    KnowledgeAsset ax2 = xmlCarrier
        .flatOpt(kc -> kc.as(KnowledgeAsset.class))
        .orElseGet(Assertions::fail);

    assertSame(Comparison.EQUIVALENT, new SurrogateDiffer().contrast(surrogate, ax2));
  }


  @Test
  void testListCarriersSurrogatesNotFound() {
    // t use of non-SemVer IDs is discouraged, but should behave consistently
    UUID assetUUID = randomAssetId().getUuid();
    String versionTag = "0.0.0";

    Answer<List<Pointer>> ptrs = semanticRepository
        .listKnowledgeAssetCarriers(assetUUID, versionTag);
    assertTrue(ptrs.isNotFound());

    Answer<List<Pointer>> ptrs2 = semanticRepository
        .listKnowledgeAssetSurrogates(assetUUID, versionTag);
    assertTrue(ptrs2.isNotFound());
  }

  @Test
  void testDiffOnConflict() {
    ResourceIdentifier axId = randomAssetId(testAssetNS());
    KnowledgeAsset ka1 = new KnowledgeAsset()
        .withAssetId(axId)
        .withName("Foo");
    KnowledgeAsset ka2 = new KnowledgeAsset()
        .withAssetId(axId)
        .withName("Bar");

    Answer<Void> ans1 = semanticRepository.setKnowledgeAssetVersion(
        axId.getUuid(), axId.getVersionTag(), ka1);
    assertTrue(ans1.isSuccess());

    Answer<Void> ans2 = semanticRepository.setKnowledgeAssetVersion(
        axId.getUuid(), axId.getVersionTag(), ka2);
    assertFalse(ans2.isSuccess());
    assertTrue(Conflict.sameAs(ans2.getOutcomeType()));

    String expl = ans2.getExplanation().asString().orElse("");
    assertTrue(expl.contains(axId.asKey().toString()));
    assertTrue(expl.contains("Foo"));
    assertTrue(expl.contains("Bar"));
  }

  @Test
  void testAssetIdWithURNamespace() {
    ResourceIdentifier assetId = newId(BASE_UUID_URN_URI, UUID.randomUUID(), VERSION_ZERO);
    ResourceIdentifier artifactId = newId(BASE_UUID_URN_URI, UUID.randomUUID(), VERSION_ZERO);
    KnowledgeAsset ka1 = new KnowledgeAsset()
        .withAssetId(assetId)
        .withName("Foo")
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(artifactId));

    Answer<Void> ans1 = semanticRepository.setKnowledgeAssetVersion(
        assetId.getUuid(), assetId.getVersionTag(), ka1);
    assertTrue(ans1.isSuccess());

    Answer<Void> ans2 = semanticRepository.setKnowledgeAssetCarrierVersion(
        assetId.getUuid(),
        assetId.getVersionTag(),
        artifactId.getUuid(),
        artifactId.getVersionTag(),
        "Foo".getBytes()
    );
    assertTrue(ans2.isSuccess());
  }

  @Test
  void testUpdateSurrogateVersion() {
    ResourceIdentifier axId =
        assetId(testAssetNS(), uuid("testSurrogateVersion"), VERSION_ZERO);
    KnowledgeAsset surr = SurrogateBuilder.newSurrogate(axId)
        .withName("Test", "")
        .get();

    Answer<Void> ans1 =
        semanticRepository.setKnowledgeAssetVersion(axId.getUuid(), axId.getVersionTag(), surr);
    assertTrue(ans1.isSuccess());

    KnowledgeArtifact self = getCanonicalSurrogateId(surr)
        .flatMap(sid -> getComputableSurrogateMetadata(sid.getUuid(), sid.getVersionTag(), surr))
        .orElseGet(Assertions::fail);

    surr.setName("Changed name");

    Answer<Void> ans2 =
        semanticRepository.setKnowledgeAssetVersion(axId.getUuid(), axId.getVersionTag(), surr);
    assertTrue(ans2.isFailure());

    ResourceIdentifier oldSurrogateId = self.getArtifactId();
    ResourceIdentifier newSurrogateId = newId(
        oldSurrogateId.getNamespaceUri(),
        oldSurrogateId.getUuid(),
        oldSurrogateId.getSemanticVersionTag().incrementMinorVersion().toString());
    self.setArtifactId(newSurrogateId);

    Answer<Void> ans3 =
        semanticRepository.setKnowledgeAssetVersion(axId.getUuid(), axId.getVersionTag(), surr);
    assertTrue(ans3.isSuccess());

    KnowledgeAsset latestSurr =
        semanticRepository.getKnowledgeAsset(axId.getUuid(), axId.getVersionTag())
            .orElseGet(Assertions::fail);
    assertEquals("Changed name", latestSurr.getName());
    assertEquals(newSurrogateId.asKey(),
        getCanonicalSurrogateId(latestSurr).orElseGet(Assertions::fail).asKey());

  }


  @Test
  void testAddCarrier() {
    ResourceIdentifier axId =
        assetId(testAssetNS(), uuid("testAddCarrier"), VERSION_ZERO);
    ResourceIdentifier carrId1 =
        artifactId(testArtifactNS(), uuid("art1"), VERSION_ZERO);
    ResourceIdentifier carrId2 =
        artifactId(testArtifactNS(), uuid("art2"), VERSION_ZERO);

    KnowledgeAsset surr = SurrogateBuilder.newSurrogate(axId)
        .withName("Test", "")
        .get();
    KnowledgeCarrier artifact1 = AbstractCarrier.of("Foo")
        .withAssetId(axId)
        .withArtifactId(carrId1)
        .withRepresentation(rep(HTML, TXT, Charset.defaultCharset()));
    KnowledgeCarrier artifact2 = AbstractCarrier.of("Bar")
        .withAssetId(axId)
        .withArtifactId(carrId2)
        .withRepresentation(rep(HTML, TXT, Charset.defaultCharset()));

    ResourceIdentifier surrogateId = getCanonicalSurrogateId(surr).orElseGet(Assertions::fail);

    // Just set the Asset (Surrogate)
    Answer<Void> ans1 =
        semanticRepository.setKnowledgeAssetVersion(axId.getUuid(), axId.getVersionTag(), surr);
    assertTrue(ans1.isSuccess());
    KnowledgeAsset surr1 =
        semanticRepository.getKnowledgeAsset(axId.getUuid(), axId.getVersionTag())
            .orElseGet(Assertions::fail);
    assertTrue(surr1.getCarriers().isEmpty());
    assertEquals(1, surr1.getSurrogate().size());

    // Add a first carrier
    Answer<Void> ans2 =
        semanticRepository
            .addKnowledgeAssetCarrier(axId.getUuid(), axId.getVersionTag(), artifact1);
    assertTrue(ans2.isSuccess());
    KnowledgeAsset surr2 =
        semanticRepository.getKnowledgeAsset(axId.getUuid(), axId.getVersionTag())
            .orElseGet(Assertions::fail);
    assertEquals(1, surr2.getCarriers().size());
    assertEquals(1, surr2.getSurrogate().size());
    ResourceIdentifier surrogateId2 = getCanonicalSurrogateId(surr2).orElseGet(Assertions::fail);
    assertEquals(surrogateId2.getSemanticVersionTag(),
        surrogateId.getSemanticVersionTag().incrementMinorVersion());

    // Re-add the same carrier - should be idempotent
    Answer<Void> ans3 =
        semanticRepository
            .addKnowledgeAssetCarrier(axId.getUuid(), axId.getVersionTag(), artifact1);
    assertTrue(ans3.isSuccess());
    KnowledgeAsset surr3 =
        semanticRepository.getKnowledgeAsset(axId.getUuid(), axId.getVersionTag())
            .orElseGet(Assertions::fail);
    assertEquals(1, surr3.getCarriers().size());
    assertEquals(1, surr3.getSurrogate().size());
    ResourceIdentifier surrogateId3 = getCanonicalSurrogateId(surr3).orElseGet(Assertions::fail);
    assertEquals(surrogateId3.getSemanticVersionTag(),
        surrogateId.getSemanticVersionTag().incrementMinorVersion());

    // Add an additional carrier
    Answer<Void> ans4 =
        semanticRepository
            .addKnowledgeAssetCarrier(axId.getUuid(), axId.getVersionTag(), artifact2);
    assertTrue(ans4.isSuccess());
    KnowledgeAsset surr4 =
        semanticRepository.getKnowledgeAsset(axId.getUuid(), axId.getVersionTag())
            .orElseGet(Assertions::fail);
    assertEquals(2, surr4.getCarriers().size());
    assertEquals(1, surr4.getSurrogate().size());
    ResourceIdentifier surrogateId4 = getCanonicalSurrogateId(surr4).orElseGet(Assertions::fail);
    assertEquals(
        surrogateId.getSemanticVersionTag().incrementMinorVersion().incrementMinorVersion(),
        surrogateId4.getSemanticVersionTag());
  }

  @Test
  void testUpdateSurrogate() {
    ResourceIdentifier axId =
        assetId(testAssetNS(), uuid("testUpdateSurrogate"), VERSION_ZERO);

    KnowledgeAsset surr = SurrogateBuilder.newSurrogate(axId)
        .withName("Test", "")
        .get();

    Answer<Void> ans1 = semanticRepository
        .setKnowledgeAssetVersion(axId.getUuid(), axId.getVersionTag(), surr);

    surr.withFormalType(Cognitive_Care_Process_Model);
    KnowledgeArtifact meta = surr.getSurrogate().get(0);
    meta.withArtifactId(SemanticIdentifier.newId(
        meta.getArtifactId().getNamespaceUri(),
        meta.getArtifactId().getUuid(),
        Version.valueOf(meta.getArtifactId().getVersionTag()).incrementPatchVersion()
    ));

    Answer<Void> ans2 = semanticRepository
        .setKnowledgeAssetVersion(axId.getUuid(), axId.getVersionTag(), surr);

    List<Pointer> ptrs = semanticRepository
        .listKnowledgeAssets(Cognitive_Care_Process_Model.getTag(), null, null, 0, -1)
        .orElseGet(Assertions::fail);
    assertEquals(1, ptrs.size());

    KnowledgeAsset asset = semanticRepository.getKnowledgeAsset(axId.getUuid())
        .orElseGet(Assertions::fail);
    assertEquals(1,asset.getFormalType().size());
    assertTrue(Cognitive_Care_Process_Model.isAnyOf(asset.getFormalType()));
  }

  @Test
  void testAssetWithDomainSpecificType() {
    ResourceIdentifier axId =
        assetId(testAssetNS(), uuid("testAssetWithDomainSpecificType"), VERSION_ZERO);

    KnowledgeAsset surr = SurrogateBuilder.newSurrogate(axId)
        .withName("Test", "")
        .get()
        .withFormalType(Clinical_Guidance_Rule);
    semanticRepository.setKnowledgeAssetVersion(axId.getUuid(),axId.getVersionTag(),surr);

    assertEquals(1,
        semanticRepository.listKnowledgeAssets()
            .orElseGet(Assertions::fail)
            .size());

    assertEquals(1,
        semanticRepository.listKnowledgeAssets(Clinical_Guidance_Rule.getTag(), null, null, 0, -1)
            .orElseGet(Assertions::fail)
            .size());
  }


}
