# Foundation backend contracts

This repository is the Spring Boot API for the paired SkillSwap frontend at
`E:\react app\pseFrontend`. The Foundation changes address backend issues #1–#4.

## Profile and verification

- `GET /api/users/me` returns the owner's email and role. Public profile,
  search, discovery, and exchange request user objects omit both fields.
- `PUT` or `POST /api/users/me/update` changes ordinary profile fields and
  skills. The supplied email and username, if present, must match the current
  values. Username changes are currently unsupported because JWT subjects use
  usernames.
- `POST /api/users/me/email-change` accepts `{"email":"new@example.com"}`.
  It emails a six-digit code to the new address while retaining the current
  address. `POST /api/users/me/email-change/verify` accepts
  `{"code":"123456"}` and changes the email after successful verification.
  Codes expire after ten minutes, have five attempts, and may be requested at
  most once per minute. The stored code is a BCrypt hash.
- `POST /api/users/me/password/send-verification` sends a code to the current
  email. `PUT /api/users/me/password` accepts JSON with `oldPassword`,
  `newPassword`, and `verificationCode`; credentials are not URL parameters.
  Codes expire after ten minutes and are consumed on success.
- Email delivery failure causes the request to fail. Verification codes are
  not logged by the email service.

## Exchange requests and chat

- `POST /api/exchange-requests/create` accepts `receiverId`, `message`,
  nonempty `canTeach`, and nonempty `wantToLearn` lists of skill names.
  Existing accepted or pending requests between the pair prevent a new one;
  rejected history does not. The service locks both users in ID order while
  checking active requests to serialize simultaneous requests. Another active
  request returns HTTP 409; invalid input returns 400 and a missing receiver
  returns 404.
- REST chat sends and reads require conversation membership and a current
  accepted exchange. WebSocket CONNECT requires an authenticated handshake;
  SUBSCRIBE checks conversation access. Only approved user queues and
  `/topic/conversations/{id}` are subscribable. STOMP SEND is limited to
  `/app/chat.send`.

## Tests and schema

`./mvnw.cmd test` uses the `test` Spring profile and an in-memory H2 database;
it does not contact the configured MySQL or real SMTP server. Unit and
controller tests cover profile privacy, verification, exchange request state,
chat access, and STOMP destinations. The production configuration currently
uses Hibernate `ddl-auto=update`, so new verification fields require a schema
review and a managed migration before a production deployment. End-to-end
SMTP delivery, MySQL concurrency, and a real WebSocket browser session remain
to be verified in an integration environment. The current WebSocket handshake
also accepts JWTs in URL query strings, which may appear in proxy access logs;
remove that transport when the browser client can authenticate the connection
without a query token. Existing JWTs are not revoked after a password change.
