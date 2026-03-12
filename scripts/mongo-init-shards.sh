#!/usr/bin/env bash
set -euo pipefail

MONGOS_CONTAINER="${MONGOS_CONTAINER:-mongos}"
CONFIG_PRIMARY_CONTAINER="${CONFIG_PRIMARY_CONTAINER:-configsvr1}"
SHARD1_PRIMARY_CONTAINER="${SHARD1_PRIMARY_CONTAINER:-shard1a}"
SHARD2_PRIMARY_CONTAINER="${SHARD2_PRIMARY_CONTAINER:-shard2a}"

wait_for_mongo() {
  local container="$1"
  local port="$2"
  local retries="${3:-60}"

  for _ in $(seq 1 "$retries"); do
    if docker exec "$container" mongosh --quiet --port "$port" --eval 'db.runCommand({ ping: 1 }).ok' >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done

  echo "Timed out waiting for $container:$port" >&2
  return 1
}

wait_for_any_primary() {
  local port="$1"
  local retries="${2:-60}"
  shift 2
  local containers=("$@")

  for _ in $(seq 1 "$retries"); do
    for container in "${containers[@]}"; do
      local state
      state="$(docker exec "$container" mongosh --quiet --port "$port" --eval "try { db.hello().isWritablePrimary ? 'primary' : 'not-primary' } catch (e) { 'error' }" 2>/dev/null || true)"
      if [[ "$state" == "primary" ]]; then
        echo "Detected writable primary on $container:$port"
        return 0
      fi
    done
    sleep 2
  done

  echo "Timed out waiting for writable primary on replicas (${containers[*]}) at port $port" >&2
  return 1
}

init_rs_if_needed() {
  local container="$1"
  local port="$2"
  local config_js="$3"

  docker exec "$container" mongosh --quiet --port "$port" --eval "
    try {
      const ok = rs.status().ok;
      if (ok === 1) {
        print('Replica set already initialized');
      } else {
        print('Replica set status not ok, leaving as-is');
      }
    } catch (e) {
      print('Initializing replica set');
      rs.initiate($config_js);
    }
  "
}

echo "Waiting for Mongo containers..."
for container in configsvr1 configsvr2 configsvr3; do
  wait_for_mongo "$container" 27019
done

echo "Ensuring config replica set is initialized..."
init_rs_if_needed "$CONFIG_PRIMARY_CONTAINER" 27019 '{_id:"configReplSet",configsvr:true,members:[{_id:0,host:"configsvr1:27019"},{_id:1,host:"configsvr2:27019"},{_id:2,host:"configsvr3:27019"}]}'

echo "Waiting for config primary..."
wait_for_any_primary 27019 90 configsvr1 configsvr2 configsvr3

echo "Waiting for shard containers..."
for container in shard1a shard1b shard1c shard2a shard2b shard2c; do
  wait_for_mongo "$container" 27018
done

echo "Ensuring shard replica sets are initialized..."
init_rs_if_needed "$SHARD1_PRIMARY_CONTAINER" 27018 '{_id:"shard1rs",members:[{_id:0,host:"shard1a:27018"},{_id:1,host:"shard1b:27018"},{_id:2,host:"shard1c:27018"}]}'
init_rs_if_needed "$SHARD2_PRIMARY_CONTAINER" 27018 '{_id:"shard2rs",members:[{_id:0,host:"shard2a:27018"},{_id:1,host:"shard2b:27018"},{_id:2,host:"shard2c:27018"}]}'

echo "Waiting for shard primaries..."
wait_for_any_primary 27018 90 shard1a shard1b shard1c
wait_for_any_primary 27018 90 shard2a shard2b shard2c

echo "Waiting for mongos..."
wait_for_mongo "$MONGOS_CONTAINER" 27017

echo "Ensuring shards are added to mongos..."
docker exec "$MONGOS_CONTAINER" mongosh --quiet --port 27017 --eval '
  const shards = (db.adminCommand({ listShards: 1 }).shards || []);
  if (!shards.some(s => s._id === "shard1rs")) {
    printjson(sh.addShard("shard1rs/shard1a:27018,shard1b:27018,shard1c:27018"));
  } else {
    print("shard1rs already added");
  }
  if (!shards.some(s => s._id === "shard2rs")) {
    printjson(sh.addShard("shard2rs/shard2a:27018,shard2b:27018,shard2c:27018"));
  } else {
    print("shard2rs already added");
  }
  printjson(db.adminCommand({ listShards: 1 }));
'

echo "Cluster initialization complete."
