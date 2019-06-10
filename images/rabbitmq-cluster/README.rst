Instructions
============

#. Rabbitmq configuration files and the SSL certificates need to be mounted at ``/etc/rabbitmq`` 
#. Takes ADMIN_PWD and POSTGRES_PWD as environment variables
#. Takes ERLANG_COOKIE as an environment variable. This is to set the erlang cookie on all rabbitmq nodes.
#. Optionally takes WAIT_HOSTS as an environment variable. Used to wait for the first node in the cluster to start up.
