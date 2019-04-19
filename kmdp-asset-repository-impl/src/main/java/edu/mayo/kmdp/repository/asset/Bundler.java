package edu.mayo.kmdp.repository.asset;

import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;

import java.util.List;

public interface Bundler {

    List<KnowledgeCarrier> bundle(String assetId, String version);

}
