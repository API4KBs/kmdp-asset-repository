package edu.mayo.kmdp.repository.asset.server.configuration;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Serialized_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.html.HtmlDeserializer;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

public class HTMLAdapter<T> implements HttpMessageConverter<T> {

  @Override
  public boolean canRead(Class<?> clazz, MediaType mediaType) {
    return false;
  }

  @Override
  public boolean canWrite(Class<?> clazz, MediaType mediaType) {
    return getSupportedMediaTypes().contains(mediaType)
        && KnowledgeCarrier.class.isAssignableFrom(clazz);
  }

  @Override
  public List<MediaType> getSupportedMediaTypes() {
    return Arrays.asList(MediaType.APPLICATION_XHTML_XML, MediaType.TEXT_HTML);
  }

  @Override
  public T read(Class<? extends T> clazz, HttpInputMessage inputMessage)
      throws IOException, HttpMessageNotReadableException {
    return null;
  }

  @Override
  public void write(Object o, MediaType contentType, HttpOutputMessage outputMessage)
      throws IOException, HttpMessageNotWritableException {
    KnowledgeCarrier kc = (KnowledgeCarrier) o;
    String html = new HtmlDeserializer()
        .applyLower(kc, Serialized_Knowledge_Expression,
            codedRep(HTML, TXT, Charset.defaultCharset()), null)
        .flatOpt(AbstractCarrier::asString)
        .orElseThrow(() -> new HttpMessageNotWritableException("Unable to write HTML"));
    outputMessage.getBody().write(html.getBytes());
  }

}
