# Upgrade Tutorial

## Legacy (v1.2) to FCS 2.0 (v2.0)

### System changes

- Changed compiler target version to Java 1.8
- Changes in dependencies:
    - Upgraded: `eu.clarin.sru:sru-server` from `1.5.0` to `1.8.0`
    - Added: `eu.clarin.sru.fcs:fcs-simple-endpoint:1.6.0`
    - Upgraded: `slf4j.version` to `1.7.32` (same as SRU/FCS dependencies)

### Configuration changes

#### [`src/main/webapp/WEB-INF/endpoint-description.xml`](src/main/webapp/WEB-INF/endpoint-description.xml)

Migrating the resource description from [`resource-info.xml`](src/main/webapp/WEB-INF/resource-info.xml) to [`endpoint-description.xml`](src/main/webapp/WEB-INF/endpoint-description.xml)

#### [`src/main/webapp/WEB-INF/sru-server-config.xml`](src/main/webapp/WEB-INF/sru-server-config.xml)

Changing the schema namespace from `http://clarin.eu/fcs/1.0` to `http://clarin.eu/fcs/resource`:

```diff
@@ -20,7 +20,7 @@
     </databaseInfo>

     <indexInfo>
-        <set name="fcs" identifier="clarin.eu/fcs/1.0">
+        <set name="fcs" identifier="http://clarin.eu/fcs/resource">
             <title xml:lang="de">CLARIN Content Search</title>
             <title xml:lang="en" primary="true">CLARIN Content Search</title>
         </set>
@@ -39,7 +39,7 @@
     </indexInfo>

     <schemaInfo>
-        <schema identifier="http://clarin.eu/fcs/1.0" name="fcs" sort="false" retrieve="true">
+        <schema identifier="http://clarin.eu/fcs/resource" name="fcs" sort="false" retrieve="true">
             <title xml:lang="en" primary="true">CLARIN Content Search</title>
         </schema>
     </schemaInfo>
```

#### [`src/main/webapp/WEB-INF/web.xml`](src/main/webapp/WEB-INF/web.xml)

Enabling FCS 2.0 by setting SRU version:

```diff
@@ -62,6 +73,15 @@
            <param-name>eu.clarin.sru.server.database</param-name>
            <param-value>rws/sru/</param-value>
        </init-param>
+        <!-- To enable SRU 2.0 for FCS 2.0 -->
+        <init-param>
+            <param-name>eu.clarin.sru.server.sruSupportedVersionMax</param-name>
+            <param-value>2.0</param-value>
+        </init-param>
+        <init-param>
+            <param-name>eu.clarin.sru.server.legacyNamespaceMode</param-name>
+            <param-value>loc</param-value>
+        </init-param>
        <init-param>
            <param-name>eu.clarin.sru.server.utils.sruServerSearchEngineClass</param-name>
            <param-value>eu.clarin.sru.cqibridge.CqiSRUSearchEngine</param-value>
```

