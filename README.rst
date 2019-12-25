.. raw:: html

  <p align="center"><img src="logo.png" alt="vermillion_logo"></p>

|codacy| |license|

.. |license| image:: https://img.shields.io/badge/license-ISC-blue.svg
    :target: https://github.com/rbccps-iisc/vermillion/blob/master/LICENSE
    
.. |codacy| image:: https://api.codacy.com/project/badge/Grade/8230f593934a4ee391f6967c24cf237f 
    :target: https://www.codacy.com?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=rbccps-iisc/iudx-resource-server&amp;utm_campaign=Badge_Grade
    
A high-performance scalable IoT middleware for smart cities

Single-node Quickstart
====================== 

If the OS is Ubuntu or debian based
-----------------------------------

* Simply run::

    curl -LJ# https://vermillion-install.herokuapp.com/ | bash


For other operating systems
---------------------------

#. Clone the repository::

    git clone https://github.com/rbccps-iisc/vermillion
    cd vermillion
    
#. Install the following dependencies manually

	- docker
	- docker-compose
	
#. Also install the following dependencies if the tests need to be run
   
	- requests
	- urllib3
	- pika
    
#. Start the installation::

    ./single-node/quick_install

#. Test the middleware using::

    ./tests/single-node functional -d 1 -a 1
