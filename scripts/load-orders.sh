#!/usr/bin/env bash
set -euo pipefail

ORDERS="${ORDERS:-200}"
CONCURRENCY="${CONCURRENCY:-10}"
ORDER_API="${ORDER_API:-http://localhost:8081}"
INVENTORY_API="${INVENTORY_API:-http://localhost:8082}"

PRODUCT_IDS=(
  "11111111-1111-1111-1111-111111111111"
  "22222222-2222-2222-2222-222222222222"
  "33333333-3333-3333-3333-333333333333"
)

usage() {
  cat <<'EOF'
Usage: scripts/load-orders.sh [--orders N] [--concurrency N] [--order-api URL] [--inventory-api URL]

Environment overrides:
  ORDERS=200
  CONCURRENCY=10
  ORDER_API=http://localhost:8081
  INVENTORY_API=http://localhost:8082
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --orders)
      ORDERS="$2"
      shift 2
      ;;
    --concurrency)
      CONCURRENCY="$2"
      shift 2
      ;;
    --order-api)
      ORDER_API="$2"
      shift 2
      ;;
    --inventory-api)
      INVENTORY_API="$2"
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

uuid() {
  cat /proc/sys/kernel/random/uuid
}

json_field() {
  local json="$1"
  local field="$2"
  echo "$json" | sed -n "s/.*\"$field\":\"\\([^\"]*\\)\".*/\\1/p" | head -n 1
}

post_json() {
  local url="$1"
  local payload="$2"
  local body_file
  body_file="$(mktemp)"
  local code
  code="$(curl -sS -o "$body_file" -w "%{http_code}" -X POST "$url" -H "Content-Type: application/json" -d "$payload")"
  local body
  body="$(cat "$body_file")"
  rm -f "$body_file"
  printf '%s\n%s\n' "$code" "$body"
}

seed_stock() {
  local qty_per_product
  qty_per_product=$((ORDERS * 3))

  echo "Seeding stock in inventory-service..."
  for product_id in "${PRODUCT_IDS[@]}"; do
    local create_payload
    create_payload="{\"productId\":\"$product_id\"}"
    post_json "$INVENTORY_API/api/stock" "$create_payload" >/dev/null || true

    local add_payload
    add_payload="{\"qty\":$qty_per_product}"
    local out
    out="$(post_json "$INVENTORY_API/api/stock/$product_id/add" "$add_payload")"
    local code
    code="$(echo "$out" | sed -n '1p')"
    if [[ "$code" -ge 400 ]]; then
      echo "Failed to add stock for $product_id. HTTP $code" >&2
      echo "$out" | sed -n '2p' >&2
      exit 1
    fi
  done
}

run_one_order() {
  local idx="$1"
  local product_id
  product_id="${PRODUCT_IDS[RANDOM % ${#PRODUCT_IDS[@]}]}"
  local customer_id
  customer_id="$(uuid)"
  local qty
  qty=$((1 + RANDOM % 3))
  local amount
  amount=$((100 + RANDOM % 900))

  local create_payload
  create_payload="{\"customerId\":\"$customer_id\",\"shipTo\":{\"city\":\"Dhaka\",\"street\":\"Road $((idx % 50 + 1))\",\"houseNo\":\"$((idx % 500 + 1))\"}}"

  local create_out create_code create_body
  create_out="$(post_json "$ORDER_API/api/orders" "$create_payload")"
  create_code="$(echo "$create_out" | sed -n '1p')"
  create_body="$(echo "$create_out" | sed -n '2p')"
  if [[ "$create_code" -ge 400 ]]; then
    echo "ERR create $idx $create_code"
    return 0
  fi

  local order_id
  order_id="$(json_field "$create_body" "orderId")"
  if [[ -z "$order_id" ]]; then
    echo "ERR parse-order-id $idx"
    return 0
  fi

  local add_payload
  add_payload="{\"productId\":\"$product_id\",\"qty\":$qty,\"unitAmount\":$amount,\"currency\":\"BDT\"}"
  local add_out add_code
  add_out="$(post_json "$ORDER_API/api/orders/$order_id/lines" "$add_payload")"
  add_code="$(echo "$add_out" | sed -n '1p')"
  if [[ "$add_code" -ge 400 ]]; then
    echo "ERR add-line $idx $add_code $order_id"
    return 0
  fi

  local place_out place_code
  place_out="$(post_json "$ORDER_API/api/orders/$order_id/place" "{}")"
  place_code="$(echo "$place_out" | sed -n '1p')"
  if [[ "$place_code" -ge 400 ]]; then
    echo "ERR place $idx $place_code $order_id"
    return 0
  fi

  echo "OK $order_id"
}

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required but not found." >&2
  exit 1
fi

seed_stock

echo "Submitting $ORDERS orders with concurrency $CONCURRENCY..."
log_file="$(mktemp)"

for i in $(seq 1 "$ORDERS"); do
  run_one_order "$i" >>"$log_file" &
  while [[ "$(jobs -r -p | wc -l | tr -d ' ')" -ge "$CONCURRENCY" ]]; do
    wait -n
  done
done

wait

ok_count="$(grep -c '^OK ' "$log_file" || true)"
err_count="$(grep -c '^ERR ' "$log_file" || true)"

echo "Load complete. OK=$ok_count ERR=$err_count"
if [[ "$err_count" -gt 0 ]]; then
  echo "Recent errors:"
  grep '^ERR ' "$log_file" | tail -n 20
fi

rm -f "$log_file"
