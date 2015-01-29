====================================================
Edition for Android - dependencies between JAR files
====================================================


Below is a list of the dependencies between Restlet libraries. You need to ensure 
that all the dependencies of the libraries that you are using are on the classpath
of your Restlet program, otherwise ClassNotFound exceptions will be thrown.

A minimal Restlet application requires the org.restlet.jar file.

To configure connectors such as HTTP server or HTTP client connectors, please refer
to the Restlet User Guide: http://restlet.org/learn/guide/2.2/


org.restlet.ext.atom (Restlet Extension - Atom)
--------------------
 - nothing beside org.restlet JAR.

org.restlet (Restlet Core - API and Engine)
-----------
 - J2SE 6.0

org.restlet.ext.crypto (Restlet Extension - Crypto)
----------------------
 - nothing beside org.restlet JAR.

org.restlet.ext.gson (Restlet Extension - GSON)
--------------------
 - com.google.code.gson_2.2
 - org.joda.time_2.3

org.restlet.ext.html (Restlet Extension - HTML)
--------------------
 - nothing beside org.restlet JAR.

org.restlet.ext.httpclient (Restlet Extension - Apache HTTP Client)
--------------------------
 - org.apache.commons.codec_1.5
 - org.apache.httpclient_4.3
 - org.apache.httpcore_4.3
 - org.apache.httpmime_4.3
 - net.jcip.annotations_1.0
 - org.apache.commons.logging_1.1
 - org.apache.james.mime4j_0.7

org.restlet.ext.jaas (Restlet Extension - JAAS)
--------------------
 - nothing beside org.restlet JAR.

org.restlet.ext.jackson (Restlet Extension - Jackson)
-----------------------
 - com.fasterxml.jackson.annotations_2.2
 - com.fasterxml.jackson.core_2.2
 - com.fasterxml.jackson.csv_2.2
 - com.fasterxml.jackson.databind_2.2
 - com.fasterxml.jackson.jaxb_2.2
 - com.fasterxml.jackson.jsonschema_2.2
 - com.fasterxml.jackson.smile_2.2
 - com.fasterxml.jackson.xml_2.2
 - com.fasterxml.jackson.yaml_2.2
 - org.yaml.snakeyaml_1.13
 - org.codehaus.woodstox.core_4.2
 - org.codehaus.woodstox.stax2api_4.2

org.restlet.ext.json (Restlet Extension - JSON)
--------------------
 - org.json_2.0

org.restlet.ext.jsslutils (Restlet Extension - jSSLutils)
-------------------------
 - org.jsslutils_1.0

org.restlet.ext.nio (Restlet Extension - NIO)
-------------------
 - nothing beside org.restlet JAR.

org.restlet.ext.odata (Restlet Extension - OData)
---------------------
 - nothing beside org.restlet JAR.

org.restlet.ext.rdf (Restlet Extension - RDF)
-------------------
 - nothing beside org.restlet JAR.

org.restlet.ext.sip (Restlet Extension - SIP)
-------------------
 - nothing beside org.restlet JAR.

org.restlet.ext.thymeleaf (Restlet Extension - Thymeleaf)
-------------------------
 - org.thymeleaf_2.1

org.restlet.ext.xml (Restlet Extension - XML)
-------------------
 - nothing beside org.restlet JAR.
