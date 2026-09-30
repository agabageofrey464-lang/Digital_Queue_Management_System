# DigiQ — architecture

Written for: someone reading or marking the project who has not seen the code.

---

## 1. Layers

Requests enter through a filter chain, land on a servlet, and go down through a
service to a DAO. Nothing skips a layer, and views cannot be reached directly.

```
Browser ──HTTP──▶ EncodingFilter ──▶ AuthFilter ──▶ Servlet
                   (UTF-8)           (role gate)      │
                                                      ├─▶ QueueService ──▶ DAO ──▶ MySQL
                                                      │        │
                                                      │        └─▶ QueueBroadcaster ──WS──▶ every open screen
                                                      │
                                                      └─▶ JSP under /WEB-INF/views   (not URL-addressable)
```

**The one rule that matters:** a servlet never calls a DAO to *change* queue state.
That goes through `QueueService`, which always does the same three things in the
same order:

1. persist the change (one transaction),
2. write the customer notification,
3. broadcast the event.

Persisting first means no screen is ever told about a change the database has not
committed.

---

## 2. Packages

| Package | Holds | Notes |
|---|---|---|
| `com.digiq.config` | `Database`, `AppContextListener` | HikariCP pool; opens at boot so bad config fails in the log, not on a click |
| `com.digiq.model` | 7 beans + 3 enums + `DashboardStats` | Plain beans; enums carry their own display label and colour tone |
| `com.digiq.dao` | 7 DAOs | All JDBC `PreparedStatement`. `TokenDAO` owns the transactions |
| `com.digiq.service` | `QueueService`, `AuthService`, `QrCodeService`, `PdfTokenService` | The rules live here |
| `com.digiq.servlet` | 25 servlets in 5 sub-packages | Thin: parse, delegate, render |
| `com.digiq.websocket` | `QueueEndpoint`, `QueueBroadcaster` | One endpoint, one event stream |
| `com.digiq.filter` | `AuthFilter`, `EncodingFilter` | Role derived from the URL prefix |
| `com.digiq.util` | `Web`, `Json`, `PasswordUtil` | Request helpers, Gson wrapper, BCrypt |

---

## 3. Data model

```
   users                     services                 counters
   ──────────                ──────────               ──────────
   id           ◀──┐         id          ◀──┐   ┌──▶  id
   full_name       │         name           │   │     name
   email (uniq)    │         code (uniq)    ├───┘     service_id ──┘
   password_hash   │         description    │         staff_id ─────┐
   role            │         avg_service_   │         status        │
   active          │           minutes      │         (OPEN/        │
                   │         active         │          PAUSED/      │
                   │                        │          CLOSED)      │
                   │                        │                       │
                   └────────────┬───────────┴───────────────────────┘
                                │
                             tokens
                             ──────────
                             id
                             token_number      ACC-0042, resets daily per service
                             qr_payload        UUID encoded into the QR
                             service_id  ──────┘
                             customer_id ──────┘
                             counter_id  ──────┘
                             status            PENDING → IN_SERVICE → COMPLETED
                                               (also CANCELLED, NO_SHOW)
                             priority          1 = jumps the queue
                             service_date      numbering resets on this
                             issued_at / called_at / completed_at
                                │
                 ┌──────────────┴──────────────┐
                 │                             │
          service_logs                   notifications
          ──────────                     ──────────
          token_id                       user_id
          counter_id                     token_id
          staff_id                       title / message
          action                         type   INFO | APPROACHING
          from_status → to_status               | CALLED | COMPLETED
          created_at                     read_flag
```

Six tables. `service_logs` is append-only and written inside the same transaction
as the status change it records, so the audit trail cannot drift from `tokens`.

---

## 4. The queue algorithm

### Issuing a token

Per service, per day. The sequence is read and the row inserted in **one
transaction** with the day's rows locked, so two people booking at the same
instant cannot be given the same number. `uq_token_day` is the backstop if they
somehow are.

### Calling the next customer

```sql
SELECT id FROM tokens
 WHERE service_id = ? AND service_date = CURDATE() AND status = 'PENDING'
 ORDER BY priority DESC, issued_at ASC
 LIMIT 1 FOR UPDATE SKIP LOCKED
```

`SKIP LOCKED` is what makes several counters safe at once — each transaction locks
a *different* row instead of queueing behind the first.

**This is load-bearing and fragile in one specific way.** The index
`idx_token_queue` must be declared `(..., priority DESC, issued_at ASC)` — the same
order as the `ORDER BY`. Against a plain ascending index the mixed ordering needs a
**filesort**, and a filesort has to read (and therefore lock) *every* matching row
before it can return one. The first counter then holds the whole queue, the second
counter's `SKIP LOCKED` skips all of it, and staff are told "nobody is waiting"
while customers are sitting there.

That was a real bug in this project, reproduced in 8 of 8 trials and fixed by the
descending index. If you change the index, change the query with it.

The transaction is also **retried on an InnoDB deadlock** (error 1213/1205, up to
four attempts with a jittered backoff). Deadlocks between two counters updating the
same index are normal under contention, not a fault; MySQL's own guidance is that
the application must be ready to reissue the transaction.

### Status flow

```
PENDING ──(staff calls)──▶ IN_SERVICE ──(staff completes)──▶ COMPLETED
   │                            │
   │                            └──(never presented)──▶ NO_SHOW
   └──(customer cancels)──▶ CANCELLED
```

Only `PENDING` can be cancelled by the customer. Once called, only staff can close
it out.

---

## 5. Real-time updates

One endpoint, `/ws/queue`. Every live screen subscribes to the same stream.

| Event | Raised when | Who acts on it |
|---|---|---|
| `TOKEN_ISSUED` | a customer books | board, staff console, admin dashboard |
| `TOKEN_CALLED` | staff call next | board (flashes), customer page, dashboard |
| `TOKEN_UPDATED` | completed / no-show / cancelled | board, customer page, dashboard |
| `COUNTER_UPDATED` | counter opened, paused, closed | board, dashboard |
| `QUEUE_CHANGED` | anything shifts the queue | customer pages re-read their position |

Every message uses the same envelope, including the `CONNECTED` handshake and the
`PONG` keepalive:

```json
{ "type": "TOKEN_CALLED", "payload": { ... } }
```

**The socket says *something moved*; it never says where you now stand.** Clients
re-read their authoritative position over HTTP (`/customer/status`). That avoids a
whole class of stale-UI bugs and means a client that misses an event self-heals on
the next one.

The client reconnects with a capped exponential backoff, so a display board left
running overnight recovers on its own.

---

## 6. Security

| Concern | Handling |
|---|---|
| Passwords | BCrypt (cost 10). The hash is never put in the view layer |
| Session fixation | Session invalidated and recreated on login |
| Authorisation | `AuthFilter` derives the role from the URL prefix, so a new page under `/admin/` is protected the moment it is added |
| SQL injection | Every statement is a `PreparedStatement` |
| XSS | All user text rendered through `<c:out>` |
| Direct view access | JSPs live under `WEB-INF` and cannot be requested |
| Cookies | `HttpOnly` session cookie |
| Account enumeration | The login form never says whether an address exists |
| Data on the wall display | The board shows token numbers and counters only — **never a customer name** |

---

## 7. Known limits

Honest list of what this does not do:

- **Single instance only.** `QueueBroadcaster` holds sessions in memory, so running
  two Tomcats behind a load balancer would split the event stream. A real
  deployment would need a shared bus.
- **Notifications are in-app only.** No SMS or email; the project brief's "alert
  system" is satisfied on-screen.
- **No password reset for customers.** Only an admin can reset one.
- **Token numbers reset daily** and are not unique across days on their own — the
  `(service, date, number)` triple is what identifies a token.
- **No pagination.** Admin tables cap at 200–250 rows. Fine for a branch, not for a
  year of history.
- **The camera scanner needs HTTPS or localhost** (a browser rule, not ours).
  Typing the printed token number works anywhere.
