package edu.mayo.kmdp.repository.asset.index.sparql;


import static edu.mayo.kmdp.repository.asset.index.sparql.DefaultKnowledgeGraphHolder.newKnowledgeGraphHolder;
import static edu.mayo.kmdp.repository.asset.index.sparql.KnowledgeGraphInfo.newKnowledgeGraphInfo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Lists;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties;
import edu.mayo.kmdp.repository.artifact.jpa.JPAKnowledgeArtifactRepository;
import edu.mayo.kmdp.repository.artifact.jpa.JPAKnowledgeArtifactRepositoryService;
import edu.mayo.kmdp.repository.asset.index.sparql.impl.JenaSparqlDAO;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JenaSparqlDaoTest {

  KnowledgeArtifactRepositoryServerProperties cfg =
      new KnowledgeArtifactRepositoryServerProperties(
          SparqlIndexTest.class.getResourceAsStream("/application.test.properties"));

  JPAKnowledgeArtifactRepository mockRepo =
      new JPAKnowledgeArtifactRepository(JPAKnowledgeArtifactRepositoryService.inMemoryDataSource(),
          cfg);

  DefaultKnowledgeGraphHolder kgHolder = newKnowledgeGraphHolder(mockRepo, newKnowledgeGraphInfo());

  JenaSparqlDAO dao = new JenaSparqlDAO(kgHolder);

  @BeforeEach
  void reinit() {
    dao.reinitialize();
  }

  @Test
  void store() {
    JenaSparqlDAO dao = this.dao;
    int tBoxSize = dao.getKnowledgeGraphHolder().getTBoxTriples().size();

    assertEquals(tBoxSize, kgHolder.testGetModel().size());

    dao.store(URI.create("http://a.tst/test1"), URI.create("http://a.tst/test2"),
        URI.create("http://a.tst/test3"));

    assertEquals(1 + tBoxSize, kgHolder.testGetModel().size());
  }

  @Test
  void runSparql() {
    JenaSparqlDAO dao = this.dao;

    dao.store(URI.create("http://a.tst/test1"), URI.create("http://a.tst/test2"),
        URI.create("http://a.tst/test3"));

    String query = "" +
        "select ?s where { ?s <http://a.tst/test2> ?o . }" +
        "";

    List<Resource> results = Lists.newArrayList();

    dao.runSparql(new ParameterizedSparqlString(query),
        Collections.emptyMap(), Collections.emptyMap(),
        x -> results.add(x.getResource("?s")));

    assertEquals(1, results.size());
    assertEquals("http://a.tst/test1", results.get(0).getURI());
  }

  @Test
  void readBySubjectPredicateAndObject() {
    JenaSparqlDAO dao = this.dao;

    dao.store(URI.create("http://a.tst/test1"), URI.create("http://a.tst/test2"),
        URI.create("http://a.tst/test3"));

    List<Resource> results = dao
        .readSubjectByPredicateAndObject(URI.create("http://a.tst/test2"),
            URI.create("http://a.tst/test3"));

    assertEquals(1, results.size());
    assertEquals("http://a.tst/test1", results.get(0).getURI());
  }

  @Test
  void readObjectBySubjectAndPredicate() {
    JenaSparqlDAO dao = this.dao;

    dao.store(URI.create("http://a.tst/test1"), URI.create("http://a.tst/test2"),
        URI.create("http://a.tst/test3"));

    List<Resource> results = dao
        .readObjectBySubjectAndPredicate(URI.create("http://a.tst/test1"),
            URI.create("http://a.tst/test2"));

    assertEquals(1, results.size());
    assertEquals("http://a.tst/test3", results.get(0).getURI());
  }

  @Test
  void readSubjectByPredicate() {
    JenaSparqlDAO dao = this.dao;

    dao.store(URI.create("http://a.tst/test1"), URI.create("http://a.tst/test2"),
        URI.create("http://a.tst/test3"));

    List<Resource> results = dao.readSubjectByPredicate(URI.create("http://a.tst/test2"));

    assertEquals(1, results.size());
    assertEquals("http://a.tst/test1", results.get(0).getURI());
  }

  @Test
  void readSubjectByObject() {
    JenaSparqlDAO dao = this.dao;

    dao.store(URI.create("http://a.tst/test1"), URI.create("http://a.tst/test2"),
        URI.create("http://a.tst/test3"));

    List<Resource> results = dao.readSubjectByObject(URI.create("http://a.tst/test3"));

    assertEquals(1, results.size());
    assertEquals("http://a.tst/test1", results.get(0).getURI());
  }

  @Test
  void testTruncate() {
    JenaSparqlDAO dao = this.dao;
    int tBoxSize = dao.getKnowledgeGraphHolder().getTBoxTriples().size();

    assertEquals(tBoxSize, kgHolder.testGetModel().size());

    dao.store(URI.create("http://a.tst/test1"), URI.create("http://a.tst/test2"),
        URI.create("http://a.tst/test3"));

    assertEquals(1 + tBoxSize, kgHolder.testGetModel().size());

    dao.reinitialize();

    assertEquals(tBoxSize, kgHolder.testGetModel().size());
  }

}