/**
 * This software is copyright (c) 2012 by
 * - TXM (http://txm.sourceforge.net/), Seminar fuer Sprachwissenschaft  (http://www.sfs.uni-tuebingen.de/)
 * This is free software. You can redistribute it
 * and/or modify it under the terms described in
 * the GNU General Public License v3 of which you
 * should have received a copy. Otherwise you can download
 * it from
 *
 *   http://www.gnu.org/licenses/gpl-3.0.txt
 *
 * @copyright Seminar fuer Sprachwissenschaft (http://www.sfs.uni-tuebingen.de/)
 *
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 *  GNU General Public License v3
 */
package eu.clarin.sru.cqibridge;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.Charset;

/**
 * This class implements a Java CQi client.
 *
 * @author Jean-Philippe Magué
 */
public class CqiClient {

    private static final byte[] CQI_PADDING = {(byte) 0x00};
    private static final byte[] CQI_STATUS_OK = {(byte) 0x01, (byte) 0x01};
    private static final byte[] CQI_STATUS_CONNECT_OK = {(byte) 0x01, (byte) 0x02};
    private static final byte[] CQI_STATUS_BYE_OK = {(byte) 0x01, (byte) 0x03};
    private static final byte[] CQI_STATUS_PING_OK = {(byte) 0x01, (byte) 0x04};
    private static final byte[] CQI_DATA_BYTE = {(byte) 0x03, (byte) 0x01};
    private static final byte[] CQI_DATA_BOOL = {(byte) 0x03, (byte) 0x02};
    private static final byte[] CQI_DATA_INT = {(byte) 0x03, (byte) 0x03};
    private static final byte[] CQI_DATA_STRING = {(byte) 0x03, (byte) 0x04};
    private static final byte[] CQI_DATA_BYTE_LIST = {(byte) 0x03, (byte) 0x05};
    private static final byte[] CQI_DATA_BOOL_LIST = {(byte) 0x03, (byte) 0x06};
    private static final byte[] CQI_DATA_INT_LIST = {(byte) 0x03, (byte) 0x07};
    private static final byte[] CQI_DATA_STRING_LIST = {(byte) 0x03, (byte) 0x08};
    private static final byte[] CQI_DATA_INT_INT = {(byte) 0x03, (byte) 0x09};
    private static final byte[] CQI_DATA_INT_INT_INT_INT = {(byte) 0x03, (byte) 0x0A};
    private static final byte[] CQI_DATA_INT_TABLE = {(byte) 0x03, (byte) 0x0B};
    private static final byte[] CQI_CTRL_CONNECT = {(byte) 0x11, (byte) 0x01};
    private static final byte[] CQI_CTRL_BYE = {(byte) 0x11, (byte) 0x02};
    private static final byte[] CQI_CTRL_LAST_GENERAL_ERROR = {(byte) 0x11, (byte) 0x05};
    private static final byte[] CQI_CTRL_LAST_CQP_ERROR = {(byte) 0x11, (byte) 0x06};
    private static final byte[] CQI_CORPUS_LIST_CORPORA = {(byte) 0x13, (byte) 0x01};
    private static final byte[] CQI_CORPUS_CHARSET = {(byte) 0x13, (byte) 0x03};
    private static final byte[] CQI_CORPUS_POSITIONAL_ATTRIBUTES = {(byte) 0x13, (byte) 0x05};
    private static final byte[] CQI_CORPUS_STRUCTURAL_ATTRIBUTES = {(byte) 0x13, (byte) 0x06};
    private static final byte[] CQI_CORPUS_STRUCTURAL_ATTRIBUTE_HAS_VALUES = {(byte) 0x13, (byte) 0x07};
    private static final byte[] CQI_CORPUS_FULL_NAME = {(byte) 0x13, (byte) 0x09};
    private static final byte[] CQI_CL_ATTRIBUTE_SIZE = {(byte) 0x14, (byte) 0x01};
    private static final byte[] CQI_CL_LEXICON_SIZE = {(byte) 0x14, (byte) 0x02};
    private static final byte[] CQI_CL_CPOS2STR = {(byte) 0x14, (byte) 0x08};
    private static final byte[] CQI_CL_CPOS2LBOUND = {(byte) 0x14, (byte) 0x20};
    private static final byte[] CQI_CL_CPOS2RBOUND = {(byte) 0x14, (byte) 0x21};
    private static final byte[] CQI_CQP_QUERY = {(byte) 0x15, (byte) 0x01};
    private static final byte[] CQI_CQP_LIST_SUBCORPORA = {(byte) 0x15, (byte) 0x02};
    private static final byte[] CQI_CQP_SUBCORPUS_SIZE = {(byte) 0x15, (byte) 0x03};
    private static final byte[] CQI_CQP_SUBCORPUS_HAS_FIELD = {(byte) 0x15, (byte) 0x04};
    private static final byte[] CQI_CQP_DUMP_SUBCORPUS = {(byte) 0x15, (byte) 0x05};
    private static final byte[] CQI_CQP_DROP_SUBCORPUS = {(byte) 0x15, (byte) 0x09};
    private static final Charset DEFAULT_CHARSET = Charset.forName("ASCII");
    public static final byte CQI_CONST_FIELD_MATCH = (byte) 0x10;
    public static final byte CQI_CONST_FIELD_MATCHEND = (byte) 0x11;
    public static final byte CQI_CONST_FIELD_TARGET = (byte) 0x00;
    /**
     * Error messages
     */
    private static final String UNEXPECTED_ANSWER = "Unexpected answer";
    private static final String SERVER_NOT_FOUND = "Server not found";
    private static final String INTERNAL_ERROR = "Internal error";
    private static final String INTERNAL_CQI_ERROR = "Internal CQI error";
    private static final String INTERNAL_CQP_ERROR = "Internal CQP error";
    private static final String INTERNAL_CL_ERROR = "Internal CL error";
    private static final String OUT_OF_MEMORY_ERROR = "Out of memory";
    private static final String CORPUS_ACCESS_ERROR = "Corpus access error";
    private static final String WRONG_ATTRIBUTE_TYPE_ERROR = "Wrong attribute type";
    private static final String OUT_OF_RANGE_ERROR = "Out of range";
    private static final String REGEX_ERROR = "Regex error";
    private static final String NO_SUCH_ATTRIBUTE_ERROR = "No such attribute";
    private static final String NO_SUCH_CORPUS_CQP_ERROR = "No such corpus";
    private static final String INVALID_FIELD_CQP_ERROR = "Invalid field";
    private static final String OUT_OF_RANGE_CQP_ERROR = "Out of range";
    private static final String SYNTAX_CQP_ERROR = "CQP Syntax error";
    private static final String GENERAL_SQP_ERROR = "General CQP error";
    private static final String CONNECTION_REFUSED_ERROR = "Connection refused";
    private static final String USER_ABORT_ERROR = "User abort";
    private static final String SYNTAX_ERROR = "Syntax error";
    private static final String GENERAL_ERROR = "General error";
    private static final String INSUFFICIENT_BUFFER_SIZE = "Insufficient buffer size";
    private static final String SERVER_IO_ERROR = "IO Error while communicating to the server";
    private static final int BUFFER_SIZE = 40;
    private Socket socket;
    private SocketAddress serverAddress;
    private DataOutput streamToServer;
    private DataInput streamFromServer;
    private final byte[] buffer = new byte[BUFFER_SIZE];

    /**
     * Instantiates a new cqi client.
     *
     * @param host the host of the CQI server
     * @param port the port of the CQI server
     *
     * @throws CqiClientException the server not found exception
     */
    public CqiClient(String host, int port) throws CqiClientException {
        try {
            this.socket = new Socket();
            this.serverAddress = new InetSocketAddress(host, port);
            this.socket.connect(serverAddress);
            this.streamToServer = new DataOutputStream(this.socket.getOutputStream());
            this.streamFromServer = new DataInputStream(this.socket.getInputStream());
        } catch (IOException e) {
            throw new CqiClientException(SERVER_NOT_FOUND, e);
        }
    }

    /**
     * Connect the client to a server
     *
     * @param username the username
     * @param password the password
     * @return true, if successful
     * @throws CqiClientException
     */
    public synchronized boolean connect(String username, String password)
            throws CqiClientException {
        try {
            this.streamToServer.write(CQI_CTRL_CONNECT);
            this.writeString(username);
            this.writeString(password);
            return (readHeaderFromServer() == CQI_STATUS_CONNECT_OK);
        } catch (IOException ex) {
            throw new CqiClientException(SERVER_IO_ERROR, ex);
        }
    }

    /**
     * Disconnect
     *
     * @return true, if successful
     *
     * @throws CqiClientException
     */
    public synchronized boolean disconnect() throws CqiClientException {
        try {
            this.streamToServer.write(CQI_CTRL_BYE);
            return (readHeaderFromServer() == CQI_STATUS_BYE_OK);
        } catch (IOException ex) {
            throw new CqiClientException(SERVER_IO_ERROR, ex);
        }
    }

    /**
     * Lists the corpora available on the server
     *
     * @return the name of the corpora
     * @throws CqiClientException
     */
    public synchronized String[] listCorpora() throws CqiClientException {
        try {
            this.streamToServer.write(CQI_CORPUS_LIST_CORPORA);
            return readStringArray(DEFAULT_CHARSET);
        } catch (IOException e) {
            throw new CqiClientException(SERVER_IO_ERROR, e);
        }
    }

    /**
     * Gives the corpus positional attributes.
     *
     * @param corpusID the corpus id
     * @return the name of the attributes
     * @throws CqiClientException
     */
    public synchronized String[] corpusPositionalAttributes(String corpus)
            throws CqiClientException {
        return genericStringToStringArray(corpus,
                CQI_CORPUS_POSITIONAL_ATTRIBUTES);
    }

    /**
     * Gives the corpus structural attributes.
     *
     * @param corpus the corpus
     * @return the name of the attributes
     * @throws CqiClientException
     */
    public synchronized String[] corpusStructuralAttributes(String corpus)
            throws CqiClientException {
        return genericStringToStringArray(corpus,
                CQI_CORPUS_STRUCTURAL_ATTRIBUTES);
    }

    /**
     * Check whether a structural attribute has values
     *
     * @param attribute the attribute
     * @return true, if it has values
     * @throws CqiClientException
     */
    public synchronized boolean corpusStructuralAttributeHasValues(
            String attribute) throws CqiClientException {
        try {
            this.streamToServer.write(CQI_CORPUS_STRUCTURAL_ATTRIBUTE_HAS_VALUES);
            this.writeString(attribute);
            return readBoolean();
        } catch (IOException e) {
            throw new CqiClientException(SERVER_IO_ERROR, e);
        }
    }

    /**
     * Write a string on the socket.
     *
     * @param string the string
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private synchronized void writeString(String string, Charset charset) throws IOException {
        byte[] bytes = string.getBytes(charset);
        this.streamToServer.writeShort(bytes.length);
        this.streamToServer.write(bytes);
    }

    private synchronized void writeString(String string) throws IOException {
        writeString(string, DEFAULT_CHARSET);
    }

    /**
     * Write int array on the socket.
     *
     * @param ints the int array
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private synchronized void writeIntArray(int[] ints) throws IOException {
        int length = ints.length;
        this.streamToServer.writeInt(length);
        for (int i = 0; i < length; i++) {
            // System.out.println(i+"/"+length);
            this.streamToServer.writeInt(ints[i]);
        }
    }

    /**
     * Write int array on the socket.
     *
     * @param ints the int array
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private synchronized void writeIntArray(int[] ints, int length) throws IOException {
        this.streamToServer.writeInt(length);
        for (int i = 0; i < length; i++) {
            this.streamToServer.writeInt(ints[i]);
        }
    }

    /**
     * Read a string from the socket.
     *
     * @return the string
     *
     * @throws CqiClientException Signals that the data read on the socket is
     * unexpected
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private synchronized String readString(Charset charset) throws
            CqiClientException, IOException {
        if (readHeaderFromServer() != CQI_DATA_STRING) {
            throw new CqiClientException(UNEXPECTED_ANSWER);
        }
        short length = this.streamFromServer.readShort();
        byte[] bytes = new byte[length];
        this.streamFromServer.readFully(bytes);
        String res = new String(bytes, charset);
        return res;
    }

    private synchronized String readString() throws
            CqiClientException, IOException {
        return readString(DEFAULT_CHARSET);
    }

    /**
     * Read a boolean from the socket.
     *
     * @return the boolean
     *
     * @throws CqiClientException
     */
    private synchronized boolean readBoolean() throws CqiClientException,
            IOException {
        if (readHeaderFromServer() != CQI_DATA_BOOL) {
            throw new CqiClientException(UNEXPECTED_ANSWER);
        }
        return this.streamFromServer.readByte() == 1;
    }

    /**
     * Read an int from the socket.
     *
     * @return the int
     *
     * @throws CqiClientException Signals that the data read on the socket is
     * unexpected
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private synchronized int readInt() throws CqiClientException, IOException {
        if (readHeaderFromServer() != CQI_DATA_INT) {
            throw new CqiClientException(UNEXPECTED_ANSWER);
        }
        int res = this.streamFromServer.readInt();
        return res;
    }

    /**
     * Read a string array from the socket.
     *
     * @return the string array
     *
     * @throws CqiClientException
     */
    private synchronized String[] readStringArray(Charset charset)
            throws CqiClientException {
        try {
            byte[] header = readHeaderFromServer();
            if (header != CQI_DATA_STRING_LIST) {
                throw new CqiClientException(UNEXPECTED_ANSWER);
            }
            int arrayLength = this.streamFromServer.readInt();
            String[] strings = new String[arrayLength];
            for (int i = 0; i < arrayLength; i++) {
                short stringLength = this.streamFromServer.readShort();
                byte[] bytes = new byte[stringLength];
                this.streamFromServer.readFully(bytes);
                strings[i] = new String(bytes, charset);
            }
            return strings;
        } catch (IOException e) {
            throw new CqiClientException("Error reading a string array", e);
        }
    }

    /**
     * Read an int array from the socket.
     *
     * @return the int array
     *
     * @throws CqiClientException
     * @throws IOException
     */
    private synchronized void readIntList(int[] output) throws CqiClientException,
            IOException {
        if ((readHeaderFromServer()) != CQI_DATA_INT_LIST) {
            throw new CqiClientException(UNEXPECTED_ANSWER);
        }
        int arrayLength = this.streamFromServer.readInt();
        if (output.length < arrayLength) {
            throw new CqiClientException(INSUFFICIENT_BUFFER_SIZE);
        }
        int bsize = arrayLength * 4;
        if (buffer.length < bsize) {
            throw new CqiClientException(INSUFFICIENT_BUFFER_SIZE);
        }
        streamFromServer.readFully(buffer, 0, bsize);
        for (int i = 0; i + 3 < bsize; i += 4) {
            output[i >> 2] = bytesToInt(buffer[i], buffer[i + 1], buffer[i + 2], buffer[i + 3]);
        }
    }

    /**
     * Bytes to int.
     *
     * @param a the a
     * @param b the b
     * @param c the c
     * @param d the d
     * @return the int
     */
    private synchronized static int bytesToInt(byte a, byte b, byte c, byte d) {
        return (((a & 0xff) << 24) | ((b & 0xff) << 16) | ((c & 0xff) << 8) | (d & 0xff));
    }

    /**
     * Read the header of the data send by the server.
     *
     * @return the header
     *
     * @throws CqiClientException\
     * @throws IOException
     */
    private synchronized byte[] readHeaderFromServer()
            throws CqiClientException, IOException {
        byte b = this.streamFromServer.readByte();
        switch (b) {
            case 0x00:// cf cqi.h:29
                return CQI_PADDING;
            case 0x01:// cf cqi.h:37
                b = this.streamFromServer.readByte();
                switch (b) {
                    case 0x01:// cf cqi.h:39
                        return CQI_STATUS_OK;
                    case 0x02:// cf cqi.h:40
                        return CQI_STATUS_CONNECT_OK;
                    case 0x03:// cf cqi.h:41
                        return CQI_STATUS_BYE_OK;
                    case 0x04:// cf cqi.h:42
                        return CQI_STATUS_PING_OK;
                }
                break;
            case 0x02:// cf cqi.h:45
                b = this.streamFromServer.readByte();
                switch (b) {
                    case 0x01:// cf cqi.h:39
                        throw new CqiClientException(GENERAL_ERROR);
                    case 0x02:// cf cqi.h:40
                        throw new CqiClientException(CONNECTION_REFUSED_ERROR);
                    case 0x03:// cf cqi.h:41
                        throw new CqiClientException(USER_ABORT_ERROR);
                    case 0x04:// cf cqi.h:42
                        throw new CqiClientException(SYNTAX_ERROR);
                    default:
                        throw new CqiClientException(INTERNAL_CQI_ERROR);
                }
            case 0x03:// cf cqi.h:53
                b = this.streamFromServer.readByte();

                switch (b) {
                    case 0x01:// cf cqi.h:39
                        return CQI_DATA_BYTE;
                    case 0x02:// cf cqi.h:40
                        return CQI_DATA_BOOL;
                    case 0x03:// cf cqi.h:41
                        return CQI_DATA_INT;
                    case 0x04:// cf cqi.h:42
                        return CQI_DATA_STRING;
                    case 0x05:// cf cqi.h:42
                        return CQI_DATA_BYTE_LIST;
                    case 0x06:// cf cqi.h:42
                        return CQI_DATA_BOOL_LIST;
                    case 0x07:// cf cqi.h:42
                        return CQI_DATA_INT_LIST;
                    case 0x08:// cf cqi.h:42
                        return CQI_DATA_STRING_LIST;
                    case 0x09:// cf cqi.h:42
                        return CQI_DATA_INT_INT;
                    case 0x0A:// cf cqi.h:42
                        return CQI_DATA_INT_INT_INT_INT;
                    case 0x0B:// cf cqi.h:42
                        return CQI_DATA_INT_TABLE;
                }
                break;
            case 0x04:// cf cqi.h:67

                b = this.streamFromServer.readByte();
                switch (b) {
                    case 0x01:// cf cqi.h:39
                        throw new CqiClientException(NO_SUCH_ATTRIBUTE_ERROR);
                    case 0x02:// cf cqi.h:40
                        throw new CqiClientException(WRONG_ATTRIBUTE_TYPE_ERROR);
                    case 0x03:// cf cqi.h:41
                        throw new CqiClientException(OUT_OF_RANGE_ERROR);
                    case 0x04:// cf cqi.h:42
                        throw new CqiClientException(REGEX_ERROR + ": " + getLastCqiError());
                    case 0x05:// cf cqi.h:42
                        throw new CqiClientException(CORPUS_ACCESS_ERROR);
                    case 0x06:// cf cqi.h:42
                        throw new CqiClientException(OUT_OF_MEMORY_ERROR);
                    case 0x07:// cf cqi.h:42
                        throw new CqiClientException(INTERNAL_ERROR);
                    default:
                        throw new CqiClientException(INTERNAL_CL_ERROR);
                }
            case 0x05:// cf cqi.h:94

                b = this.streamFromServer.readByte();
                switch (b) {
                    case 0x01:// cf cqi.h:39
                        throw new CqiClientException(GENERAL_SQP_ERROR);
                    case 0x02:// cf cqi.h:40
                        throw new CqiClientException(NO_SUCH_CORPUS_CQP_ERROR);
                    case 0x03:// cf cqi.h:41
                        throw new CqiClientException(INVALID_FIELD_CQP_ERROR);
                    case 0x04:// cf cqi.h:42
                        throw new CqiClientException(OUT_OF_RANGE_CQP_ERROR);
                    case 0x05:// cf cqi.h:44
                        throw new CqiClientException(SYNTAX_CQP_ERROR + ": " + getLastCQPError());
                    default:
                        throw new CqiClientException(INTERNAL_CQP_ERROR);
                }
        }
        return null;
    }

    /**
     * Ask the server to execute a function with a String->String signature.
     *
     * @param string the argument
     * @param function the function to be executed
     * @return the result
     * @throws CqiClientException Signals that the data read on the socket is
     * unexpected
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private synchronized String genericStringToString(String string,
            byte[] function) throws CqiClientException, IOException {
        this.streamToServer.write(function);
        this.writeString(string, DEFAULT_CHARSET);
        return readString(DEFAULT_CHARSET);
    }

    /**
     * Ask the server to execute a function with a String->StringArray
     * signature.
     *
     * @param string the argument
     * @param function the function to be executed
     * @return the result
     * @throws CqiClientException
     */
    private synchronized String[] genericStringToStringArray(String string,
            byte[] function) throws CqiClientException {
        try {
            this.streamToServer.write(function);
            this.writeString(string, DEFAULT_CHARSET);
        } catch (IOException e) {
            throw new CqiClientException(SERVER_IO_ERROR, e);
        }
        return readStringArray(DEFAULT_CHARSET);
    }

    /**
     * Ask the server to execute a function with a String x int[]->String[]
     * signature.
     *
     * @param string the string argument
     * @param ints the int[] argument
     * @param function the function to be executed
     * @return the result
     * @throws CqiClientException
     */
    private synchronized String[] genericStringXIntArraytoStringArray(
            String string, int[] ints, byte[] function, Charset charset) throws
            CqiClientException {
        try {
            this.streamToServer.write(function);
            this.writeString(string, charset);
            this.writeIntArray(ints);
            String[] res = readStringArray(charset);
            return res;
        } catch (IOException e) {
            throw new CqiClientException(SERVER_IO_ERROR, e);
        }

    }

    /**
     * Ask the server to execute a function with a String x int[]->int[]
     * signature.
     *
     * @param string the string argument
     * @param ints the int[] argument
     * @param function the function to be executed
     * @return the result
     * @throws CqiClientException
     */
    private synchronized void genericStringXIntArraytoIntArray(String string,
            int[] ints, byte[] function, int[] output, int size) throws CqiClientException {
        try {
            this.streamToServer.write(function);
            this.writeString(string);
            this.writeIntArray(ints, size);
            readIntList(output);
        } catch (IOException e) {
            throw new CqiClientException(SERVER_IO_ERROR, e);
        }
    }

    /**
     * return the last CQP error.
     *
     * @return the last error
     * @throws CqiClientException Signals that the data read on the socket is
     * unexpected
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws CqiServerError Signals that the cqi server raised an error
     */
    private synchronized String getLastCqiError() throws CqiClientException, IOException {
        this.streamToServer.write(CQI_CTRL_LAST_GENERAL_ERROR);

        try {
            String ret = readString();
            return ret;
        } catch (CqiClientException e) {
            return "getLastCQiError: " + e;
        }
    }

    /**
     * return the last CQP error.
     *
     * @return the last error
     * @throws CqiClientException Signals that the data read on the socket is
     * unexpected
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws CqiServerError Signals that the cqi server raised an error
     */
    public synchronized String getLastCQPError() throws CqiClientException, IOException {

        this.streamToServer.write(CQI_CTRL_LAST_CQP_ERROR);

        try {
            String ret = readString();
            return ret;
        } catch (CqiClientException e) {
            return "getLastCQPError: " + e;
        }
    }

    /**
     * Gives the corpus charset.
     *
     * @param corpus the corpus
     * @return the name of the charset
     * @throws CqiClientException Signals that the data read on the socket is
     * unexpected
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws CqiServerError Signals that the cqi server raised an error
     */
    public synchronized String corpusCharset(String corpus)
            throws CqiClientException, IOException {
        return genericStringToString(corpus, CQI_CORPUS_CHARSET);
    }

    /**
     * Gives the corpus full name.
     *
     * @param corpus the corpus
     * @return the full name
     * @throws CqiClientException Signals that the data read on the socket is
     * unexpected
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws CqiServerError Signals that the cqi server raised an error
     */
    public synchronized String corpusFullName(String corpus)
            throws CqiClientException, IOException {
        return genericStringToString(corpus, CQI_CORPUS_FULL_NAME);
    }

    /**
     * Drop a corpus.
     *
     * @param corpus the corpus
     */
    public synchronized void dropCorpus(String corpus) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gives an attribute size (the number of token).
     *
     * @param attribute the attribute
     * @return the size
     * @throws CqiClientException
     */
    public synchronized int attributeSize(String attribute) throws
            CqiClientException {
        try {
            this.streamToServer.write(CQI_CL_ATTRIBUTE_SIZE);
            this.writeString(attribute);
            return readInt();
        } catch (IOException e) {
            throw new CqiClientException(SERVER_IO_ERROR, e);
        }
    }

    /**
     * Gives the lexicon size of an attribute.
     *
     * @param attribute the attribute
     * @return the int
     * @throws CqiClientException @returns the number of entries in the lexicon
     * of a positional attribute
     */
    public synchronized int lexiconSize(String attribute) throws CqiClientException {
        try {
            this.streamToServer.write(CQI_CL_LEXICON_SIZE);
            this.writeString(attribute);
            return readInt();
        } catch (IOException e) {
            throw new CqiClientException(SERVER_IO_ERROR, e);
        }
    }

    /**
     * Converts an array of position to their value given an attribute.
     *
     * @param attribute the attribute
     * @param cpos the cpos
     * @return the values
     * @throws CqiClientException
     */
    public synchronized String[] cpos2Str(String attribute, int[] cpos, Charset charset)
            throws CqiClientException {
        return genericStringXIntArraytoStringArray(attribute, cpos,
                CQI_CL_CPOS2STR, charset);
    }

    /**
     * Computes for each position of an array the position of the left boundary
     * of the enclosing structural attribute.
     *
     * @param attribute the attribute
     * @param cpos the cpos
     * @return the positions of the left boundaries
     * @throws CqiClientException
     */
    public synchronized void cpos2LBound(String attribute, int[] cpos, int[] output, int size)
            throws CqiClientException {
        genericStringXIntArraytoIntArray(attribute, cpos, CQI_CL_CPOS2LBOUND, output, size);
    }

    /**
     * Computes for each position of an array the position of the right boundary
     * of the enclosing structural attribute.
     *
     * @param attribute the attribute
     * @param cpos the cpos
     * @return the positions of the right boundaries
     * @throws CqiClientException
     */
    public synchronized void cpos2RBound(String attribute, int[] cpos, int[] output, int size)
            throws CqiClientException {
        genericStringXIntArraytoIntArray(attribute, cpos, CQI_CL_CPOS2RBOUND, output, size);
    }

    /**
     * Runs a CQP query.
     *
     * @param corpus the corpus
     * @param subcorpus the subcorpus
     * @param query the query
     * @param charset the charset
     * @throws CqiClientException
     */
    private synchronized void cqpQuery(String corpus, String subcorpus,
            String query, Charset charset) throws CqiClientException {
        try {
            this.streamToServer.write(CQI_CQP_QUERY);
            this.writeString(corpus);
            this.writeString(subcorpus);
            this.writeString(query, charset);
            this.readHeaderFromServer();
        } catch (IOException e) {
            throw new CqiClientException(SERVER_IO_ERROR, e);
        }

    }

    /**
     * Runs a CQP query.
     *
     * @param corpus the corpus
     * @param subcorpus the subcorpus
     * @param query the query
     * @param charset the charset
     * @throws CqiClientException
     */
    public synchronized CqiResult cqpQuery(String corpus, String query) throws CqiClientException {
        String queryName = String.format("Q%d", System.currentTimeMillis());
        String subcorpus = String.format("%s:%s", corpus, queryName);
        Charset charset;
        try {
            charset = Charset.forName(corpusCharset(corpus));
        } catch (IOException e) {
            throw new CqiClientException(SERVER_IO_ERROR, e);
        }
        cqpQuery(corpus, queryName, query, charset);
        return new CqiResult(this, corpus, subcorpus, charset, subCorpusSize(subcorpus));
    }

    /**
     * Lists all the subcorpora of a corpus.
     *
     * @param corpus the corpus
     * @return the name of the subcorpora
     * @throws CqiClientException
     */
    public synchronized String[] listSubcorpora(String corpus)
            throws CqiClientException {
        return genericStringToStringArray(corpus, CQI_CQP_LIST_SUBCORPORA);
    }

    /**
     * Gives the size of a subcorpus .
     *
     * @param subcorpus the subcorpus
     *
     * @return the size
     *
     * @throws CqiClientException
     */
    public synchronized int subCorpusSize(String subcorpus) throws
            CqiClientException {
        try {
            this.streamToServer.write(CQI_CQP_SUBCORPUS_SIZE);
            this.writeString(subcorpus);
            return this.readInt();
        } catch (IOException e) {
            throw new CqiClientException(SERVER_IO_ERROR, e);
        }
    }

    /**
     * Checks wether a subcorpus has a field.
     *
     * @param subcorpus the subcorpus
     * @param field the field
     *
     * @return true, if the subcorpus has the field
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws CqiClientException Signals that the data read on the socket is
     * unexpected
     */
    public synchronized boolean subCorpusHasField(String subcorpus, byte field)
            throws IOException, CqiClientException {
        this.streamToServer.write(CQI_CQP_SUBCORPUS_HAS_FIELD);
        this.writeString(subcorpus);
        this.streamToServer.writeByte(field);
        return this.readBoolean();
    }

    /**
     * Dumps the values of <field> for match ranges <first> .. <last> in
     * <subcorpus>. <field> is one of the CQI_CONST_FIELD_* constants.
     *
     * @param subcorpus the subcorpus
     * @param field the field
     * @param first the first
     * @param last the last
     *
     * @return the values
     *
     * @throws CqiClientException
     */
    public synchronized void dumpSubCorpus(String subcorpus, byte field,
            int first, int last, int[] output) throws CqiClientException {
        try {
            this.streamToServer.write(CQI_CQP_DUMP_SUBCORPUS);
            this.writeString(subcorpus);
            this.streamToServer.writeByte(field);
            this.streamToServer.writeInt(first);
            this.streamToServer.writeInt(last);
            this.readIntList(output);
        } catch (IOException e) {
            throw new CqiClientException(SERVER_IO_ERROR, e);
        }
    }

    /**
     * Drops a subcorpus.
     *
     * @param subcorpus the subcorpus
     *
     * @throws CqiClientException
     */
    public synchronized void dropSubCorpus(String subcorpus) throws
            CqiClientException {
        try {
            this.streamToServer.write(CQI_CQP_DROP_SUBCORPUS);
            this.writeString(subcorpus);
            this.readHeaderFromServer();
        } catch (IOException e) {
            throw new CqiClientException(SERVER_IO_ERROR, e);
        }
    }
}
