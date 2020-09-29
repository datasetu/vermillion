#!/bin/bash
#!/bin/bash
docker run --rm -ti --network single-node_vermillion-net -v /Users/pct960/Documents/RBCCPS/vermillion/single-node/data:/tmp elasticdump/elasticsearch-dump \
    --input=http://elasticsearch:9200/archive \
    --fsCompress \
    --overwrite=true \
    --limit=10000 \
    --output=/tmp/archive.json.gz \
    --type=data

