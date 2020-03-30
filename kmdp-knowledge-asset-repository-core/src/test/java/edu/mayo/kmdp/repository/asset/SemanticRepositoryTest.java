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

import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.metadata.annotations.resources.SimpleAnnotation;
import edu.mayo.kmdp.metadata.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.surrogate.Dependency;
import edu.mayo.kmdp.metadata.surrogate.Representation;
import edu.mayo.kmdp.metadata.surrogate.resources.KnowledgeAsset;
import edu.mayo.kmdp.metadata.v2.surrogate.SurrogateBuilder;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.repository.artifact.exceptions.ResourceNotFoundException;
import edu.mayo.ontology.taxonomies.api4kp.responsecodes.ResponseCodeSeries;
import edu.mayo.ontology.taxonomies.skos.relatedconcept.RelatedConceptSeries;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.AbstractCarrier;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.identifiers.ConceptIdentifier;
import org.omg.spec.api4kp._1_0.id.Pointer;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static edu.mayo.kmdp.util.Util.uuid;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassetrole.KnowledgeAssetRoleSeries.Operational_Concept_Definition;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.*;
import static edu.mayo.ontology.taxonomies.kao.rel.dependencyreltype.DependencyTypeSeries.Depends_On;
import static edu.mayo.ontology.taxonomies.kmdo.annotationreltype.AnnotationRelTypeSeries.Defines;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.HL7_ELM;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;


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
    KnowledgeAssetRepositoryService repo = KnowledgeAssetRepositoryService.selfContainedRepository();

    assertNotNull(repo
            .setVersionedKnowledgeAsset(uuid("foo"), "1",
                    new KnowledgeAsset().withFormalType(Care_Process_Model)));
    assertNotNull(repo
            .setVersionedKnowledgeAsset(uuid("foo2"), "1",
                    new KnowledgeAsset().withFormalType(Care_Process_Model)));
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
        .listKnowledgeAssets(Care_Process_Model.getTag(), null, -1, -1)
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

    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset =
        semanticRepository.getKnowledgeAsset(newAssetId).orElse(null);

    assertNotNull(asset);
    String expected = BASE_URI + newAssetId;
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
        .setVersionedKnowledgeAsset(uuid("foo"), "1",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "2",
            new KnowledgeAsset()
                .withFormalType(Care_Process_Model)));
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset =
        semanticRepository
            .getKnowledgeAsset(uuid("foo"))
            .orElse(null);

    assertNotNull(asset);
    assertEquals("2", asset.getAssetId().getVersion());
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
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset =
        semanticRepository
            .getKnowledgeAsset(uuid("foo"))
            .orElse(null);

    assertNotNull(asset);
    assertEquals("2", asset.getAssetId().getVersion());
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
    Answer<edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset> response =
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
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset = semanticRepository
        .getVersionedKnowledgeAsset(uuid("foo"), "5")
        .orElse(null);

    assertNotNull(asset);
    assertEquals("5", asset.getAssetId().getVersion());
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
    Answer<edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset> response =
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
    Answer<edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset> response =
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
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset = new KnowledgeAsset()
        .withFormalType(Care_Process_Model)
        .withAssetId(new URIIdentifier()
            .withUri(URI.create(BASE_URI + "45a81582-1b1d-3439-9400-6e2fee0c3f52"))
            .withVersionId(
                URI.create(BASE_URI + "b9a26917-0a79-483d-b0e8-6610ba9aad5b/versions/1")));
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
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset = new KnowledgeAsset()
        .withFormalType(Care_Process_Model)
        .withAssetId(new URIIdentifier()
            .withUri(URI.create(BASE_URI + "45a81582-1b1d-3439-9400-6e2fee0c3f52"))
            .withVersionId(
                URI.create(BASE_URI + "45a81582-1b1d-3439-9400-6e2fee0c3f52/versions/2")));
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
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset = new KnowledgeAsset()
        .withFormalType(Predictive_Model)
        .withAssetId(new URIIdentifier()
            .withUri(URI.create(BASE_URI + "45a81582-1b1d-3439-9400-6e2fee0c3f52"))
            .withVersionId(
                URI.create(BASE_URI + "45a81582-1b1d-3439-9400-6e2fee0c3f52/versions/1")));
    Answer<Void> response = semanticRepository
        .setVersionedKnowledgeAsset(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "1",
            asset);
    //expect 204 status code
    assertEquals(ResponseCodeSeries.NoContent, response.getOutcomeType());

    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset assetResult = semanticRepository
        .getVersionedKnowledgeAsset(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "1")
        .orElse(null);
    assertNotNull(assetResult);

    assertEquals(Predictive_Model, assetResult.getFormalType().get(0));
  }


  @Test
  void testInconsistentVersionId_shouldFail() {
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset = new KnowledgeAsset()
        .withFormalType(Predictive_Model)
        .withAssetId(new URIIdentifier()
            .withUri(URI.create(BASE_URI + "45a81582-1b1d-3439-9400-6e2fee0c3f52"))
            .withVersionId(
                URI.create(BASE_URI + "45a81582-1b1d-3439-9400-6e2fee0c3f52/versions/1")));
    Answer<Void> response = semanticRepository
        .setVersionedKnowledgeAsset(UUID.fromString("45a81582-1b1d-3439-9400-6e2fee0c3f52"), "2",
            asset);
    assertEquals(ResponseCodeSeries.Conflict, response.getOutcomeType());
  }


  @Test
  void testInconsistentAssetId_shouldFail() {
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset = new KnowledgeAsset()
        .withFormalType(Predictive_Model)
        .withAssetId(new URIIdentifier()
            .withUri(URI.create(BASE_URI + "45a81582-1b1d-3439-9400-6e2fee0c3f52"))
            .withVersionId(
                URI.create(BASE_URI + "b9a26917-0a79-483d-b0e8-6610ba9aad5b/versions/1")));
    Answer<Void> response = semanticRepository
        .setVersionedKnowledgeAsset(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"), "1",
            asset);
    assertEquals(ResponseCodeSeries.Conflict, response.getOutcomeType());
  }

  @Test
  void testMissingAssetId_getsSet() {
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset = new KnowledgeAsset()
        .withFormalType(Predictive_Model);
    Answer<Void> response = semanticRepository
        .setVersionedKnowledgeAsset(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"), "1",
            asset);
    assertEquals(ResponseCodeSeries.NoContent, response.getOutcomeType());
    String expectedAssetId = BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52";
    String expectedVersionId = BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52/versions/1";
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset ka = semanticRepository
        .getVersionedKnowledgeAsset(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"), "1")
        .orElse(null);
    assertNotNull(ka);

    assertEquals(ka.getAssetId().getUri().toString(), expectedAssetId);
    assertEquals(ka.getAssetId().getVersionId().toString(), expectedVersionId);
  }

  @Test
  void testMissingVersionOnly_getsSet() {
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset = new KnowledgeAsset()
      .withFormalType(Predictive_Model)
    .withAssetId(new URIIdentifier()
      .withUri(URI.create(BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52"))
      );
    Answer<Void> response = semanticRepository
      .setVersionedKnowledgeAsset(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"), "1",
        asset);
    assertEquals(ResponseCodeSeries.NoContent, response.getOutcomeType());

    String expectedAssetId = BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52";
    String expectedVersionId = BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52/versions/1";
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset ka = semanticRepository
      .getVersionedKnowledgeAsset(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"), "1")
      .orElse(null);
    assertNotNull(ka);

    assertEquals(ka.getAssetId().getUri().toString(), expectedAssetId);
    assertEquals(ka.getAssetId().getVersionId().toString(), expectedVersionId);
  }

  @Test
  void testMissingAssetIdOnly_getsSet() {
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset = new KnowledgeAsset()
      .withFormalType(Predictive_Model)
      .withAssetId(new URIIdentifier()
        .withVersionId(
          URI.create(BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52/versions/1"))
      );
    Answer<Void> response = semanticRepository
      .setVersionedKnowledgeAsset(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"), "1",
        asset);
    assertEquals(ResponseCodeSeries.NoContent, response.getOutcomeType());
    String expectedAssetId = BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52";
    String expectedVersionId = BASE_URI + "12a81582-1b1d-3439-9400-6e2fee0c3f52/versions/1";
    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset ka = semanticRepository
      .getVersionedKnowledgeAsset(UUID.fromString("12a81582-1b1d-3439-9400-6e2fee0c3f52"), "1")
      .orElse(null);
    assertNotNull(ka);

    assertEquals(ka.getAssetId().getUri().toString(), expectedAssetId);
    assertEquals(ka.getAssetId().getVersionId().toString(), expectedVersionId);
  }


  @Test
  void initAndGetAssetByType() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    List<Pointer> assets = semanticRepository
        .listKnowledgeAssets(Care_Process_Model.getTag(), null, -1, -1)
        .orElse(emptyList());

    assertEquals(1, assets.size());
  }

  @Test
  void initAndGetAssetByWrongType() {
    assertNotNull(semanticRepository
            .setVersionedKnowledgeAsset(uuid("foo"), "1",
                    new KnowledgeAsset().withFormalType(Care_Process_Model)));
    List<Pointer> assets = semanticRepository
            .listKnowledgeAssets(Formal_Ontology.getTag(), null, -1, -1)
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
        .listKnowledgeAssets(Care_Process_Model.getTag(), null, -1, -1)
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
        .listKnowledgeAssets(Care_Process_Model.getTag(), null, -1, -1)
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
        .listKnowledgeAssets(Care_Process_Model.getTag(), null, -1, -1)
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
        .listKnowledgeAssets(Care_Process_Model.getTag(), null, -1, -1)
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
        new KnowledgeAsset().withSubject(
            new SimpleAnnotation()
                .withRel(RelatedConceptSeries.Has_Broader.asConcept())
                .withExpr(new ConceptIdentifier()
                    .withConceptId(URI.create("http://something"))))));

    List<Pointer> pointers = semanticRepository
        .listKnowledgeAssets(null, RelatedConceptSeries.Has_Broader.asConcept().getTag(), -1, -1)
        .orElse(emptyList());
    assertNotNull(pointers);
    assertEquals(1, pointers.size());
  }

  @Test
  void initAndGetAssetByWrongAnnotation() {
    assertNotNull(semanticRepository.setVersionedKnowledgeAsset(uuid("1"), "1",
            new KnowledgeAsset().withSubject(
                    new SimpleAnnotation()
                            .withRel(RelatedConceptSeries.Has_Broader.asConcept())
                            .withExpr(new ConceptIdentifier()
                                    .withConceptId(URI.create("http://something"))))));

    List<Pointer> pointers = semanticRepository
            .listKnowledgeAssets(null, RelatedConceptSeries.Has_Narrower.asConcept().getTag(), -1, -1)
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
            .withSubject(new SimpleAnnotation()
                .withRel(Defines.asConcept())
                .withExpr(dizziness))));

    List<Pointer> pointers = semanticRepository
        .listKnowledgeAssets(Operational_Concept_Definition.getTag(), null, -1, -1)
        .orElse(emptyList());
    assertEquals(1, pointers.size());
  }

  @Test
  void initAndGetAllDefinitionsWithMultiple() {
    assertNotNull(semanticRepository.setVersionedKnowledgeAsset(uuid("1"), "1",
        new KnowledgeAsset().withRole(Operational_Concept_Definition)
            .withSubject(new SimpleAnnotation()
                .withRel(Defines.asConcept())
                .withExpr(dizziness))));

    assertNotNull(semanticRepository.setVersionedKnowledgeAsset(uuid("2"), "1",
        new KnowledgeAsset().withRole(Operational_Concept_Definition)
            .withSubject(new SimpleAnnotation()
                .withRel(Defines.asConcept())
                .withExpr(sleep_apnea))));

    List<Pointer> pointers = semanticRepository
        .listKnowledgeAssets(Operational_Concept_Definition.getTag(), null, -1, -1)
        .orElse(emptyList());
    assertEquals(2, pointers.size());
  }

  @Test
  void initAndGetAllDefinitionsWithMultipleVersions() {
    assertNotNull(semanticRepository.setVersionedKnowledgeAsset(uuid("1"), "1",
        new KnowledgeAsset().withRole(Operational_Concept_Definition)
            .withSubject(new SimpleAnnotation()
                .withRel(Defines.asConcept())
                .withExpr(dizziness))));

    assertNotNull(semanticRepository.setVersionedKnowledgeAsset(uuid("1"), "2",
        new KnowledgeAsset().withRole(Operational_Concept_Definition)
            .withSubject(new SimpleAnnotation()
                .withRel(Defines.asConcept())
                .withExpr(dizziness))));

    List<Pointer> pointers = semanticRepository
        .listKnowledgeAssets(Operational_Concept_Definition.getTag(), null, -1, -1)
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
                        DatatypeHelper.uri(Registry.BASE_UUID_URN, uuid("q").toString(), "z"))
                    .withRepresentation(new Representation()
                        .withLanguage(HTML))
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
                .withArtifactId(DatatypeHelper.toURIIdentifier(SurrogateBuilder.randomArtifactId()))
                .withRepresentation(new Representation().withLanguage(
                            HL7_ELM))));
    semanticRepository.addKnowledgeAssetCarrier(u1, "1", "HI1!".getBytes());

    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset asset1 = semanticRepository.getVersionedKnowledgeAsset(u1, "1").getOptionalValue().get();

    semanticRepository.setVersionedKnowledgeAsset(u2, "1", new KnowledgeAsset()
            .withRelated(new Dependency().withRel(Depends_On).withTgt(asset1))
            .withCarriers(new ComputableKnowledgeArtifact()
                .withArtifactId(DatatypeHelper.toURIIdentifier(SurrogateBuilder.randomArtifactId()))
                .withRepresentation(new Representation().withLanguage(
                            HL7_ELM))));
    semanticRepository.addKnowledgeAssetCarrier(u2, "1", "HI2!".getBytes());

    List<KnowledgeCarrier> carriers = semanticRepository.getKnowledgeArtifactBundle(u2, "1",
            Depends_On.getTag(), -1, null).getOptionalValue()
            .orElseGet(Collections::emptyList);

    assertEquals(2, carriers.size());
  }


}
