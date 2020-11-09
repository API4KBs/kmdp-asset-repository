package edu.mayo.kmdp.repository.asset.server.configuration;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.html.HtmlDeserializer;
import edu.mayo.kmdp.util.ws.HTMLKnowledgeCarrierWrapper;
import java.io.IOException;
import java.nio.charset.Charset;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * Custom converter that unwraps HTML content for end-user agent clients
 * Extends the generic HTMLKnowledgeCarrierWrapper to use a HTMLDeserializer
 */
public class HTMLAdapter extends HTMLKnowledgeCarrierWrapper {

  @Override
  public void write(KnowledgeCarrier kc, MediaType contentType, HttpOutputMessage outputMessage)
      throws IOException, HttpMessageNotWritableException {
    byte[] html = new HtmlDeserializer()
        .applyLower(kc, Encoded_Knowledge_Expression,
            codedRep(HTML, TXT, Charset.defaultCharset(), Encodings.DEFAULT), null)
        .flatOpt(AbstractCarrier::asBinary)
        .orElseThrow(() -> new HttpMessageNotWritableException("Unable to write HTML"));
    outputMessage.getBody().write(html);
  }

}
