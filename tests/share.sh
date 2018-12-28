#!/bin/bash

curl -ik -X GET https://localhost:8888/share -H 'apikey: '$2'' -H 'follow-id: '$3'' -H 'id: '$1''
