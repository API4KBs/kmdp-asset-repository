package edu.mayo.kmdp.repository.asset.index.sparql;

import static edu.mayo.kmdp.repository.asset.index.sparql.SparqlIndex.InternalQueryManager.TRIPLE_OBJECT_QUERY;
import static edu.mayo.kmdp.repository.asset.index.sparql.SparqlIndex.InternalQueryManager.TRIPLE_SUBJECT_QUERY;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.PreDestroy;
import org.apache.jena.graph.GraphEvents;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Basic in/out functions for interacting with a Jena triple store.
 */
@Component
public class JenaSparqlDAO {

  private static final Logger logger = LoggerFactory.getLogger(JenaSparqlDAO.class);

  @Autowired
  KnowledgeGraphHolder knowledgeGraphHolder;

  /**
   * Default constructor
   */
  public JenaSparqlDAO() {
    // default constructor
  }

  /**
   * Testing constructor
   */
  public JenaSparqlDAO(
      KnowledgeGraphHolder graphHolder) {
    this.knowledgeGraphHolder = graphHolder;
  }

  protected void reinitialize() {
    knowledgeGraphHolder.resetGraph();
  }

  @PreDestroy
  public void shutdown() {
    knowledgeGraphHolder.persistGraph();
    getKnowledgeGraph().close();
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

    getKnowledgeGraph().notifyEvent(GraphEvents.startRead);
    try {
      getKnowledgeGraph().add(s);
    } finally {
      getKnowledgeGraph().notifyEvent(GraphEvents.finishRead);
    }
  }

  /**
   * Store a list of RDF Statements.
   *
   * @param statements the triples to store
   */
  public void store(List<Statement> statements) {
    getKnowledgeGraph().notifyEvent(GraphEvents.startRead);
    try {
      getKnowledgeGraph().add(statements);
    } finally {
      getKnowledgeGraph().notifyEvent(GraphEvents.finishRead);
    }
  }

  /**
   * Remove a list of RDF Statements.
   *
   * @param statements the triples to remove
   */
  public void remove(List<Statement> statements) {
    getKnowledgeGraph().notifyEvent(GraphEvents.startRead);
    try {
      getKnowledgeGraph().remove(statements);
    } finally {
      getKnowledgeGraph().notifyEvent(GraphEvents.finishRead);
    }
  }

  /**
   * Remove all the statements that share a common subject
   *
   * @param subjectURI the subject S* such that any triple <S* P O> will be removed
   */
  public void removeBySubject(String subjectURI) {
    List<Statement> stats = new LinkedList<>();
    getKnowledgeGraph().listStatements(
        new SimpleSelector() {
          @Override
          public boolean test(Statement s) {
            return s.getSubject().getURI().equals(subjectURI);
          }
        }).forEachRemaining(stats::add);
    remove(stats);
  }

  /**
   * Remove all the statements that share any one of a common set of subjects
   *
   * @param subjectURIs the subjects S* such that any triple <S* P O> will be removed
   */
  public void removeBySubjects(Set<String> subjectURIs) {
    List<Statement> stats = new LinkedList<>();
    getKnowledgeGraph().listStatements(
        new SimpleSelector() {
          @Override
          public boolean test(Statement s) {
            return subjectURIs.stream().anyMatch(id -> s.getSubject().getURI().equals(id));
          }
        }).forEachRemaining(stats::add);
    remove(stats);
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

    try (QueryExecution qexec = QueryExecutionFactory.create(pss.asQuery(), getKnowledgeGraph())) {
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
    return getKnowledgeGraph().listStatements().toList();
  }

  /**
   * Returns true if a statement (s,p,o) exists
   *
   * @param st The (s,p,o) statement
   * @return true if (s,p,o) is a statement that is part of the model
   */
  public boolean checkStatementExists(Statement st) {
    return getKnowledgeGraph().contains(st);
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

  /**
   * Testing purposes only
   *
   * @return
   */
  protected Model getKnowledgeGraph() {
    return this.knowledgeGraphHolder.getJenaModel();
  }


}
