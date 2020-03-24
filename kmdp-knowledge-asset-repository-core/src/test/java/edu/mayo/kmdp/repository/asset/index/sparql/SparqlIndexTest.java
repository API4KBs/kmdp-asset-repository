package edu.mayo.kmdp.repository.asset.index.sparql;

import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.repository.asset.index.IndexPointer;
import edu.mayo.ontology.taxonomies.kao.rel.dependencyreltype.DependencyTypeSeries;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SparqlIndexTest {

  private JenaSparqlDao dao;

  private JenaSparqlDao getDao() {
    JdbcDataSource ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:test");
    ds.setUser("sa");
    ds.setPassword("sa");

    JenaSparqlDao dao = new JenaSparqlDao(ds, true);

    this.dao = dao;

    return dao;
  }

  @Test
  public void testGetRelatedAssets() {
    SparqlIndex index = new SparqlIndex(this.getDao());

    URI uri1 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/1/versions/1");
    URI uri2 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/2/versions/1");
    URI uri3 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/3/versions/1");
    URI uri4 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/4/versions/1");

    dao.store(uri1, DependencyTypeSeries.Depends_On.getConceptId(), uri2);
    dao.store(uri2, DependencyTypeSeries.Depends_On.getConceptId(), uri3);
    dao.store(uri3, DependencyTypeSeries.Depends_On.getConceptId(), uri4);

    IndexPointer pointer = new IndexPointer(DatatypeHelper.toURIIDentifier(uri1.toString()));

    Set<IndexPointer> related = index.getRelatedAssets(pointer);

    assertEquals(4, related.size());
  }

  @Test
  public void testGetRelatedAssetsWithPredicate() {
    SparqlIndex index = new SparqlIndex(this.getDao());

    URI uri1 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/1/versions/1");
    URI uri2 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/2/versions/1");
    URI uri3 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/3/versions/1");
    URI uri4 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/4/versions/1");

    dao.store(uri1, DependencyTypeSeries.Depends_On.getConceptId(), uri2);
    dao.store(uri2, DependencyTypeSeries.Depends_On.getConceptId(), uri3);
    dao.store(uri3, DependencyTypeSeries.Depends_On.getConceptId(), uri4);

    IndexPointer pointer = new IndexPointer(DatatypeHelper.toURIIDentifier(uri1.toString()));

    Set<IndexPointer> related = index.getRelatedAssets(pointer, DependencyTypeSeries.Depends_On.getConceptId());

    assertEquals(4, related.size());
  }

  @Test
  public void testGetRelatedAssetsDifferentPredicate() {
    SparqlIndex index = new SparqlIndex(this.getDao());

    URI uri1 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/1/versions/1");
    URI uri2 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/2/versions/1");
    URI uri3 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/3/versions/1");
    URI uri4 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/4/versions/1");

    dao.store(uri1, DependencyTypeSeries.Depends_On.getConceptId(), uri2);
    dao.store(uri2, DependencyTypeSeries.Imports.getConceptId(), uri3);
    dao.store(uri3, DependencyTypeSeries.Depends_On.getConceptId(), uri4);

    IndexPointer pointer = new IndexPointer(DatatypeHelper.toURIIDentifier(uri1.toString()));

    Set<IndexPointer> related = index.getRelatedAssets(pointer);

    assertEquals(4, related.size());
  }

}