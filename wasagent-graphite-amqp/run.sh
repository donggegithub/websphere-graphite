#!/bin/bash

cd $(dirname "$0")

# Must point to a valid IBM 1.6 JRE installation
JAVA_HOME=""
JAVA_OPTS="-Dwasagent.host=0.0.0.0 -Dwasagent.port=9090 -Dwasagent.configuration=websphere.properties"

# WAS Agent classpath
CLASSPATH=".:wasagent.jar"
for jar in $(find "lib" -name '*.jar'); do
  CLASSPATH=${CLASSPATH}:${jar};
done

# Starts the agent
${JAVA_HOME}/bin/java -Xmx16m -cp ${CLASSPATH} ${JAVA_OPTS} net.wait4it.graphite.wasagent.core.WASAgent > /dev/null 2>&1 &
