/**
 * This software is copyright (c) 2012 by
 *  - Seminar fuer Sprachwissenschaft  (http://www.sfs.uni-tuebingen.de/)
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

public class CqiClientException extends Exception {

    public CqiClientException(String message) {
        super(message);
    }

    public CqiClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
