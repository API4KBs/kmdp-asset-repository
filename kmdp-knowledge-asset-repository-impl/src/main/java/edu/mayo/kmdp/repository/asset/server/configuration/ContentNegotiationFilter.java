package edu.mayo.kmdp.repository.asset.server.configuration;

import edu.mayo.kmdp.util.Util;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
/**
 * This class normalizes the interactions with the server
 * where the client can either be a (code generated) ApiClient or a User Agent (e.g. browser)
 *
 * OpenAPI clients and servers reserve the 'Accept' header to discriminate between XML
 * and JSON as formats used to serialize the returned objects - or HTML to return human readable content.
 *
 * API4KP uses the 'X-Accept' header to carry a formal MIME type to control the
 * representation/serialization of Artifacts returned via Answer+KnowledgeCarrier wrapper monads.
 *
 * Four scenarios are possible:
 * 1) Both Accept and X-Accept: X-Acccept determines the language and serialization of the Artifact,
 * which is then serialized/encoded and wrapped in a KnowledgeCarrier. The resulting KnowledgeCarrier
 * itself is serialized according to Accept
 * 2) No headers - defaults are used for both X-Accept (depending on Artifact) and Accept
 * 3) Accept only - The value of Accept is used for both Carrier and Artifact, after rewriting
 * the Accept MIME type as a formal X-Accept MIME type
 * 4) X-Accept only - X-Accept informs the serialization of the Artifact, while Accept defaults
 */
public class ContentNegotiationFilter implements Filter {

  @Override
  public void doFilter(
  ServletRequest request,
  ServletResponse response,
  FilterChain chain) throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;

    MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(req);

    String accept = req.getHeader("Accept");
    String xAccept = req.getHeader("X-Accept");

    mutableRequest.addXAccept(
        Util.isEmpty(xAccept) ? ModelMIMECoder.recodeAll(accept, ModelMIMECoder.WEIGHT_DEFAULT) : xAccept);

    chain.doFilter(mutableRequest, response);
  }

}