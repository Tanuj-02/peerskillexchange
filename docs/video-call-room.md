# Video calls and the shared board

The `video-call` branch keeps one learning room per accepted exchange. A room can now contain multiple calls. Either member can start or join the current call, and either can end it. Closing the room also ends its active call. The room owner can restart a closed room while its accepted exchange remains active. Restarting keeps the room ID, board operations, and past call history; a new call gets a new call ID. Video and audio use browser WebRTC; the Spring WebSocket relays only SDP and ICE signaling. The server does not record media. Screen sharing replaces the outgoing camera video track while the share is active. Members can expand either video, including the partner's screen share, to full screen.

## API and signaling

All routes require the room member's JWT and an accepted exchange:

| Route | Purpose |
| --- | --- |
| `GET /api/rooms/{roomId}/calls` | Call history and active call |
| `POST /api/rooms/{roomId}/calls` | Start or return the active call |
| `POST /api/rooms/{roomId}/calls/{callId}/end` | End a call |
| `POST /api/rooms/{roomId}/reopen` | Owner restarts a closed room, retaining board and call history |
| `GET /api/rooms/{roomId}/calls/{callId}/notes` | Read saved call notes |
| `POST /api/rooms/{roomId}/calls/{callId}/notes` | Save `{requestId, content}` during a call |
| `POST /api/rooms/{roomId}/calls/{callId}/ai/jobs` | Queue a summary for an ended call with notes |
| `GET /api/rooms/{roomId}/calls/{callId}/ai/jobs/latest` | Poll that call's latest AI job |

Authenticated members send `READY`, `OFFER`, `ANSWER`, `ICE`, or `LEAVE` to `/app/rooms.signal/{roomId}` with `callId`, `targetUserId`, and optional `payload`. The backend verifies room membership, active call, target peer, kind, and payload size before broadcasting a `SIGNAL` event on `/topic/rooms/{roomId}`. Media itself does not enter Kafka, Redis, SQL, or Groq.

## Call summaries

The AI worker receives only the current call's saved notes or optional browser speech captions plus exchange skill names. It cannot hear the call or summarize speech that was not captured. The user must opt in to browser speech recognition; browser support and speech service privacy vary. A call without saved notes cannot queue a summary. Jobs are idempotent by request ID, limited to three generations per call, and reuse the existing Kafka worker and Groq validation path. Generated text is labeled and can be reported. Do not put secrets in notes.

## Board and performance

The board now stores `STROKE`, `SHAPE` (rectangle, ellipse, line, arrow), `TEXT`, `ERASE`, and `CLEAR`. Operations retain IDs and room sequence numbers. The browser shows its own new items immediately with reduced opacity until the durable result arrives. A completion future wakes a local API instance when its Kafka consumer commits the item; a short SQL check still supports a consumer on another instance. Uncertain saves retain their original UUID for retry. The browser samples long pen paths down to 256 points while preserving the endpoint.

On 22 Nov 2026, a controlled local run with MySQL, Redis, and Kafka measured **12 sequential two-point stroke saves after two warmups**: p50 87 ms, p95 122 ms, max 122 ms. This is an API latency sample on one local machine, not a load test or deployment estimate. A two-browser local test also verified video tracks, screen-share track replacement, rectangle and text synchronization, notes, and call end. One Groq job based on a completed call's notes succeeded with a summary and three questions. The signaling race fix was followed by build checks, but the final two-browser repeat was blocked by automatic approval review's usage limit.

## Deployment requirements and limits

Apply [the MySQL migration](migrations/2026-11-22-video-call.sql) before running this branch against an existing MySQL database. The old `board_operation.type` enum excludes new item types, and existing `event_json`/`strokes_json` `TINYTEXT` columns truncate ordinary board data. Hosted deployments need a reviewed migration for the new call tables and `ai_session_job.call_id`; Hibernate `ddl-auto=update` is only a local development convenience.

Set `VITE_WEBRTC_ICE_SERVERS` to a JSON array of ICE servers for the frontend build. The default public STUN server may work on a local or permissive network; a hosted call needs a TURN service for peers behind restrictive NAT or firewalls. TURN credentials embedded in a frontend build are visible to clients, so use short-lived credentials issued by a trusted service. HTTPS is required for camera, microphone, and screen capture outside localhost. The current Spring simple STOMP broker is local to one API process; multiple API instances need a shared broker or sticky routing for room signaling and presence. No deployed WebRTC or load test has been run.
