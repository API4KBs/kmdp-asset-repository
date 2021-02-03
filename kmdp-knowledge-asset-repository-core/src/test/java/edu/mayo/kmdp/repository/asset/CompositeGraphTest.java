package edu.mayo.kmdp.repository.asset;

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomAssetId;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Depends_On;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Imports;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRoleSeries.Composite_Knowledge_Asset;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.structuralreltype.StructuralPartTypeSeries.Has_Structural_Component;
import static org.omg.spec.api4kp._20200801.taxonomy.structuralreltype.StructuralPartTypeSeries.Has_Structuring_Component;

import edu.mayo.kmdp.language.parsers.rdf.JenaRdfParser;
import edu.mayo.kmdp.util.JenaUtil;
import java.util.List;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.CompositeStructType;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.Component;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.Link;
import org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyType;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.structuralreltype.StructuralPartType;

class CompositeGraphTest extends RepositoryTestBase {


  @Test
  void testStructFromComplexGraph() {

    KnowledgeAsset[] assets = initAssetNodes(10);

    relate(assets, 5, 3, Depends_On);
    relate(assets, 1, 3, Imports);
    relate(assets, 2, 4, Depends_On);
    relate(assets, 1, 2, Has_Structural_Component);
    relate(assets, 0, 1, Has_Structural_Component);
    relate(assets, 0, 3, Has_Structural_Component);
    makeComposite(assets[0]);

    uploadAll(assets);

    Answer<CompositeKnowledgeCarrier> kc =
        semanticRepository.getCompositeKnowledgeAssetSurrogate(
            assets[0].getAssetId().getUuid(), assets[0].getAssetId().getVersionTag());
    assertTrue(kc.isSuccess());

    Model m = new JenaRdfParser()
        .applyLift(kc.get().getStruct(), Abstract_Knowledge_Expression, codedRep(OWL_2), null)
        .flatOpt(x -> x.as(Model.class))
        .orElseGet(Assertions::fail);

    assertTrue(contains(m, 0, Has_Structural_Component, 1, assets));
    assertTrue(contains(m, 0, Has_Structural_Component, 3, assets));
    assertTrue(contains(m, 1, Has_Structural_Component, 2, assets));
    assertTrue(contains(m, 0, Has_Structural_Component, 2, assets));
    assertTrue(contains(m, 2, Depends_On, 4, assets));

  }


  @Test
  void testAnonymousToNamedStructFromComplexGraph() {
    ResourceIdentifier compositeAssetId = randomAssetId(testAssetNS());

    KnowledgeAsset[] assets = initAssetNodes(10);

    relate(assets, 0, 1, Imports);
    relate(assets, 0, 2, Imports);
    relate(assets, 2, 3, Depends_On);
    relate(assets, 1, 4, Depends_On);
    relate(assets, 4, 5, Depends_On);
    relate(assets, 1, 6, Imports);
    relate(assets, 6, 7, Depends_On);
    relate(assets, 7, 8, Depends_On);
    relate(assets, 8, 9, Imports);

    uploadAll(assets);

    Model m = seedCompositeAsset(compositeAssetId, assets[0].getAssetId(), assets);

    assertTrue(contains(m, compositeAssetId, Has_Structural_Component, 0, assets));
    assertTrue(contains(m, compositeAssetId, Has_Structural_Component, 1, assets));
    assertTrue(contains(m, compositeAssetId, Has_Structural_Component, 2, assets));
    assertTrue(contains(m, compositeAssetId, Has_Structural_Component, 6, assets));
    List<Statement> hasPart = m.listStatements(
        createResource(compositeAssetId.getVersionId().toString()),
        createProperty(Has_Structural_Component.getReferentId().toString()),
        (RDFNode) null).toList();
    assertEquals(4, hasPart.size());

    String z = log(JenaUtil.asString(m), assets);

    assertTrue(contains(m, 0, Depends_On, 1, assets));
    assertTrue(contains(m, 0, Depends_On, 2, assets));
    assertTrue(contains(m, 1, Depends_On, 6, assets));

    assertTrue(contains(m, 2, Depends_On, 3, assets));
    assertTrue(contains(m, 1, Depends_On, 4, assets));
    assertTrue(contains(m, 6, Depends_On, 7, assets));

    assertFalse(contains(m, 4, Depends_On, 5, assets));
    assertFalse(contains(m, 7, Depends_On, 8, assets));

    assertTrue(contains(m, 0, Imports, 1, assets));
    assertTrue(contains(m, 0, Imports, 2, assets));
    assertTrue(contains(m, 1, Imports, 6, assets));
    assertTrue(contains(m, 0, Imports, 6, assets));

    assertFalse(contains(m, 2, Imports, 3, assets));
    assertFalse(contains(m, 1, Imports, 4, assets));
    assertFalse(contains(m, 8, Imports, 9, assets));
  }

  @Test
  void testAnonymousStructWithNonExistingAsset() {
    ResourceIdentifier rootId = randomAssetId(testAssetNS());
    Answer<KnowledgeCarrier> struct =
        semanticRepository.getAnonymousCompositeKnowledgeAssetStructure(
            rootId.getUuid(), rootId.getVersionTag());
    assertTrue(struct.isNotFound());
  }

  @Test
  void testAnonymousCarrierWithNonExistingAsset() {
    ResourceIdentifier rootId = randomAssetId(testAssetNS());
    Answer<CompositeKnowledgeCarrier> carrier =
        semanticRepository.getAnonymousCompositeKnowledgeAssetCarrier(
            rootId.getUuid(), rootId.getVersionTag());
    assertTrue(carrier.isNotFound());
  }

  @Test
  void testAnonymousSurrogateWithNonExistingAsset() {
    ResourceIdentifier rootId = randomAssetId(testAssetNS());
    Answer<CompositeKnowledgeCarrier> surrogate =
        semanticRepository.getAnonymousCompositeKnowledgeAssetSurrogate(
            rootId.getUuid(), rootId.getVersionTag());
    assertTrue(surrogate.isNotFound());
  }

  private Model seedCompositeAsset(
      ResourceIdentifier compositeAssetId,
      ResourceIdentifier rootId,
      KnowledgeAsset[] assets) {
    Answer<KnowledgeCarrier> struct =
        semanticRepository.getAnonymousCompositeKnowledgeAssetStructure(
            rootId.getUuid(), rootId.getVersionTag());
    assertTrue(struct.isSuccess());

    log(struct.flatOpt(AbstractCarrier::asString).get(), assets);

    CompositeKnowledgeCarrier ckc = new CompositeKnowledgeCarrier()
        .withAssetId(compositeAssetId)
        .withRootId(rootId)
        .withStructType(CompositeStructType.GRAPH)
        .withStruct(struct.get());

    Answer<Void> upload = semanticRepository.addCanonicalKnowledgeAssetSurrogate(
        compositeAssetId.getUuid(), compositeAssetId.getVersionTag(), ckc);
    assertTrue(upload.isSuccess());

    Answer<KnowledgeCarrier> compositeStruct =
        semanticRepository.getCompositeKnowledgeAssetStructure(
            compositeAssetId.getUuid(), compositeAssetId.getVersionTag());
    assertTrue(compositeStruct.isSuccess());

    return new JenaRdfParser()
        .applyLift(compositeStruct.get(), Abstract_Knowledge_Expression, codedRep(OWL_2), null)
        .flatOpt(x -> x.as(Model.class))
        .orElseGet(Assertions::fail);
  }

  private String log(String full, KnowledgeAsset[] assets) {
    String log = full;
    for (int j = 0; j < assets.length; j++) {
      log = log.replaceAll(
          assets[j].getAssetId().getVersionId().toString(), "A_" + j);
    }
    log = log.replaceAll("https://www.omg.org/spec/API4KP/api4kp/","");
    log = log.replaceAll("https://www.omg.org/spec/API4KP/api4kp-kao/","");
    return log;
  }


  private void makeComposite(KnowledgeAsset asset) {
    asset.withRole(Composite_Knowledge_Asset)
        .withLinks(new Component()
            .withRel(Has_Structuring_Component)
            .withHref(randomAssetId()));
  }

  private void uploadAll(KnowledgeAsset[] assets) {
    for (KnowledgeAsset asset : assets) {
      Answer<Void> ans = semanticRepository.setKnowledgeAssetVersion(
          asset.getAssetId().getUuid(),
          asset.getAssetId().getVersionTag(),
          asset);
      assertTrue(ans.isSuccess());
    }
  }

  private void relate(KnowledgeAsset[] assets, int i, int j, Term rel) {
    ResourceIdentifier o = assets[j].getAssetId();
    Link link = null;
    if (rel instanceof DependencyType) {
      link = new Dependency().withRel((DependencyType) rel)
          .withHref(o);
    }
    if (rel instanceof StructuralPartType) {
      link = new Component().withRel((StructuralPartType) rel)
          .withHref(o);
    }
    if (link == null) {
      fail();
    }
    assets[i].withLinks(link);
  }

  private KnowledgeAsset[] initAssetNodes(int number) {
    KnowledgeAsset[] assets = new KnowledgeAsset[number];
    int K = KnowledgeAssetTypeSeries.values().length;
    for (int j = 0; j < number; j++) {
      assets[j] = new KnowledgeAsset()
          .withAssetId(randomAssetId(testAssetNS()))
          .withFormalType(KnowledgeAssetTypeSeries.values()[j % K]);
    }
    return assets;
  }

  private boolean contains(Model m, int s, Term pred, int o, KnowledgeAsset[] assets) {
    return contains(m, assets[s].getAssetId(), pred, o, assets);
  }

  private boolean contains(Model m, ResourceIdentifier subj, Term pred, int o, KnowledgeAsset[] assets) {
    return m.contains(
        JenaUtil.objA(
            subj.getVersionId(),
            pred.getReferentId(),
            assets[o].getAssetId().getVersionId()));
  }
}
