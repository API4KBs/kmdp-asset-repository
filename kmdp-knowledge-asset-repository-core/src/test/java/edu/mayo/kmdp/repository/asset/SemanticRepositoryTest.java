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

import static edu.mayo.kmdp.util.Util.uuid;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassetrole.KnowledgeAssetRoleSeries.Operational_Concept_Definition;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Care_Process_Model;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Formal_Ontology;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Predictive_Model;
import static edu.mayo.ontology.taxonomies.kao.rel.dependencyreltype.DependencyTypeSeries.Depends_On;
import static edu.mayo.ontology.taxonomies.kmdo.annotationreltype.AnnotationRelTypeSeries.Defines;
import static edu.mayo.ontology.taxonomies.kmdo.annotationreltype.AnnotationRelTypeSeries.In_Terms_Of;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.HL7_ELM;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;

import edu.mayo.kmdp.metadata.v2.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.v2.surrogate.Dependency;
import edu.mayo.kmdp.metadata.v2.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.v2.surrogate.SurrogateBuilder;
import edu.mayo.kmdp.metadata.v2.surrogate.annotations.Annotation;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.repository.artifact.exceptions.ResourceNotFoundException;
import edu.mayo.kmdp.util.DateTimeUtil;
import edu.mayo.ontology.taxonomies.api4kp.responsecodes.ResponseCodeSeries;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.AbstractCarrier;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.id.ConceptIdentifier;
import org.omg.spec.api4kp._1_0.id.IdentifierConstants;
import org.omg.spec.api4kp._1_0.id.Pointer;
import org.omg.spec.api4kp._1_0.id.SemanticIdentifier;
import org.omg.spec.api4kp._1_0.id.Term;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;


class SemanticRepositoryTest extends RepositoryTestBase {

  private static final String BASE_URI = Registry.MAYO_ASSETS_BASE_URI;

  @Test
  void testInit() {
    assertNotNull(semanticRepository);
  }

  // listKnowledgeAssets
  @Test
  void testListAll() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo2"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    List<Pointer> assets = semanticRepository
        .listKnowledgeAssets().orElse(emptyList());

    assertNotNull(assets);
    assertEquals(2, assets.size());
  }

  @Test
  void testListAllInMemoryHelperMethod() {
    KnowledgeAssetRepositoryService repo = KnowledgeAssetRepositoryService
        .selfContainedRepository();

    assertTrue(repo
        .setVersionedKnowledgeAsset(uuid("foo"), "1",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model))
        .isSuccess());

    assertTrue(repo
        .setVersionedKnowledgeAsset(uuid("foo2"), "1",
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
        .setVersionedKnowledgeAsset(uuid("foo"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "2",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    List<Pointer> assets = semanticRepository
        .listKnowledgeAssets()
        .orElse(emptyList());

    assertNotNull(assets);
    assertEquals(1, assets.size());
  }

  @Test
  void testListAllMultipleVersionsIsLatest() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "2",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    List<Pointer> assets = semanticRepository
        .listKnowledgeAssets()
        .orElse(emptyList());

    assertNotNull(assets);
    assertEquals(1, assets.size());

    assertEquals("2", assets.get(0).getVersionTag());
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
        .setVersionedKnowledgeAsset(uuid("foo"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    List<Pointer> assets = semanticRepository
        .listKnowledgeAssets(Care_Process_Model.getTag(), null, null, -1, -1)
        .orElse(emptyList());

    assertNotNull(assets);
    assertEquals(1, assets.size());

    assertEquals(Care_Process_Model.getRef(), assets.get(0).getType());
  }

  // initKnowledgeAsset
  @Test
  void testInitAssetReturnsUUIDAndIsCreated() {
    Answer<UUID> responseEntity =
        semanticRepository.initKnowledgeAsset();
    assertEquals(ResponseCodeSeries.Created, responseEntity.getOutcomeType());
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
        .setVersionedKnowledgeAsset(uuid("foo"), "1",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "2",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    KnowledgeAsset asset =
        semanticRepository
            .getKnowledgeAsset(uuid("foo"))
            .orElse(null);

    assertNotNull(asset);
    assertEquals("2", asset.getAssetId().getVersionTag());
  }

  @Test
  void testGetLatestOutOfOrder() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "5",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "2",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    KnowledgeAsset asset =
        semanticRepository
            .getKnowledgeAsset(uuid("foo"))
            .orElse(null);

    assertNotNull(asset);
    assertEquals("2", asset.getAssetId().getVersionTag());
  }

  @Test
  void testGetLatestAssetNotFound() {
    // 404 status returned if attempting to retrieve the latest version of an asset that doesn't exist
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "5",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "2",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    Answer<KnowledgeAsset> response =
        semanticRepository.getKnowledgeAsset(uuid("fooDoesNotExist"));

    assertEquals(ResponseCodeSeries.NotFound, response.getOutcomeType());
  }

  // getKnowledgeAssetVersions

  @Test
  void getVersions() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "1",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "2",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));

    List<Pointer> versions = semanticRepository
        .getKnowledgeAssetVersions(uuid("foo"))
        .orElse(emptyList());

    assertNotNull(versions);
    assertEquals(2, versions.size());
  }

  @Test
  void getVersionsAssetNotFound() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "1",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "2",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));

    assertThrows(
        ResourceNotFoundException.class,
        () -> semanticRepository
            .getKnowledgeAssetVersions(uuid("fooNotFound")));
  }

  //getVersionedKnowledgeAsset

  @Test
  void testSpecificVersionSuccess() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "5",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "2",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    KnowledgeAsset asset = semanticRepository
        .getVersionedKnowledgeAsset(uuid("foo"), "5")
        .orElse(null);

    assertNotNull(asset);
    assertEquals("5", asset.getAssetId().getVersionTag());
  }

  @Test
  void testSpecificVersionAssetNotFound() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "5",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "2",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    Answer<KnowledgeAsset> response =
        semanticRepository.getVersionedKnowledgeAsset(uuid("fooDoeNotExist"), "5");
    assertEquals(ResponseCodeSeries.NotFound, response.getOutcomeType());
  }

  @Test
  void testSpecificVersionAssetExistsVersionNotFound() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "5",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "2",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    Answer<KnowledgeAsset> response =
        semanticRepository.getVersionedKnowledgeAsset(uuid("foo"), "12345");
    assertEquals(ResponseCodeSeries.NotFound, response.getOutcomeType());
  }

  // setVersionedKnowledgeAsset
  @Test
  void testSetVersionedAssetAssetDoesNotExist() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "1",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    KnowledgeAsset asset = new KnowledgeAsset()
        .withFormalType(Care_Process_Model)
        .withAssetId(
            SurrogateBuilder.assetId(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "1"));
    Answer<Void> response = semanticRepository
        .setVersionedKnowledgeAsset(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "1",
            asset);
    //expect 204 status code
    assertEquals(ResponseCodeSeries.NoContent, response.getOutcomeType());
  }

  @Test
  void testSetVersionedAssetVersionDoesNotExist() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "1",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    KnowledgeAsset asset = new KnowledgeAsset()
        .withFormalType(Care_Process_Model)
        .withAssetId(
            SurrogateBuilder.assetId(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "2"));
    Answer<Void> response = semanticRepository
        .setVersionedKnowledgeAsset(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "2",
            asset);
    assertEquals(ResponseCodeSeries.NoContent, response.getOutcomeType());

    List<Pointer> versions = semanticRepository
        .getKnowledgeAssetVersions(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"))
        .orElse(emptyList());

    assertEquals(2, versions.size());
  }

  @Test
  void testSetVersionedAssetVersionAlreadyExists_IsReplaced() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "1",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    KnowledgeAsset asset = new KnowledgeAsset()
        .withFormalType(Predictive_Model)
        .withAssetId(
            SurrogateBuilder.assetId(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "1"));
    Answer<Void> response = semanticRepository
        .setVersionedKnowledgeAsset(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "1",
            asset);
    //expect 204 status code
    assertEquals(ResponseCodeSeries.NoContent, response.getOutcomeType());

    KnowledgeAsset assetResult = semanticRepository
        .getVersionedKnowledgeAsset(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "1")
        .orElse(null);
    assertNotNull(assetResult);

    assertEquals(Predictive_Model, assetResult.getFormalType().get(0));
  }


  @Test
  void testInconsistentVersionId_shouldFail() {
    KnowledgeAsset asset = new KnowledgeAsset()
        .withFormalType(Predictive_Model)
        .withAssetId(
            SurrogateBuilder.assetId(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "1"));
    Answer<Void> response = semanticRepository
        .setVersionedKnowledgeAsset(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "2",
            asset);
    assertEquals(ResponseCodeSeries.Conflict, response.getOutcomeType());
  }


  @Test
  void testInconsistentAssetId_shouldFail() {
    KnowledgeAsset asset = new KnowledgeAsset()
        .withFormalType(Predictive_Model)
        .withAssetId(
            SurrogateBuilder.assetId(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "1"));
    Answer<Void> response = semanticRepository
        .setVersionedKnowledgeAsset(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"), "1",
            asset);
    assertEquals(ResponseCodeSeries.Conflict, response.getOutcomeType());
  }

  @Test
  void testMissingAssetId_getsSet() {
    KnowledgeAsset asset = new KnowledgeAsset()
        .withFormalType(Predictive_Model);
    Answer<Void> response = semanticRepository
        .setVersionedKnowledgeAsset(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"), "1",
            asset);
    assertEquals(ResponseCodeSeries.NoContent, response.getOutcomeType());
    String expectedAssetId = BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52";
    String expectedVersionId = BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52/versions/1";
    KnowledgeAsset ka = semanticRepository
        .getVersionedKnowledgeAsset(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"), "1")
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
        .setVersionedKnowledgeAsset(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"), "1",
            asset);
    assertEquals(ResponseCodeSeries.NoContent, response.getOutcomeType());

    String expectedAssetId = BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52";
    String expectedVersionId = BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52/versions/1";
    KnowledgeAsset ka = semanticRepository
        .getVersionedKnowledgeAsset(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"), "1")
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
            SemanticIdentifier.newVersionId(URI.create(BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52/versions/1")));

    Answer<Void> response = semanticRepository
        .setVersionedKnowledgeAsset(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"), "1",
            asset);
    assertEquals(ResponseCodeSeries.NoContent, response.getOutcomeType());
    String expectedAssetId = BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52";
    String expectedVersionId = BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52/versions/1";
    KnowledgeAsset ka = semanticRepository
        .getVersionedKnowledgeAsset(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"), "1")
        .orElse(null);
    assertNotNull(ka);

    assertEquals(ka.getAssetId().getResourceId().toString(), expectedAssetId);
    assertEquals(ka.getAssetId().getVersionId().toString(), expectedVersionId);
  }


  @Test
  void initAndGetAssetByType() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    List<Pointer> assets = semanticRepository
        .listKnowledgeAssets(Care_Process_Model.getTag(), null, null, -1, -1)
        .orElse(emptyList());

    assertEquals(1, assets.size());
  }

  @Test
  void initAndGetAssetByRole() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "1",
            new KnowledgeAsset().withRole(Operational_Concept_Definition)));
    List<Pointer> assets = semanticRepository
        .listKnowledgeAssets(Operational_Concept_Definition.getTag(), null, null, -1, -1)
        .orElse(emptyList());

    assertEquals(1, assets.size());
  }

  @Test
  void initAndGetAssetByWrongType() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "1",
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
        semanticRepository.setVersionedKnowledgeAsset(UUID.randomUUID(),
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
        .setVersionedKnowledgeAsset(uuid("foo"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "2",
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
        .setVersionedKnowledgeAsset(uuid("foo"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "2",
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
        semanticRepository.setVersionedKnowledgeAsset(UUID.randomUUID(),
            "1",
            axx));
    List<Pointer> assets = semanticRepository.listKnowledgeAssets()
        .orElse(emptyList());

    assertNotNull(assets);
    assertEquals(1, assets.size());
  }

  @Test
  void initAndGetAssetByAnnotation() {
    assertNotNull(semanticRepository.setVersionedKnowledgeAsset(uuid("1"), "1",
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
    assertNotNull(semanticRepository.setVersionedKnowledgeAsset(uuid("1"), "1",
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
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));

    semanticRepository
        .setKnowledgeAssetCarrierVersion(uuid("foo"), "1", uuid("q"), "z", "there".getBytes());
    KnowledgeCarrier artifact = semanticRepository
        .getCanonicalKnowledgeAssetCarrier(uuid("foo"), "1")
        .orElse(null);

    assertNotNull(artifact);
    assertEquals("there", new String(((BinaryCarrier) artifact).getEncodedExpression()));
  }

  @Test
  void addKnowledgeAssetCarriers() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));

    semanticRepository
        .setKnowledgeAssetCarrierVersion(uuid("foo"), "1", uuid("q"), "z", "there".getBytes());
    List<Pointer> artifacts = semanticRepository
        .getKnowledgeAssetCarriers(uuid("foo"), "1")
        .orElse(emptyList());

    assertEquals(1, artifacts.size());
  }


  private ConceptIdentifier dizziness = Term.newTerm(
      "dizzy",
      UUID.randomUUID(),
      Registry.BASE_UUID_URN_URI,
      URI.create("uri:urn:dizzy"),
      IdentifierConstants.VERSION_LATEST,
      "Dizzy",
      DateTimeUtil.now()).asConceptIdentifier();

  private ConceptIdentifier sleep_apnea = Term.newTerm(
      "sleepApnea",
      UUID.randomUUID(),
      Registry.BASE_UUID_URN_URI,
      URI.create("uri:urn:sleepApnea"),
      IdentifierConstants.VERSION_LATEST,
      "Sleep Apnea",
      DateTimeUtil.now()).asConceptIdentifier();

  @Test
  void addKnowledgeAssetCarriersMultiple() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));

    semanticRepository
        .setKnowledgeAssetCarrierVersion(uuid("foo"), "1", uuid("q"), "z", "there".getBytes());
    semanticRepository
        .setKnowledgeAssetCarrierVersion(uuid("foo"), "1", uuid("q"), "x", "there".getBytes());
    List<Pointer> artifacts = semanticRepository
        .getKnowledgeAssetCarriers(uuid("foo"), "1")
        .orElse(emptyList());

    assertNotNull(artifacts);
    assertEquals(2, artifacts.size());
  }

  @Test
  void initAndGetAllDefinitions() {
    assertNotNull(semanticRepository.setVersionedKnowledgeAsset(uuid("1"), "1",
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
    assertNotNull(semanticRepository.setVersionedKnowledgeAsset(uuid("1"), "1",
        new KnowledgeAsset().withRole(Operational_Concept_Definition)
            .withAnnotation(new Annotation()
                .withRel(Defines.asConceptIdentifier())
                .withRef(dizziness))));

    assertNotNull(semanticRepository.setVersionedKnowledgeAsset(uuid("2"), "1",
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
    assertNotNull(semanticRepository.setVersionedKnowledgeAsset(uuid("1"), "1",
        new KnowledgeAsset().withRole(Operational_Concept_Definition)
            .withAnnotation(new Annotation()
                .withRel(Defines.asConceptIdentifier())
                .withRef(dizziness))));

    assertNotNull(semanticRepository.setVersionedKnowledgeAsset(uuid("1"), "2",
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
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "1",
            new KnowledgeAsset()
                .withName("Test")
                .withCarriers(new ComputableKnowledgeArtifact()
                    .withArtifactId(
                        SemanticIdentifier.newId(Registry.BASE_UUID_URN_URI, uuid("q"), "z"))
                    .withRepresentation(rep(HTML))
                )
        ));

    semanticRepository
        .setKnowledgeAssetCarrierVersion(uuid("foo"), "1", uuid("q"), "z", "there".getBytes());

    Answer<KnowledgeCarrier> kc = semanticRepository.getKnowledgeAssetCarrierVersion(
        uuid("foo"), "1",
        uuid("q"), "z"
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

    semanticRepository.setVersionedKnowledgeAsset(u1, "1", new KnowledgeAsset()
        .withCarriers(new ComputableKnowledgeArtifact()
            .withArtifactId(SurrogateBuilder.randomArtifactId())
            .withRepresentation(rep(HL7_ELM))));
    semanticRepository.addKnowledgeAssetCarrier(u1, "1", "HI1!".getBytes());

    KnowledgeAsset asset1 = semanticRepository.getVersionedKnowledgeAsset(u1, "1")
        .orElse(new KnowledgeAsset());

    semanticRepository.setVersionedKnowledgeAsset(u2, "1", new KnowledgeAsset()
        .withLinks(new Dependency().withRel(Depends_On).withHref(asset1.getAssetId()))
        .withCarriers(new ComputableKnowledgeArtifact()
            .withArtifactId(SurrogateBuilder.randomArtifactId())
            .withRepresentation(rep(HL7_ELM))));
    semanticRepository.addKnowledgeAssetCarrier(u2, "1", "HI2!".getBytes());

    List<KnowledgeCarrier> carriers = semanticRepository.getKnowledgeArtifactBundle(u2, "1",
        Depends_On.getTag(), -1, null).getOptionalValue()
        .orElseGet(Collections::emptyList);

    assertEquals(2, carriers.size());
  }


}
