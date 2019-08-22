 #!/bin/bash

curl -i -u "admin:5PhWQrMcXHHUx9SROYcHgvsopxRld5Yr" -XPUT \
"http://rabbit1:15672/api/parameters/shovel/%2f/my-shovel/" -d '{"value":{"src-protocol":
"amqp091", "src-uri": "amqp://admin:5PhWQrMcXHHUx9SROYcHgvsopxRld5Yr@rabbit1:5672/%2f","src-exchange": \
"admin/dev.protected", "src-exchange-key": "#", "dest-protocol": "amqp091", "dest-uri": "amqp://admin:5PhWQrMcXHHUx9SROYcHgvsopxRld5Yr@rabbit2:5672/%2f", "dest-queue": "beaver/dev"}}'
