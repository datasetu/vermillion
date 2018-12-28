#!/bin/bash
curl -ik -X GET https://localhost:8888/deregister -H 'apikey: '$2'' -H 'entity: '$3'' -H 'id: '$1''
