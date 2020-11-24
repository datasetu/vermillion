#/bin/bash

#TODO: Make sure paths are correct when running this file

docker run --rm -ti --network setup_vermillion-net -v ${PWD}/data:/tmp elasticdump/elasticsearch-dump \
    --input=http://elasticsearch:9200/archive \
    --output=http://elasticsearch:9200/archive_sampled \
    --type=mapping

docker run --rm -ti --network setup_vermillion-net -v ${PWD}/data:/tmp elasticdump/elasticsearch-dump \
    --input=http://elasticsearch:9200/archive \
    --output=http://elasticsearch:9200/archive_sampled \
    --size=10000 \
    --limit=5000 \
    --searchBody='{"query":{"bool":{"filter":[{"term":{"category.keyword":"varanasi-swm-vehicles"}}]}}}' \
    --type=data

docker run --rm -ti --network setup_vermillion-net -v ${PWD}/data:/tmp elasticdump/elasticsearch-dump \
    --input=http://elasticsearch:9200/archive \
    --output=http://elasticsearch:9200/archive_sampled \
    --size=10000 \
    --limit=5000 \
    --searchBody='{"query":{"bool":{"filter":[{"term":{"category.keyword":"varanasi-swm-workers"}}]}}}' \
    --type=data

docker run --rm -ti --network setup_vermillion-net -v ${PWD}/data:/tmp elasticdump/elasticsearch-dump \
    --input=http://elasticsearch:9200/archive \
    --output=http://elasticsearch:9200/archive_sampled \
    --limit=5000 \
    --searchBody='{"query":{"bool":{"filter":[{"term":{"category.keyword":"varanasi-aqm"}}]}}}' \
    --type=data

docker run --rm -ti --network setup_vermillion-net -v ${PWD}/data:/tmp elasticdump/elasticsearch-dump \
    --input=http://elasticsearch:9200/archive \
    --output=http://elasticsearch:9200/archive_sampled \
    --limit=5000 \
    --searchBody='{"query":{"bool":{"filter":[{"term":{"category.keyword":"varanasi-swm-bins"}}]}}}' \
    --type=data
