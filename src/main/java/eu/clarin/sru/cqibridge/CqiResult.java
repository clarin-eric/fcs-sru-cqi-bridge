/**
 * This software is copyright (c) 2012 by - Seminar fuer Sprachwissenschaft
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

import java.nio.charset.Charset;

/**
 *
 * @author akislev
 */
public class CqiResult {

    public enum Field {

        MATCH,
        CONTEXT
    }
    private static final int BUFFER_SIZE = 10;
    private static final int MATCH_START = 0;
    private static final int MATCH_END = 1;
    private static final int CONTEXT_START = 2;
    private static final int CONTEXT_END = 3;
    private final int[][] results = new int[4][BUFFER_SIZE];
    private final CqiClient client;
    private final String corpusName;
    private final String subCorpusName;
    private final Charset charset;
    private final int size;
    private int dataSize;
    private int dataIndex;
    private int positionIndex = -1;
    private final String contextStructuralAttributeName;

    protected CqiResult(CqiClient client, String corpusName, String subCorpusName, Charset charset, int size) {
        this.client = client;
        this.corpusName = corpusName;
        this.subCorpusName = subCorpusName;
        this.size = size;
        this.charset = charset;
        this.contextStructuralAttributeName = String.format("%s.s", corpusName);
    }

    public int getIndex() {
        return dataIndex - dataSize + positionIndex;
    }

    public boolean absolute(int match) throws CqiClientException {
        if (match < 0 || match >= size) {
            return false;
        }
        dataIndex = match;
        return updateIndices();
    }

    public boolean next() throws CqiClientException {
        if (++positionIndex >= dataSize) {
            return updateIndices();
        } else {
            return true;
        }
    }

    private boolean updateIndices() throws CqiClientException {
        if (dataIndex < size) {
            dataIndex += BUFFER_SIZE;
            positionIndex = 0;
            if (dataIndex > size) {
                dataSize = size - (dataIndex - BUFFER_SIZE);
                dataIndex = size;
            } else {
                dataSize = BUFFER_SIZE;
            }
            client.dumpSubCorpus(subCorpusName, CqiClient.CQI_CONST_FIELD_MATCH, dataIndex - dataSize, dataIndex - 1, results[MATCH_START]);
            client.dumpSubCorpus(subCorpusName, CqiClient.CQI_CONST_FIELD_MATCHEND, dataIndex - dataSize, dataIndex - 1, results[MATCH_END]);
            client.cpos2LBound(contextStructuralAttributeName, results[MATCH_START], results[CONTEXT_START], dataSize);
            client.cpos2RBound(contextStructuralAttributeName, results[MATCH_END], results[CONTEXT_END], dataSize);
            return true;
        } else {
            return false;
        }
    }

    public Range getRange(Field field) throws CqiClientException {
        return new Range(field.ordinal() * 2, positionIndex);
    }

    public void clear() throws CqiClientException {
        client.dropSubCorpus(subCorpusName);
    }

    public int size() {
        return size;
    }

    public class Range {

        private final int resultNum;
        private final int index;
        private int[] range;

        public Range(int resultSet, int index) {
            this.resultNum = resultSet;
            this.index = index;
        }

        public int getStart() {
            return results[resultNum][index];
        }

        public int getEnd() {
            return results[resultNum + 1][index];
        }

        public int[] getPositions() {
            if (range == null) {
                int start = results[resultNum][index];
                range = new int[results[resultNum + 1][index] - start + 1];
                for (int i = 0; i < range.length; i++) {
                    range[i] = i + start;
                }
            }
            return range;
        }

        public String[] getValues(String positionalAttribute) throws CqiClientException {
            return client.cpos2Str(String.format("%s.%s", corpusName, positionalAttribute), getPositions(), charset);
        }
    }
}
