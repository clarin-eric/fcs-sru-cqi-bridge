/**
 * This software is copyright (c) 2012 by - Institut fuer Deutsche Sprache
 * (http://www.ids-mannheim.de), Seminar fuer Sprachwissenschaft
 * (http://www.sfs.uni-tuebingen.de/) This is free software. You can
 * redistribute it and/or modify it under the terms described in the GNU General
 * Public License v3 of which you should have received a copy. Otherwise you can
 * download it from
 *
 * http://www.gnu.org/licenses/gpl-3.0.txt
 *
 * @copyright Seminar fuer Sprachwissenschaft (http://www.sfs.uni-tuebingen.de/)
 *
 * @license http://www.gnu.org/licenses/gpl-3.0.txt GNU General Public License
 * v3
 */
package eu.clarin.sru.cqibridge;

import eu.clarin.cqi.client.CqiClient;
import eu.clarin.cqi.client.CqiClientException;
import eu.clarin.cqi.client.CqiResult;
import eu.clarin.sru.server.*;
import eu.clarin.sru.server.SRUQueryParserRegistry.Builder;
import eu.clarin.sru.server.fcs.Constants;
import eu.clarin.sru.server.fcs.EndpointDescription;
import eu.clarin.sru.server.fcs.SimpleEndpointSearchEngineBase;
import eu.clarin.sru.server.fcs.XMLStreamWriterHelper;
import eu.clarin.sru.server.fcs.utils.SimpleEndpointDescriptionParser;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLRelation;
import org.z3950.zing.cql.CQLTermNode;
import org.z3950.zing.cql.Modifier;

/**
 *
 * @author akislev
 */
public class CqiSRUSearchEngine extends SimpleEndpointSearchEngineBase {

    private static final String PARAM_CQI_SERVER_HOST = "cqi.serverHost";
    private static final String PARAM_CQI_SERVER_PORT = "cqi.serverPort";
    private static final String PARAM_CQI_SERVER_USERNAME = "cqi.serverUsername";
    private static final String PARAM_CQI_SERVER_PASSWORD = "cqi.serverPassword";
    private static final String PARAM_CQI_DEFAULT_CORPUS = "cqi.defaultCorpus";
    private static final String PARAM_CQI_DEFAULT_CORPUS_PID = "cqi.defaultCorpusPID";
    private static final String PARAM_CQI_DEFAULT_CORPUS_REF = "cqi.defaultCorpusRef";
    private static final String PARAM_SUPPORT_LEGACY_KWIC_DATAVIEW = "eu.clarin.sru.cqibridge.supportLegacyKWIC";
    public static final String PARAM_RESOURCE_INVENTORY_URL = "eu.clarin.sru.cqibridge.resourceInventoryURL";
    private static final String RESOURCE_INVENTORY_URL = "/WEB-INF/endpoint-description.xml";

    public static final String CLARIN_FCS_RECORD_SCHEMA = "http://clarin.eu/fcs/resource";

    private static final String CQI_SUPPORTED_RELATION_CQL_1_1 = "scr";
    private static final String CQI_SUPPORTED_RELATION_CQL_1_2 = "=";
    private static final String CQI_SUPPORTED_RELATION_EXACT = "exact";
    private static final String INDEX_CQL_SERVERCHOICE = "cql.serverChoice";
    private static final String INDEX_FCS_WORDS = "words";

    public static final String WORD_POSITIONAL_ATTRIBUTE = "word";
    public static final String CONTEXT_STRUCTURAL_ATTRIBUTE = "s";

    private static final Pattern SPACE_PATTERN = Pattern.compile("\\s+");

    private static final Logger logger = LoggerFactory.getLogger(CqiSRUSearchEngine.class);

    private CqiClient client;
    private String defaultCorpusName;
    private String defaultCorpusPID;
    private String defaultCorpusRef;
    private boolean supportLegacyKWIC;

    @Override
    protected EndpointDescription createEndpointDescription(ServletContext context, SRUServerConfig config,
            Map<String, String> params) throws SRUConfigException {
        try {
            URL url = null;
            String riu = params.get(PARAM_RESOURCE_INVENTORY_URL);
            if ((riu == null) || riu.isEmpty()) {
                url = context.getResource(RESOURCE_INVENTORY_URL);
                logger.debug("using bundled 'endpoint-description.xml' file");
            } else {
                url = new File(riu).toURI().toURL();
                logger.debug("using external file '{}'", riu);
            }

            return SimpleEndpointDescriptionParser.parse(url);
        } catch (MalformedURLException mue) {
            throw new SRUConfigException("Malformed URL for initializing resource info inventory", mue);
        }
    }

    @Override
    protected void doInit(ServletContext context, SRUServerConfig config,
            SRUQueryParserRegistry.Builder queryParserBuilder, Map<String, String> params) throws SRUConfigException {
        logger.info("CqiSRUSearchEngine::doInit {}", config.getPort());
        /*
         * Perform search engine specific initialization in this method, e.g.
         * set up a database connection, etc.
         */
        final String serverHost = params.get(PARAM_CQI_SERVER_HOST);
        if (serverHost == null) {
            throw new SRUConfigException("parameter \"" + PARAM_CQI_SERVER_HOST + "\" is mandatory");
        }
        logger.info("using cqi server host: {}", serverHost);
        final String serverPortString = params.get(PARAM_CQI_SERVER_PORT);
        if (serverPortString == null) {
            throw new SRUConfigException("parameter \"" + PARAM_CQI_SERVER_PORT + "\" is mandatory");
        }
        final int serverPort = Integer.parseInt(serverPortString);
        logger.info("using cqi server port: {}", serverPort);
        final String username = params.get(PARAM_CQI_SERVER_USERNAME);
        if (username == null) {
            throw new SRUConfigException("parameter \"" + PARAM_CQI_SERVER_USERNAME + "\" is mandatory");
        }
        final String password = params.get(PARAM_CQI_SERVER_PASSWORD);
        if (password == null) {
            throw new SRUConfigException("parameter \"" + PARAM_CQI_SERVER_PASSWORD + "\" is mandatory");
        }
        defaultCorpusName = params.get(PARAM_CQI_DEFAULT_CORPUS);
        if (defaultCorpusName == null) {
            throw new SRUConfigException("parameter \"" + PARAM_CQI_DEFAULT_CORPUS + "\" is mandatory");
        }
        defaultCorpusPID = params.get(PARAM_CQI_DEFAULT_CORPUS_PID);
        if (defaultCorpusPID == null) {
            throw new SRUConfigException("parameter \"" + PARAM_CQI_DEFAULT_CORPUS_PID + "\" is mandatory");
        }
        defaultCorpusRef = params.get(PARAM_CQI_DEFAULT_CORPUS_REF);
        if (defaultCorpusRef == null) {
            throw new SRUConfigException("parameter \"" + PARAM_CQI_DEFAULT_CORPUS_REF + "\" is mandatory");
        }

        supportLegacyKWIC = parseBoolean(params.get(PARAM_SUPPORT_LEGACY_KWIC_DATAVIEW));
        logger.info("legacy KWIC dataview support enabled");

        try {
            client = new CqiClient(serverHost, serverPort);
        } catch (CqiClientException ex) {
            throw new SRUConfigException("can't initialize the cqi client", ex);
        }
        try {
            client.connect(username, password);
        } catch (CqiClientException ex) {
            throw new SRUConfigException("can't connect to the cqi server", ex);
        }
    }

    @Override
    protected SRUScanResultSet doScan(SRUServerConfig config, SRURequest request, SRUDiagnosticList diagnostics)
            throws SRUException {
        return null;
    }

    @Override
    public SRUSearchResultSet search(SRUServerConfig config, SRURequest request, SRUDiagnosticList diagnostics)
            throws SRUException {
        /*
         * sanity check: make sure we are asked to return stuff in CLARIN FCS
         * format if a recordSchema is specified.
         */
        final String recordSchemaIdentifier = request.getRecordSchemaIdentifier();
        // this might not be required as the SRU library already checks and handles this
        // based on the sru-server-config.xml file
        if ((recordSchemaIdentifier != null) && !recordSchemaIdentifier.equals(CLARIN_FCS_RECORD_SCHEMA)) {
            logger.debug("record schema = got:{} / supports:{} / same:{}", new Object[] { recordSchemaIdentifier,
                    CLARIN_FCS_RECORD_SCHEMA, recordSchemaIdentifier.equals(CLARIN_FCS_RECORD_SCHEMA) });
            throw new SRUException(SRUConstants.SRU_UNKNOWN_SCHEMA_FOR_RETRIEVAL, recordSchemaIdentifier,
                    "Record schema \"" + recordSchemaIdentifier + "\" is not supported by this endpoint.");
        }

        /*
         * commence search ...
         */

        if (!request.isQueryType(Constants.FCS_QUERY_TYPE_CQL)) {
            /*
             * Got something else we don't support. Send error ...
             */
            throw new SRUException(
                    SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
                    "Queries with queryType '" +
                            request.getQueryType() +
                            "' are not supported by this CLARIN-FCS Endpoint.");
        }
        /*
         * Got a CQL query (either SRU 1.1 or higher).
         * Translate to a proper CQP query ...
         */
        final CQLQueryParser.CQLQuery query = request.getQuery(CQLQueryParser.CQLQuery.class);
        final CQLNode queryNode = query.getParsedQuery();
        final String cqpQuery = translateCQLtoCQP(queryNode);

        int startRecord = (request.getStartRecord() < 1) ? 1 : request.getStartRecord();
        final int maximumRecords = startRecord - 1 + request.getMaximumRecords();
        if (startRecord > 0) {
            startRecord--;
        }

        logger.info("running query = \"{}\", offset = {}, limit = {}, in = {}",
                new Object[] { cqpQuery, startRecord, maximumRecords, defaultCorpusName });
        try {
            final CqiResult result = client.query(defaultCorpusName, cqpQuery, CONTEXT_STRUCTURAL_ATTRIBUTE);
            if ((result.size() > 0 && !result.absolute(startRecord)) || (result.size() == 0 && startRecord > 0)) {
                diagnostics.addDiagnostic(SRUConstants.SRU_FIRST_RECORD_POSITION_OUT_OF_RANGE,
                        Integer.toString(startRecord + 1), null);
            }

            return new CqiSRUSearchResultSet(request, diagnostics, result, defaultCorpusPID, defaultCorpusRef,
                    supportLegacyKWIC);
        } catch (CqiClientException e) {
            logger.error("error processing query", e);
            throw new SRUException(SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
                    "Error processing query (" + e.getMessage() + ").", e);
        }
    }

    private String translateCQLtoCQP(CQLNode query) throws SRUException {
        if (query instanceof CQLTermNode) {
            final CQLTermNode root = (CQLTermNode) query;

            // only allow "cql.serverChoice" and "words" index
            if (!(INDEX_CQL_SERVERCHOICE.equals(root.getIndex()) || INDEX_FCS_WORDS.equals(root.getIndex()))) {
                throw new SRUException(SRUConstants.SRU_UNSUPPORTED_INDEX, root.getIndex(),
                        "Index \"" + root.getIndex() + "\" is not supported.");
            }

            // only allow "=" relation without any modifiers
            final CQLRelation relation = root.getRelation();
            final String baseRel = relation.getBase();
            if (!(CQI_SUPPORTED_RELATION_CQL_1_1.equals(baseRel) || CQI_SUPPORTED_RELATION_CQL_1_2.equals(baseRel)
                    || CQI_SUPPORTED_RELATION_EXACT.equals(baseRel))) {
                throw new SRUException(SRUConstants.SRU_UNSUPPORTED_RELATION,
                        relation.getBase(), "Relation \""
                                + relation.getBase() + "\" is not supported.");
            }
            List<Modifier> modifiers = relation.getModifiers();
            if ((modifiers != null) && !modifiers.isEmpty()) {
                Modifier modifier = modifiers.get(0);
                throw new SRUException(SRUConstants.SRU_UNSUPPORTED_RELATION_MODIFIER, modifier.getValue(),
                        "Relation modifier \"" + modifier.getValue() + "\" is not supported.");
            }

            // check term
            final String term = root.getTerm();
            if ((term == null) || term.isEmpty()) {
                throw new SRUException(SRUConstants.SRU_EMPTY_TERM_UNSUPPORTED, "An empty term is not supported.");
            }
            // convert to cqp by inserting quotes around each token
            return String.format("\"%s\"", SPACE_PATTERN.matcher(term).replaceAll("\" \""));

        }
        throw new SRUException(SRUConstants.SRU_QUERY_FEATURE_UNSUPPORTED,
                "Server currently only supports term-only queries (CQL conformance level 0).");
    }

    /**
     * Convince method for parsing a string to boolean. Values <code>1</code>,
     * <code>true</code>, <code>yes</code> yield a <em>true</em> boolean value
     * as a result, all others (including <code>null</code>) a <em>false</em>
     * boolean value.
     *
     * @param value the string to parse
     * @return <code>true</code> if the supplied string was considered something
     *         representing a <em>true</em> boolean value, <code>false</code>
     *         otherwise
     * @see eu.clarin.sru.server.fcs.SimpleEndpointSearchEngineBase#parseBoolean(String)
     */
    protected static boolean parseBoolean(String value) {
        if (value != null) {
            return value.equals("1") || Boolean.parseBoolean(value);
        }
        return false;
    }
}
