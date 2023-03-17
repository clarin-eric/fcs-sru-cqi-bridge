package eu.clarin.sru.cqibridge;

import java.util.NoSuchElementException;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import eu.clarin.cqi.client.CqiClientException;
import eu.clarin.cqi.client.CqiResult;
import eu.clarin.sru.server.SRUConstants;
import eu.clarin.sru.server.SRUDiagnostic;
import eu.clarin.sru.server.SRUDiagnosticList;
import eu.clarin.sru.server.SRUException;
import eu.clarin.sru.server.SRURequest;
import eu.clarin.sru.server.SRUResultCountPrecision;
import eu.clarin.sru.server.SRUSearchResultSet;
import eu.clarin.sru.server.fcs.XMLStreamWriterHelper;

import static eu.clarin.sru.cqibridge.CqiSRUSearchEngine.WORD_POSITIONAL_ATTRIBUTE;
import static eu.clarin.sru.cqibridge.CqiSRUSearchEngine.CLARIN_FCS_RECORD_SCHEMA;

public class CqiSRUSearchResultSet extends SRUSearchResultSet {

    private SRURequest request;
    private CqiResult result;
    private String corpusPID;
    private String corpusRef;

    private int recordCount;
    private String resultSetId = null;
    private int currentRecordCursor = 0;
    private int startRecord = 1;
    private int maximumRecords = 1000;

    protected CqiSRUSearchResultSet(SRURequest request, SRUDiagnosticList diagnostics, final CqiResult result,
            String corpusPID, String corpusRef) {
        super(diagnostics);
        this.request = request;
        this.result = result;
        this.corpusPID = corpusPID;
        this.corpusRef = corpusRef;

        startRecord = (request.getStartRecord() < 1) ? 1 : request.getStartRecord();
        currentRecordCursor = startRecord - 1;
        maximumRecords = startRecord - 1 + request.getMaximumRecords();
        recordCount = request.getMaximumRecords();
    }

    @Override
    public int getRecordCount() {
        if (result != null && result.size() > -1) {
            return result.size() < maximumRecords ? result.size() : maximumRecords;
        }
        return 0;
    }

    @Override
    public int getTotalRecordCount() {
        if (result != null) {
            return result.size();
        }
        return -1;
    }

    @Override
    public SRUResultCountPrecision getResultCountPrecision() {
        return SRUResultCountPrecision.EXACT;
    }

    @Override
    public int getResultSetTTL() {
        return -1;
    }

    @Override
    public String getResultSetId() {
        return resultSetId;
    }

    @Override
    public String getRecordIdentifier() {
        return null;
    }

    @Override
    public String getRecordSchemaIdentifier() {
        return request.getRecordSchemaIdentifier() != null ? request.getRecordSchemaIdentifier()
                : CLARIN_FCS_RECORD_SCHEMA;
    }

    @Override
    public SRUDiagnostic getSurrogateDiagnostic() {
        if ((getRecordSchemaIdentifier() != null) && !CLARIN_FCS_RECORD_SCHEMA.equals(getRecordSchemaIdentifier())) {
            return new SRUDiagnostic(SRUConstants.SRU_RECORD_NOT_AVAILABLE_IN_THIS_SCHEMA, getRecordSchemaIdentifier(),
                    "Record is not available in record schema \"" + getRecordSchemaIdentifier() + "\".");
        }
        return null;
    }

    @Override
    public boolean nextRecord() throws SRUException {
        if (currentRecordCursor < getRecordCount()) {
            currentRecordCursor++;
            try {
                result.next();
            } catch (CqiClientException e) {
                throw new NoSuchElementException(e.getMessage());
            }
            return true;
        }
        return false;
    }

    @Override
    public void writeRecord(XMLStreamWriter writer) throws XMLStreamException {
        final int contextStart = result.getContextStart();
        final int contextEnd = result.getContextEnd();
        final int relMatchStart = result.getMatchStart() - contextStart;
        final int relMatchEnd = result.getMatchEnd() - contextStart + 1;
        final int relContextEnd = contextEnd - contextStart + 1;

        String[] words;
        try {
            words = result.getValues(WORD_POSITIONAL_ATTRIBUTE, contextStart, contextEnd);
        } catch (CqiClientException e) {
            throw new XMLStreamException(
                    "can't obtain the values of the positional attribute '" + WORD_POSITIONAL_ATTRIBUTE + "'", e);
        }

        String leftContext = matchToString(words, 0, relMatchStart);
        String keyWord = matchToString(words, relMatchStart, relMatchEnd);
        String rightContext = matchToString(words, relMatchEnd, relContextEnd);

        // HITS + KWIC dataviews (KWIC legacy support)
        XMLStreamWriterHelper.writeResourceWithHitsDataViewLegacy(writer, corpusPID, corpusRef, leftContext, keyWord,
                rightContext);
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

}
