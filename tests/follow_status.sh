#!/bin/bash

curl -ik -X GET https://localhost:8888/follow-status -H 'apikey: '$2'' -H 'id: '$1''
