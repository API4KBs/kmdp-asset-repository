package edu.mayo.kmdp.repository.asset.index.sparql;

import com.google.common.collect.Lists;
import org.apache.jena.rdf.model.Resource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JenaSparqlDaoTest {

  private JenaSparqlDao getDao() {
    JdbcDataSource ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:test");
    ds.setUser("sa");
    ds.setPassword("sa");

    JenaSparqlDao dao = new JenaSparqlDao(ds, true);

    return dao;
  }

  @Test
  void store() {
    JenaSparqlDao dao = this.getDao();

    assertEquals(0, dao.getModel().size());

    dao.store(URI.create("http://test1"), URI.create("http://test2"), URI.create("http://test3"));

    assertEquals(1, dao.getModel().size());
  }

  @Test
  void runSparql() {
    JenaSparqlDao dao = this.getDao();

    dao.store(URI.create("http://test1"), URI.create("http://test2"), URI.create("http://test3"));

    String query = "" +
            "select ?s where { ?s ?p ?o . }" +
            "";

    List<Resource> results = Lists.newArrayList();

    dao.runSparql(query, null, (x) -> results.add(x.getResource("?s")));

    assertEquals(1, results.size());
    assertEquals("http://test1", results.get(0).getURI());
  }

  @Test
  void readBySubjectPredicateAndObject() {
    JenaSparqlDao dao = this.getDao();

    dao.store(URI.create("http://test1"), URI.create("http://test2"), URI.create("http://test3"));

    List<Resource> results = dao.readSubjectByPredicateAndObject(URI.create("http://test2"), URI.create("http://test3"));

    assertEquals(1, results.size());
    assertEquals("http://test1", results.get(0).getURI());
  }

  @Test
  void readObjectBySubjectAndPredicate() {
    JenaSparqlDao dao = this.getDao();

    dao.store(URI.create("http://test1"), URI.create("http://test2"), URI.create("http://test3"));

    List<Resource> results = dao.readObjectBySubjectAndPredicate(URI.create("http://test1"), URI.create("http://test2"));

    assertEquals(1, results.size());
    assertEquals("http://test3", results.get(0).getURI());
  }

  @Test
  void readSubjectByPredicate() {
    JenaSparqlDao dao = this.getDao();

    dao.store(URI.create("http://test1"), URI.create("http://test2"), URI.create("http://test3"));

    List<Resource> results = dao.readSubjectByPredicate(URI.create("http://test2"));

    assertEquals(1, results.size());
    assertEquals("http://test1", results.get(0).getURI());
  }

}