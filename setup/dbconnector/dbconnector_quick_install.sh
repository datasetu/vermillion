#!/bin/bash

cd iudx-dbconnector

if [ -e iudx-dbconnector-0.0.1-SNAPSHOT-jar-with-dependencies.jar ]
then 
rm iudx-dbconnector-0.0.1-SNAPSHOT-jar-with-dependencies.jar
fi

cp target/iudx-dbconnector-0.0.1-SNAPSHOT-jar-with-dependencies.jar .

tmux new-session -d -s dbconnector 'java -jar iudx-dbconnector-0.0.1-SNAPSHOT-jar-with-dependencies.jar -d64 -Xms512m -Xmx4g'
