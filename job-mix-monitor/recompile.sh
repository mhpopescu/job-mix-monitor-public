#!/bin/bash

export JAVA_HOME=`cat conf/env.JAVA_HOME`
export PATH=$JAVA_HOME/bin:$PATH

mkdir bin 2> /dev/null

CP="."

for a in lib/*.jar; do
    CP="$CP:$a"
done

javac -classpath ${CP} src/*.java -d bin