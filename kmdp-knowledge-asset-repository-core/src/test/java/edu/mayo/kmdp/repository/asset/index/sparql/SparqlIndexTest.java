package edu.mayo.kmdp.repository.asset.index.sparql;

import static edu.mayo.kmdp.repository.asset.index.sparql.KnowledgeGraphHolder.newKnowledgeGraphHolder;
import static edu.mayo.kmdp.repository.asset.index.sparql.KnowledgeGraphInfo.newKnowledgeGraphInfo;
import static edu.mayo.kmdp.repository.asset.index.sparql.SparqlIndex.newSparqlIndex;
import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties;
import edu.mayo.kmdp.repository.artifact.jpa.JPAKnowledgeArtifactRepository;
import edu.mayo.kmdp.repository.artifact.jpa.JPAKnowledgeArtifactRepositoryService;
import java.net.URI;
import java.util.Set;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries;

class SparqlIndexTest {

  private JenaSparqlDAO dao;

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
    KnowledgeGraphHolder kgHelper = newKnowledgeGraphHolder(mockRepo, newKnowledgeGraphInfo());
    JenaSparqlDAO dao = new JenaSparqlDAO(kgHelper);

    this.dao = dao;

    return dao;
  }

  @Test
  void testGetRelatedAssets() {
    SparqlIndex index = newSparqlIndex(this.getDao(), newKnowledgeGraphInfo());

    URI uri1 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/1/versions/1");
    URI uri2 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/2/versions/1");
    URI uri3 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/3/versions/1");
    URI uri4 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/4/versions/1");

    dao.store(uri1, DependencyTypeSeries.Depends_On.getReferentId(), uri2);
    dao.store(uri2, DependencyTypeSeries.Depends_On.getReferentId(), uri3);
    dao.store(uri3, DependencyTypeSeries.Depends_On.getReferentId(), uri4);

    ResourceIdentifier pointer = SemanticIdentifier.newVersionId(uri1);

    Set<ResourceIdentifier> related = index.getRelatedAssets(pointer);

    assertEquals(4, related.size());
  }

  @Test
  void testGetRelatedAssetsWithPredicate() {
    SparqlIndex index = newSparqlIndex(this.getDao(), newKnowledgeGraphInfo());

    URI uri1 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/1/versions/1");
    URI uri2 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/2/versions/1");
    URI uri3 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/3/versions/1");
    URI uri4 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/4/versions/1");

    dao.store(uri1, DependencyTypeSeries.Depends_On.getReferentId(), uri2);
    dao.store(uri2, DependencyTypeSeries.Depends_On.getReferentId(), uri3);
    dao.store(uri3, DependencyTypeSeries.Depends_On.getReferentId(), uri4);

    ResourceIdentifier pointer = SemanticIdentifier.newVersionId(uri1);

    Set<ResourceIdentifier> related = index.getRelatedAssets(pointer, DependencyTypeSeries.Depends_On.getReferentId());

    assertEquals(4, related.size());
  }

  @Test
  void testGetRelatedAssetsDifferentPredicate() {
    SparqlIndex index = newSparqlIndex(this.getDao(), newKnowledgeGraphInfo());

    URI uri1 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/1/versions/1");
    URI uri2 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/2/versions/1");
    URI uri3 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/3/versions/1");
    URI uri4 = URI.create("https://clinicalknowledgemanagement.mayo.edu/assets/4/versions/1");

    dao.store(uri1, DependencyTypeSeries.Depends_On.getReferentId(), uri2);
    dao.store(uri2, DependencyTypeSeries.Imports.getReferentId(), uri3);
    dao.store(uri3, DependencyTypeSeries.Depends_On.getReferentId(), uri4);

    ResourceIdentifier pointer = SemanticIdentifier.newVersionId(uri1);

    Set<ResourceIdentifier> related = index.getRelatedAssets(pointer);

    assertEquals(4, related.size());
  }

}