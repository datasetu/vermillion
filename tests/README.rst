Test cases for Corinthian
=========================

Functional test cases
---------------------

Accessible only to admins
^^^^^^^^^^^^^^^^^^^^^^^^

* Register owner
* Deregister owner

Accessible only to owners
^^^^^^^^^^^^^^^^^^^^^^^^

* Register
* Deregister

Accessible only to devices
^^^^^^^^^^^^^^^^^^^^^^^^^^

* Publish
* Subscribe

Accessible to both devices and their respective owners
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

* Share
* Follow
* Unfollow
* Bind
* Unbind
* Follow requests
* Follow status

Security test cases
-------------------

Common to all APIs
^^^^^^^^^^^^^^^^^^

* Invalid ID
* Invalid apikey

Create/Delete owners
^^^^^^^^^^^^^^^^^^^^^^

* Accesible only via localhost

Publish
^^^^^^^

* To non-existent exchange
* Without authorisation
* To another device's exchange
* To ``amq.topic`` ``amq.direct`` ``amq.headers`` and ``amq.fanout``
* "Subscribe only" devices trying to publish - *
* Without a schema - *
* Invalid message-type

Subscribe
^^^^^^^^^

* With invalid message type
* With invalid num-messages
* After validity has expired - *

Bind
^^^^

* To unauthorised exchange 
* To non-existsent exchange 
* To non-existent queue - * 
* With a different topic from what was requested in follow
* Using owner's apikey
* After the authorised validity has expired - *
* Cross-owner binding
* Cross-device binding
* Invalid message-type


Unbind
^^^^^^

* To unauthorised exchange 
* To non-existsent exchange 
* To non-existsent queue 
* With a different topic from what was requested in follow
* Using owner's apikey
* After the authorised validity has expired - *
* Cross-owner unbinding
* Cross-device unbinding
* Invalid message-type


Share
^^^^^

* On behalf of another device
* On behalf of another owner 
* Using an invalid follow ID
* Using the same device's follow ID
* Share without follow - effectively same as invalid follow-id 

Follow
^^^^^^

* Invalid from
* Invalid to 
* Invalid validity
* Invalid topic - ?
* Cross-owner follow
* Cross-device follow
* Multiple follow requests for the same device
* Invalid message-type

Unfollow
^^^^^^^^

* Using wrong ID
* Invalid message-type
