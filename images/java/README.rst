Instructions
============

#. The java project directory needs to be mounted at ``/``
#. Takes ``PROJECT_DIR`` and ``JAR_NAME`` as mandatory environment variables
#. Optionally takes ``JAVA_OPTS`` environment variable for setting java options during runtime
#. Takes ``RUN_ONLY`` as an optional environment variable. If set to ``true`` it runs the jar file specified in ``JAR_NAME`` instead of compiling and running
#. Also takes valid `docker-compose-wait <https://github.com/ufoscout/docker-compose-wait>`_ arguments as optional environment variables.
#. Optionally takes ``MVN_OPTS`` environment variable for setting mvn options during runtime

