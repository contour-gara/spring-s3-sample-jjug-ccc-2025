#!/bin/sh
# リソースをメモリに置く仕様のため、起動直後に引数のバケットを作り直す。
# バケット作成完了の合図として /tmp/ready を作成し、compose のヘルスチェックに検知させる。
set -eu

PORT=9297
ENDPOINT="https://127.0.0.1:${PORT}"
CA_FILE="${HOME}/.aws/emulator/${PORT}/ca.pem"
REGION="${AWS_REGION:-ap-northeast-1}"

# アプリやテストの固定アクセスキーで接続するため、-a off で認証を無効化する。
# 使い捨てコンテナ起動ごとの新規インストール計上を防ぐため、-t off でテレメトリを切る。
aws-emulator -b 0.0.0.0 -p "${PORT}" -a off -t off &
EMULATOR_PID=$!
trap 'kill -TERM "${EMULATOR_PID}"; wait "${EMULATOR_PID}"' TERM INT

until curl -sf --cacert "${CA_FILE}" "${ENDPOINT}/__admin/health" > /dev/null 2>&1; do
  kill -0 "${EMULATOR_PID}"
  sleep 1
done

for BUCKET in "$@"; do
  curl -sf --cacert "${CA_FILE}" --aws-sigv4 "aws:amz:${REGION}:s3" --user "test:test" -X PUT "${ENDPOINT}/${BUCKET}" \
    --data-binary "<CreateBucketConfiguration><LocationConstraint>${REGION}</LocationConstraint></CreateBucketConfiguration>"
done

touch /tmp/ready
wait "${EMULATOR_PID}"
