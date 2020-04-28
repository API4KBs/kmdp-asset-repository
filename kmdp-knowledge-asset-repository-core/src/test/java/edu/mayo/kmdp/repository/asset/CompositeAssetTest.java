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

import static edu.mayo.kmdp.metadata.v2.surrogate.SurrogateBuilder.randomArtifactId;
import static edu.mayo.kmdp.metadata.v2.surrogate.SurrogateBuilder.randomAssetId;
import static edu.mayo.kmdp.metadata.v2.surrogate.SurrogateHelper.getSurrogateId;
import static edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryService.transitiveDependencies;
import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.TXT;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.HL7_ELM;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.SPARQL_1_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;

import edu.mayo.kmdp.metadata.v2.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.v2.surrogate.Dependency;
import edu.mayo.kmdp.metadata.v2.surrogate.KnowledgeAsset;
import edu.mayo.ontology.taxonomies.kao.rel.dependencyreltype.DependencyTypeSeries;
import edu.mayo.ontology.taxonomies.kao.rel.structuralreltype.StructuralPartTypeSeries;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.AbstractCarrier;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.Composite;
import org.omg.spec.api4kp._1_0.datatypes.Bindings;
import org.omg.spec.api4kp._1_0.id.ResourceIdentifier;
import org.omg.spec.api4kp._1_0.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;


class CompositeAssetTest extends RepositoryTestBase {

  @Test
  void testPublishSetOrientedCompositeMultiple() {
    ResourceIdentifier id1 = randomAssetId();
    ResourceIdentifier id2 = randomAssetId();

    KnowledgeAsset a1 = new KnowledgeAsset()
        .withAssetId(id1)
        .withCarriers(new ComputableKnowledgeArtifact()
            .withArtifactId(randomArtifactId())
            .withRepresentation(rep(HTML)));
    KnowledgeAsset a2 = new KnowledgeAsset()
        .withAssetId(id2)
        .withCarriers(new ComputableKnowledgeArtifact()
            .withArtifactId(randomArtifactId())
            .withRepresentation(rep(HL7_ELM)));

    KnowledgeCarrier ckc = AbstractCarrier.ofIdentifiableSet(
        rep(Knowledge_Asset_Surrogate_2_0),
        KnowledgeAsset::getAssetId,
        ka -> getSurrogateId(ka,Knowledge_Asset_Surrogate_2_0,null)
            .orElse(randomArtifactId()),
        Arrays.asList(a1, a2));

    Answer<Void> result = semanticRepository.addCanonicalKnowledgeAssetSurrogate(
        ckc.getAssetId().getUuid(), ckc.getAssetId().getVersionTag(),ckc);
    assertTrue(result.isSuccess());

    assertEquals(3,
        semanticRepository.listKnowledgeAssets().orElse(Collections.emptyList()).size());

    assertTrue(semanticRepository.getKnowledgeAsset(id1.getUuid()).isSuccess());
    assertTrue(semanticRepository.getKnowledgeAsset(id2.getUuid()).isSuccess());

    String query = "" +
        "select ?o where { ?s <" + StructuralPartTypeSeries.Has_Part.getRef() + "> ?o . }";

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
        .withCarriers(new ComputableKnowledgeArtifact()
            .withArtifactId(randomArtifactId())
            .withRepresentation(rep(HTML)));
    KnowledgeAsset a2 = new KnowledgeAsset()
        .withAssetId(id2)
        .withCarriers(new ComputableKnowledgeArtifact()
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
        "select ?o where { ?s <" + StructuralPartTypeSeries.Has_Part.getRef() + "> ?o . }";
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
        .withLinks(new Dependency().withRel(DependencyTypeSeries.Depends_On).withHref(id2));
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
        .withLinks(new Dependency().withRel(DependencyTypeSeries.Depends_On).withHref(id2))
        .withCarriers(new ComputableKnowledgeArtifact()
            .withArtifactId(randomArtifactId())
            .withRepresentation(rep(HTML))
            .withInlinedExpression("AAA"));
    KnowledgeAsset a2 = new KnowledgeAsset()
        .withAssetId(id2)
        .withCarriers(new ComputableKnowledgeArtifact()
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




}
