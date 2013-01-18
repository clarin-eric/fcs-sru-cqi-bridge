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
import eu.clarin.sru.server.utils.SRUSearchEngineBase;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Pattern;
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
public class CqiSRUSearchEngine extends SRUSearchEngineBase {

    private static final String PARAM_CQI_SERVER_HOST = "cqi.serverHost";
    private static final String PARAM_CQI_SERVER_PORT = "cqi.serverPort";
    private static final String PARAM_CQI_SERVER_USERNAME = "cqi.serverUsername";
    private static final String PARAM_CQI_SERVER_PASSWORD = "cqi.serverPassword";
    private static final String PARAM_CQI_DEFAULT_CORPUS = "cqi.defaultCorpus";
    private static final String PARAM_CQI_DEFAULT_CORPUS_PID = "cqi.defaultCorpusPID";
    private static final String PARAM_CQI_DEFAULT_CORPUS_REF = "cqi.defaultCorpusRef";
    private static final String CQI_SUPPORTED_RELATION_CQL_1_1 = "scr";
    private static final String CQI_SUPPORTED_RELATION_CQL_1_2 = "=";
    private static final String CQI_SUPPORTED_RELATION_EXACT = "exact";
    private static final String INDEX_CQL_SERVERCHOICE = "cql.serverChoice";
    private static final String INDEX_FCS_WORDS = "words";
    private static final String INDEX_FCS_RESOURCE = "fcs.resource";
    private static final String FCS_RESOURCE_TERM_WILDCARD = "*";
    private static final String FCS_RESOURCE_TERM_ANY = "any";
    private static final String FCS_NS = "http://clarin.eu/fcs/1.0";
    private static final String FCS_PREFIX = "fcs";
    private static final String FCS_KWIC_NS = "http://clarin.eu/fcs/1.0/kwic";
    private static final String FCS_KWIC_PREFIX = "kwic";
    private static final String CLARIN_FCS_RECORD_SCHEMA = FCS_NS;
    private static final String X_CMD_RESOURCE_INFO = "x-cmd-resource-info";
    private static final Pattern SPACE_PATTERN = Pattern.compile("\\s+");
    private static final String WORD_POSITIONAL_ATTRIBUTE = "word";
    private static final String CONTEXT_STRUCTURAL_ATTRIBUTE = "s";
    private static final Logger logger =
            LoggerFactory.getLogger(CqiSRUSearchEngine.class);
    private static final ResourceInfo[] RESOURCE_INFOS = new ResourceInfo[]{
        new ResourceInfo("tueba-ddc", -1, false,
        Arrays.asList("en", "TuebaDDC",
        "de", "TübaDDC"),
        Arrays.asList("en", "Tübingen Treebank of Written German - Diachronic Corpus.",
        "de", "Tübingen Baumbank des Deutschen - Diachrones Corpus."),
        Arrays.asList("deu"),
        Arrays.asList("text", "fcs.words")),};
    private CqiClient client;
    private String defaultCorpusName;
    private String defaultCorpusPID;
    private String defaultCorpusRef;

    @Override
    public void init(SRUServerConfig config, Map<String, String> params)
            throws SRUConfigException {
        final String serverHost = params.get(PARAM_CQI_SERVER_HOST);
        if (serverHost == null) {
            throw new SRUConfigException("parameter \""
                    + PARAM_CQI_SERVER_HOST + "\" is mandatory");
        }
        logger.info("using cqi server host: {}", serverHost);
        final String serverPortString = params.get(PARAM_CQI_SERVER_PORT);
        if (serverPortString == null) {
            throw new SRUConfigException("parameter \""
                    + PARAM_CQI_SERVER_PORT + "\" is mandatory");
        }
        final int serverPort = Integer.parseInt(serverPortString);
        logger.info("using cqi server port: {}", serverPort);
        final String username = params.get(PARAM_CQI_SERVER_USERNAME);
        if (username == null) {
            throw new SRUConfigException("parameter \""
                    + PARAM_CQI_SERVER_USERNAME + "\" is mandatory");
        }
        final String password = params.get(PARAM_CQI_SERVER_PASSWORD);
        if (password == null) {
            throw new SRUConfigException("parameter \""
                    + PARAM_CQI_SERVER_PASSWORD + "\" is mandatory");
        }
        defaultCorpusName = params.get(PARAM_CQI_DEFAULT_CORPUS);
        if (defaultCorpusName == null) {
            throw new SRUConfigException("parameter \""
                    + PARAM_CQI_DEFAULT_CORPUS + "\" is mandatory");
        }
        defaultCorpusPID = params.get(PARAM_CQI_DEFAULT_CORPUS_PID);
        if (defaultCorpusPID == null) {
            throw new SRUConfigException("parameter \""
                    + PARAM_CQI_DEFAULT_CORPUS_PID + "\" is mandatory");
        }
        defaultCorpusRef = params.get(PARAM_CQI_DEFAULT_CORPUS_REF);
        if (defaultCorpusRef == null) {
            throw new SRUConfigException("parameter \""
                    + PARAM_CQI_DEFAULT_CORPUS_REF + "\" is mandatory");
        }
        try {
            client = new CqiClient(serverHost, serverPort);
        } catch (CqiClientException ex) {
            throw new SRUConfigException("can't initialize a cqi client", ex);
        }
        try {
            client.connect(username, password);
        } catch (CqiClientException ex) {
            throw new SRUConfigException("can't connect to the cqi server", ex);
        }
    }

    @Override
    public SRUExplainResult explain(SRUServerConfig config,
            SRURequest request, SRUDiagnosticList diagnostics)
            throws SRUException {
        return null;
    }

    @Override
    public SRUScanResultSet scan(SRUServerConfig config, SRURequest request,
            SRUDiagnosticList diagnostics) throws SRUException {
        /*
         * handle scan on CLARIN FCS fcs.resource;
         * otherwise return an empty scan result set ...
         */
        final ResourceInfo[] result =
                translateFcsScanResource(request.getScanClause());
        final boolean provideResourceInfo = (result != null)
                && parseBoolean(request.getExtraRequestData(X_CMD_RESOURCE_INFO));
        return new SRUScanResultSet(diagnostics) {
            private int idx = -1;

            @Override
            public boolean nextTerm() {
                return (result != null) && (++idx < result.length);
            }

            @Override
            public String getValue() {
                return result[idx].getCorpusId();
            }

            @Override
            public int getNumberOfRecords() {
                return result[idx].getResourceCount();
            }

            @Override
            public String getDisplayTerm() {
                return null;
            }

            @Override
            public SRUScanResultSet.WhereInList getWhereInList() {
                return null;
            }

            @Override
            public boolean hasExtraTermData() {
                return provideResourceInfo;
            }

            @Override
            public void writeExtraTermData(XMLStreamWriter writer)
                    throws XMLStreamException {
                if (provideResourceInfo) {
                    result[idx].writeResourceInfo(writer, null);
                }
            }
        };
    }

    @Override
    public SRUSearchResultSet search(SRUServerConfig config,
            SRURequest request, SRUDiagnosticList diagnostics)
            throws SRUException {
        /*
         * sanity check: make sure we are asked to return stuff in CLARIN FCS
         * format if a recordSchema is specified.
         */
        final String recordSchemaIdentifier =
                request.getRecordSchemaIdentifier();
        if ((recordSchemaIdentifier != null)
                && !recordSchemaIdentifier.equals(CLARIN_FCS_RECORD_SCHEMA)) {
            throw new SRUException(
                    SRUConstants.SRU_UNKNOWN_SCHEMA_FOR_RETRIEVAL,
                    recordSchemaIdentifier, "Record schema \""
                    + recordSchemaIdentifier
                    + "\" is not supported by this endpoint.");
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
        } else if (startRecord == -1) {
            startRecord = 0;
        }
        logger.info("running query = \"{}\", offset = {}, limit = {}",
                new Object[]{cqpQuery, startRecord, maximumRecords});
        try {
            final CqiResult result = client.query(defaultCorpusName, cqpQuery, CONTEXT_STRUCTURAL_ATTRIBUTE);
            if ((result.size() > 0 && !result.absolute(startRecord)) || (result.size() == 0 && startRecord > 0)) {
                diagnostics.addDiagnostic(SRUConstants.SRU_FIRST_RECORD_POSITION_OUT_OF_RANGE, Integer.toString(startRecord + 1), null);
            }

            return new SRUSearchResultSet(diagnostics) {
                private int pos = 0;

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
                        return pos++ < maximumRecords && result.next();
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
                    final int matchStart = result.getMatchStart();
                    final int matchEnd = result.getMatchEnd();
                    final int relMatchStart = matchStart - contextStart;
                    final int relMatchEnd = matchEnd - contextStart + 1;
                    final int relContextEnd = contextEnd - contextStart + 1;
                    final StringBuilder leftContext = new StringBuilder();
                    final StringBuilder keyWord = new StringBuilder();
                    final StringBuilder rightContext = new StringBuilder();
                    String[] words;
                    try {
                        words = result.getValues(WORD_POSITIONAL_ATTRIBUTE, contextStart, contextEnd);
                    } catch (CqiClientException e) {
                        throw new XMLStreamException("can't obtain the values of the positional attribute '" + WORD_POSITIONAL_ATTRIBUTE + "'", e);
                    }
                    boolean isFirst = true;
                    for (int i = 0; i < relMatchStart; i++) {
                        if (isFirst) {
                            isFirst = false;
                        } else {
                            leftContext.append(' ');
                        }
                        leftContext.append(words[i]);
                    }
                    isFirst = true;
                    for (int i = relMatchStart; i < relMatchEnd; i++) {
                        if (isFirst) {
                            isFirst = false;
                        } else {
                            keyWord.append(' ');
                        }
                        keyWord.append(words[i]);
                    }
                    isFirst = true;
                    for (int i = relMatchEnd; i < relContextEnd; i++) {
                        if (isFirst) {
                            isFirst = false;
                        } else {
                            rightContext.append(' ');
                        }
                        rightContext.append(words[i]);
                    }
                    writer.setPrefix(FCS_PREFIX, FCS_NS);
                    writer.writeStartElement(FCS_NS, "Resource");
                    writer.writeNamespace(FCS_PREFIX, FCS_NS);
                    writer.writeAttribute("pid", defaultCorpusPID);
                    writer.writeAttribute("ref", defaultCorpusRef);
                    writer.writeStartElement(FCS_NS, "DataView");
                    writer.writeAttribute("type", "kwic");

                    writer.setPrefix(FCS_KWIC_PREFIX, FCS_KWIC_NS);
                    writer.writeStartElement(FCS_KWIC_NS, "kwic");
                    writer.writeNamespace(FCS_KWIC_PREFIX, FCS_KWIC_NS);

                    writer.writeStartElement(FCS_KWIC_NS, "c");
                    writer.writeAttribute("type", "left");
                    writer.writeCharacters(leftContext.toString());
                    writer.writeEndElement(); // "c" element

                    writer.writeStartElement(FCS_KWIC_NS, "kw");
                    writer.writeCharacters(keyWord.toString());
                    writer.writeEndElement(); // "kw" element

                    writer.writeStartElement(FCS_KWIC_NS, "c");
                    writer.writeAttribute("type", "right");
                    writer.writeCharacters(rightContext.toString());
                    writer.writeEndElement(); // "c" element

                    writer.writeEndElement(); // "kwic" element

                    writer.writeEndElement(); // "DataView" element
                    writer.writeEndElement(); // "Resource" element
                }
            };
        } catch (CqiClientException e) {
            logger.error("error processing query", e);
            throw new SRUException(
                    SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
                    "Error processing query (" + e.getMessage() + ").", e);
        }
    }

    private ResourceInfo[] translateFcsScanResource(CQLNode query)
            throws SRUException {
        if (query instanceof CQLTermNode) {
            final CQLTermNode root = (CQLTermNode) query;
            logger.debug("index = '{}', relation = '{}', term = '{}'",
                    new Object[]{root.getIndex(),
                        root.getRelation().getBase(), root.getTerm()});

            String index = root.getIndex();
            if (!(INDEX_FCS_RESOURCE.equals(index) || INDEX_CQL_SERVERCHOICE.equals(index))) {
                throw new SRUException(SRUConstants.SRU_UNSUPPORTED_INDEX,
                        root.getIndex(), "Index \"" + root.getIndex()
                        + "\" is not supported in scan operation.");
            }


            // only allow "=" relation without any modifiers
            final CQLRelation relationNode = root.getRelation();
            String relation = relationNode.getBase();
            if (!(CQI_SUPPORTED_RELATION_CQL_1_1.equals(relation)
                    || CQI_SUPPORTED_RELATION_CQL_1_2.equals(relation)
                    || CQI_SUPPORTED_RELATION_EXACT.equals(relation))) {
                throw new SRUException(SRUConstants.SRU_UNSUPPORTED_RELATION,
                        relationNode.getBase(), "Relation \""
                        + relationNode.getBase()
                        + "\" is not supported in scan operation.");
            }
            Vector<Modifier> modifiers = relationNode.getModifiers();
            if ((modifiers != null) && !modifiers.isEmpty()) {
                Modifier modifier = modifiers.get(0);
                throw new SRUException(
                        SRUConstants.SRU_UNSUPPORTED_RELATION_MODIFIER,
                        modifier.getValue(), "Relation modifier \""
                        + modifier.getValue()
                        + "\" is not supported in scan operation.");
            }

            String term = root.getTerm();
            if ((term == null) || term.isEmpty()) {
                throw new SRUException(SRUConstants.SRU_EMPTY_TERM_UNSUPPORTED,
                        "An empty term is not supported in scan operation.");
            }

            /*
             * generate result: currently we only have a flat hierarchy, so
             * return an empty result on any attempt to do a recursive scan ...
             */
            if ((INDEX_CQL_SERVERCHOICE.equals(index)
                    && INDEX_FCS_RESOURCE.equals(term))
                    || (INDEX_FCS_RESOURCE.equals(index)
                    && (FCS_RESOURCE_TERM_WILDCARD.equals(term)
                    || FCS_RESOURCE_TERM_ANY.equalsIgnoreCase(term)))) {
                return RESOURCE_INFOS;
            } else {
                return null;
            }
        } else {
            throw new SRUException(SRUConstants.SRU_QUERY_FEATURE_UNSUPPORTED,
                    "Scan clause too complex.");
        }
    }

    private String translateCQLtoCQP(CQLNode query) throws SRUException {
        if (query instanceof CQLTermNode) {
            final CQLTermNode root = (CQLTermNode) query;

            // only allow "cql.serverChoice" and "words" index
            if (!(INDEX_CQL_SERVERCHOICE.equals(root.getIndex())
                    || INDEX_FCS_WORDS.equals(root.getIndex()))) {
                throw new SRUException(SRUConstants.SRU_UNSUPPORTED_INDEX,
                        root.getIndex(), "Index \"" + root.getIndex()
                        + "\" is not supported.");
            }

            // only allow "=" relation without any modifiers
            final CQLRelation relation = root.getRelation();
            final String baseRel = relation.getBase();
            if (!(CQI_SUPPORTED_RELATION_CQL_1_1.equals(baseRel)
                    || CQI_SUPPORTED_RELATION_CQL_1_2.equals(baseRel)
                    || CQI_SUPPORTED_RELATION_EXACT.equals(baseRel))) {
                throw new SRUException(SRUConstants.SRU_UNSUPPORTED_RELATION,
                        relation.getBase(), "Relation \""
                        + relation.getBase() + "\" is not supported.");
            }
            Vector<Modifier> modifiers = relation.getModifiers();
            if ((modifiers != null) && !modifiers.isEmpty()) {
                Modifier modifier = modifiers.get(0);
                throw new SRUException(
                        SRUConstants.SRU_UNSUPPORTED_RELATION_MODIFIER,
                        modifier.getValue(), "Relation modifier \""
                        + modifier.getValue() + "\" is not supported.");
            }

            // check term
            final String term = root.getTerm();
            if ((term == null) || term.isEmpty()) {
                throw new SRUException(SRUConstants.SRU_EMPTY_TERM_UNSUPPORTED,
                        "An empty term is not supported.");
            }
            //convert to cqp by inserting quotes around each token
            return String.format("\"%s\"", SPACE_PATTERN.matcher(term).replaceAll("\" \""));

        }
        throw new SRUException(SRUConstants.SRU_QUERY_FEATURE_UNSUPPORTED,
                "Server currently supportes term-only query "
                + "(CQL conformance level 0).");
    }

    private boolean parseBoolean(String value) {
        if (value != null) {
            return value.endsWith("1") || Boolean.parseBoolean(value);
        }
        return false;
    }
}
