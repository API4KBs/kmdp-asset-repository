package edu.mayo.kmdp.repository.asset.server.configuration;

import edu.mayo.kmdp.util.Util;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class MutableHttpServletRequest extends HttpServletRequestWrapper {

  public static final String EXT_ACCEPT = "X-Accept";
  private String xAccept = null;

  public MutableHttpServletRequest(HttpServletRequest request) {
    super(request);
  }

  public void addXAccept(String value) {
    if (Util.isEmpty(super.getHeader(EXT_ACCEPT))) {
      xAccept = value;
    }
  }

  @Override
  public Enumeration<String> getHeaders(String name) {
    if (EXT_ACCEPT.equalsIgnoreCase(name)) {
      String xa = super.getHeader(EXT_ACCEPT);
      if (Util.isEmpty(xa)) {
        xa = xAccept;
      }
      if (Util.isEmpty(xa)) {
        return Collections.enumeration(Collections.emptyList());
      } else {
        return Collections.enumeration(Arrays.asList(xa.split(",")));
      }
    }
    return ((HttpServletRequest) getRequest()).getHeaders(name);
  }

  @Override
  public String getHeader(String name) {
    if (EXT_ACCEPT.equalsIgnoreCase(name)) {
      String baseXAccept = super.getHeader(EXT_ACCEPT);
      return Util.isEmpty(baseXAccept) ? xAccept : baseXAccept;
    }
    return ((HttpServletRequest) getRequest()).getHeader(name);
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    Set<String> set = new HashSet<>();
    set.add(EXT_ACCEPT);

    Enumeration<String> e = ((HttpServletRequest) getRequest()).getHeaderNames();
    while (e.hasMoreElements()) {
      set.add(e.nextElement());
    }

    return Collections.enumeration(set);
  }
}
