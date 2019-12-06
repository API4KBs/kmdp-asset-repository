package edu.mayo.kmdp.repository.asset.server.configuration;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class ContentNegotiationFilter implements Filter {

  @Override
  public void doFilter(
  ServletRequest request,
  ServletResponse response,
  FilterChain chain) throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;

    MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(req);

    mutableRequest.addXAccept(req.getHeader("Accept"));
    chain.doFilter(mutableRequest, response);
  }

}