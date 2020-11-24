#!/bin/bash

#TODO: Make sure paths are correct when running this file

docker run --rm --network setup_vermillion-net -v ${PWD}/data:/tmp elasticdump/elasticsearch-dump \
    --input=/tmp/archive.json.gz \
    --fsCompress \
    --limit=100000 \
    --output=http://elasticsearch:9200/archive \
    --type=data

