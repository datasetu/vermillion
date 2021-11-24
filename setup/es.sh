#!/bin/bash

set -ex

curl -XPUT "http://127.0.0.1:9200/archive/_settings" -H 'Content-Type: application/json' -d'{   "max_result_window" : 100000 }'
echo "Changed the Elastic search size limit"