#!/bin/bash
docker run --rm -ti --network single-node_vermillion-net -v /Users/pct960/Documents/RBCCPS/vermillion/single-node/data:/tmp elasticdump/elasticsearch-dump \
    --input=/tmp/archive.json.gz \
    --fsCompress \
    --limit=100000 \
    --output=http://elasticsearch:9200/archive \
    --type=data

