FROM dxflrs/garage:v2.3.0 AS garage

FROM alpine:3.22 AS builder
RUN apk add --no-cache curl
COPY --from=garage /garage /garage
COPY garage.toml /etc/garage.toml
RUN set -eu; \
    /garage server > /tmp/garage.log 2>&1 & \
    GARAGE_PID=$!; \
    until /garage status > /dev/null 2>&1; do sleep 1; done; \
    /garage layout assign -z dc1 -c 1G "$(/garage node id -q | cut -d '@' -f 1)"; \
    /garage layout apply --version 1; \
    /garage key import --yes -n local testtest testtesttesttest; \
    /garage key allow --create-bucket testtest; \
    /garage bucket create jjug-ccc-2025; \
    /garage bucket allow --read --write --owner jjug-ccc-2025 --key testtest; \
    curl -sf --aws-sigv4 "aws:amz:ap-northeast-1:s3" --user "testtest:testtesttesttest" -X PUT "http://localhost:3900/jjug-ccc-2025?cors" --data-binary '<CORSConfiguration><CORSRule><AllowedOrigin>http://localhost:3000</AllowedOrigin><AllowedMethod>GET</AllowedMethod><AllowedMethod>PUT</AllowedMethod><AllowedHeader>*</AllowedHeader></CORSRule></CORSConfiguration>'; \
    kill -TERM $GARAGE_PID; \
    wait $GARAGE_PID || true

FROM dxflrs/garage:v2.3.0
COPY garage.toml /etc/garage.toml
COPY --from=builder /var/lib/garage /var/lib/garage
