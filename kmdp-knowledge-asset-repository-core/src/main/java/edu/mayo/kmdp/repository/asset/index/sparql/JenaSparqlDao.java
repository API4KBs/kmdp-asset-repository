package edu.mayo.kmdp.repository.asset.index.sparql;

import static edu.mayo.kmdp.repository.asset.SemanticKnowledgeAssetRepository.KB_UUID;
import static org.omg.spec.api4kp._1_0.id.IdentifierConstants.VERSION_LATEST;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.mayo.kmdp.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import edu.mayo.kmdp.metadata.v2.surrogate.SurrogateBuilder;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sdb.SDBFactory;
import org.apache.jena.sdb.Store;
import org.apache.jena.sdb.StoreDesc;
import org.apache.jena.sdb.sql.SDBConnection;
import org.apache.jena.sdb.store.DatabaseType;
import org.apache.jena.sdb.store.LayoutType;
import org.omg.spec.api4kp._1_0.AbstractCarrier;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.id.SemanticIdentifier;
import org.omg.spec.api4kp._1_0.services.KnowledgeBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.stereotype.Component;

/**
 * Basic in/out functions for interacting with a Jena triple store.
 */
@Component
public class JenaSparqlDao implements KnowledgeBaseApiInternal._getKnowledgeBase {

  private static final Logger logger = LoggerFactory.getLogger(JenaSparqlDao.class);

  /**
   * If this is set, it will be used to determine the database type.
   *
   * If not, the server will try to guess.
   */
  @Value("${databaseType:}")
  private String databaseType;

  /**
   * IMPORTANT!
   * If true this will drop and recreate all tables.
   */
  @Value("${clearAndCreateTables:false}")
  private boolean clearAndCreateTables = false;

  @Autowired
  private DataSource dataSource;

  private Model model;

  private KnowledgeBase kBase;

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
   * IMPORTANT: setting 'clearAndCreateTables' to true will really drop and recreate all tables.
   * Don't set that to true unless you REALLY need to (for example, for testing only).
   * If you set this to true you will lose any existing data.
   *
   * @param dataSource a SQL data source for persistence
   * @param clearAndCreateTables if true, resets the DB
   */
  public JenaSparqlDao(DataSource dataSource, boolean clearAndCreateTables) {
    this.dataSource = dataSource;
    this.clearAndCreateTables = clearAndCreateTables;
    this.init();
  }

  /**
   * Create a completely in-memory Jena store.
   *
   * NOTE: This should be only used for testing purposes. For
   * general use, see the constructors with the {@link DataSource} arguments.
   *
   * @return a self-contained DAO that does not require a DB connection
   */
  public static JenaSparqlDao inMemoryDao() {
    JenaSparqlDao dao = new JenaSparqlDao();
    dao.model = ModelFactory.createDefaultModel();

    return dao;
  }

  protected void clearAndCreateTables() {
    this.store.getTableFormatter().create();
  }

  /**
   * Initialize the data store. This will create tables/indexes if necessary.
   */
  @PostConstruct
  public void init() {
    this.registerNewDatabaseTypes(DatabaseType::registerName);

    DatabaseType type;

    if (StringUtils.isNotBlank(this.databaseType)) {
      type = DatabaseType.fetch(this.databaseType);
    } else {
      try {
        type = DatabaseType.fetch(JdbcUtils.extractDatabaseMetaData(this.dataSource, "getDatabaseProductName"));
      } catch (MetaDataAccessException e) {
        throw new BeanInitializationException("Could not determine database type.", e);
      }
    }

    StoreDesc storeDesc = new StoreDesc(LayoutType.LayoutTripleNodesHash, type);

    SDBConnection conn;
    try {
      conn = new SDBConnection(this.dataSource);
    } catch (SQLException e) {
      throw new BeanInitializationException("Error conneting to DataSource", e);
    }

    this.store = SDBFactory.connectStore(conn, storeDesc);

    if (this.clearAndCreateTables) {
      logger.warn("!!! Clearing and recreating all RDF tables. !!!");
      this.clearAndCreateTables();
    }

    this.model = SDBFactory.connectDefaultModel(store);

    this.kBase = new KnowledgeBase()
        .withManifestation(AbstractCarrier.ofTree(model)
            .withAssetId(SurrogateBuilder.randomAssetId())
            .withArtifactId(SurrogateBuilder.randomArtifactId())
        )
        .withKbaseId(SemanticIdentifier.newIdAsPointer(KB_UUID,VERSION_LATEST));
  }

  /**
   * Subclasses can register database types that aren't included here in the
   * Jetan {@link DatabaseType} enum.
   * @param consumer a function that registers a MS SQL Database type
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
   * Store a single RDF triple S,P,O.
   *
   * @param subject S
   * @param predicate P
   * @param object O
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
   * @param statement the triple to store
   */
  public void store(Statement statement) {
    this.model.add(statement);
  }

  /**
   * Store a list of RDF Statements.
   *
   * @param statements the triples to store
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
      params.entrySet().forEach(entry -> pss.setIri(entry.getKey(), entry.getValue()));
    }

    try (QueryExecution qexec = QueryExecutionFactory.create(pss.toString(), this.model)) {
      ResultSet rs = qexec.execSelect();

      rs.forEachRemaining(consumer);
    }
  }

  /**
   * Read Subject by Predicate and Object:
   *
   * -> select ?s where { ?s ?p ?o . } 
   * for given ?p and ?o
   * 
   * @param predicate ?p
   * @param object ?o
   * @return ?s
   */
  public List<Resource> readSubjectByPredicateAndObject(URI predicate, URI object) {
    String query = "select ?s where { ?s ?p ?o . }";

    Map<String, String> params = Maps.newHashMap();
    params.put("?p", predicate.toString());
    params.put("?o", object.toString());

    List<Resource> resourceList = Lists.newArrayList();

    this.runSparql(query, params, result -> resourceList.add(result.getResource("?s")));

    return resourceList;
  }

  /**
   * Read by Object by Subject and Predicate:
   *
   * --> select ?o where { ?s ?p  ?o . }
   * for given ?s and ?p
   *
   * @param subject ?s
   * @param predicate ?p
   * @return ?o
   */
  public List<Resource> readObjectBySubjectAndPredicate(URI subject, URI predicate) {
    String query = "select ?o where { ?s ?p  ?o . }";

    Map<String, String> params = Maps.newHashMap();
    params.put("?p", predicate.toString());
    params.put("?s", subject.toString());

    List<Resource> resourceList = Lists.newArrayList();

    this.runSparql(query, params, result -> resourceList.add(result.getResource("?o")));

    return resourceList;
  }

  /**
   * Read Subject by Predicate:
   *
   * --> select ?s where { ?s ?p  ?o . }
   * for given ?p
   *
   * @param predicate ?p
   * @return ?s
   */
  public List<Resource> readSubjectByPredicate(URI predicate) {
    String query = "select ?s where { ?s ?p  ?o . }";

    Map<String, String> params = Maps.newHashMap();
    params.put("?p", predicate.toString());

    List<Resource> resourceList = Lists.newArrayList();

    this.runSparql(query, params, result -> resourceList.add(result.getResource("?s")));

    return resourceList;
  }
  
  /**
   * Read Subject by Object:
   *
   * --> select ?s where { ?s ?p  ?o . }
   * for given ?o
   * 
   * @param object ?o
   * @return ?s
   */
  public List<Resource> readSubjectByObject(URI object) {
    String query = "select ?s where { ?s ?p  ?o . }";

    Map<String, String> params = Maps.newHashMap();
    params.put("?o", object.toString());

    List<Resource> resourceList = Lists.newArrayList();

    this.runSparql(query, params, result -> resourceList.add(result.getResource("?s")));

    return resourceList;
  }

  protected Model getModel() {
    return model;
  }

  protected Store getStore() {
    return store;
  }

  @Override
  public Answer<KnowledgeBase> getKnowledgeBase(UUID kbaseId, String versionTag) {
    if (KB_UUID.equals(kbaseId)) {
      return Answer.of(kBase);
    }
    return Answer.notFound();
  }

}
