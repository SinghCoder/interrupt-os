#!/usr/bin/env bash
set -euo pipefail

profile="codex-home-admin"
region="ap-south-1"
stack_name="interrupt-os"
secret_name="interrupt-os/providers"

for required_name in SERPAPI_KEY OPENAI_API_KEY TYPESAFE_API_KEY; do
  if [[ -z "${!required_name:-}" ]]; then
    echo "$required_name is not configured" >&2
    exit 1
  fi
done

secret_exists=false
if aws secretsmanager describe-secret --secret-id "$secret_name" --profile "$profile" --region "$region" >/dev/null 2>&1; then
  secret_exists=true
fi

if [[ "$secret_exists" == "true" ]]; then
  current_secret="$(aws secretsmanager get-secret-value \
    --secret-id "$secret_name" \
    --profile "$profile" \
    --region "$region" \
    --query SecretString \
    --output text)"
  signing_key="$(jq -r '.INSTALLATION_SIGNING_KEY' <<<"$current_secret")"
else
  signing_key="$(openssl rand -hex 32)"
fi
secret_json="$(jq -n \
  --arg serp "$SERPAPI_KEY" \
  --arg openai "$OPENAI_API_KEY" \
  --arg typesafe "$TYPESAFE_API_KEY" \
  --arg signing "$signing_key" \
  '{SERPAPI_KEY:$serp,OPENAI_API_KEY:$openai,TYPESAFE_API_KEY:$typesafe,INSTALLATION_SIGNING_KEY:$signing}')"

if [[ "$secret_exists" == "true" ]]; then
  printf '%s' "$secret_json" | aws secretsmanager put-secret-value \
    --secret-id "$secret_name" \
    --secret-string file:///dev/stdin \
    --profile "$profile" \
    --region "$region" \
    --query VersionId \
    --output text >/dev/null
else
  printf '%s' "$secret_json" | aws secretsmanager create-secret \
    --name "$secret_name" \
    --description "Interrupt OS provider credentials" \
    --secret-string file:///dev/stdin \
    --profile "$profile" \
    --region "$region" \
    --query ARN \
    --output text >/dev/null
fi

npm run typecheck
npm test
npm run build

artifact_dir="$(mktemp -d /tmp/interrupt-os-lambda.XXXXXX)"
cp dist/index.js "$artifact_dir/index.js"
(cd "$artifact_dir" && zip -q function.zip index.js)

aws cloudformation deploy \
  --template-file infrastructure/template.yaml \
  --stack-name "$stack_name" \
  --capabilities CAPABILITY_IAM \
  --profile "$profile" \
  --region "$region" \
  --no-fail-on-empty-changeset

function_name="$(aws cloudformation describe-stacks \
  --stack-name "$stack_name" \
  --profile "$profile" \
  --region "$region" \
  --query 'Stacks[0].Outputs[?OutputKey==`FunctionName`].OutputValue' \
  --output text)"

aws lambda update-function-code \
  --function-name "$function_name" \
  --zip-file "fileb://$artifact_dir/function.zip" \
  --profile "$profile" \
  --region "$region" \
  --query FunctionArn \
  --output text >/dev/null

aws lambda wait function-updated-v2 \
  --function-name "$function_name" \
  --profile "$profile" \
  --region "$region"

aws cloudformation describe-stacks \
  --stack-name "$stack_name" \
  --profile "$profile" \
  --region "$region" \
  --query 'Stacks[0].Outputs[?OutputKey==`ApiUrl`].OutputValue' \
  --output text
