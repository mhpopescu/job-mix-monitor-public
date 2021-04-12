#!/bin/bash

#
# run_client.sh clusters
#
# example:
#   run_client.sh "HPCS:LBL:ORNL"
#

export JAVA_HOME=`cat conf/env.JAVA_HOME`
export PATH=$JAVA_HOME/bin:$PATH
CP="bin"
for a in lib/*.jar; do
    CP="$CP:$a"
done

# argument with default value
clusters=${1-Hiroshima:LBL_HPCS:Bari:GRIF_IPNO:Legnaro:CNAF:Kolkata-CREAM:ORNL:UPB:NIHAM:GSI:Torino:Subatech}
clustersEos=${2-UPB:ORNL:LBL_HPCS}

pwd
mkdir log 2> /dev/null
tag=`date +%Y.%m.%d-%H:%M:%S`
mv log/JobMixClient log/JobMixClient-$tag 2> /dev/null
mv log/app log/app-$tag 2> /dev/null

echo "running JobMixClient ..."
echo ;

echo "logs are in \"log\" directory"
echo ;

# Use this for testing outside docker
# url="http://localhost:5000/api/"

url="http://util:5000/api"

java \
    -server -Xmx256m -XX:CompileThreshold=500 \
    -classpath ${CP} \
    -Djava.security.policy=conf/policy.all \
    -Dlia.Monitor.ConfigURL=file:conf/App.properties \
    -Djava.util.logging.config.class=lia.Monitor.monitor.LoggerConfigClass \
    JobMixClient -u $url -c $clusters -e $clustersEos>> log/JobMixClient 2>&1

echo ;
