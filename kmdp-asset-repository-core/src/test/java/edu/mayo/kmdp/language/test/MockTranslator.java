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


import static edu.mayo.ontology.taxonomies.api4kp.knowledgeoperations._20190801.KnowledgeProcessingOperation.Translation_Task;
import static edu.mayo.ontology.taxonomies.krformat._20190801.SerializationFormat.TXT;
import static edu.mayo.ontology.taxonomies.krformat._20190801.SerializationFormat.XML_1_1;
import static edu.mayo.ontology.taxonomies.krlanguage._20190801.KnowledgeRepresentationLanguage.HTML;
import static edu.mayo.ontology.taxonomies.krlanguage._20190801.KnowledgeRepresentationLanguage.KNART_1_3;

import edu.mayo.kmdp.tranx.server.TransxionApiDelegate;
import edu.mayo.kmdp.util.ws.ResponseHelper;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import javax.inject.Named;
import org.omg.spec.api4kp._1_0.services.KPOperation;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.omg.spec.api4kp._1_0.services.ParameterDefinitions;
import org.omg.spec.api4kp._1_0.services.SyntacticRepresentation;
import org.omg.spec.api4kp._1_0.services.tranx.TransrepresentationOperator;
import org.springframework.http.ResponseEntity;

@Named
@KPOperation(Translation_Task)
public class MockTranslator implements TransxionApiDelegate {

  public static final UUID kpIdentifier = UUID.fromString("41c9758e-00d1-4348-bf05-73aae9c5e43e");

  protected static final TransrepresentationOperator op = new TransrepresentationOperator()
      .withOperatorId(kpIdentifier.toString())
      .withFrom(getFrom())
      .withInto(getTo());

  @Override
  public ResponseEntity<KnowledgeCarrier> applyTransrepresentation(String txId,
      KnowledgeCarrier sourceArtifact, Properties params) {
    return ResponseHelper.attempt(translate(sourceArtifact));
  }

  @Override
  public ResponseEntity<TransrepresentationOperator> getTransrepresentation(String txId) {
    return ResponseHelper.succeed(op);
  }

  @Override
  public ResponseEntity<ParameterDefinitions> getTransrepresentationAcceptedParameters(
      String txId) {
    return ResponseHelper
        .succeed(new org.omg.spec.api4kp._1_0.services.resources.ParameterDefinitions());
  }

  @Override
  public ResponseEntity<SyntacticRepresentation> getTransrepresentationOutput(String txId) {
    return ResponseHelper.succeed(getTo());
  }

  @Override
  public ResponseEntity<List<TransrepresentationOperator>> listOperators(
      SyntacticRepresentation from, SyntacticRepresentation into, String method) {
    return ResponseHelper.notSupported();
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