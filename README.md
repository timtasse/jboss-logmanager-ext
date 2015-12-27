JBoss Log Manager Extended
============

This provides some extended support for the JBoss Log Manager. These are experiments that may eventually make it into
the [JBoss Log Manager](https://github.com/jboss-logging/jboss-logmanager). Items that make it into the JBoss Log Manager
will be deprecated for at most one release before they are removed.

2 Implementations are currently available
*  SocketHandler<br />
   for use with LogStash or other LoggingUtilities
*  ZippedPeriodicRotatingFileHandler<br />
   Enhanced PeriodicRotatingFileHandler with zipping of rotated file and maxBackups is implemented

Installation
------------
Build the project with maven and then copy the jar file to a folder that is accessible by the WildFly.
Use the jboss-cli to add the module
```
module add --name=org.jboss.logmanager.ext --dependencies=org.jboss.logmanager,javax.json.api,javax.xml.stream.api --resources=/tmp/jboss-logmanager-ext-1.0.0.Alpha5-SNAPSHOT.jar`
```
the Logger can than configured by WebUI or the jboss-cli or direct in the config.
Maybe the WebUI or jboss-cli does not detect all properties correctly, then the config must used.
```xml
<custom-handler name="SERVER" class="org.jboss.logmanager.ext.handlers.ZippedPeriodicRotatingFileHandler" module="org.jboss.logmanager.ext">
    <formatter>
        <named-formatter name="PATTERN"/>
    </formatter>
    <properties>
        <property name="append" value="true"/>
        <property name="zipFormat" value="GZIP"/>
        <property name="suffix" value=".yyyy-MM-dd"/>
        <property name="autoFlush" value="true"/>
        <property name="maxBackups" value="14"/>
        <property name="fileName" value="${jboss.server.log.dir}/server.log"/>
    </properties>
</custom-handler>
```

ZippedPeriodicRotatingFileHandler
------------

It supports 2 zipformats, the gzip and the classic zip format.
Can configured with the following properties.

| Property  | Values |
| --------- | ------ |
| append    | true,false |
| autoFlush | true,false |
| suffix    | SimpleDateFormat parsable String |
| zipFormat | GZIP,ZIP |
| maxBackups | Integer (0 means no deletion) |
| fileName  | Path for this Logfile |

SocketHandler
------------

Sends the logmessage over the network via TCP or UDP connection.
The correct Formatter for the remote logging system must used, for example JSON.
The Formatter JsonFormatter, XmlFormatter and LogstashFormatter are included in the package.
Can configured with the following properties.

| Property  | Values |
| --------- | ------ |
| hostname  | the hostname to connect to |
| port      | the TCP or UDP port |
| protocol  | TCP or UDP |

