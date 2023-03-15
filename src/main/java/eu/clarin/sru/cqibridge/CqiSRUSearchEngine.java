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
import eu.clarin.sru.server.fcs.ResourceInfoInventory;
import eu.clarin.sru.server.fcs.SimpleEndpointSearchEngineBase;
import eu.clarin.sru.server.fcs.XMLStreamWriterHelper;
import eu.clarin.sru.server.fcs.utils.SimpleResourceInfoInventoryParser;
import java.net.MalformedURLException;
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
    private static final String RESOURCE_INFO_INVENTORY_URL = "/WEB-INF/resource-info.xml";

    private static final String CQI_SUPPORTED_RELATION_CQL_1_1 = "scr";
    private static final String CQI_SUPPORTED_RELATION_CQL_1_2 = "=";
    private static final String CQI_SUPPORTED_RELATION_EXACT = "exact";
    private static final String INDEX_CQL_SERVERCHOICE = "cql.serverChoice";
    private static final String INDEX_FCS_WORDS = "words";
    private static final String CLARIN_FCS_RECORD_SCHEMA = "http://clarin.eu/fcs/1.0";
    private static final Pattern SPACE_PATTERN = Pattern.compile("\\s+");
    private static final String WORD_POSITIONAL_ATTRIBUTE = "word";
    private static final String CONTEXT_STRUCTURAL_ATTRIBUTE = "s";

    private static final Logger logger = LoggerFactory.getLogger(CqiSRUSearchEngine.class);

    private CqiClient client;
    private String defaultCorpusName;
    private String defaultCorpusPID;
    private String defaultCorpusRef;

    @Override
    protected void doInit(ServletContext context, SRUServerConfig config, Map<String, String> params)
            throws SRUConfigException {
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
    protected ResourceInfoInventory createResourceInfoInventory(ServletContext context, SRUServerConfig config,
            Map<String, String> params) throws SRUConfigException {
        /*
         * Create a new instance of a class that implements the
         * ResourceInfoInventory interface and return it. The resource info
         * inventory is used for endpoint resource enumeration (see CLARIN FCS
         * specification)
         */
        try {
            return SimpleResourceInfoInventoryParser.parse(context.getResource(RESOURCE_INFO_INVENTORY_URL));
        } catch (MalformedURLException e) {
            throw new SRUConfigException("error initializing resource info inventory", e);
        }

    }

    @Override
    public SRUSearchResultSet search(SRUServerConfig config, SRURequest request, SRUDiagnosticList diagnostics)
            throws SRUException {
        /*
         * sanity check: make sure we are asked to return stuff in CLARIN FCS
         * format if a recordSchema is specified.
         */
        final String recordSchemaIdentifier = request.getRecordSchemaIdentifier();
        if ((recordSchemaIdentifier != null) && !recordSchemaIdentifier.equals(CLARIN_FCS_RECORD_SCHEMA)) {
            throw new SRUException(SRUConstants.SRU_UNKNOWN_SCHEMA_FOR_RETRIEVAL, recordSchemaIdentifier,
                    "Record schema \"" + recordSchemaIdentifier + "\" is not supported by this endpoint.");
        }

        /*
         * commence search ...
         */
        final CQLNode query = request.getQuery();
        int startRecord = request.getStartRecord();
        final int maximumRecords = request.getMaximumRecords();

        final String cqpQuery = translateCQLtoCQP(query);
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

            return new SRUSearchResultSet(diagnostics) {
                private int pos = -1;

                @Override
                public int getTotalRecordCount() {
                    return result.size();
                }

                @Override
                public int getRecordCount() {
                    return result.size();
                }

                @Override
                public String getRecordSchemaIdentifier() {
                    return CLARIN_FCS_RECORD_SCHEMA;
                }

                @Override
                public String getRecordIdentifier() {
                    return null;
                }

                @Override
                public boolean nextRecord() {
                    try {
                        return ++pos < maximumRecords && result.next();
                    } catch (CqiClientException e) {
                        throw new NoSuchElementException(e.getMessage());
                    }
                }

                @Override
                public SRUDiagnostic getSurrogateDiagnostic() {
                    return null;
                }

                @Override
                public void writeRecord(XMLStreamWriter writer)
                        throws XMLStreamException {
                    final int contextStart = result.getContextStart();
                    final int contextEnd = result.getContextEnd();
                    final int relMatchStart = result.getMatchStart() - contextStart;
                    final int relMatchEnd = result.getMatchEnd() - contextStart + 1;
                    final int relContextEnd = contextEnd - contextStart + 1;
                    String[] words;
                    try {
                        words = result.getValues(WORD_POSITIONAL_ATTRIBUTE, contextStart, contextEnd);
                    } catch (CqiClientException e) {
                        throw new XMLStreamException("can't obtain the values of the positional attribute '"
                                + WORD_POSITIONAL_ATTRIBUTE + "'", e);
                    }
                    String leftContext = matchToString(words, 0, relMatchStart);
                    String keyWord = matchToString(words, relMatchStart, relMatchEnd);
                    String rightContext = matchToString(words, relMatchEnd, relContextEnd);
                    XMLStreamWriterHelper.writeResourceWithKWICDataView(writer, defaultCorpusPID, defaultCorpusRef,
                            leftContext, keyWord, rightContext);
                }
            };
        } catch (CqiClientException e) {
            logger.error("error processing query", e);
            throw new SRUException(SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
                    "Error processing query (" + e.getMessage() + ").", e);
        }
    }

    private static String matchToString(String[] words, int fromIndex, int toIndex) {
        final StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (int i = fromIndex; i < toIndex; i++) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(' ');
            }
            sb.append(words[i]);
        }
        return sb.toString();
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
}
