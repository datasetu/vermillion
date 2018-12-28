#!/bin/bash
  
curl -ik -X GET https://localhost:8888/follow-requests -H 'apikey: '$2'' -H 'id: '$1''
