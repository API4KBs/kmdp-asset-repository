package edu.mayo.kmdp.repository.asset.server;

import org.omg.spec.api4kp._20200801.services.repository.asset.KARSHrefBuilder;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerProperties;
import edu.mayo.kmdp.util.Util;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public class ServerContextAwareHrefBuilder extends KARSHrefBuilder {

  private static final Logger logger = LoggerFactory.getLogger(ServerContextAwareHrefBuilder.class);

  @Value("${edu.mayo.kmdp.repository.asset.baseURL:}")
  private String environmentHost;
  private boolean hasEnvOverride;

  public ServerContextAwareHrefBuilder(
      KnowledgeAssetRepositoryServerProperties cfg) {
    super(cfg);

  }

  @PostConstruct
  void init() {
    this.hasEnvOverride = Util.isNotEmpty(environmentHost);
  }

  /**
   * Determine the base URL used to construct full URLs
   * Uses, in order of precedence
   *  - an environment configured variable:
   *  $edu.mayo.kmdp.repository.asset.baseURL
   *  - the base URL determined from the current Servlet Request
   *  - the compile-time KnowledgeAssetRepositoryServerConfig cfg used to initialize this object
   *  KnowledgeAssetRepositoryOptions.SERVER_HOST
   *
   * @return
   */
  @Override
  public String getHost() {
    if (hasEnvOverride) {
      return environmentHost;
    }
    try {
      return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    } catch (Exception e) {
      logger.info(e.getMessage());
      return super.getHost();
    }
  }

  @Override
  public String getCurrentURL() {
    return ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString();
  }
}
