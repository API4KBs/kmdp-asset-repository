package edu.mayo.kmdp.repository.asset.index.sparql;

import static edu.mayo.kmdp.repository.asset.index.sparql.SparqlIndex.InternalQueryManager.TRIPLE_OBJECT_QUERY;
import static edu.mayo.kmdp.repository.asset.index.sparql.SparqlIndex.InternalQueryManager.TRIPLE_SUBJECT_QUERY;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.IdentifierConstants.VERSION_LATEST;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.net.URI;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.GraphEvents;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sdb.SDBFactory;
import org.apache.jena.sdb.Store;
import org.apache.jena.sdb.StoreDesc;
import org.apache.jena.sdb.sql.SDBConnection;
import org.apache.jena.sdb.sql.SDBExceptionSQL;
import org.apache.jena.sdb.store.DatabaseType;
import org.apache.jena.sdb.store.LayoutType;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeBase;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder;
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

  @Autowired
  private DataSource dataSource;

  private Model model;

  private KnowledgeBase kBase;

  private Store store;

  public JenaSparqlDao() {
    //
  }

  /**
   * Create a new DAO instance.
   *
   * @param dataSource           a SQL data source for persistence
   */
  public JenaSparqlDao(DataSource dataSource) {
    this.dataSource = dataSource;
    this.init();
  }

  /**
   * Create a completely in-memory Jena store.
   * <p>
   * NOTE: This should be only used for testing purposes. For general use, see the constructors with
   * the {@link DataSource} arguments.
   *
   * @return a self-contained DAO that does not require a DB connection
   */
  public static JenaSparqlDao inMemoryDao() {
    JenaSparqlDao dao = new JenaSparqlDao();
    dao.model = ModelFactory.createDefaultModel();

    return dao;
  }


  protected void reinitialize() {
    if (store != null) {
      // inMemoryDAOs do not have a store attached
      this.store.getTableFormatter().truncate();
    }
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
        type = DatabaseType
            .fetch(JdbcUtils.extractDatabaseMetaData(this.dataSource, "getDatabaseProductName"));
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

    try {
      long n = store.getSize();
      logger.info("TRIPLE store detected, with {} triples", n);
    } catch (SDBExceptionSQL sqle) {
      logger.warn(sqle.getMessage());
      logger.warn("Please check that your DB is setup properly");
      this.store.getTableFormatter().create();
    }

    this.model = SDBFactory.connectDefaultModel(store);
  }

  /**
   * Subclasses can register database types that aren't included here in the Jetan {@link
   * DatabaseType} enum.
   *
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
   * Store a single RDF triple S,P,O. Used in testing only
   *
   * @param subject   S
   * @param predicate P
   * @param object    O
   */
  public void store(URI subject, URI predicate, URI object) {
    Statement s = ResourceFactory.createStatement(
        createResource(subject.toString()),
        ResourceFactory.createProperty(predicate.toString()),
        createResource(object.toString()));

    this.model.notifyEvent(GraphEvents.startRead);
    try {
      this.model.add(s);
    } finally {
      this.model.notifyEvent(GraphEvents.finishRead);
    }
  }

  /**
   * Store a list of RDF Statements.
   *
   * @param statements the triples to store
   */
  public void store(List<Statement> statements) {
    this.model.notifyEvent(GraphEvents.startRead);
    try {
      this.model.add(statements);
    } finally {
      this.model.notifyEvent(GraphEvents.finishRead);
    }
  }

  /**
   * Run a custom SPARQL query.
   *
   * @param pss
   * @param params
   * @param consumer
   */
  public void runSparql(ParameterizedSparqlString pss, Map<String, URI> params,
      Map<String, Literal> literalParams, Consumer<QuerySolution> consumer) {
    params.forEach((key, value) -> pss.setIri(key, value.toString()));
    literalParams.forEach(pss::setLiteral);

    try (QueryExecution qexec = QueryExecutionFactory.create(pss.asQuery(), this.model)) {
      ResultSet rs = qexec.execSelect();
      rs.forEachRemaining(consumer);
    }

    pss.clearParams();
  }

  /**
   * Read Subject by Predicate and Object:
   * <p>
   * -> select ?s where { ?s ?p ?o . } for given ?p and ?o
   *
   * @param predicate ?p
   * @param object    ?o
   * @return ?s
   */
  public List<Resource> readSubjectByPredicateAndObject(URI predicate, URI object) {
    Map<String, URI> params = Maps.newHashMap();
    params.put("?p", predicate);
    params.put("?o", object);

    List<Resource> resourceList = Lists.newArrayList();

    this.runSparql(TRIPLE_SUBJECT_QUERY, params, Collections.emptyMap(),
        result -> resourceList.add(result.getResource("?s")));

    return resourceList;
  }

  /**
   * Read by Object by Subject and Predicate:
   * <p>
   * --> select ?o where { ?s ?p ?o . } for given ?s and ?p
   *
   * @param subject   ?s
   * @param predicate ?p
   * @return ?o
   */
  public List<Resource> readObjectBySubjectAndPredicate(URI subject, URI predicate) {

    Map<String, URI> params = Maps.newHashMap();
    params.put("?p", predicate);
    params.put("?s", subject);

    List<Resource> resourceList = Lists.newArrayList();

    this.runSparql(TRIPLE_OBJECT_QUERY, params, Collections.emptyMap(),
        result -> resourceList.add(result.getResource("?o")));

    return resourceList;
  }

  /**
   * Read by Object by Subject and Predicate:
   * <p>
   * --> select ?o where { ?s ?p ?o . } for given ?s and ?p
   *
   * @param subject   ?s
   * @param predicate ?p
   * @return ?o
   */
  public List<Literal> readValueBySubjectAndPredicate(URI subject, URI predicate) {
    Map<String, URI> params = Maps.newHashMap();
    params.put("?p", predicate);
    params.put("?s", subject);

    List<Literal> valueList = Lists.newArrayList();

    this.runSparql(TRIPLE_OBJECT_QUERY, params, Collections.emptyMap(),
        result -> valueList.add(result.getLiteral("?o")));

    return valueList;
  }

  /**
   * Read Subject by Predicate:
   * <p>
   * --> select ?s where { ?s ?p ?o . } for given ?p
   *
   * @param predicate ?p
   * @return ?s
   */
  public List<Resource> readSubjectByPredicate(URI predicate) {
    Map<String, URI> params = Maps.newHashMap();
    params.put("?p", predicate);

    List<Resource> resourceList = Lists.newArrayList();

    this.runSparql(TRIPLE_SUBJECT_QUERY, params, Collections.emptyMap(),
        result -> resourceList.add(result.getResource("?s")));

    return resourceList;
  }

  /**
   * Read All:
   * <p>
   * --> select ?s ?p ?o
   *
   * @return ?s
   */
  public List<Statement> readAll() {
    return model.listStatements().toList();
  }

  /**
   * Returns true if a statement (s,p,o) exists
   *
   * @param st The (s,p,o) statement
   * @return true if (s,p,o) is a statement that is part of the model
   */
  public boolean checkStatementExists(Statement st) {
    return model.contains(st);
  }

  /**
   * Read Subject by Object:
   * <p>
   * --> select ?s where { ?s ?p ?o . } for given ?o
   *
   * @param object ?o
   * @return ?s
   */
  public List<Resource> readSubjectByObject(URI object) {
    Map<String, URI> params = Maps.newHashMap();
    params.put("?o", object);

    List<Resource> resourceList = Lists.newArrayList();

    this.runSparql(TRIPLE_SUBJECT_QUERY, params, Collections.emptyMap(),
        result -> resourceList.add(result.getResource("?s")));

    return resourceList;
  }

  protected Model getModel() {
    return model;
  }

  protected Store getStore() {
    return store;
  }

  @Override
  public Answer<KnowledgeBase> getKnowledgeBase(UUID kbaseId, String versionTag, String params) {
    if (this.getKnowledgeBase().getKbaseId().getUuid().equals(kbaseId)) {
      return Answer.of(getKnowledgeBase());
    }
    return Answer.notFound();
  }

  public KnowledgeBase getKnowledgeBase() {
    if (this.kBase == null) {
      lazyInitializeKnowledgeBase();
    }
    return kBase;
  }

  private void lazyInitializeKnowledgeBase() {
    this.kBase = new KnowledgeBase()
        .withManifestation(AbstractCarrier.ofTree(model)
            .withAssetId(SurrogateBuilder.randomAssetId())
            .withArtifactId(SurrogateBuilder.randomArtifactId())
            .withRepresentation(rep(OWL_2))
        )
        .withKbaseId(SemanticIdentifier.newIdAsPointer(UUID.randomUUID(), VERSION_LATEST));
  }

}
