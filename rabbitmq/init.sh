#!/bin/sh
set -eu

api=http://rabbitmq:15672/api
auth="${RABBITMQ_USERNAME}:${RABBITMQ_PASSWORD}"

curl --fail --silent --show-error \
    --retry 30 --retry-all-errors --retry-delay 2 --retry-max-time 90 \
    --user "$auth" "$api/overview" >/dev/null

put() {
    curl --fail --silent --show-error \
        --user "$auth" \
        --header 'content-type: application/json' \
        --request PUT \
        --data "$2" \
        "$api/$1" >/dev/null
}

post() {
    curl --fail --silent --show-error \
        --user "$auth" \
        --header 'content-type: application/json' \
        --request POST \
        --data "$2" \
        "$api/$1" >/dev/null
}

put 'exchanges/%2F/ocr.processing' \
    '{"type":"direct","durable":true,"auto_delete":false,"internal":false,"arguments":{}}'
put 'exchanges/%2F/ocr.reading' \
    '{"type":"direct","durable":true,"auto_delete":false,"internal":false,"arguments":{}}'
put 'exchanges/%2F/ocr.processing.dlx' \
    '{"type":"direct","durable":true,"auto_delete":false,"internal":false,"arguments":{}}'

put 'queues/%2F/ocr-processing' \
    '{"durable":true,"auto_delete":false,"arguments":{"x-dead-letter-exchange":"ocr.processing.dlx","x-dead-letter-routing-key":"ocr.processing.failed"}}'
put 'queues/%2F/ocr-processing-dlq' \
    '{"durable":true,"auto_delete":false,"arguments":{}}'
put 'queues/%2F/ocr-reading-return' \
    '{"durable":true,"auto_delete":false,"arguments":{}}'

post 'bindings/%2F/e/ocr.processing/q/ocr-processing' \
    '{"routing_key":"ocr.processing","arguments":{}}'
post 'bindings/%2F/e/ocr.reading/q/ocr-reading-return' \
    '{"routing_key":"ocr.reading","arguments":{}}'
post 'bindings/%2F/e/ocr.processing.dlx/q/ocr-processing-dlq' \
    '{"routing_key":"ocr.processing.failed","arguments":{}}'
