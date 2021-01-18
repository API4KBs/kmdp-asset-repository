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

import static edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryService.transitiveDependencies;
import static edu.mayo.kmdp.util.JenaUtil.objA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomArtifactId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomAssetId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper.getSurrogateId;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Depends_On;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Imports;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRoleSeries.Composite_Knowledge_Asset;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Clinical_Rule;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Documentation_Template;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Equation;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Formal_Grammar;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Information_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Value_Set;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HL7_ELM;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.SPARQL_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.structuralreltype.StructuralPartTypeSeries.Has_Structural_Component;
import static org.omg.spec.api4kp._20200801.taxonomy.structuralreltype.StructuralPartTypeSeries.Has_Structuring_Component;

import edu.mayo.kmdp.kbase.query.sparql.v1_1.JenaQuery;
import edu.mayo.kmdp.language.parsers.rdf.JenaRdfParser;
import edu.mayo.kmdp.util.JenaUtil;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.Component;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;


class CompositeAssetTest extends RepositoryTestBase {

  @Test
  void testPublishSetOrientedCompositeMultiple() {
    ResourceIdentifier id1 = randomAssetId();
    ResourceIdentifier id2 = randomAssetId();

    KnowledgeAsset a1 = new KnowledgeAsset()
        .withAssetId(id1)
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(randomArtifactId())
            .withRepresentation(rep(HTML)));
    KnowledgeAsset a2 = new KnowledgeAsset()
        .withAssetId(id2)
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(randomArtifactId())
            .withRepresentation(rep(HL7_ELM)));

    KnowledgeCarrier ckc = AbstractCarrier.ofIdentifiableSet(
        rep(Knowledge_Asset_Surrogate_2_0),
        KnowledgeAsset::getAssetId,
        ka -> getSurrogateId(
            ka,Knowledge_Asset_Surrogate_2_0,null)
            .orElse(randomArtifactId()),
        KnowledgeAsset::getName,
        Arrays.asList(a1, a2));

    Answer<Void> result = semanticRepository.addCanonicalKnowledgeAssetSurrogate(
        ckc.getAssetId().getUuid(), ckc.getAssetId().getVersionTag(), ckc);
    assertTrue(result.isSuccess());

    assertEquals(3,
        semanticRepository.listKnowledgeAssets().orElse(Collections.emptyList()).size());

    assertTrue(semanticRepository.getKnowledgeAsset(id1.getUuid()).isSuccess());
    assertTrue(semanticRepository.getKnowledgeAsset(id2.getUuid()).isSuccess());

    String query = "" +
        "select ?o where { ?s <" + Has_Structural_Component.getReferentId() + "> ?o . }";

    KnowledgeCarrier queryCarrier = AbstractCarrier.of(query)
        .withRepresentation(rep(SPARQL_1_1, TXT, Charset.defaultCharset()));

    List<Bindings> binds = semanticRepository.queryKnowledgeAssetGraph(queryCarrier)
        .orElse(Collections.emptyList());
    assertEquals(2, binds.size());

    assertEquals(
        new HashSet<>(Arrays.asList(id1.getVersionId(),id2.getVersionId())),
        binds.stream().map(b -> b.get("o")).map(x -> URI.create(x.toString())).collect(
            Collectors.toSet()));
  }

  @Test
  void testPublishAnonymousComposite() {
    ResourceIdentifier id1 = randomAssetId();
    ResourceIdentifier id2 = randomAssetId();

    KnowledgeAsset a1 = new KnowledgeAsset()
        .withAssetId(id1)
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(randomArtifactId())
            .withRepresentation(rep(HTML)));
    KnowledgeAsset a2 = new KnowledgeAsset()
        .withAssetId(id2)
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(randomArtifactId())
            .withRepresentation(rep(HL7_ELM)));

    KnowledgeCarrier ckc = AbstractCarrier.ofAnonymousComposite(
        rep(Knowledge_Asset_Surrogate_2_0),
        KnowledgeAsset::getAssetId,
        ka -> getSurrogateId(ka,Knowledge_Asset_Surrogate_2_0,null)
            .orElse(randomArtifactId()),
        Arrays.asList(a1, a2));

    Answer<Void> result = semanticRepository.addCanonicalKnowledgeAssetSurrogate(
        ckc.getAssetId().getUuid(), ckc.getAssetId().getVersionTag(),ckc);
    assertTrue(result.isSuccess());

    assertEquals(2,
        semanticRepository.listKnowledgeAssets().orElse(Collections.emptyList()).size());

    assertTrue(semanticRepository.getKnowledgeAsset(id1.getUuid()).isSuccess());
    assertTrue(semanticRepository.getKnowledgeAsset(id2.getUuid()).isSuccess());
    assertTrue(semanticRepository.getKnowledgeAsset(ckc.getAssetId().getUuid()).isFailure());

    String query = "" +
        "select ?o where { ?s <" + Has_Structural_Component.getReferentId() + "> ?o . }";
    KnowledgeCarrier queryCarrier = AbstractCarrier.of(query)
        .withRepresentation(rep(SPARQL_1_1, TXT, Charset.defaultCharset()));

    List<Bindings> binds = semanticRepository.queryKnowledgeAssetGraph(queryCarrier)
        .orElse(Collections.emptyList());
    assertEquals(0, binds.size());
  }



  @Test
  void testGetAnonymousCompositeAsset() {
    ResourceIdentifier id1 = randomAssetId();
    ResourceIdentifier id2 = randomAssetId();

    KnowledgeAsset a1 = new KnowledgeAsset()
        .withAssetId(id1)
        .withLinks(new Dependency().withRel(Depends_On).withHref(id2));
    KnowledgeAsset a2 = new KnowledgeAsset()
        .withAssetId(id2);

    Answer<Void> ax1 = semanticRepository.setKnowledgeAssetVersion(
        id1.getUuid(), id1.getVersionTag(), a1);
    assertTrue(ax1.isSuccess());
    Answer<Void> ax2 = semanticRepository.setKnowledgeAssetVersion(
        id2.getUuid(), id2.getVersionTag(), a2);
    assertTrue(ax2.isSuccess());


    Answer<CompositeKnowledgeCarrier> kc = semanticRepository.getAnonymousCompositeKnowledgeAssetSurrogate(
        id1.getUuid(),id1.getVersionTag(),transitiveDependencies(id1));
    assertTrue(kc.isSuccess());

    CompositeKnowledgeCarrier ckc = kc.get();
    assertEquals(2, ckc.getComponent().size());

  }


  @Test
  void testGetAnonymousCompositeArtifact() {
    ResourceIdentifier id1 = randomAssetId();
    ResourceIdentifier id2 = randomAssetId();

    KnowledgeAsset a1 = new KnowledgeAsset()
        .withAssetId(id1)
        .withLinks(new Dependency().withRel(Depends_On).withHref(id2))
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(randomArtifactId())
            .withRepresentation(rep(HTML))
            .withInlinedExpression("AAA"));
    KnowledgeAsset a2 = new KnowledgeAsset()
        .withAssetId(id2)
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(randomArtifactId())
            .withRepresentation(rep(HTML))
            .withInlinedExpression("BBB"));

    Answer<Void> ax1 = semanticRepository.setKnowledgeAssetVersion(
        id1.getUuid(), id1.getVersionTag(), a1);
    assertTrue(ax1.isSuccess());
    Answer<Void> ax2 = semanticRepository.setKnowledgeAssetVersion(
        id2.getUuid(), id2.getVersionTag(), a2);
    assertTrue(ax2.isSuccess());


    Answer<CompositeKnowledgeCarrier> kc = semanticRepository.getAnonymousCompositeKnowledgeAssetCarrier(
        id1.getUuid(),id1.getVersionTag(),transitiveDependencies(id1));
    assertTrue(kc.isSuccess());

    CompositeKnowledgeCarrier ckc = kc.get();
    assertEquals(2, ckc.getComponent().size());

  }


  @Test
  void testGetNamedCompositeAssetWithInferredStructure() {
    // If a 'structuring component' is not explicitly indicated,
    // a shallow Composite Structure is inferred in a best effort manner
    // This behavior may be deprecated at some point, and is currently enabled
    // as a cheaper, more efficient option for simpler use cases
    ResourceIdentifier id1 = randomAssetId();
    ResourceIdentifier id2 = randomAssetId();
    ResourceIdentifier id3 = randomAssetId();
    ResourceIdentifier id4 = randomAssetId();

    KnowledgeAsset a3 = new KnowledgeAsset().withAssetId(id3);

    KnowledgeAsset a2 = new KnowledgeAsset()
        .withAssetId(id2)
        .withFormalType(Information_Model)
        .withLinks(new Component().withRel(Has_Structural_Component).withHref(id3));

    KnowledgeAsset a1 = new KnowledgeAsset()
        .withAssetId(id1)
        .withFormalType(Clinical_Rule)
        .withRole(Composite_Knowledge_Asset)
        .withLinks(new Component().withRel(Has_Structural_Component).withHref(id2))
        .withLinks(new Component().withRel(Has_Structural_Component).withHref(id4));

    Answer<Void> ax1 = semanticRepository.setKnowledgeAssetVersion(
        id1.getUuid(), id1.getVersionTag(), a1);
    assertTrue(ax1.isSuccess());
    Answer<Void> ax2 = semanticRepository.setKnowledgeAssetVersion(
        id2.getUuid(), id2.getVersionTag(), a2);
    assertTrue(ax2.isSuccess());
    Answer<Void> ax3 = semanticRepository.setKnowledgeAssetVersion(
        id3.getUuid(), id3.getVersionTag(), a3);
    assertTrue(ax3.isSuccess());

    Answer<CompositeKnowledgeCarrier> kc =
        semanticRepository.getCompositeKnowledgeAssetSurrogate(id1.getUuid(), id1.getVersionTag());
    assertTrue(kc.isSuccess());

    CompositeKnowledgeCarrier ckc = kc.get();
    assertEquals(3, ckc.getComponent().size());
    assertEquals(id1, ckc.getAssetId());
    assertEquals(id1, ckc.getRootId());

    Model m = new JenaRdfParser()
        .applyLift(ckc.getStruct(), Abstract_Knowledge_Expression, codedRep(OWL_2), null)
        .flatOpt(x -> x.as(Model.class))
        .orElseGet(Assertions::fail);

    assertTrue(contains(m, id1, Has_Structural_Component, id2));
    assertTrue(contains(m, id1, Has_Structural_Component, id4));
    assertFalse(contains(m, id2, Has_Structural_Component, id3));
  }

  @Test
  void testGetNamedCompositeAssetStructure() {
    ResourceIdentifier structId = randomAssetId();

    ResourceIdentifier id1 = randomAssetId();
    ResourceIdentifier id2 = randomAssetId();
    ResourceIdentifier id3 = randomAssetId();
    ResourceIdentifier id4 = randomAssetId();
    ResourceIdentifier id5 = randomAssetId();
    ResourceIdentifier id6 = randomAssetId();

    KnowledgeAsset a6 = new KnowledgeAsset()
        .withAssetId(id6)
        .withFormalType(Documentation_Template)
        .withLinks(new Dependency().withRel(Depends_On).withHref(id4));
    KnowledgeAsset a5 = new KnowledgeAsset()
        .withAssetId(id5)
        .withFormalType(Formal_Grammar);
    KnowledgeAsset a4 = new KnowledgeAsset()
        .withAssetId(id4)
        .withFormalType(Value_Set);
    KnowledgeAsset a3 = new KnowledgeAsset()
        .withAssetId(id3)
        .withFormalType(Equation);

    KnowledgeAsset a2 = new KnowledgeAsset()
        .withAssetId(id2)
        .withFormalType(Information_Model)
        .withLinks(new Dependency().withRel(Imports).withHref(id4))
        .withLinks(new Dependency().withRel(Depends_On).withHref(id5))
        .withLinks(new Component().withRel(Has_Structural_Component).withHref(id3));

    KnowledgeAsset a1 = new KnowledgeAsset()
        .withAssetId(id1)
        .withFormalType(Clinical_Rule)
        .withLinks(new Component().withRel(Has_Structural_Component).withHref(id2))
        .withLinks(new Component().withRel(Has_Structural_Component).withHref(id4))
        .withLinks(new Component().withRel(Has_Structuring_Component).withHref(structId));

    Answer<Void> ax1 = semanticRepository.setKnowledgeAssetVersion(
        id1.getUuid(), id1.getVersionTag(), a1);
    assertTrue(ax1.isSuccess());
    Answer<Void> ax2 = semanticRepository.setKnowledgeAssetVersion(
        id2.getUuid(), id2.getVersionTag(), a2);
    assertTrue(ax2.isSuccess());
    Answer<Void> ax3 = semanticRepository.setKnowledgeAssetVersion(
        id3.getUuid(), id3.getVersionTag(), a3);
    assertTrue(ax3.isSuccess());
    Answer<Void> ax4 = semanticRepository.setKnowledgeAssetVersion(
        id4.getUuid(), id4.getVersionTag(), a4);
    assertTrue(ax4.isSuccess());
    Answer<Void> ax5 = semanticRepository.setKnowledgeAssetVersion(
        id5.getUuid(), id5.getVersionTag(), a5);
    assertTrue(ax5.isSuccess());
    Answer<Void> ax6 = semanticRepository.setKnowledgeAssetVersion(
        id6.getUuid(), id6.getVersionTag(), a6);
    assertTrue(ax6.isSuccess());

    Answer<CompositeKnowledgeCarrier> kc =
        semanticRepository.getCompositeKnowledgeAssetSurrogate(id1.getUuid(),id1.getVersionTag());
    assertTrue(kc.isSuccess());

    CompositeKnowledgeCarrier ckc = kc.get();
    assertEquals(4, ckc.getComponent().size());
    assertEquals(id1, ckc.getAssetId());
    assertEquals(id1, ckc.getRootId());

    Model m = new JenaRdfParser()
        .applyLift(ckc.getStruct(), Abstract_Knowledge_Expression, codedRep(OWL_2), null)
        .flatOpt(x -> x.as(Model.class))
        .orElseGet(Assertions::fail);

    String log = JenaUtil.asString(m)
        .replace(id1.getTag(), "id1")
        .replace(id2.getTag(), "id2")
        .replace(id3.getTag(), "id3")
        .replace(id4.getTag(), "id4")
        .replace(id5.getTag(), "id5")
        .replace(id6.getTag(), "id6")
        .replace(structId.getTag(), "struct")
        .replace("https://clinicalknowledgemanagement.mayo.edu/assets/", "")
        .replace("/versions/0.0.0", "")
        .replace("https://www.omg.org/spec/API4KP/api4kp/", "");

    assertTrue(contains(m, id1, Has_Structuring_Component, structId));
    assertTrue(contains(m, id1, Has_Structural_Component, id2));
    assertTrue(contains(m, id1, Has_Structural_Component, id4));
    assertTrue(contains(m, id2, Has_Structural_Component, id3));
    assertTrue(contains(m, id1, Has_Structural_Component, id3));
    assertTrue(contains(m, id2, Imports, id4));
    assertTrue(contains(m, id2, Depends_On, id5));

    assertFalse(contains(m, id6, Depends_On, id4));
    assertFalse(contains(m, id6, Imports, id4));
  }

  private Model toGraph(List<Bindings> binds) {
    Model model = ModelFactory.createDefaultModel();
    binds.stream()
        .filter(b -> !b.get("p").toString().contains("artifacts"))
        .filter(b -> !b.get("s").toString().contains("artifacts"))
        .filter(b -> !b.get("o").toString().contains("artifacts"))
        .filter(b -> !b.get("o").toString().contains("kmdp"))
        .filter(b -> !b.get("p").toString().contains("api4kp-series"))
        .filter(b -> !b.get("p").toString().contains("dc/elements"))
        .forEach(b -> model.add(
            objA(b.get("s").toString(), b.get("p").toString(), b.get("o").toString())));
    return model;
  }

  private boolean contains(Model m, ResourceIdentifier subj, Term pred, ResourceIdentifier obj) {
    return m.contains(
        JenaUtil.objA(subj.getVersionId(),pred.getReferentId(),obj.getVersionId()));
  }


}
