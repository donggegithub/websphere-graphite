WAS Agent
=========

A network tool for WebSphere Application Server monitoring, that provides performance statistics in a suitable format
for [Graphite][graphite].

* [Features](#features)
* [Concepts](#concepts)
* [Prerequisites](#prerequisites)
    * [General settings](#general-settings)
    * [PMI settings](#pmi-settings)
* [Installing WAS Agent](#installing-was-agent)
    * [Build](#build)
    * [Installation](#installation)
    * [Configuration](#configuration)
* [Using WAS Agent](#using-was-agent)
    * [Starting the agent](#starting-the-agent)
    * [Running queries](#running-queries)
    * [Options](#options)

Features
--------

Current features are:

 * JVM heap monitoring
 * Server thread pools monitoring
 * Transactions monitoring
 * JDBC datasources monitoring
 * JMS connection factories monitoring
 * SIB queues depth monitoring
 * HTTP sessions monitoring
 * Servlets service time monitoring
 * Clustering support

Concepts
--------

WAS Agent relies on the use of WebSphere Performance Monitoring infrastructure (PMI), as well as the regular
JMX API. The agent embedds a small Jetty container, and the monitoring itself is made through simple HTTP requests.
This approach allows short response times for monitoring queries, and a very low resource consumption.

Prerequisites
-------------

### General settings

You just need to install an IBM 1.6 JRE on each host you plan to run the agent.

### PMI settings

For each JVM you want to monitor, you have to enable the following statistics:

    JVM Runtime.HeapSize
    JVM Runtime.ProcessCpuUsage
    JVM Runtime.UsedMemory
    Thread Pools.ActiveCount
    Thread Pools.PoolSize
    Transaction Manager.ActiveCount
    JDBC Connection Pools.FreePoolSize
    JDBC Connection Pools.PoolSize
    JDBC Connection Pools.WaitingThreadCount
    JCA Connection Pools.FreePoolSize
    JCA Connection Pools.PoolSize
    JCA Connection Pools.WaitingThreadCount
    Servlet Session Manager.LiveCount
    Web Applications.ServiceTime

For WAS 7.0 and WAS 8.x only, you can also enable this one:

    Thread Pools.ConcurrentHungThreadCount

You can find below a sample Jython script to set the appropriate configuration with wsadmin.
It works with WAS 6.1, 7.0 and 8.x:

```python
# wasagent PMI settings script

# Change these values
node_name = 'hydre2'
server_name = 'h2srv1'

# PMI statistics values
stats = {'jvmRuntimeModule': '1,3,5',
         'threadPoolModule': '3,4,8',
         'transactionModule': '4',
         'connectionPoolModule': '5,6,7',
         'j2cModule': '5,6,7',
         'servletSessionsModule': '7',
         'webAppModule': '13'}

# Recursive function to configure the whole PMI subtree
def set_pmimodules(module, value):
    AdminConfig.modify(module, [['enable', value]])
    pmimodules = AdminConfig.showAttribute(module, 'pmimodules')
    if pmimodules != '[]':
        pmimodules = pmimodules[1:-1]
        pmimodules = pmimodules.split(' ')
        for pmimodule in pmimodules:
            set_pmimodules(pmimodule, value)

# Script starts here
import string

cell_name = AdminControl.getCell()
node_id = AdminConfig.getid('/Cell:%s/Node:%s' % (cell_name, node_name))
server_id = AdminConfig.getid('/Cell:%s/Node:%s/Server:%s' % (cell_name, node_name, server_name))

if len(node_id) == 0:
    raise Exception('invalid node name \'%s\'' % node_name)
if len(server_id) == 0:
    raise Exception('invalid server name \'%s\'' % server_name)

pmi_service = AdminConfig.list('PMIService', server_id)
pmi_module =  AdminConfig.list('PMIModule', server_id)

print '\nSetting custom statistic set level...'
params = [['enable', 'true'], ['statisticSet', 'custom']]
AdminConfig.modify(pmi_service, params)
print 'ok.'

modules = AdminConfig.showAttribute(pmi_module, 'pmimodules')
modules = modules[1:-1]
modules = modules.split(' ')

print '\nEnabling specific PMI counters...'
for key in stats.keys():
    for module in modules:
        if AdminConfig.showAttribute(module, 'moduleName') == key:
           print string.ljust(' . %s:' % key, 25), string.ljust(stats[key], 0)
           set_pmimodules(module, stats[key])
print 'ok.'

print '\nSaving changes...'
AdminConfig.save()
print 'ok.'
```

If you want to make runtime tests, you can use the following snippets:

```python
# wasagent PMI settings script
# Use only with WAS 8.x and 7.0 versions

# Change these values
node_name = 'hydre2'
server_name = 'h2srv1'

# Script starts here
perf_name = AdminControl.completeObjectName('type=Perf,node=%s,process=%s,*' % (node_name, server_name))
signature  = ['java.lang.String', 'java.lang.Boolean']

# JVM settings
params = ['jvmRuntimeModule=1,3,5', java.lang.Boolean('true')]
AdminControl.invoke(perf_name, 'setCustomSetString', params, signature)

# Thread pools settings
params = ['threadPoolModule=3,4,8', java.lang.Boolean('true')]
AdminControl.invoke(perf_name, 'setCustomSetString', params, signature)

# Transactions settings
params = ['transactionModule=4', java.lang.Boolean('true')]
AdminControl.invoke(perf_name, 'setCustomSetString', params, signature)

# JDBC connection pools setting
params = ['connectionPoolModule=5,6,7', java.lang.Boolean('true')]
AdminControl.invoke(perf_name, 'setCustomSetString', params, signature)

# JMS connection factories settings
params = ['j2cModule=5,6,7', java.lang.Boolean('true')]
AdminControl.invoke(perf_name, 'setCustomSetString', params, signature)

# HTTP sessions settings
params = ['servletSessionsModule=7', java.lang.Boolean('true')]
AdminControl.invoke(perf_name, 'setCustomSetString', params, signature)

# Servlets settings
params = ['webAppModule=13', java.lang.Boolean('true')]
AdminControl.invoke(perf_name, 'setCustomSetString', params, signature)

# Uncomment to persist changes
#
#AdminControl.invoke(perf_name, 'savePMIConfiguration')
```

```python
# wasagent PMI settings script
# Use only with WAS 6.1 version

# Change these values
node_name = 'hydre2'
server_name = 'h2srv1'

# Script starts here
perf_name = AdminControl.completeObjectName('type=Perf,node=%s,process=%s,*' % (node_name, server_name))
perf_mbean = AdminControl.makeObjectName(perf_name)

signature  = ['java.lang.String', 'java.lang.Boolean']

# JVM settings
params = ['jvmRuntimeModule=1,3,5', java.lang.Boolean('true')]
AdminControl.invoke_jmx(perf_mbean, 'setCustomSetString', params, signature)

# Thread pools settings
params = ['threadPoolModule=3,4', java.lang.Boolean('true')]
AdminControl.invoke_jmx(perf_mbean, 'setCustomSetString', params, signature)

# Transactions settings
params = ['transactionModule=4', java.lang.Boolean('true')]
AdminControl.invoke_jmx(perf_mbean, 'setCustomSetString', params, signature)

# JDBC connection pools setting
params = ['connectionPoolModule=5,6,7', java.lang.Boolean('true')]
AdminControl.invoke_jmx(perf_mbean, 'setCustomSetString', params, signature)

# JMS connection factories settings
params = ['j2cModule=5,6,7', java.lang.Boolean('true')]
AdminControl.invoke_jmx(perf_mbean, 'setCustomSetString', params, signature)

# HTTP sessions settings
params = ['servletSessionsModule=7', java.lang.Boolean('true')]
AdminControl.invoke_jmx(perf_mbean, 'setCustomSetString', params, signature)

# Servlets settings
params = ['webAppModule=13', java.lang.Boolean('true')]
AdminControl.invoke_jmx(perf_mbean, 'setCustomSetString', params, signature)

# Uncomment to persist changes
#
#AdminControl.invoke(perf_name, 'savePMIConfiguration')
```

Installing WAS Agent
--------------------

### Build

Download a zip archive of the master branch by using the link on this page. The two directories we are interested in
are 'src' and 'lib'. First things first, copy the WebSphere admin client jar in the lib directory. You can find the
required file in the 'runtimes' directory of your product installation.

WAS 8.0:

`${WAS_INSTALL_ROOT}/runtimes/com.ibm.ws.admin.client_8.0.0.jar`

WAS 7.0:

`${WAS_INSTALL_ROOT}/runtimes/com.ibm.ws.admin.client_7.0.0.jar`

WAS 6.1:

`${WAS_INSTALL_ROOT}/runtimes/com.ibm.ws.admin.client_6.1.0.jar`

You can use Apache Ant and the provided 'build.xml' file to build the plugin.

### Installation

For WAS 7.0 and WAS 8.x, you need one instance of the agent per cell, which you will typically deploy on the dmgr host.
For WAS 6.1, you need one instance of the agent per node.

Assuming you are using a root directory named 'wasagent', the layout will be as following:

```
wasagent
|-- lib
|   |-- com.ibm.ws.admin.client_X.X.X.jar
|   |-- jetty-servlet-7.6.2.v20120308.jar
|   `-- servlet-api-2.5.jar
|-- run.sh
|-- wasagent.jar
`-- websphere.properties
```
Just copy appropriate files and directories from your build directory into the 'wasagent' directory.

### Configuration

The 'websphere.properties' file contains the basic informations used to connect to a WebSphere Application Server.
You have to set the contents of this file according to your environment.

```bash
# WAS Agent properties file
#
# These credentials are used to connect to each
# target WAS instance of the cell. The user has
# to be mapped to the 'monitor' role, no need to
# use an administrative account.
#
username=
password=

# These keystores are used to establish a secured connection beetween
# the agent and the target WAS instance. "${USER_INSTALL_ROOT}" should
# be replaced by the dmgr profile path of the cell for WAS 7.0 and 8.x,
# or by the nodeagent profile path of the node for WAS 6.1.
#
# PKCS12 keystores sample config
#
javax.net.ssl.trustStore=${USER_INSTALL_ROOT}/etc/trust.p12
javax.net.ssl.trustStorePassword=WebAS
javax.net.ssl.trustStoreType=PKCS12
javax.net.ssl.keyStore=${USER_INSTALL_ROOT}/etc/key.p12
javax.net.ssl.keyStorePassword=WebAS
javax.net.ssl.keyStoreType=PKCS12
#
# JKS keystores sample config (default keystore type is JKS)
#
#javax.net.ssl.trustStore=${USER_INSTALL_ROOT}/etc/DummyClientTrustFile.jks
#javax.net.ssl.trustStorePassword=WebAS
#javax.net.ssl.keyStore=${USER_INSTALL_ROOT}/etc/DummyClientKeyFile.jks
#javax.net.ssl.keyStorePassword=WebAS
```

Using WAS Agent
---------------

### Starting the agent

Amend the 'run.sh' script to set the path of your JRE installation, and type:

```
./run.sh
```

This script is provided for testing purpose only, so you will have to write your own init script.

### Running queries

We assume that the agent is running and listening on 'hydre1:9090', and we want to monitor a remote WAS instance
listening on its default SOAP connector (8880).

Let's try this simple query:

    curl -X POST -d 'hostname=hydre2&port=8880&jvm&thread-pool=Default,WebContainer' http://hydre1:9090/wasagent/WASAgent

We get the following output:

    hydre2.h2srv1.jvm.cpuUsage 1 1357230342
    hydre2.h2srv1.jvm.currentHeapSize 160 1357230342
    hydre2.h2srv1.jvm.currentHeapUsed 102 1357230342
    hydre2.h2srv1.jvm.maximumHeapSize 256 1357230342
    hydre2.h2srv1.threadPool.Default.activeCount 2 1357230342
    hydre2.h2srv1.threadPool.Default.currentPoolSize 4 1357230342
    hydre2.h2srv1.threadPool.Default.hungCount 0 1357230342
    hydre2.h2srv1.threadPool.Default.maximumPoolSize 20 1357230342
    hydre2.h2srv1.threadPool.WebContainer.activeCount 0 1357230342
    hydre2.h2srv1.threadPool.WebContainer.currentPoolSize 0 1357230342
    hydre2.h2srv1.threadPool.WebContainer.hungCount 0 1357230342
    hydre2.h2srv1.threadPool.WebContainer.maximumPoolSize 50 1357230342

By default, the metric scheme is build as follows:

    <target hostname> + <WAS logical name> + <test type> + (item) + <metric name> <value> <timestamp>

The last part of the metric (from the WAS logical name) can not be changed. However, you can provide a prefix and / or
a suffix option, for instance:

    curl -X POST -d 'prefix=emea.fr&hostname=hydre2&suffix=was&port=8880&jvm&thread-pool=Default,WebContainer' ...

Which will give you this result:

    emea.fr.hydre2.was.h2srv1.jvm.cpuUsage 1 1357237139
    emea.fr.hydre2.was.h2srv1.jvm.currentHeapSize 160 1357237139
    emea.fr.hydre2.was.h2srv1.jvm.currentHeapUsed 111 1357237139
    emea.fr.hydre2.was.h2srv1.jvm.maximumHeapSize 256 1357237139
    emea.fr.hydre2.was.h2srv1.threadPool.Default.activeCount 1 1357237139
    emea.fr.hydre2.was.h2srv1.threadPool.Default.currentPoolSize 4 1357237139
    emea.fr.hydre2.was.h2srv1.threadPool.Default.hungCount 0 1357237139
    emea.fr.hydre2.was.h2srv1.threadPool.Default.maximumPoolSize 20 1357237139
    emea.fr.hydre2.was.h2srv1.threadPool.WebContainer.activeCount 0 1357237139
    emea.fr.hydre2.was.h2srv1.threadPool.WebContainer.currentPoolSize 0 1357237139
    emea.fr.hydre2.was.h2srv1.threadPool.WebContainer.hungCount 0 1357237139
    emea.fr.hydre2.was.h2srv1.threadPool.WebContainer.maximumPoolSize 50 1357237139

You can group different options in a single query. If metric names contain characters that can not be used in a metric
scheme (like "." and "/"), they will be replaced by an underscore.

### Options

Here is a summary of the available options:

#### hostname (mandatory)

The hostname of the WAS instance you want to monitor.

#### port (mandatory)

The SOAP connector of the WAS instance you want to monitor.

#### prefix (optional)

The prefix string will be placed at the beginning of the metric scheme. Don't use a trailing period (i.e. 'emea.fr' is
correct, 'emea.fr.' is not).

#### suffix (optional)

The suffix string will placed right after the hostname in the metric scheme. Don't use a leading period (i.e. 'was' is
correct, '.was' is not).

#### jvm (optional)

This option doesn't take any value. It produces the following output:

    jvm.cpuUsage
    jvm.currentHeapSize
    jvm.currentHeapUsed
    jvm.maximumHeapSize

#### thread-pool (optional)

This option takes a comma separated list of thread pool names, or a wildcard character (`thread-pool=*`) for the whole
thread pool list. The following output is produced when the wildcard character is passed:

    threadPool.Default.activeCount
    threadPool.Default.currentPoolSize
    threadPool.Default.hungCount
    threadPool.Default.maximumPoolSize
    threadPool.HAManager_thread_pool.activeCount
    threadPool.HAManager_thread_pool.currentPoolSize
    threadPool.HAManager_thread_pool.hungCount
    threadPool.HAManager_thread_pool.maximumPoolSize
    threadPool.MessageListenerThreadPool.activeCount
    threadPool.MessageListenerThreadPool.currentPoolSize
    threadPool.MessageListenerThreadPool.hungCount
    threadPool.MessageListenerThreadPool.maximumPoolSize
    threadPool.ORB_thread_pool.activeCount
    threadPool.ORB_thread_pool.currentPoolSize
    threadPool.ORB_thread_pool.hungCount
    threadPool.ORB_thread_pool.maximumPoolSize
    threadPool.ProcessDiscovery.activeCount
    threadPool.ProcessDiscovery.currentPoolSize
    threadPool.ProcessDiscovery.hungCount
    threadPool.ProcessDiscovery.maximumPoolSize
    threadPool.SIBFAPInboundThreadPool.activeCount
    threadPool.SIBFAPInboundThreadPool.currentPoolSize
    threadPool.SIBFAPInboundThreadPool.hungCount
    threadPool.SIBFAPInboundThreadPool.maximumPoolSize
    threadPool.SIBFAPThreadPool.activeCount
    threadPool.SIBFAPThreadPool.currentPoolSize
    threadPool.SIBFAPThreadPool.hungCount
    threadPool.SIBFAPThreadPool.maximumPoolSize
    threadPool.SoapConnectorThreadPool.activeCount
    threadPool.SoapConnectorThreadPool.currentPoolSize
    threadPool.SoapConnectorThreadPool.hungCount
    threadPool.SoapConnectorThreadPool.maximumPoolSize
    threadPool.TCPChannel_DCS.activeCount
    threadPool.TCPChannel_DCS.currentPoolSize
    threadPool.TCPChannel_DCS.hungCount
    threadPool.TCPChannel_DCS.maximumPoolSize
    threadPool.WMQJCAResourceAdapter.activeCount
    threadPool.WMQJCAResourceAdapter.currentPoolSize
    threadPool.WMQJCAResourceAdapter.hungCount
    threadPool.WMQJCAResourceAdapter.maximumPoolSize
    threadPool.WebContainer.activeCount
    threadPool.WebContainer.currentPoolSize
    threadPool.WebContainer.hungCount
    threadPool.WebContainer.maximumPoolSize

#### jta (optional)

This option doesn't take any value. It produces the following output:

    jta.activeCount

#### jdbc (optional)

This option takes a comma separated list of datasource names, or a wildcard character (`jdbc=*`) for the whole
datasource list. It produces the following output:

    jdbc.<item>.activeCount
    jdbc.<item>.currentPoolSize
    jdbc.<item>.maximumPoolSize
    jdbc.<item>.waitingThreadCount
    
#### jms (optional)

This option takes a comma separated list of JMS connection factory names, or a wildcard character (`jms=*`) for the
whole connection factory list. It produces the following output:

    jms.<item>.activeCount
    jms.<item>.currentPoolSize
    jms.<item>.maximumPoolSize
    jms.<item>.waitingThreadCount

#### application (optional)

This option takes a comma separated list of application names, or a wildcard character (`application=*`) for the whole
application list. It produces the following output:

    application.<item>.liveCount
    
"item" is of the form `<logical application name>#<web module name>`.

#### servlet (optional)

This option takes a comma separated list of servlet names, or a wildcard character (`servlet=*`) for the whole
servlet list. It produces the following output:

    servlet.<item>.serviceTime

"item" is of the form `<logical application name>#<web module name>.<servlet name>`.

[graphite]: http://graphite.wikidot.com
[wasagent]: http://yannlambret.github.com/websphere/
