package edu.mayo.kmdp.repository.asset.index.sparql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Lists;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties;
import edu.mayo.kmdp.repository.artifact.jpa.JPAKnowledgeArtifactRepository;
import edu.mayo.kmdp.repository.artifact.jpa.JPAKnowledgeArtifactRepositoryService;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Resource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JenaSparqlDaoTest {

  private JenaSparqlDAO getDao() {
    JdbcDataSource ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:test");
    ds.setUser("sa");
    ds.setPassword("sa");

    KnowledgeArtifactRepositoryServerProperties cfg =
        new KnowledgeArtifactRepositoryServerProperties(
            SparqlIndexTest.class.getResourceAsStream("/application.test.properties"));

    JPAKnowledgeArtifactRepository mockRepo =
        new JPAKnowledgeArtifactRepository(JPAKnowledgeArtifactRepositoryService.inMemoryDataSource(), cfg);
    KnowledgeGraphHolder kgHelper = new KnowledgeGraphHolder(mockRepo);

    return new JenaSparqlDAO(kgHelper);
  }

  @BeforeEach
  void reinit() {
    getDao().reinitialize();
  }

  @Test
  void store() {
    JenaSparqlDAO dao = this.getDao();
    int tBoxSize = dao.knowledgeGraphHolder.getTBoxTriples().size();

    assertEquals(tBoxSize, dao.getKnowledgeGraph().size());

    dao.store(URI.create("http://test1"), URI.create("http://test2"), URI.create("http://test3"));

    assertEquals(1 + tBoxSize, dao.getKnowledgeGraph().size());
  }

  @Test
  void runSparql() {
    JenaSparqlDAO dao = this.getDao();

    dao.store(URI.create("http://test1"), URI.create("http://test2"), URI.create("http://test3"));

    String query = "" +
            "select ?s where { ?s <http://test2> ?o . }" +
            "";

    List<Resource> results = Lists.newArrayList();

    dao.runSparql(new ParameterizedSparqlString(query),
        Collections.emptyMap(), Collections.emptyMap(),
        x -> results.add(x.getResource("?s")));

    assertEquals(1, results.size());
    assertEquals("http://test1", results.get(0).getURI());
  }

  @Test
  void readBySubjectPredicateAndObject() {
    JenaSparqlDAO dao = this.getDao();

    dao.store(URI.create("http://test1"), URI.create("http://test2"), URI.create("http://test3"));

    List<Resource> results = dao.readSubjectByPredicateAndObject(URI.create("http://test2"), URI.create("http://test3"));

    assertEquals(1, results.size());
    assertEquals("http://test1", results.get(0).getURI());
  }

  @Test
  void readObjectBySubjectAndPredicate() {
    JenaSparqlDAO dao = this.getDao();

    dao.store(URI.create("http://test1"), URI.create("http://test2"), URI.create("http://test3"));

    List<Resource> results = dao.readObjectBySubjectAndPredicate(URI.create("http://test1"), URI.create("http://test2"));

    assertEquals(1, results.size());
    assertEquals("http://test3", results.get(0).getURI());
  }

  @Test
  void readSubjectByPredicate() {
    JenaSparqlDAO dao = this.getDao();

    dao.store(URI.create("http://test1"), URI.create("http://test2"), URI.create("http://test3"));

    List<Resource> results = dao.readSubjectByPredicate(URI.create("http://test2"));

    assertEquals(1, results.size());
    assertEquals("http://test1", results.get(0).getURI());
  }

  @Test
  void readSubjectByObject() {
    JenaSparqlDAO dao = this.getDao();

    dao.store(URI.create("http://test1"), URI.create("http://test2"), URI.create("http://test3"));

    List<Resource> results = dao.readSubjectByObject(URI.create("http://test3"));

    assertEquals(1, results.size());
    assertEquals("http://test1", results.get(0).getURI());
  }

  @Test
  void testTruncate() {
    JenaSparqlDAO dao = this.getDao();
    int tBoxSize = dao.knowledgeGraphHolder.getTBoxTriples().size();

    assertEquals(tBoxSize, dao.getKnowledgeGraph().size());

    dao.store(URI.create("http://test1"), URI.create("http://test2"), URI.create("http://test3"));

    assertEquals(1 + tBoxSize, dao.getKnowledgeGraph().size());

    dao.reinitialize();

    assertEquals(tBoxSize, dao.getKnowledgeGraph().size());
  }

}