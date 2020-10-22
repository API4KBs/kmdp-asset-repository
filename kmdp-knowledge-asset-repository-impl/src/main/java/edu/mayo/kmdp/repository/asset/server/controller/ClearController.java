package edu.mayo.kmdp.repository.asset.server.controller;

import edu.mayo.kmdp.repository.asset.SemanticKnowledgeAssetRepository;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A simple REST controller to expose a single clear endpoint.
 *
 * Note that this endpoint exists outside of the Swagger specification and
 * is a utility function not part of the documented API. Please understand
 * the implications of using this endpoint.
 *
 * Note that this endpoint will not process the call unless an `allowClearAll` environment
 * variable is explicitly set to `true`.
 */
@RestController
public class ClearController {

  private static final Logger logger = LoggerFactory
      .getLogger(ClearController.class);

  /**
   * IMPORTANT!
   * If true this will expose an endpoint to clear all tables.
   */
  @Value("${allowClearAll:false}")
  private boolean allowClearAll = false;

  @Autowired
  private SemanticKnowledgeAssetRepository assetRepository;

  @PostConstruct
  void logClearAll() {
    if (this.allowClearAll) {
      logger.warn("!!! WARNING !!! The REST endpoint to clear all Asset Repository content is ACTIVE! If you did not intent this,"
          + " ensure the environment variable `allowClearAll` is either unset or set to false.");
    } else {
      logger.info("The REST endpoint to clear all Asset Repository content is INACTIVE.");
    }
  }

  @DeleteMapping("/all")
  public void clearAll() {
    if (this.allowClearAll) {
      assetRepository.clear();
    } else {
      throw new UnsupportedOperationException("`clearAll` is not allowed in this environment.");
    }
  }

}