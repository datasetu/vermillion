#!/bin/bash

if [ "$QUICK_INSTALL" == "true" ];
    then
    cd iudx-authenticator
    if [ -e iudx-authenticator-0.0.1-SNAPSHOT-fat.jar ]
    then
    rm iudx-authenticator-0.0.1-SNAPSHOT-fat.jar
    fi
    java -jar target/iudx-authenticator-0.0.1-SNAPSHOT-fat.jar
else
    cd iudx-authenticator
    mvn clean
    mvn package -DskipTests
    java -jar target/iudx-authenticator-0.0.1-SNAPSHOT-fat.jar
fi
