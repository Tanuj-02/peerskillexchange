# Whiteboard event log and cache

## Local setup

Run `docker compose --env-file docs/board-compose.env -f compose.board.yml up -d`, then start the Spring Boot API with its existing database, JWT, mail, and frontend environment variables. The explicit Compose env file avoids treating the application's own environment file as Compose syntax. The Compose file runs a single Kafka broker and Redis on localhost for development. It is not a production cluster. Set `KAFKA_BOOTSTRAP_SERVERS`, `REDIS_URL`, `BOARD_KAFKA_TOPIC`, and `BOARD_KAFKA_GROUP` when the services are elsewhere. `BOARD_KAFKA_ENABLED=false` is for existing isolated tests only; it bypasses durability and must not be used to serve real collaborative rooms.

## Write and recovery path

Each accepted operation has a client UUID and is published to Kafka with the room ID as key. The API waits for the broker acknowledgment before recording the publication receipt. The Kafka consumer applies the operation to the SQL event table, advances the room's applied offset in the same transaction, and broadcasts the live update. Duplicate deliveries use the room ID and operation UUID to avoid a second stroke. When the database projection trails an acknowledged publication, history and board projection requests return HTTP 503. A client retries an uncertain save with the *same* UUID.

The SQL snapshot stores visible strokes, sequence number, Kafka partition, and applied offset every 20 operations (`board.snapshot.interval`). Redis caches a versioned visible board for 30 minutes. A Redis miss, stale value, flush, or outage rebuilds from the latest SQL snapshot plus later SQL operations; Redis is not required to accept or read a durable board. Kafka consumer offsets provide replay when the projection worker restarts or fails before it commits a message. Redis presence heartbeat keys expire after 10 seconds.

The `GET /api/rooms/{id}/board` response contains `sequenceNumber`, `kafkaOffset`, `strokes`, and `source`. It checks room membership before reading the cache. The frontend starts from that sequence, fetches later operations, and reconnects through history catch-up. `GET /api/rooms/{id}/operations?after=N` remains available for incremental replay.

## Operational limits

- Kafka is the durable event log only while its data and retention are preserved. Keep topic retention longer than the maximum expected projection outage; restoring a database older than Kafka retention can leave unrecoverable gaps. Back up the SQL database and Kafka data. Do not use `docker compose down -v` on data that must survive.
- The consumer retries a failed record without skipping it. Investigate poison events or database failures before resetting consumer offsets. The API reports recovery while a published event is unapplied.
- This development Compose setup has one broker and replica. Production needs replicated Kafka, secured broker and Redis connections, durable volumes, and monitored consumer lag.
- The existing in-process STOMP simple broker delivers live events only to clients connected to the same API instance. Multi-instance WebSocket fan-out needs a shared broker relay in a later deployment milestone.
- The browser currently carries its JWT in the WebSocket handshake query. Reverse proxies must avoid logging that query; moving to a safer short-lived WebSocket credential remains deployment work.
- No database migration tool is configured; production schema changes need an explicit reviewed migration before rollout. Existing `ddl-auto=update` is intended for local development.

## Checks

`mvnw.cmd test` covers projection fallback, broker failure, Kafka ordering and duplicate delivery, SQL snapshots, authorization, and existing WebSocket behavior. The Kafka integration test uses an embedded broker and intentionally runs without Redis to verify database reconstruction. A two-browser smoke test and a multi-instance WebSocket deployment still require manual environment testing.

When the local Compose services are running, select the opt-in live infrastructure check with `mvnw.cmd -q -Dtest=BoardLiveInfrastructureIT test`. It uses the H2 test profile and a dedicated Kafka topic, verifies Redis cache hits, deletes only its own projection key to test reconstruction, and checks presence TTL. It is intentionally excluded from the default test suite because it requires live containers.
