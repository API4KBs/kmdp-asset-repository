package edu.mayo.kmdp.repository.asset.index.sparql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.mayo.ontology.taxonomies.kao.rel.dependencyreltype.DependencyTypeSeries;
import java.net.URI;
import java.util.Set;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.id.ResourceIdentifier;
import org.omg.spec.api4kp._1_0.id.SemanticIdentifier;

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

    dao.store(uri1, DependencyTypeSeries.Depends_On.getRef(), uri2);
    dao.store(uri2, DependencyTypeSeries.Depends_On.getRef(), uri3);
    dao.store(uri3, DependencyTypeSeries.Depends_On.getRef(), uri4);

    ResourceIdentifier pointer = SemanticIdentifier.newVersionId(uri1);

    Set<ResourceIdentifier> related = index.getRelatedAssets(pointer);

    assertEquals(4, related.size());
  }

  @Test
  public void testGetRelatedAssetsWithPredicate() {
    SparqlIndex index = new SparqlIndex(this.getDao());

    URI uri1 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/1/versions/1");
    URI uri2 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/2/versions/1");
    URI uri3 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/3/versions/1");
    URI uri4 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/4/versions/1");

    dao.store(uri1, DependencyTypeSeries.Depends_On.getRef(), uri2);
    dao.store(uri2, DependencyTypeSeries.Depends_On.getRef(), uri3);
    dao.store(uri3, DependencyTypeSeries.Depends_On.getRef(), uri4);

    ResourceIdentifier pointer = SemanticIdentifier.newVersionId(uri1);

    Set<ResourceIdentifier> related = index.getRelatedAssets(pointer, DependencyTypeSeries.Depends_On.getRef());

    assertEquals(4, related.size());
  }

  @Test
  public void testGetRelatedAssetsDifferentPredicate() {
    SparqlIndex index = new SparqlIndex(this.getDao());

    URI uri1 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/1/versions/1");
    URI uri2 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/2/versions/1");
    URI uri3 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/3/versions/1");
    URI uri4 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/4/versions/1");

    dao.store(uri1, DependencyTypeSeries.Depends_On.getRef(), uri2);
    dao.store(uri2, DependencyTypeSeries.Imports.getRef(), uri3);
    dao.store(uri3, DependencyTypeSeries.Depends_On.getRef(), uri4);

    ResourceIdentifier pointer = SemanticIdentifier.newVersionId(uri1);

    Set<ResourceIdentifier> related = index.getRelatedAssets(pointer);

    assertEquals(4, related.size());
  }

}