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
package edu.mayo.kmdp.language.test;


import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.KNART_1_3;
import static java.util.Collections.singletonList;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;

import edu.mayo.kmdp.language.translators.AbstractSimpleTranslator;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.id.IdentifierConstants;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;

@Named
@KPOperation(KnowledgeProcessingOperationSeries.Syntactic_Translation_Task)
public class MockTranslator extends AbstractSimpleTranslator<byte[],String> {

  public static final UUID kpIdentifier = UUID.fromString("41c9758e-00d1-4348-bf05-73aae9c5e43e");

  public MockTranslator() {
    setId(SemanticIdentifier.newId(kpIdentifier, IdentifierConstants.VERSION_ZERO));
  }

  @Override
  public List<SyntacticRepresentation> getFrom() {
    return singletonList(rep(KNART_1_3, XML_1_1, Charset.defaultCharset(), Encodings.DEFAULT));
  }

  @Override
  public List<SyntacticRepresentation> getInto() {
    return singletonList(rep(HTML, TXT));
  }

  @Override
  public KnowledgeRepresentationLanguage getTargetLanguage() {
    return HTML;
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return KNART_1_3;
  }

  @Override
  protected Optional<String> transformBinary(ResourceIdentifier assetId, byte[] bytes,
      SyntacticRepresentation srcRep,
      SyntacticRepresentation tgtRep, Properties config) {
    return Optional.of(translate(bytes));
  }

  public String translate(byte[] source) {
    return "<html>\n"
        + "<head>\n"
        + "<title>" + "Mock Rule" + "</title>\n"
        + "</head>\n"
        + "<body>";
  }


}