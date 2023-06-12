# FCS SRU CQI Bridge

This package implements an FCS 2.0 SRU endpoint front end for a CQI Server. CQL queries are translated into CQP and sent to the CQI server, then the result is written in an SRU/FCS specific format.

If you previously deployed the CQI-Bridge in version `1.2`, take a look at [`UPGRADING.md`](UPGRADING.md) for migrating to FCS 2.0 (version `2.0`).

Limitations:
- Currently only one corpus is supported! _For multiple corpora take a look at the [fcs-korp-endpoint](https://github.com/clarin-eric/fcs-korp-endpoint) example implementation._
- only supports BASIC search (Hits DataView) via CQL _(basically full-text search)_

More Information about
- CQI: http://cwb.sourceforge.net/cqi.php
- CWB: http://cwb.sourceforge.net/
- FCS: https://www.clarin.eu/content/content-search

## Configuration

Modify [`sru-server-config.xml`](src/main/webapp/WEB-INF/sru-server-config.xml) and [`endpoint-description.xml`](src/main/webapp/WEB-INF/endpoint-description.xml) to have information about your corpora.

In addition you'll have to modify [`web.xml`](src/main/webapp/WEB-INF/web.xml) to contain the configuration of your CQI server, in particular the following params must be present:
- `cqi.serverHost` - the host name of your CQI server
- `cqi.serverPort` - the port of your CQI server
- `cqi.serverUsername` - the user name on your CQI server
- `cqi.serverPassword` - the password for the above user
- `cqi.defaultCorpus` - a corpus which will be used for all the queries
- `cqi.defaultCorpusPID` - the PID of the corpus above
- `cqi.defaultCorpusRef` - the Ref of the corpus above

You may need to change the CQI [`CONTEXT_STRUCTURAL_ATTRIBUTE = "s"`](src/main/java/eu/clarin/sru/cqibridge/CqiSRUSearchEngine.java) if your corpus is structured differently.

### Legacy Support

By default, this endpoint will wrap results with the FCS _Hits_ Data View. If you need to support the legacy FCS _KWIC_ Data View (which is deprecated) but was the only Data View supported by the previous version of the endpoint implementation, you can re-enable it by setting the `eu.clarin.sru.cqibridge.supportLegacyKWIC` parameter in the [`web.xml`](src/main/webapp/WEB-INF/web.xml) to `true`. Both the Hits and KWIC Data Views will then be sent.
_Note, however, that in some cases clients will aggregate results from both Data Views (to support legacy endpoints) so that results may appear twice._

## Building & Deployment

To build the application run:

```bash
mvn clean package
```

The application war file (`sru-cqibridge-2.0.war`) can be found in the folder [`target/`](target/).
Place and link it into the webapps folder of your Jetty or Tomcat ... server for deployment.

### Requirements

- Java 8+
- Some `war` webapps deployment server like Jetty or Tomcat
- a locally reachable CQI/CQP-Server to connect and send queries to
