#!/bin/bash

REGION=$1
UUID=$2

MODEL_NAME=fraud-ml-model-$UUID
EP_NAME=fraud-ml-model-ep-$UUID
DEPLOYED_MODEL_NAME=fraud-ml-deployed-model-$UUID

echo "REGION = $REGION"
echo "MODEL_NAME = $MODEL_NAME"
echo "EP_NAME = $EP_NAME"
echo "DEPLOYED_MODEL_NAME = $DEPLOYED_MODEL_NAME"

MODEL_ID=$(gcloud ai models list   --region=$REGION   --filter=display_name=$MODEL_NAME 2>/dev/null | grep "$MODEL_NAME" | awk '{ print $1 }')
ENDPOINT_ID=$(gcloud ai endpoints list --region=$REGION --filter=display_name=$EP_NAME 2>/dev/null | grep "$EP_NAME" | awk '{ print $1 }')
DEPLOYED_MODEL_ID=$(gcloud ai endpoints describe $ENDPOINT_ID --region=$REGION 2>/dev/null | grep "id:" | awk -F  "'" '{print $2}')

echo "MODEL_ID = $MODEL_ID"
echo "ENDPOINT_ID = $ENDPOINT_ID"
echo "DEPLOYED_MODEL_ID = $DEPLOYED_MODEL_ID"

if ! { [ -z "$ENDPOINT_ID" ] && [ -z "$DEPLOYED_MODEL_ID" ]; }; then
	echo "RAN it"
	yes | gcloud ai endpoints undeploy-model $ENDPOINT_ID --deployed-model-id=$DEPLOYED_MODEL_ID --region=us-central1
fi

if ! { [ -z "$ENDPOINT_ID" ]; }; then
	yes | gcloud ai endpoints delete $ENDPOINT_ID --region=us-central1
fi

if ! { [ -z "$MODEL_ID" ]; }; then
	yes | gcloud ai models delete $MODEL_ID --region=us-central1
fi

if test -f "./scripts/ENDPOINT_ID-$UUID.output"; then
    rm ./scripts/ENDPOINT_ID-$UUID.output
fi
