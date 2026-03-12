#!/usr/bin/env bash
set -euo pipefail

MONGOS_CONTAINER="${MONGOS_CONTAINER:-mongos}"
CONFIG_PRIMARY_CONTAINER="${CONFIG_PRIMARY_CONTAINER:-configsvr1}"
SHARD1_PRIMARY_CONTAINER="${SHARD1_PRIMARY_CONTAINER:-shard1a}"
SHARD2_PRIMARY_CONTAINER="${SHARD2_PRIMARY_CONTAINER:-shard2a}"

echo "=== Replica Set Health ==="
docker exec "$CONFIG_PRIMARY_CONTAINER" mongosh --quiet --port 27019 --eval 'printjson(rs.status().members.map(m => ({name: m.name, stateStr: m.stateStr, health: m.health})))'
docker exec "$SHARD1_PRIMARY_CONTAINER" mongosh --quiet --port 27018 --eval 'printjson(rs.status().members.map(m => ({name: m.name, stateStr: m.stateStr, health: m.health})))'
docker exec "$SHARD2_PRIMARY_CONTAINER" mongosh --quiet --port 27018 --eval 'printjson(rs.status().members.map(m => ({name: m.name, stateStr: m.stateStr, health: m.health})))'

echo "=== Shard Registry ==="
docker exec "$MONGOS_CONTAINER" mongosh --quiet --port 27017 --eval 'printjson(db.adminCommand({ listShards: 1 }))'

echo "=== Balancer + sh.status() ==="
docker exec "$MONGOS_CONTAINER" mongosh --quiet --port 27017 --eval '
  print("Balancer enabled: " + sh.getBalancerState());
  printjson(sh.status());
'

echo "=== Collection Sharding Metadata ==="
docker exec "$MONGOS_CONTAINER" mongosh --quiet --port 27017 --eval '
  use config;
  const namespaces = ["orderdb.orders", "orderdb.order_events", "paymentdb.payments"];
  printjson(db.collections.find({ _id: { $in: namespaces } }, { _id: 1, key: 1, unique: 1, dropped: 1 }).toArray());
'

echo "=== Chunk Distribution ==="
docker exec "$MONGOS_CONTAINER" mongosh --quiet --port 27017 --eval '
  use config;
  const namespaces = ["orderdb.orders", "orderdb.order_events", "paymentdb.payments"];
  for (const ns of namespaces) {
    const chunks = db.chunks.aggregate([
      { $match: { ns: ns } },
      { $group: { _id: "$shard", count: { $sum: 1 } } },
      { $sort: { _id: 1 } }
    ]).toArray();
    print("Namespace: " + ns);
    printjson(chunks);
  }
'
