Instructions
============

#. The ``iudx-api-server`` directory needs to be mounted at ``/iudx-api-server/``
#. Takes ADMIN_PWD and POSTGRES_PWD as mandatory environment variables
#. Takes QUICK_INSTALL as an optional environment variable. If set to ``true`` it executes the existing ``jar`` file instead of running a ``mvn package``
#. Also takes valid `docker-compose-wait <https://github.com/ufoscout/docker-compose-wait>`_ arguments as optional environment variables.
