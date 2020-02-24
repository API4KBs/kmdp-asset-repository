package edu.mayo.kmdp.repository.asset.index.sparql;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sdb.SDBFactory;
import org.apache.jena.sdb.Store;
import org.apache.jena.sdb.StoreDesc;
import org.apache.jena.sdb.sql.SDBConnection;
import org.apache.jena.sdb.store.DatabaseType;
import org.apache.jena.sdb.store.LayoutType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Basic in/out functions for interacting with a Jena triple store.
 */
@Component
public class JenaSparqlDao {

  /**
   * If this is set, it will be used to determine the database type.
   *
   * If not, the server will try to guess.
   */
  @Value("${databaseType:}")
  private String databaseType;

  @Autowired
  private DataSource dataSource;

  private Model model;

  private Store store;

  public JenaSparqlDao() {
    //
  }

  public JenaSparqlDao(DataSource dataSource) {
    this(dataSource, false);
  }

  /**
   * Create a new DAO instance.
   *
   * IMPORTANT: setting 'clearAllTables' to true will really drop and recreate all tables.
   * Don't set that to true unless you REALLY need to (for example, for testing only).
   * If you set this to true you will lose any existing data.
   *
   * @param dataSource
   * @param clearAllTables
   */
  public JenaSparqlDao(DataSource dataSource, boolean clearAllTables) {
    this.dataSource = dataSource;
    this.init();

    if (clearAllTables) {
      this.store.getTableFormatter().create();
    }
  }

  /**
   * Initialize the data store. This will create tables/indexes if necessary.
   */
  @PostConstruct
  public void init() {
    this.registerNewDatabaseTypes((name, type) -> DatabaseType.registerName(name, type));

    DatabaseType type;

    if (StringUtils.isNotBlank(this.databaseType)) {
      type = DatabaseType.fetch(this.databaseType);
    } else {
      try {
        type = DatabaseType.fetch(JdbcUtils.extractDatabaseMetaData(this.dataSource, "getDatabaseProductName"));
      } catch (MetaDataAccessException e) {
        throw new RuntimeException("Could not determine database type.", e);
      }
    }

    StoreDesc storeDesc = new StoreDesc(LayoutType.LayoutTripleNodesHash, type);

    SDBConnection conn;
    try {
      conn = new SDBConnection(this.dataSource);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    this.store = SDBFactory.connectStore(conn, storeDesc);

    this.model = SDBFactory.connectDefaultModel(store);
  }

  /**
   * Subclasses can register database types that aren't included here in the
   * Jetan {@link DatabaseType} enum.
   * @param consumer
   */
  protected void registerNewDatabaseTypes(BiConsumer<String, DatabaseType> consumer) {
    consumer.accept("Microsoft SQL Server", DatabaseType.SQLServer);
  }

  @PreDestroy
  public void shutdown() {
    this.model.close();
    this.store.close();
  }

  /**
   * Store a single RDF S,P,O.
   *
   * @param subject
   * @param predicate
   * @param object
   */
  public void store(URI subject, URI predicate, URI object) {
    Statement s = ResourceFactory.createStatement(
            ResourceFactory.createResource(subject.toString()),
            ResourceFactory.createProperty(predicate.toString()),
            ResourceFactory.createResource(object.toString()));

    this.model.add(s);
  }

  /**
   * Store a singe RDF Statement.
   *
   * @param statement
   */
  public void store(Statement statement) {
    this.model.add(statement);
  }

  /**
   * Store a list of RDF Statements.
   *
   * @param statements
   */
  public void store(List<Statement> statements) {
    this.model.add(statements);
  }

  /**
   * Run a custom SPARQL query.
   *
   * @param sparql
   * @param params
   * @param consumer
   */
  public void runSparql(String sparql, Map<String, String> params, Consumer<QuerySolution> consumer) {
    ParameterizedSparqlString pss = new ParameterizedSparqlString(sparql);

    if (params != null) {
      params.entrySet().stream().forEach(entry -> {
        pss.setIri(entry.getKey(), entry.getValue());
      });
    }

    QueryExecution qexec = QueryExecutionFactory.create(pss.toString(), this.model);

    ResultSet rs = qexec.execSelect();

    rs.forEachRemaining(consumer);
  }

  /**
   * Read Subject by Predicate and Object:
   *
   * -> select ?s where { ?s ?p ?o . }
   *
   * @param predicate
   * @param object
   * @return
   */
  public List<Resource> readSubjectByPredicateAndObject(URI predicate, URI object) {
    String query = "select ?s where { ?s ?p ?o . }";

    Map<String, String> params = Maps.newHashMap();
    params.put("?p", predicate.toString());
    params.put("?o", object.toString());

    List<Resource> resourceList = Lists.newArrayList();

    this.runSparql(query, params, (result) -> resourceList.add(result.getResource("?s")));

    return resourceList;
  }

  /**
   * Read by Object by Subject and Predicate:
   *
   * --> select ?o where { ?s ?p  ?o . }
   *
   * @param subject
   * @param predicate
   * @return
   */
  public List<Resource> readObjectBySubjectAndPredicate(URI subject, URI predicate) {
    String query = "select ?o where { ?s ?p  ?o . }";

    Map<String, String> params = Maps.newHashMap();
    params.put("?p", predicate.toString());
    params.put("?s", subject.toString());

    List<Resource> resourceList = Lists.newArrayList();

    this.runSparql(query, params, (result) -> resourceList.add(result.getResource("?o")));

    return resourceList;
  }

  /**
   * Read Subject by Predicate:
   *
   * --> select ?s where { ?s ?p  ?o . }
   *
   * @param predicate
   * @return
   */
  public List<Resource> readSubjectByPredicate(URI predicate) {
    String query = "select ?s where { ?s ?p  ?o . }";

    Map<String, String> params = Maps.newHashMap();
    params.put("?p", predicate.toString());

    List<Resource> resourceList = Lists.newArrayList();

    this.runSparql(query, params, (result) -> resourceList.add(result.getResource("?s")));

    return resourceList;
  }

  protected Model getModel() {
    return model;
  }

  protected Store getStore() {
    return store;
  }

}
