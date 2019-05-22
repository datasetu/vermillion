Stuff To do
===========

Initial setup
-------------

#. Add ssh key of the controller to all machines - Could be manual or automated
#. Use a non root user - Probably later
#. Install python3, python3-pip, python3-setuptools, ansible, docker and docker-compose on the controller machine
#. If non root user is used, add user to the docker group
#. Run ansible all -u {{user}} -m ping
#. If step 3 is successful, then use a role to do initial setup on all hosts - Install python3, python3-pip python-setuptools, build-essential, docker, docker-compose

Swarm tasks
-----------

#. Identify the list of managers and workers
#. Pick a random manager node and run docker swarm init
#. Run ``docker swarm join-token manager`` on other manager nodes
#. Add wokers to the swarm using the join token
#. Run the docker-compose file on a manager
#. Add cron job to run docker ps on system restart


RabbitMQ tasks
--------------
#. Try using RabbitMQ in cluster mode - Most likely the throughput is going to be low
#. Modify the proxy code to add shovel when a bind is called - If it gets too complex, use an HTTP API to achieve the same
#. Generate docker-compose files based on the supplied configuration
