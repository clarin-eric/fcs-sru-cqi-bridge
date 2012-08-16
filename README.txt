GENERAL INFO:
-------------
This package implements an SRU end point front end for a CQI Server. CQL queries
are translated into CQP and sent to the CQI server then the result is written in
SRU specific format. Currently only one corpus is supported.

More Information about CQI:
    http://cwb.sourceforge.net/cqi.php
More Information about CWB:
    http://cwb.sourceforge.net/

HOW TO USE:
-----------
Make sure you have SRU/CQL Server package. Modify sru-server-config.xml to have
information about your corpora. In addition you'll have to modify web.xml to
contain the configuration of your CQI server, in particular the following params
must be present:
cqi.serverHost - the host name of your CQI server
cqi.serverPort - the port of your CQI server
cqi.serverUsername - the user name on your CQI server
cqi.serverPassword - the password for the above user
cqi.defaultCorpus - a corpus which will be used for all the queries
cqi.defaultCorpusPID - the PID of the corpus above
