package edu.mayo.kmdp.repository.asset.index.sparql;

import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.aspects.LogLevel;
import org.omg.spec.api4kp._20200801.aspects.Loggable;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;

public interface KnowledgeGraphHolder {

  KnowledgeGraphInfo getInfo();

  Answer<Void> saveKnowledgeGraph();

  void resetGraph();

  KnowledgeCarrier getKnowledgeGraph();
}
