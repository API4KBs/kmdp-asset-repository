package edu.mayo.kmdp.repository.asset.negotiation;

import static edu.mayo.kmdp.util.Util.isNotEmpty;

import java.net.URI;
import java.util.stream.Stream;
import org.omg.spec.api4kp._20200801.id.Link;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.repository.asset.KARSHrefBuilder;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

/**
 * Helper class used to manage 'locators' - URLs that point to Carriers and Surrogates
 */
public final class LocatorHelper {

  private LocatorHelper() {
    // functions only
  }

  /**
   * rewrites the self-referential KARS locators in a canonical Surrogate.
   * <p>
   * URI-based Knowledge resource identifiers are mappable to KARS API endpoints. For example, a
   * UUID could be mapped to an asset ID series as {KARS}/cat/assets/{uuid}. The URI is then
   * expected to dereference to a URL where the KARS API are exposed, e.g. {KARS base URL}/cat/...
   * <p>
   * In this scenario, some locator URLs in the surrogate may be pre-resolved to point to the same
   * server where the canonical {@link KnowledgeAsset} surrogate itself can be accessed. As the
   * Surrogate is moved from a different implementation / tier or KARS to another, these URLs need
   * to be rewritten, to use the target server base URL.
   * <p>
   * This approach may be discontinued, to the extent that 'this' KARS implementation is able to
   * generate self-reflective links on demand. In API4KP 1.0, where multiple locators are supported,
   * it will dedcied whether to keep links to a 'source' KARS - only or both
   *
   * @param assetSurrogate The surrogate with {@link KnowledgeArtifact#getLocator()}, and/or
   *                       Pointer-based {@link Link} that reference KARS endpoints
   * @param hrefBuilder    the manager of 'this' KARS endpoints
   * @deprecated This method  will be rewritten when API4KP 1.0 is implemented. APi4KP 1.0 supports
   * multiple locators and is better equipped to handle federation/distribution
   */
  @Deprecated(since = "API4KP 1.0-Beta")
  public static void rewriteSelfLinks(KnowledgeAsset assetSurrogate, KARSHrefBuilder hrefBuilder) {
    if (hrefBuilder == null) {
      return;
    }
    Stream.concat(
            assetSurrogate.getCarriers().stream(),
            assetSurrogate.getSurrogate().stream())
        .forEach(ka -> {
          ka.getLinks().forEach(link -> rewriteRef(link.getHref(), hrefBuilder));
          rewriteLocator(ka, hrefBuilder);
        });
    assetSurrogate.getLinks().forEach(link -> rewriteRef(link.getHref(), hrefBuilder));
  }

  private static void rewriteLocator(KnowledgeArtifact ka, KARSHrefBuilder hrefBuilder) {
    ka.setLocator(claimKarsRef(ka.getLocator(), hrefBuilder));
  }

  private static URI claimKarsRef(URI locator, KARSHrefBuilder hrefBuilder) {
    if (locator == null || hrefBuilder == null) {
      return locator;
    }
    if (locator.getPath() != null
        && locator.getPath().contains("/cat/assets")) {
      var sb = new StringBuilder(hrefBuilder.getHost())
          .append(locator.getPath());
      if (isNotEmpty(locator.getQuery())) {
        sb.append("?").append(locator.getQuery());
      }
      if (isNotEmpty(locator.getFragment())) {
        sb.append("#").append(locator.getFragment());
      }
      return URI.create(sb.toString());
    }
    return locator;
  }

  private static void rewriteRef(SemanticIdentifier href, KARSHrefBuilder hrefBuilder) {
    if (href instanceof Pointer) {
      var ptr = (Pointer) href;
      ptr.setHref(claimKarsRef(ptr.getHref(), hrefBuilder));
    }
  }

}
