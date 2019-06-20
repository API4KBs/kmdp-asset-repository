package edu.mayo.kmdp.language.test;


import static edu.mayo.ontology.taxonomies.api4kp.knowledgeoperations._2018._06.KnowledgeProcessingOperation.Translation_Task;
import static edu.mayo.ontology.taxonomies.krformat._2018._08.SerializationFormat.TXT;
import static edu.mayo.ontology.taxonomies.krformat._2018._08.SerializationFormat.XML_1_1;
import static edu.mayo.ontology.taxonomies.krlanguage._2018._08.KnowledgeRepresentationLanguage.HTML;
import static edu.mayo.ontology.taxonomies.krlanguage._2018._08.KnowledgeRepresentationLanguage.KNART_1_3;

import edu.mayo.kmdp.tranx.server.TransxionApiDelegate;
import edu.mayo.kmdp.util.ws.ResponseHelper;
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