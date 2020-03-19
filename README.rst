.. raw:: html

  <p align="center"><img src="logo.png" alt="vermillion_logo"></p>

|github-workflow| |codacy| |license| |dependabot| |docker-build-status| |docker-build-automation| |gitter|

.. |github-workflow|  image:: https://github.com/rbccps-iisc/vermillion/workflows/CI/badge.svg
   :target: https://github.com/rbccps-iisc/vermillion/actions         
.. |license| image:: https://img.shields.io/github/license/rbccps-iisc/vermillion
   :target: https://github.com/rbccps-iisc/vermillion/blob/master/LICENSE
.. |codacy| image:: https://api.codacy.com/project/badge/Grade/d5c93fe3cec44982bcdcca7470a27b68    
   :target: https://www.codacy.com/manual/pct960/vermillion?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=rbccps-iisc/vermillion&amp;utm_campaign=Badge_Grade
.. |dependabot| image:: https://img.shields.io/badge/dependabot-enabled-yellow
   :target: https://dependabot.com/
.. |docker-build-status| image:: https://img.shields.io/docker/cloud/build/iudx/java
   :target: https://hub.docker.com/repository/docker/iudx/java/builds
.. |docker-build-automation| image:: https://img.shields.io/docker/cloud/automated/iudx/java
   :target: https://hub.docker.com/repository/docker/iudx/java/builds
.. |gitter| image:: https://badges.gitter.im/vermillion-chat/community.svg
   :target: https://gitter.im/vermillion-chat/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge   
    
Vermillion is a high performance, scalable and secure IoT middleware platform developed using `Vertx <https://vertx.io>`_. It is a middleware stack which focuses primarily on the compute layer in a smart city IoT deployment. One of the chief goals of Vermillion is to enable seamless data exchange between data producers and consumers in a smart city.


Performance
===========

Performance has been *the* key consideration while developing Vermillion. Intial results show that the middleware was able to handle at least ``130,000`` HTTPS requests/sec across ``12`` compute nodes. We suspect that this is not its true limit (and that it might actually be much higher), since a plateau was never reached in the results graph. The test tool that we used (`Tsung <http://tsung.erlang-projects.org/>`_) was not able to accurately report numbers for higher number of users and connections. 

A detailed white paper on this project published in Arxiv can be found `here <https://arxiv.org/abs/2003.08361>`_. The following is a graph depicting results from the 12-node test:

  .. figure:: 12-nodes.png
     :scale: 50 %
     :figclass: align-center
   
     Performance of the 12-node deployment (6 brokers + 6 proxies)


Single-node Quickstart
====================== 

If the OS is Ubuntu or debian based
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

* Simply run
  
  .. code-block:: shell

    curl -LJ# https://vermillion-install.herokuapp.com/ | bash


For other operating systems
^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. Clone the repository
   
   .. code-block:: shell

    git clone https://github.com/rbccps-iisc/vermillion
    cd vermillion
    
#. Install the following dependencies manually

   - docker
   - docker-compose
	
#. Also install the following dependencies if the tests need to be run
   
   - requests
   - urllib3
   - pika==0.13.0
    
#. Start the installation

   .. code-block:: shell

     ./single-node/quick_install

#. Test the middleware using

   .. code-block:: shell
   
     ./tests/single-node functional -d 1 -a 1

