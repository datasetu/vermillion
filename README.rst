.. raw:: html

  <p align="center"><img src="logo.png" alt="vermillion_logo"></p>

|github-workflow| |codacy| |license| |dependabot| |docker-build-status| |docker-build-automation|

.. |github-workflow|  image:: https://img.shields.io/github/workflow/status/rbccps-iisc/vermillion/CI
   :target: https://github.com/rbccps-iisc/vermillion/actions         
.. |license| image:: https://img.shields.io/badge/license-ISC-blue.svg
   :target: https://github.com/rbccps-iisc/vermillion/blob/master/LICENSE
.. |codacy| image:: https://api.codacy.com/project/badge/Grade/d5c93fe3cec44982bcdcca7470a27b68    
   :target: https://www.codacy.com/manual/pct960/vermillion?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=rbccps-iisc/vermillion&amp;utm_campaign=Badge_Grade
.. |dependabot| image:: https://img.shields.io/badge/dependabot-enabled-yellow
   :target: https://dependabot.com/
.. |docker-build-status| image:: https://img.shields.io/docker/cloud/build/iudx/java
   :target: https://hub.docker.com/repository/docker/iudx/java/builds
.. |docker-build-automation| image:: https://img.shields.io/docker/cloud/automated/iudx/java
   :target: https://hub.docker.com/repository/docker/iudx/java/builds
    
A high-performance scalable IoT middleware for smart cities

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
