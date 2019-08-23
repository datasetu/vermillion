IUDX resource server
--------------------
|travis| |codacy| |license|

.. |travis| image:: https://travis-ci.com/rbccps-iisc/iudx-resource-server.svg?token=qoDCvWvt1jKW5rCqosmf&branch=master
    :target: https://travis-ci.org/rbccps-iisc/iudx-resource-server
    
.. |license| image:: https://img.shields.io/badge/license-ISC-blue.svg
    :target: https://github.com/rbccps-iisc/iudx-resource-server/blob/master/LICENSE
    
.. |codacy| image:: https://api.codacy.com/project/badge/Grade/8230f593934a4ee391f6967c24cf237f 
    :target: https://www.codacy.com?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=rbccps-iisc/iudx-resource-server&amp;utm_campaign=Badge_Grade
    
**(Docs and badges are out of date. Will update after project completion)**

An IUDX compliant IoT resource server for smart cities

Documentation: https://iudx.readthedocs.io


Quickstart
========== 


#. Clone the repository::

    git clone https://github.com/rbccps-iisc/iudx-resource-server
    cd iudx-resource-server
    git submodule init
    git submodule update
    
#. Install the required dependencies (Host OS must be Ubuntu)::

    ./test-suite/require.sh

#. If the host OS is not Ubuntu then install the following dependencies manually

	- docker
	- docker-compose
	
   Also install the following dependencies if the tests need to be run
   
	- requests
	- urllib3
	- pika
    
#. Start the installation::

    ./docker/install

#. Test the middleware using::

    ./test-suite/test functional --random


.. Restricting admin APIs
.. ====================
.. By default the admin related APIs are allowed from any host. To restrict access 
.. of admin APIs through localhost only: unset the "ALLOW_ADMIN_APIS_FROM_OTHER_HOSTS"
.. environment variable (in the docker/.env file). 

Use-case diagrams
=================

.. image:: https://raw.githubusercontent.com/rbccps-iisc/corinthian/master/DOCS/usecase-diagrams/uc.svg?sanitize=true

Sequence diagrams
=================

- Registration 

.. image:: https://raw.githubusercontent.com/rbccps-iisc/corinthian/master/DOCS/sequence-diagrams/register.svg?sanitize=true

- Share/Follow 

.. image:: https://raw.githubusercontent.com/rbccps-iisc/corinthian/master/DOCS/sequence-diagrams/follow-share-publish-subscribe.svg?sanitize=true

API
===

All APIs are based on the following format: /<caller of the API>/<action>

- /admin/register-owner

.. image:: https://raw.githubusercontent.com/rbccps-iisc/corinthian/master/DOCS/api/register-owner.svg?sanitize=true

- /owner/register-entity

.. image:: https://raw.githubusercontent.com/rbccps-iisc/corinthian/master/DOCS/api/register-entity.svg?sanitize=true

- /entity/publish 

.. image:: https://raw.githubusercontent.com/rbccps-iisc/corinthian/master/DOCS/api/publish.svg?sanitize=true

- /entity/subscribe

.. image:: https://raw.githubusercontent.com/rbccps-iisc/corinthian/master/DOCS/api/subscribe.svg?sanitize=true

- /owner/follow

.. image:: https://raw.githubusercontent.com/rbccps-iisc/corinthian/master/DOCS/api/follow.svg?sanitize=true

- /owner/follow-status

.. image:: https://raw.githubusercontent.com/rbccps-iisc/corinthian/master/DOCS/api/follow-status.svg?sanitize=true
