#!/bin/bash

set -e

[ -z "$PROJECT_DIR" ] && echo "PROJECT_DIR is not set" && exit 1
[ -z "$JAR_NAME" ] && echo "JAR_NAME is not set" && exit 1
#[ -z "$JAVA_OPTS" ] && echo "JAR_OPTS is not set" && exit 1
if [ "$ENVI" == "test" ];
then

  if [ "$RUN_ONLY" == "true" ];

    then
      cd $PROJECT_DIR
      if [ -e $JAR_NAME ]
      then
	    rm $JAR_NAME
      fi
      cp target/$JAR_NAME .
      java $JAVA_OPTS -jar $JAR_NAME
 else
    cd $PROJECT_DIR
    mvn $MVN_OPTS clean package
    cp target/$JAR_NAME .
    java $JAVA_OPTS -jar $JAR_NAME
fi

else

if [ "$RUN_ONLY" == "true" ];

then
    cd $PROJECT_DIR
    if [ -e $JAR_NAME ]
    then
	rm $JAR_NAME
    fi
    cp target/$JAR_NAME .
    java -jar $JAR_NAME
else
    cd $PROJECT_DIR
    mvn $MVN_OPTS clean package
    cp target/$JAR_NAME .
    java  -jar $JAR_NAME
fi
fi
ENV JAVA_TOOL_OPTIONS