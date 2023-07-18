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


import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Syntactic_Translation_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.KNART_1_3;

import edu.mayo.kmdp.language.translators.AbstractSimpleTranslator;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.services.transrepresentation.TransrepresentationOperator;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;

@Named
@KPOperation(Syntactic_Translation_Task)
public class MockTranslator extends AbstractSimpleTranslator<String,String> {

  public static final UUID kpIdentifier = UUID.fromString("41c9758e-00d1-4348-bf05-73aae9c5e43e");

  protected TransrepresentationOperator op = new TransrepresentationOperator()
      .withOperatorId(SemanticIdentifier.newId(kpIdentifier))
      .withFrom(getFrom())
      .withInto(getInto());

  public MockTranslator() {
    setId(op.getOperatorId());
  }

  @Override
  public Answer<KnowledgeCarrier> applyTransrepresent(KnowledgeCarrier sourceArtifact,
      String xAccept, String xParams) {
    return Answer.of(translate(sourceArtifact));
  }


  @Override
  public List<SyntacticRepresentation> getFrom() {
    return Collections.singletonList(rep(KNART_1_3,XML_1_1));
  }

  @Override
  public List<SyntacticRepresentation> getInto() {
    return Collections.singletonList(rep(HTML,TXT));
  }

  @Override
  public KnowledgeRepresentationLanguage getTargetLanguage() {
    return HTML;
  }

  public KnowledgeCarrier translate(KnowledgeCarrier source) {
    return source;
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return KNART_1_3;
  }
}