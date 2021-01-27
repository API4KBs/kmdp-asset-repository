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

import static edu.mayo.kmdp.util.JenaUtil.objA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomArtifactId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomAssetId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper.getSurrogateId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper.toAggregateAsset;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper.toAnonymousCompositeAsset;
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
import org.omg.spec.api4kp._20200801.Composite;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.Component;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper;


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

    KnowledgeCarrier ckc = SurrogateHelper.toNamedCompositeAsset(
        randomAssetId(),
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
        new HashSet<>(Arrays.asList(id1.getVersionId(), id2.getVersionId())),
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

    KnowledgeCarrier ckc = toAggregateAsset(Arrays.asList(a1, a2));

    Answer<Void> result = semanticRepository.addCanonicalKnowledgeAssetSurrogate(
        ckc.getAssetId().getUuid(), ckc.getAssetId().getVersionTag(), ckc);
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

    Answer<CompositeKnowledgeCarrier> kc = semanticRepository
        .getAnonymousCompositeKnowledgeAssetSurrogate(
            id1.getUuid(), id1.getVersionTag());
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

    Answer<CompositeKnowledgeCarrier> kc = semanticRepository
        .getAnonymousCompositeKnowledgeAssetCarrier(
            id1.getUuid(), id1.getVersionTag());
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
        semanticRepository.getCompositeKnowledgeAssetSurrogate(id1.getUuid(), id1.getVersionTag());
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
    //assertTrue(contains(m, id2, Imports, id4));
    assertTrue(contains(m, id2, Depends_On, id5));

    assertFalse(contains(m, id6, Depends_On, id4));
    assertFalse(contains(m, id6, Imports, id4));
  }

  @Test
  void testGetNamedCompositeCarriers() {
    ResourceIdentifier axId0 = randomAssetId();
    ResourceIdentifier sId = randomAssetId();
    ResourceIdentifier axId1 = randomAssetId();
    ResourceIdentifier axId2 = randomAssetId();
    ResourceIdentifier artId1 = randomArtifactId();
    ResourceIdentifier artId2 = randomArtifactId();

    KnowledgeAsset ka0 = new KnowledgeAsset()
        .withAssetId(axId0)
        .withName("Comp")
        .withRole(Composite_Knowledge_Asset)
        .withLinks(new Component().withRel(Has_Structural_Component).withHref(axId1))
        .withLinks(new Component().withRel(Has_Structural_Component).withHref(axId2))
        .withLinks(new Component().withRel(Has_Structuring_Component).withHref(sId));

    KnowledgeAsset ka1 = new KnowledgeAsset()
        .withAssetId(axId1)
        .withName("Foo")
        .withLinks(new Dependency().withRel(Depends_On).withHref(axId2));
    KnowledgeAsset ka2 = new KnowledgeAsset()
        .withAssetId(axId2)
        .withName("Bar");

    semanticRepository.setKnowledgeAssetVersion(
        axId0.getUuid(), axId0.getVersionTag(), ka0);

    semanticRepository.setKnowledgeAssetVersion(
        axId1.getUuid(), axId1.getVersionTag(), ka1);
    semanticRepository.setKnowledgeAssetCarrierVersion(
        axId1.getUuid(), axId1.getVersionTag(), artId1.getUuid(), artId1.getVersionTag(),
        ka1.getName().getBytes());

    semanticRepository.setKnowledgeAssetVersion(
        axId2.getUuid(), axId2.getVersionTag(), ka2);
    semanticRepository.setKnowledgeAssetCarrierVersion(
        axId2.getUuid(), axId2.getVersionTag(), artId2.getUuid(), artId2.getVersionTag(),
        ka2.getName().getBytes());

    Answer<CompositeKnowledgeCarrier> carrierAns =
        semanticRepository.getCompositeKnowledgeAssetCarrier(
            axId0.getUuid(), axId0.getVersionTag());
    CompositeKnowledgeCarrier carrier = carrierAns.orElseGet(Assertions::fail);

    assertEquals(2, carrier.getComponent().size());
    assertTrue(carrier.components()
        .allMatch(k -> k.getAssetId().asKey().equals(axId1.asKey())
            || k.getAssetId().asKey().equals(axId2.asKey())));
    assertTrue(carrier.components()
        .allMatch(k -> k.getArtifactId().asKey().equals(artId1.asKey())
            || k.getArtifactId().asKey().equals(artId2.asKey())));
    assertTrue(carrier.components()
        .allMatch(k -> k.asString().orElse("").matches("Foo|Bar")));
  }


  @Test
  void testAnonymousComposites() {
    ResourceIdentifier axId1 = randomAssetId();
    ResourceIdentifier axId2 = randomAssetId();
    ResourceIdentifier artId1 = randomArtifactId();
    ResourceIdentifier artId2 = randomArtifactId();

    KnowledgeAsset ka1 = new KnowledgeAsset()
        .withAssetId(axId1)
        .withName("Foo")
        .withLinks(new Dependency().withRel(Depends_On).withHref(axId2));
    KnowledgeAsset ka2 = new KnowledgeAsset()
        .withAssetId(axId2)
        .withName("Bar");

    semanticRepository.setKnowledgeAssetVersion(
        axId1.getUuid(), axId1.getVersionTag(), ka1);
    semanticRepository.setKnowledgeAssetCarrierVersion(
        axId1.getUuid(), axId1.getVersionTag(), artId1.getUuid(), artId1.getVersionTag(),
        ka1.getName().getBytes());

    semanticRepository.setKnowledgeAssetVersion(
        axId2.getUuid(), axId2.getVersionTag(), ka2);
    semanticRepository.setKnowledgeAssetCarrierVersion(
        axId2.getUuid(), axId2.getVersionTag(), artId2.getUuid(), artId2.getVersionTag(),
        ka2.getName().getBytes());

    JenaRdfParser parser = new JenaRdfParser();

    Answer<CompositeKnowledgeCarrier> ckcAns =
        semanticRepository.getAnonymousCompositeKnowledgeAssetSurrogate(
            axId1.getUuid(), axId1.getVersionTag(), null);
    CompositeKnowledgeCarrier ckc = ckcAns.orElseGet(Assertions::fail);
    assertEquals(axId1.asKey(), ckc.getRootId().asKey());
    assertEquals(2, ckc.getComponent().size());
    assertTrue(ckc.components()
        .allMatch(k -> k.getAssetId().asKey().equals(axId1.asKey())
            || k.getAssetId().asKey().equals(axId2.asKey())));

    Answer<KnowledgeCarrier> structAns = semanticRepository
        .getAnonymousCompositeKnowledgeAssetStructure(
            axId1.getUuid(), axId1.getVersionTag());
    KnowledgeCarrier struct = structAns.orElseGet(Assertions::fail);
    Model m = parser.applyLift(struct, Abstract_Knowledge_Expression, codedRep(OWL_2), null)
        .flatOpt(kc -> kc.as(Model.class)).orElseGet(Assertions::fail);

    Answer<CompositeKnowledgeCarrier> carrierAns =
        semanticRepository.getAnonymousCompositeKnowledgeAssetCarrier(
            axId1.getUuid(), axId1.getVersionTag());
    CompositeKnowledgeCarrier carrier = carrierAns.orElseGet(Assertions::fail);

    assertEquals(2, carrier.getComponent().size());
    assertTrue(carrier.components()
        .allMatch(k -> k.getAssetId().asKey().equals(axId1.asKey())
            || k.getAssetId().asKey().equals(axId2.asKey())));
    assertTrue(carrier.components()
        .allMatch(k -> k.getArtifactId().asKey().equals(artId1.asKey())
            || k.getArtifactId().asKey().equals(artId2.asKey())));
    assertTrue(carrier.components()
        .allMatch(k -> k.asString().orElse("").matches("Foo|Bar")));
  }


  @Test
  void testAnonymousCompositesWithDeepDepenedencies() {
    ResourceIdentifier axId1 = randomAssetId();
    ResourceIdentifier axId2 = randomAssetId();
    ResourceIdentifier axId3 = randomAssetId();

    KnowledgeAsset ka1 = new KnowledgeAsset()
        .withAssetId(axId1).withName("Foo")
        .withLinks(new Dependency().withRel(Depends_On).withHref(axId2));
    KnowledgeAsset ka2 = new KnowledgeAsset()
        .withAssetId(axId2).withName("Bar")
        .withLinks(new Dependency().withRel(Depends_On).withHref(axId3));
    KnowledgeAsset ka3 = new KnowledgeAsset()
        .withAssetId(axId2).withName("Baz");

    semanticRepository.setKnowledgeAssetVersion(
        axId1.getUuid(), axId1.getVersionTag(), ka1);
    semanticRepository.setKnowledgeAssetVersion(
        axId2.getUuid(), axId2.getVersionTag(), ka2);
    semanticRepository.setKnowledgeAssetVersion(
        axId3.getUuid(), axId3.getVersionTag(), ka3);

    JenaRdfParser parser = new JenaRdfParser();

    Answer<KnowledgeCarrier> structAns = semanticRepository
        .getAnonymousCompositeKnowledgeAssetStructure(
            axId1.getUuid(), axId1.getVersionTag());
    KnowledgeCarrier struct = structAns.orElseGet(Assertions::fail);
    Model m = parser.applyLift(struct, Abstract_Knowledge_Expression, codedRep(OWL_2), null)
        .flatOpt(kc -> kc.as(Model.class)).orElseGet(Assertions::fail);

    assertTrue(m.contains(objA(axId1.getVersionId(), Depends_On.getReferentId(), axId2.getVersionId())));
    assertTrue(m.contains(objA(axId2.getVersionId(), Depends_On.getReferentId(), axId3.getVersionId())));
  }



  @Test
  void testNamedComposites() {
    ResourceIdentifier axId0 = randomAssetId();
    ResourceIdentifier sId = randomAssetId();
    ResourceIdentifier axId1 = randomAssetId();
    ResourceIdentifier axId2 = randomAssetId();
    ResourceIdentifier artId1 = randomArtifactId();
    ResourceIdentifier artId2 = randomArtifactId();

    KnowledgeAsset ka0 = new KnowledgeAsset()
        .withAssetId(axId0)
        .withName("Comp")
        .withRole(Composite_Knowledge_Asset)
        .withLinks(new Component().withRel(Has_Structural_Component).withHref(axId1))
        .withLinks(new Component().withRel(Has_Structural_Component).withHref(axId2))
        .withLinks(new Component().withRel(Has_Structuring_Component).withHref(sId));

    KnowledgeAsset ka1 = new KnowledgeAsset()
        .withAssetId(axId1)
        .withName("Foo")
        .withLinks(new Dependency().withRel(Depends_On).withHref(axId2));
    KnowledgeAsset ka2 = new KnowledgeAsset()
        .withAssetId(axId2)
        .withName("Bar");

    semanticRepository.setKnowledgeAssetVersion(
        axId0.getUuid(), axId0.getVersionTag(), ka0);

    semanticRepository.setKnowledgeAssetVersion(
        axId1.getUuid(), axId1.getVersionTag(), ka1);
    semanticRepository.setKnowledgeAssetCarrierVersion(
        axId1.getUuid(), axId1.getVersionTag(), artId1.getUuid(), artId1.getVersionTag(),
        ka1.getName().getBytes());

    semanticRepository.setKnowledgeAssetVersion(
        axId2.getUuid(), axId2.getVersionTag(), ka2);
    semanticRepository.setKnowledgeAssetCarrierVersion(
        axId2.getUuid(), axId2.getVersionTag(), artId2.getUuid(), artId2.getVersionTag(),
        ka2.getName().getBytes());

    JenaRdfParser parser = new JenaRdfParser();

    Answer<CompositeKnowledgeCarrier> ckcAns =
        semanticRepository.getCompositeKnowledgeAssetSurrogate(
            axId0.getUuid(), axId0.getVersionTag(), null,null);
    CompositeKnowledgeCarrier ckc = ckcAns.orElseGet(Assertions::fail);
    assertEquals(axId0.asKey(), ckc.getRootId().asKey());
    assertEquals(axId0.asKey(), ckc.getAssetId().asKey());
    assertEquals(3, ckc.getComponent().size());
    assertTrue(ckc.components()
        .allMatch(k -> k.getAssetId().asKey().equals(axId0.asKey())
            || k.getAssetId().asKey().equals(axId1.asKey())
            || k.getAssetId().asKey().equals(axId2.asKey())));
    assertEquals(axId0.asKey(), ckc.mainComponent().getAssetId().asKey());

    assertNotNull(ckc.getStruct());
    assertEquals(sId.asKey(), ckc.getStruct().getAssetId().asKey());

    Model m1 = parser
        .applyLift(ckc.getStruct(), Abstract_Knowledge_Expression, codedRep(OWL_2), null)
        .flatOpt(x -> x.as(Model.class))
        .orElseGet(Assertions::fail);

    Answer<KnowledgeCarrier> structAns = semanticRepository
        .getCompositeKnowledgeAssetStructure(
            axId0.getUuid(), axId0.getVersionTag());
    KnowledgeCarrier struct = structAns.orElseGet(Assertions::fail);
    assertEquals(sId.asKey(), struct.getAssetId().asKey());

    Model m2 = parser
        .applyLift(struct, Abstract_Knowledge_Expression, codedRep(OWL_2), null)
        .flatOpt(kc -> kc.as(Model.class))
        .orElseGet(Assertions::fail);

    m1.listStatements().forEachRemaining(st -> assertTrue(m2.contains(st)));
    m2.listStatements().forEachRemaining(st -> assertTrue(m1.contains(st)));

    Answer<CompositeKnowledgeCarrier> carrierAns =
        semanticRepository.getCompositeKnowledgeAssetCarrier(
            axId0.getUuid(), axId0.getVersionTag());
    CompositeKnowledgeCarrier carrier = carrierAns.orElseGet(Assertions::fail);
    assertNotNull(carrier.getStruct());

    assertEquals(2, carrier.getComponent().size());
    assertTrue(carrier.components()
        .allMatch(k -> k.getAssetId().asKey().equals(axId1.asKey())
            || k.getAssetId().asKey().equals(axId2.asKey())));
    assertTrue(carrier.components()
        .allMatch(k -> k.getArtifactId().asKey().equals(artId1.asKey())
            || k.getArtifactId().asKey().equals(artId2.asKey())));
    assertTrue(carrier.components()
        .allMatch(k -> k.asString().orElse("").matches("Foo|Bar")));
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
        JenaUtil.objA(subj.getVersionId(), pred.getReferentId(), obj.getVersionId()));
  }


}
