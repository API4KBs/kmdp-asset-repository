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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.newSurrogate;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Care_Process_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Plans_Processes_Pathways_And_Protocol_Definitions;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Rules_Policies_And_Guidelines;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Decision_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Formal_Ontology;

import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties.KnowledgeArtifactRepositoryOptions;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper.VersionIncrement;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {"allowClearAll=true"})
class AssetRemovalTest extends RepositoryTestBase {

  boolean verbose = false;

  @BeforeEach
  void initialize() {
    prepopulate();
  }

  @AfterEach
  void cleanse() {
    semanticRepository.clearKnowledgeAssetCatalog();
  }


  @Test
  void testClearAll() {
    assertTrue(
        semanticRepository.clearKnowledgeAssetCatalog()
            .isSuccess());

    assertTrue(semanticRepository.listKnowledgeAssets().map(List::isEmpty)
        .orElseGet(Assertions::fail));
    checkEmpty();
  }

  @Test
  void testDeleteOneByOne() {
    assertTrue(semanticRepository.deleteKnowledgeAssets().isSuccess());

    assertTrue(semanticRepository.listKnowledgeAssets().map(List::isEmpty)
        .orElseGet(Assertions::fail));
    checkEmpty();
  }


  @Test
  void testDeleteByAssetType() {
    assertTrue(semanticRepository.deleteKnowledgeAssets(Care_Process_Model.getTag(), null, null)
        .isSuccess());

    assertEquals(1,
        semanticRepository.listKnowledgeAssets().map(List::size)
            .orElseGet(Assertions::fail));
  }

  @Test
  void testDeleteByTypeWithNoContent() {
    assertTrue(semanticRepository.deleteKnowledgeAssets(Formal_Ontology.getTag(), null, null)
        .isSuccess());

    assertEquals(3,
        semanticRepository.listKnowledgeAssets().map(List::size)
            .orElseGet(Assertions::fail));
  }

  @Test
  void testDeleteByAssetType2() {
    assertTrue(semanticRepository.deleteKnowledgeAssets(Decision_Model.getTag(), null, null)
        .isSuccess());

    assertEquals(2,
        semanticRepository.listKnowledgeAssets().map(List::size)
            .orElseGet(Assertions::fail));
    assertEquals(2,
        semanticRepository.listKnowledgeAssetVersions(uuid("foo")).map(List::size)
            .orElseGet(Assertions::fail));
  }

  @Test
  void testDeleteAssetByVersion() {
    assertTrue(semanticRepository.deleteKnowledgeAssetVersion(uuid("foo"), "1.0.0")
        .isSuccess());

    assertEquals(3,
        semanticRepository.listKnowledgeAssets().map(List::size)
            .orElseGet(Assertions::fail));
    assertEquals(1,
        semanticRepository.listKnowledgeAssetVersions(uuid("foo")).map(List::size)
            .orElseGet(Assertions::fail));

    assertEquals("2.0.0",
        semanticRepository.getKnowledgeAsset(uuid("foo"))
            .map(KnowledgeAsset::getAssetId)
            .map(ResourceIdentifier::getVersionTag)
        .orElseGet(Assertions::fail));
  }

  @Test
  void testDeleteAssetByLatestVersion() {
    assertTrue(semanticRepository.deleteKnowledgeAssetVersion(uuid("foo"), "2.0.0")
        .isSuccess());

    assertEquals("1.0.0",
        semanticRepository.getKnowledgeAsset(uuid("foo"))
            .map(KnowledgeAsset::getAssetId)
            .map(ResourceIdentifier::getVersionTag)
        .orElseGet(Assertions::fail));
  }


  @Test
  void testDeleteByID() {
    assertTrue(semanticRepository.deleteKnowledgeAsset(uuid("foo"))
        .isSuccess());

    Answer<List<Pointer>> versions =
        semanticRepository.listKnowledgeAssetVersions(uuid("foo"));
    assertTrue(versions.isNotFound());
  }

  @Test
  void testDeleteByID2() {
    assertTrue(semanticRepository.deleteKnowledgeAsset(uuid("foo2"))
        .isSuccess());

    assertEquals(2,
        semanticRepository.listKnowledgeAssetVersions(uuid("foo")).map(List::size)
            .orElseGet(Assertions::fail));
  }

  @Test
  void testAssetRemovalWithIncrementalSurrogate() {
    UUID assetId = uuid("foo3");
    String assetV = "1.0.0";

    KnowledgeAsset surr;
    surr = semanticRepository.getKnowledgeAsset(assetId, assetV)
        .orElseGet(Assertions::fail);
    surr.setDescription("Add something");
    SurrogateHelper.incrementVersion(surr, VersionIncrement.MINOR);
    semanticRepository.setKnowledgeAssetVersion(assetId, assetV, surr);

    surr = semanticRepository.getKnowledgeAsset(assetId, assetV)
        .orElseGet(Assertions::fail);
    surr.setDescription("Add something else");
    SurrogateHelper.incrementVersion(surr, VersionIncrement.MINOR);
    semanticRepository.setKnowledgeAssetVersion(assetId, assetV, surr);

    surr = semanticRepository.getKnowledgeAsset(assetId, assetV)
        .orElseGet(Assertions::fail);
    assertEquals("0.2.0",
        surr.getSurrogate().get(0).getArtifactId().getVersionTag());
    assertTrue(surr.getDescription().contains("else"));

    List<Pointer> metas = semanticRepository.listKnowledgeAssetSurrogates(assetId, assetV)
        .orElseGet(Assertions::fail);
    assertEquals(1, metas.size());

    List<Pointer> history = semanticRepository
        .listKnowledgeAssetSurrogateVersions(assetId, assetV, metas.get(0).getUuid())
        .orElseGet(Assertions::fail);
    assertEquals(3, history.size());
    assertEquals(Arrays.asList("0.2.0", "0.1.0", "0.0.0"),
        history.stream().map(ResourceIdentifier::getVersionTag).collect(Collectors.toList()));

    assertTrue(
        semanticRepository.deleteKnowledgeAsset(assetId).isSuccess());
    Answer<List<Pointer>> history2 = semanticRepository
        .listKnowledgeAssetSurrogateVersions(assetId, assetV, metas.get(0).getUuid());
    assertTrue(history2.isNotFound());

    semanticRepository.deleteKnowledgeAsset(uuid("foo"));
    semanticRepository.deleteKnowledgeAsset(uuid("foo2"));

    checkEmpty();
  }

  private void prepopulate() {
    ResourceIdentifier id11 = SemanticIdentifier.newId(uuid("foo"), "1.0.0");
    KnowledgeAsset surr11 = newSurrogate(id11)
        .withName("Example A", "Descr A")
        .withFormalType(Plans_Processes_Pathways_And_Protocol_Definitions, Care_Process_Model)
        .get();
    assertTrue(semanticRepository
        .setKnowledgeAssetVersion(id11.getUuid(), id11.getVersionTag(), surr11).isSuccess());
    printout(surr11);

    ResourceIdentifier id12 = SemanticIdentifier.newId(uuid("foo"), "2.0.0");
    KnowledgeAsset surr12 = newSurrogate(id12)
        .withName("Example Ax", "Revised A")
        .withFormalType(Plans_Processes_Pathways_And_Protocol_Definitions, Care_Process_Model)
        .get();
    assertTrue(semanticRepository
        .setKnowledgeAssetVersion(id12.getUuid(), id12.getVersionTag(), surr12).isSuccess());
    printout(surr12);

    ResourceIdentifier id2 = SemanticIdentifier.newId(uuid("foo2"), "1.0.0");
    KnowledgeAsset surr2 = newSurrogate(id2)
        .withName("Example B", "Descr B")
        .withFormalType(Plans_Processes_Pathways_And_Protocol_Definitions, Care_Process_Model)
        .get()
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(SemanticIdentifier.randomId()));
    assertTrue(semanticRepository
        .setKnowledgeAssetVersion(id2.getUuid(), id2.getVersionTag(), surr2).isSuccess());
    printout(surr2);

    ResourceIdentifier id3 = SemanticIdentifier.newId(uuid("foo3"), "1.0.0");
    KnowledgeAsset surr3 = newSurrogate(id3)
        .withName("Example C", "Descr C")
        .withFormalType(Rules_Policies_And_Guidelines, Decision_Model)
        .get();
    assertTrue(semanticRepository
        .setKnowledgeAssetVersion(id3.getUuid(), id3.getVersionTag(), surr3).isSuccess());
    printout(surr3);
  }

  private void printout(KnowledgeAsset ax) {
    if (!verbose) {
      return;
    }
    System.out.println("Asset " + ax.getName());
    System.out.println(" " + ax.getAssetId().getUuid() + " : " + ax.getAssetId().getVersionTag());
    System.out.println("  Version " + ax.getAssetId().getVersionTag());
    System.out.println("  - Carriers: ");
    ax.getCarriers().forEach(carr -> {
      System.out.println(
          "\t " + carr.getArtifactId().getUuid() + " : " + carr.getArtifactId().getVersionTag());
    });
    System.out.println("  - Surrogates: ");
    ax.getSurrogate().forEach(surr -> {
      System.out.println(
          "\t " + surr.getArtifactId().getUuid() + " : " + surr.getArtifactId().getVersionTag());
    });
  }


  private void checkEmpty() {
    String repoId = artifactCfg.getTyped(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID);

    List<Statement> triples = jenaSparqlDao.readAll()
        .stream()
        .filter(this::isInstanceTriple)
        .collect(Collectors.toList());

    assertTrue(triples.isEmpty());

    List<Pointer> assetPtr = semanticRepository.listKnowledgeAssets()
        .orElseGet(Assertions::fail);
    assertTrue(assetPtr.isEmpty());

    List<Pointer> ptrs = artifactRepository.listKnowledgeArtifacts(repoId)
        .orElseGet(Assertions::fail);
    assertEquals(2, ptrs.size());
    assertTrue(ptrs.stream()
        .anyMatch(ptr -> ptr.getUuid().equals(kgHolder.getInfo().knowledgeGraphArtifactId().getUuid())));
    assertTrue(ptrs.stream()
        .anyMatch(ptr -> ptr.getUuid().equals(kgHolder.getInfo().knowledgeGraphSurrogateId().getUuid())));
  }

  private boolean isInstanceTriple(Statement s) {
    if (s.getSubject().getURI().startsWith("https://www.omg.org/spec")) {
      return false;
    }
    if (s.getSubject().getURI().startsWith("http://ontology.mayo.edu/")) {
      return false;
    }
    return true;
  }

}
