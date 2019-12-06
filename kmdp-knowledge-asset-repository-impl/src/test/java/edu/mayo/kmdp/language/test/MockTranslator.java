/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.mayo.kmdp.language.test;


import static edu.mayo.ontology.taxonomies.api4kp.knowledgeoperations.KnowledgeProcessingOperationSeries.Translation_Task;
import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.TXT;
import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.XML_1_1;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.KNART_1_3;

import edu.mayo.kmdp.tranx.server.TransxionApiInternal;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import javax.inject.Named;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.services.KPOperation;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.omg.spec.api4kp._1_0.services.ParameterDefinitions;
import org.omg.spec.api4kp._1_0.services.SyntacticRepresentation;
import org.omg.spec.api4kp._1_0.services.tranx.TransrepresentationOperator;

@Named
@KPOperation(Translation_Task)
public class MockTranslator implements TransxionApiInternal {

  public static final UUID kpIdentifier = UUID.fromString("41c9758e-00d1-4348-bf05-73aae9c5e43e");

  protected static final TransrepresentationOperator op = new TransrepresentationOperator()
      .withOperatorId(kpIdentifier.toString())
      .withFrom(getFrom())
      .withInto(getTo());

  @Override
  public Answer<KnowledgeCarrier> applyTransrepresentation(String txId,
      KnowledgeCarrier sourceArtifact, Properties params) {
    return Answer.of(translate(sourceArtifact));
  }

  @Override
  public Answer<TransrepresentationOperator> getTransrepresentation(String txId) {
    return Answer.of(op);
  }

  @Override
  public Answer<ParameterDefinitions> getTransrepresentationAcceptedParameters(
      String txId) {
    return Answer.of(new org.omg.spec.api4kp._1_0.services.resources.ParameterDefinitions());
  }

  @Override
  public Answer<SyntacticRepresentation> getTransrepresentationOutput(String txId) {
    return Answer.of(getTo());
  }

  @Override
  public Answer<List<TransrepresentationOperator>> listOperators(
      SyntacticRepresentation from, SyntacticRepresentation into, String method) {
    return Answer.unsupported();
  }

  protected static SyntacticRepresentation getFrom() {
    return new org.omg.spec.api4kp._1_0.services.resources.SyntacticRepresentation()
        .withLanguage(KNART_1_3).withFormat(XML_1_1);
  }

  protected static SyntacticRepresentation getTo() {
    return new org.omg.spec.api4kp._1_0.services.resources.SyntacticRepresentation()
        .withLanguage(HTML).withFormat(TXT);
  }

  public KnowledgeCarrier translate(KnowledgeCarrier source) {
    return source;
  }

}