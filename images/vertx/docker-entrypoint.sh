#!/bin/bash

if [ "$QUICK_INSTALL" == "true" ];
    then
    cd iudx-api-server
    if [ -e iudx-api-server-0.0.1-SNAPSHOT-fat.jar ]
    then
    rm iudx-api-server-0.0.1-SNAPSHOT-fat.jar
    fi
    cp target/iudx-api-server-0.0.1-SNAPSHOT-fat.jar .
    
    java $JAVA_OPTIONS -jar iudx-api-server-0.0.1-SNAPSHOT-fat.jar
else
    cd iudx-api-server
    mvn -T 1C clean
    mvn -T 1C package
    cp target/iudx-api-server-0.0.1-SNAPSHOT-fat.jar .
    java $JAVA_OPTIONS -jar iudx-api-server-0.0.1-SNAPSHOT-fat.jar
fi
