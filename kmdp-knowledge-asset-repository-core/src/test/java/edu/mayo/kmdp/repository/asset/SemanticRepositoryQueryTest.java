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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Care_Process_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.SPARQL_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.RDF_XML_Syntax;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.Turtle;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;


class SemanticRepositoryQueryTest extends RepositoryTestBase {

  @Test
  void testInit() {
    assertNotNull(semanticRepository);
  }

  @BeforeEach
  void prepopulate() {
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(uuid("foo2"), "1",
            new KnowledgeAsset().withFormalType(Care_Process_Model)));
  }

  @Test
  void testQuery() {
    String query = "" +
        "select ?s where { ?s a ?o . }" +
        "";

    KnowledgeCarrier queryCarrier = AbstractCarrier.of(query)
        .withRepresentation(rep(SPARQL_1_1, TXT, Charset.defaultCharset()));

    List<Bindings> binds = semanticRepository.queryKnowledgeAssetGraph(queryCarrier)
        .orElse(Collections.emptyList());

    assertEquals(2, binds.size());
  }

  @Test
  void testGraph() {
    Answer<KnowledgeCarrier> graphAns =
        semanticRepository.getKnowledgeGraph(
            ModelMIMECoder.encode(rep(OWL_2, RDF_XML_Syntax)));
    assertTrue(graphAns.isSuccess());
    KnowledgeCarrier graph = graphAns.get();

    assertTrue(graph.is(String.class));
    assertTrue(graph.asString().filter(str -> str.startsWith("<rdf:RDF")).isPresent());
    assertSame(OWL_2,
        graph.getRepresentation().getLanguage().asEnum());
    assertSame(RDF_XML_Syntax,
        graph.getRepresentation().getSerialization().asEnum());
  }

  @Test
  void testGraphDefault() {
    Answer<KnowledgeCarrier> graphAns = semanticRepository.getKnowledgeGraph();
    assertTrue(graphAns.isSuccess());
    KnowledgeCarrier graph = graphAns.get();

    assertTrue(graph.is(String.class));
    assertSame(OWL_2,
        graph.getRepresentation().getLanguage().asEnum());
    assertSame(RDF_XML_Syntax,
        graph.getRepresentation().getSerialization().asEnum());
  }

}
