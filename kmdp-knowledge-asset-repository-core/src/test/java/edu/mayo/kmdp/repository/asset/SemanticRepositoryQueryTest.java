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

import static edu.mayo.kmdp.util.Util.uuid;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassetrole.KnowledgeAssetRoleSeries.Operational_Concept_Definition;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Care_Process_Model;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Formal_Ontology;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Predictive_Model;
import static edu.mayo.ontology.taxonomies.kao.rel.dependencyreltype.DependencyTypeSeries.Depends_On;
import static edu.mayo.ontology.taxonomies.kmdo.annotationreltype.AnnotationRelTypeSeries.Defines;
import static edu.mayo.ontology.taxonomies.kmdo.annotationreltype.AnnotationRelTypeSeries.In_Terms_Of;
import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.TXT;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.HL7_ELM;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.SPARQL_1_1;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;

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
import edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.AbstractCarrier;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.datatypes.Bindings;
import org.omg.spec.api4kp._1_0.id.Pointer;
import org.omg.spec.api4kp._1_0.identifiers.ConceptIdentifier;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;


class SemanticRepositoryQueryTest extends RepositoryTestBase {

  @Test
  void testInit() {
    assertNotNull(semanticRepository);
  }

  @BeforeEach
  void prepopulate() {
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setVersionedKnowledgeAsset(uuid("foo2"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
  }

  @Test
  void testQuery() {
    String query = "" +
        "select ?s where { ?s a ?o . }" +
        "";

    KnowledgeCarrier queryCarrier = AbstractCarrier.of(query)
        .withRepresentation(rep(SPARQL_1_1, TXT));

    List<Bindings> binds = semanticRepository.queryKnowledgeAssets(queryCarrier)
        .orElse(Collections.emptyList());

    assertEquals(2, binds.size());
  }

}
