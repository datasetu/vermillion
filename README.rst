IUDX resource server
--------------------
|travis| |codacy| |license|

.. |travis| image:: https://travis-ci.com/rbccps-iisc/iudx-resource-server.svg?token=qoDCvWvt1jKW5rCqosmf&branch=master
    :target: https://travis-ci.org/rbccps-iisc/iudx-resource-server
    
.. |license| image:: https://img.shields.io/badge/license-ISC-blue.svg
    :target: https://github.com/rbccps-iisc/iudx-resource-server/blob/master/LICENSE
    
.. |codacy| image:: https://api.codacy.com/project/badge/Grade/8230f593934a4ee391f6967c24cf237f 
    :target: https://www.codacy.com?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=rbccps-iisc/iudx-resource-server&amp;utm_campaign=Badge_Grade
    
An IUDX compliant IoT resource server for smart cities

Documentation: https://iudx.readthedocs.io **(Out of date. Will update soon)**


Single-node Quickstart
====================== 


#. Clone the repository::

    git clone https://github.com/rbccps-iisc/iudx-resource-server
    cd iudx-resource-server
    
#. Install the required dependencies (Host OS must be Ubuntu)::

    ./tests/single-node/require.sh

#. If the host OS is not Ubuntu then install the following dependencies manually

	- docker
	- docker-compose
	
   Also install the following dependencies if the tests need to be run
   
	- requests
	- urllib3
	- pika
    
#. Start the installation::

    ./single-node/quick_install

#. Test the middleware using::

    ./tests/single-node functional -d 1 -a 1
