# DigiQ verification suite

Written for: whoever needs to re-check that this project actually works, including
a marker who wants evidence rather than claims.

These are **not unit tests.** They drive the real, running application over HTTP
and WebSocket against a real MySQL database, which is the only way the bugs
described below were ever going to show up.

## Running them

Start the app first, then:

```powershell
.\run.ps1                    # from the project root
.\tests\run-all.ps1 -Reset   # -Reset reloads the schema first (recommended)
```

`-Reset` wipes and reseeds the database. Leave it off to test against current data.

Node.js is needed for suites 2–5; suite 1 is pure PowerShell.

## What each suite covers

| File | Checks |
|---|---|
| `static-check-imports.js` | Every referenced Java type resolves to an import or the same package. Needs no server. |
| `static-check-el.js` | Every JSP `${...}` property maps to a real getter. Catches typos the compiler cannot see, because EL is resolved at runtime. |
| `01-smoke.ps1` | Public pages, the full customer journey (book → QR → PDF → live status), the staff console (call, scan, complete), all seven admin screens, role isolation, the custom 404. |
| `02-websocket.js` | Connects to `/ws/queue`, asserts the `CONNECTED` handshake and `PONG` keepalive, then triggers real actions and asserts `TOKEN_ISSUED`, `TOKEN_CALLED`, `QUEUE_CHANGED` and `TOKEN_UPDATED` arrive. Also asserts the public board payload **leaks no customer name**. |
| `03-features.js` | Registration (including duplicate email and password mismatch), wrong-password rejection, admin CRUD for services and users, duplicate token-code rejection, **priority ordering**, notification creation and mark-all-read, token cancellation. |
| `04-concurrency.js` | Two counters serving one queue. Fires paired `call next` requests simultaneously for three rounds and asserts they always get **different** tokens and that no token is ever served twice. |
| `05-lock-behaviour.js` | A focused database experiment, not an app test. Opens two connections and proves how `FOR UPDATE SKIP LOCKED` behaves with and without the descending index. Run it if you ever change `idx_token_queue`. |
| `06-public-access.js` | The access model: public pages reachable without signing in, a stranger can register and is signed straight in, that account can use the customer area but is blocked from every `/admin/*` and `/staff/*` page, **posting `role=ADMIN` in the registration form is ignored**, and the sign-in page does not print the administrator password. |

## Why suite 5 exists

Suite 4 was passing intermittently. Suite 5 is what found out why.

The "call next" query orders by `priority DESC, issued_at ASC`. Against a plain
ascending index that mixed ordering needs a **filesort**, and a filesort must read —
and therefore lock — *every* matching row before it can return one. So the first
counter silently locked the whole queue, the second counter's `SKIP LOCKED` skipped
all of it, and staff were told "nobody is waiting" while customers were queued.

Measured, not guessed:

```
  before (ascending index):  second caller came back EMPTY in 8/8 trials
  after  (descending index): second caller came back EMPTY in 0/8 trials
```

The fix is one line in `database/schema.sql` — declaring `idx_token_queue` as
`(service_id, service_date, status, priority DESC, issued_at ASC)` so the index
order matches the query order and `EXPLAIN` stops reporting `Using filesort`.

**If you ever change that index, run `05-lock-behaviour.js` again.** The bug is
invisible in single-user testing and does not raise an error — it just quietly
tells a counter the queue is empty.

## A note on the harness

Four "failures" during development turned out to be bugs in these tests, not the
app: Node's `fetch` drops `Set-Cookie` across redirects (so the login session was
lost), and one suite reused the wrong password for an account it had just created.
Both are fixed here. If a suite fails, check the database state before assuming the
application is at fault — that is how those were caught.
