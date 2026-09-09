#!/usr/bin/env bash
#
# Offline deployment-binding signing tool.
#
# Run this ONLY on a machine that is not the application server (per spec:
# "приватний ключ підпису ніколи не в коді/git/образі — лише на окремій
# offline signing-машині"). It produces:
#   1. deployment-binding.json  -> ship this to the app server
#      (DEPLOYMENT_BINDING_FILE / classpath resource), safe to commit to a
#      private ops repo, contains no secret material.
#   2. deployment-binding-public-key.b64 -> the value for
#      DEPLOYMENT_BINDING_PUBLIC_KEY on the app server.
#   3. <deployment-id>-ed25519-private.pem -> KEEP OFFLINE. Never copy this
#      onto the application server or into any image/repo. Needed only to
#      re-sign a future binding (e.g. rotating AUTHORIZED_GROUP_CHAT_ID).
#
# Usage:
#   ./generate-binding.sh <authorized_chat_id> <environment> [existing_private_key.pem]
#
# Example:
#   ./generate-binding.sh -1001234567890 production
#
set -euo pipefail

if [[ $# -lt 2 ]]; then
    echo "Usage: $0 <authorized_chat_id> <environment> [existing_private_key.pem]" >&2
    exit 1
fi

AUTHORIZED_CHAT_ID="$1"
ENVIRONMENT="$2"
EXISTING_KEY="${3:-}"

APPLICATION_IDENTIFIER="eurovision-analytics-platform"
BINDING_VERSION="1"
DEPLOYMENT_ID="$(uuidgen 2>/dev/null || python3 -c 'import uuid; print(uuid.uuid4())')"
NONCE="$(openssl rand -hex 16)"
CREATED_AT="$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)"

WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

if [[ -n "$EXISTING_KEY" ]]; then
    PRIVATE_KEY_PATH="$EXISTING_KEY"
else
    PRIVATE_KEY_PATH="./${DEPLOYMENT_ID}-ed25519-private.pem"
    openssl genpkey -algorithm ed25519 -out "$PRIVATE_KEY_PATH"
    chmod 600 "$PRIVATE_KEY_PATH"
    echo "Generated new Ed25519 private key: $PRIVATE_KEY_PATH (KEEP THIS OFFLINE)"
fi

PUBLIC_KEY_B64="$(openssl pkey -in "$PRIVATE_KEY_PATH" -pubout -outform DER | base64 -w0)"

# Canonical payload MUST match BindingDocument#canonicalPayload() field order exactly.
CANONICAL_FILE="$WORKDIR/canonical.txt"
printf '%s\n%s\n%s\n%s\n%s\n%s\n%s' \
    "$DEPLOYMENT_ID" \
    "$AUTHORIZED_CHAT_ID" \
    "$ENVIRONMENT" \
    "$CREATED_AT" \
    "$BINDING_VERSION" \
    "$APPLICATION_IDENTIFIER" \
    "$NONCE" > "$CANONICAL_FILE"

SIGNATURE_FILE="$WORKDIR/signature.bin"
openssl pkeyutl -sign -inkey "$PRIVATE_KEY_PATH" -rawin -in "$CANONICAL_FILE" -out "$SIGNATURE_FILE"
SIGNATURE_B64="$(base64 -w0 < "$SIGNATURE_FILE")"

cat > deployment-binding.json << EOF
{
  "deploymentId": "$DEPLOYMENT_ID",
  "authorizedChatId": $AUTHORIZED_CHAT_ID,
  "environment": "$ENVIRONMENT",
  "createdAt": "$CREATED_AT",
  "version": "$BINDING_VERSION",
  "applicationIdentifier": "$APPLICATION_IDENTIFIER",
  "nonce": "$NONCE",
  "signature": "$SIGNATURE_B64"
}
EOF

echo -n "$PUBLIC_KEY_B64" > deployment-binding-public-key.b64

echo
echo "Wrote deployment-binding.json (ship to app server)"
echo "Wrote deployment-binding-public-key.b64 (set as DEPLOYMENT_BINDING_PUBLIC_KEY)"
echo
echo "Verify offline before shipping:"
echo "  openssl pkeyutl -verify -pubin -inkey <(openssl pkey -in $PRIVATE_KEY_PATH -pubout) \\"
echo "    -rawin -in $CANONICAL_FILE -sigfile $SIGNATURE_FILE"
