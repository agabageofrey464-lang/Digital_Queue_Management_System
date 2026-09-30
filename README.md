# DigiQ — Digital Queue Management System

A web-based queue management system that replaces handwritten tokens, verbal
announcements and physical waiting lines with a live digital queue.

Customers book a token from their phone and watch their position update in real
time. Counter staff call, scan and close out tickets from a dedicated console.
Administrators configure services and counters and read the numbers that come out
of it. A public "Now Serving" board runs unattended on a waiting-room screen.

Built on the stack the project brief specifies: **Java Servlets + JSP + MySQL on
Apache Tomcat**, with **WebSockets** for the live updates.

---

## Contents

- [What it does](#what-it-does)
- [Technology](#technology)
- [Getting it running](#getting-it-running)
- [Demo accounts](#demo-accounts)
- [How the queue works](#how-the-queue-works)
- [Project layout](#project-layout)
- [Screens](#screens)
- [Design notes](#design-notes)
- [Build status](#build-status)

---

## What it does

### Customer
- Browse services with live queue depth and an estimated wait.
- Book a token, optionally flagged **priority** (elderly, disability, expectant).
- A ticket page with the token number, a scannable QR code and a live position counter.
- Download a printable **A5 PDF ticket** with the QR code embedded.
- Cancel a token that has not been called yet.
- **Turn-approaching alerts** — told when they reach the front three, and again when called.

### Counter staff
- A console bound to the counter the administrator assigned them.
- Open / pause / close the counter.
- **Call next** — pulls the longest-waiting customer, priority first.
- Complete a service or mark a **no-show**.
- **Scan a QR ticket** with the device camera, or type the printed token number.
- Live view of the waiting queue and the day's activity.

### Administrator
- Dashboard: today's volume, waiting count, average wait and service time, counter states.
- **Analytics** over a 7 / 14 / 30 day window — demand over time, outcome mix, arrivals by
  hour, wait vs service time per service, and counter throughput. Every chart has a table view.
- CRUD for services, counters and user accounts, plus password resets.
- Live queue monitor filtered by service and status.
- **Service logs** — an immutable audit trail of every status change, with who made it.

### Public display board
- Full-screen "Now Serving" grid, one tile per counter.
- A newly called number flashes so the waiting room notices it.
- Unauthenticated by design, and shows **token numbers only, never customer names**,
  so it is safe to leave on a wall.

---

## Technology

| Layer | Choice |
|---|---|
| Language | Java 17+ (compiles to release 17; JDK 21/23 are fine) |
| Web | Servlets 4.0 + JSP/JSTL (`javax.*` namespace) |
| Server | Apache Tomcat 9.x |
| Database | MySQL 8.x |
| Pooling | HikariCP |
| Live updates | JSR-356 WebSockets (`/ws/queue`) |
| QR codes | ZXing |
| PDF tickets | OpenPDF |
| Passwords | BCrypt (jBCrypt) |
| JSON | Gson |
| Charts | Chart.js 4 (CDN, SRI-pinned) |
| QR scanning | html5-qrcode (CDN, SRI-pinned) |
| Build | Maven → WAR |

> Tomcat 9 is required, not Tomcat 10+. Tomcat 10 moved to the `jakarta.*`
> namespace; this project uses `javax.*`, matching the project brief.

---

## Getting it running

### Quickest way — one command

There is **no single file you "run"**: this is a Java *web* application. Maven
builds `target/digiq.war`, Tomcat serves it, and you open a **URL**. This script
does all of that for you:

```
run.bat
```

Double-click it, or from PowerShell:

```powershell
.\run.ps1              # build if needed, start MySQL + Tomcat, open the browser
.\run.ps1 -Build       # force a rebuild first
.\run.ps1 -Reset       # reload the database from schema.sql (wipes data)
.\run.ps1 -NoBrowser   # don't open a browser
```

It starts MySQL and Tomcat only if they are not already up, creates and seeds the
database on first run, deploys the WAR, waits until the app answers, and prints the
sign-in details. To stop:

```powershell
.\stop.ps1          # stop Tomcat, leave MySQL up
.\stop.ps1 -All     # stop both
```

`stop.ps1` only touches processes started from the portable toolchain, so a Java or
MySQL you installed for something else is left alone.

Then open **<http://localhost:8080/digiq/>**.

The manual steps below are what the script automates — follow them if you are
deploying somewhere else.

### 1. Prerequisites

- **JDK 17 or newer** (Temurin, Corretto or Oracle)
- **Apache Tomcat 9.x** (e.g. 9.0.84)
- **MySQL 8.x** — 8.0+ is required, for `SKIP LOCKED` and descending indexes
- **Maven 3.8+** — or use **NetBeans 24**, which bundles Maven

On this machine these are already installed portably under
`%USERPROFILE%\toolchain` (see [Build status](#build-status)). Note that the
pre-existing `JAVA_HOME` points at a broken Amazon Corretto install whose `bin`
directory is empty — set `JAVA_HOME` to the Temurin JDK there instead.

Check the JDK is visible:

```bash
java -version
javac -version
```

### 2. Create the database

```bash
mysql -u root -p < database/schema.sql
```

This drops and recreates the `digiq` schema, creates all six tables, seeds
services, counters and accounts, and generates **14 days of demo history** so the
analytics charts have something to plot on first run.

### 3. Point the app at your MySQL

Edit `src/main/resources/db.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/digiq?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
db.username=root
db.password=YOUR_PASSWORD
```

Any value can be overridden at deploy time without rebuilding:

```
-Ddigiq.db.password=secret
```

### 4. Build

```bash
mvn clean package
```

This produces `target/digiq.war`.

### 5. Deploy

Copy the WAR into Tomcat and start it:

```bash
cp target/digiq.war $CATALINA_HOME/webapps/
$CATALINA_HOME/bin/startup.sh      # startup.bat on Windows
```

Then open **<http://localhost:8080/digiq/>**.

On startup the log line `DigiQ: database pool ready (...)` confirms the database
connection. If you instead see `DATABASE UNAVAILABLE`, fix `db.properties` — the
listener reports it at boot rather than letting the first click fail.

#### In NetBeans 24
Open the folder as a Maven project, add your Tomcat 9 under
*Services → Servers*, then **Run**.

---

## Demo accounts

All seeded accounts use the password **`Digiq@123`**.

| Role | Email |
|---|---|
| Administrator | `admin@digiq.com` |
| Counter staff | `grace@digiq.com` (Counter 1, Account Opening) |
| Counter staff | `daniel@digiq.com` (Counter 2, Cash Deposit) |
| Counter staff | `miriam@digiq.com` (Counter 3, Cash Withdrawal) |
| Customer | `joseph@mail.com` |
| Customer | `sarah@mail.com` |

> The seeded hash is BCrypt with the `$2a$` prefix, because jBCrypt 0.4 rejects
> `$2b$`/`$2y$`. `PasswordUtil` normalises those prefixes anyway, so a hash
> generated by other tooling still authenticates.

### A good demo run

1. Sign in as **joseph@mail.com**, book a token for Account Opening.
2. Open `/digiq/board` in a second window — the queue depth moves.
3. Sign in as **grace@digiq.com** in a third window, press **Call next**.
4. Watch the board flash the number and the customer's page flip to *"Go to Counter 1"* —
   all three screens update with no refresh.

---

## How the queue works

### Token numbering
Per service, per day: `ACC-0001`, `ACC-0002`, … The sequence is read and the row
inserted inside one transaction with the day's rows locked, so two customers
booking simultaneously cannot be handed the same number. The unique key
`uq_token_day` is the backstop.

### Calling the next customer
```sql
SELECT id FROM tokens
 WHERE service_id = ? AND service_date = CURDATE() AND status = 'PENDING'
 ORDER BY priority DESC, issued_at ASC
 LIMIT 1 FOR UPDATE SKIP LOCKED
```
`SKIP LOCKED` is what makes several counters safe at once: each transaction locks
a different row instead of blocking, so two staff pressing **Call next** together
get two different customers rather than the same one.

This depends on `idx_token_queue` being declared `(..., priority DESC, issued_at ASC)`
— the same order as the `ORDER BY`. Against a plain ascending index the mixed
ordering needs a filesort, which locks every matching row rather than one, and the
second counter is then wrongly told the queue is empty. If you change that index,
change this query with it. The transaction is also retried on an InnoDB deadlock,
which is normal under contention rather than a fault.

### Status flow
```
PENDING ──(staff calls)──▶ IN_SERVICE ──(staff completes)──▶ COMPLETED
   │                            │
   │                            └──(never presented)──▶ NO_SHOW
   └──(customer cancels)──▶ CANCELLED
```
Every transition writes a `service_logs` row inside the same transaction, so the
audit trail cannot drift from the tokens table.

### Live updates
One WebSocket endpoint, `/ws/queue`, broadcasts `TOKEN_ISSUED`, `TOKEN_CALLED`,
`TOKEN_UPDATED`, `COUNTER_UPDATED` and `QUEUE_CHANGED`. Clients treat an event as
*"something moved"* and re-read authoritative state over HTTP — the socket never
carries a customer's position, because only the database knows it. The client
reconnects with a capped backoff, so a display board left running overnight
recovers on its own.

---

## Project layout

```
Digital_Queue_Management_System/
├── run.bat / run.ps1            Start everything and open the app
├── stop.bat / stop.ps1          Stop the servers
├── pom.xml                      Maven build → digiq.war
├── README.md                    This file
├── docs/ARCHITECTURE.md         Layers, ER diagram, queue algorithm, known limits
├── tests/                       Verification suite + its own README
├── database/schema.sql          Schema + seed + 14 days of demo history
├── src/main/resources/
│   └── db.properties            JDBC settings (override with -Ddigiq.*)
├── src/main/java/com/digiq/
│   ├── config/                  Hikari pool, startup listener
│   ├── model/                   User, Service, Counter, Token, ServiceLog,
│   │                            Notification, DashboardStats + enums
│   ├── dao/                     JDBC data access, one class per aggregate
│   ├── service/                 QueueService (the rules), Auth, QR, PDF
│   ├── servlet/                 auth · customer · staff · admin · display
│   ├── websocket/               Endpoint + broadcaster
│   ├── filter/                  Role gate, UTF-8 encoding
│   └── util/                    Passwords, JSON, request helpers
└── src/main/webapp/
    ├── WEB-INF/web.xml          Session config + error pages
    ├── WEB-INF/views/           JSPs (unreachable by direct URL)
    └── assets/css · js          Design system, client runtime, charts
```

**Layering rule:** servlets never touch a DAO for anything that changes queue
state — that goes through `QueueService`, which persists, notifies and broadcasts
in that order, so no screen learns about a change the database has not committed.

### URL map

| Path | Who | Purpose |
|---|---|---|
| `/` | public | Landing page with live queue depth |
| `/login`, `/register`, `/logout` | public | Authentication |
| `/board`, `/board/data` | public | Display board + its JSON snapshot |
| `/customer/home` | customer | Book a token |
| `/customer/token?id=` | customer | Live ticket with QR |
| `/customer/token/pdf?id=` | customer | Printable A5 ticket |
| `/customer/token/qr?id=` | customer | QR code PNG |
| `/customer/tokens` | customer | Booking history |
| `/customer/status?id=` | customer | Position JSON |
| `/customer/notifications` | customer | Alert feed |
| `/staff/console` | staff | Counter console |
| `/staff/action` | staff | Call / complete / no-show / open / pause / close |
| `/staff/scan` | staff | Resolve a scanned QR or token number |
| `/admin/dashboard` | admin | Operations overview |
| `/admin/analytics`, `/admin/analytics/data` | admin | Charts + JSON |
| `/admin/services`, `/admin/counters`, `/admin/users` | admin | Configuration |
| `/admin/tokens`, `/admin/logs` | admin | Live queue, audit trail |
| `/ws/queue` | all | WebSocket event stream |

---

## Screens

All eight areas are built: landing page, sign in, register, customer home, live
ticket, token history, notifications, counter console (with camera scanner),
admin dashboard, analytics, services, counters, users, live queue, service logs,
and the public display board — plus 403/404/500 pages.

---

## Design notes

The interface is a small design system in `assets/css/digiq.css`, not ad-hoc
styling:

- **Tokens first.** Colour, radius, shadow and spacing are CSS custom properties.
- **Dark mode is selected, not inverted.** The dark palette is its own set of
  steps, declared under both `prefers-color-scheme` and an explicit
  `data-theme` toggle so the user's choice wins either way. The theme is applied
  before first paint, so there is no light flash.
- **Charts follow a validated palette.** The categorical hues were checked with a
  colour-vision-deficiency validator in both light and dark mode
  (worst-pair ΔE 9.2 light / 9.4 dark against an ≥8 target; normal-vision ΔE 24.0 / 20.9
  against an ≥15 floor). There are no dual-axis charts anywhere — where two
  measures appear together they share a unit (minutes). Single-series bars carry
  direct value labels, and every chart has a table view, so meaning never rests
  on colour alone.
- **Accessible by default.** Visible focus rings, `aria-label`s on icon buttons,
  live regions for toasts, keyboard-dismissible modals, and a
  `prefers-reduced-motion` block.
- Responsive to phone width, with a print stylesheet for the ticket.

### Who can do what

Registration is **open to the public**, and always creates a **customer** —
`AuthService.register` hardcodes the role, so posting `role=ADMIN` in the form
achieves nothing. Staff and administrator accounts can only be created from the
admin console.

| Area | Who |
|---|---|
| `/`, `/login`, `/register`, `/board` | anyone, signed in or not |
| `/customer/*` | any signed-in customer |
| `/staff/*` | counter staff only |
| `/admin/*` | administrators only |

Because the sign-in page is public, it does **not** list the demo accounts by
default. Switch them back on for a local walkthrough with the context parameter in
`web.xml`:

```xml
<context-param>
  <param-name>digiq.showDemoAccounts</param-name>
  <param-value>true</param-value>
</context-param>
```

Leave it `false` whenever anyone else can reach the application — that box prints
the administrator password, which would make every role check pointless.

The sign-in field accepts **either an email address or a plain username**, so an
account whose login name is not an email still works.

### Security
- BCrypt password hashing; the hash never reaches the view layer.
- Session is regenerated on login to prevent fixation.
- `AuthFilter` derives the required role from the URL prefix, so a new page under
  `/admin/` is protected the moment it is added.
- All SQL uses `PreparedStatement`; all user-supplied text is escaped with `<c:out>`.
- JSPs live under `WEB-INF`, so no view can be requested directly.
- `HttpOnly` session cookie; the login form does not reveal whether an address exists.
- The public board deliberately exposes no personal data.

---

## Build status

**Built, deployed and exercised end to end.**

Verified against Temurin JDK 17.0.20, Maven 3.9.9, Tomcat 9.0.84 and MySQL 8.0.45
on Windows 11:

- `mvn clean package` — 55 classes, no errors, **no warnings** under `-Xlint:all`;
- all **24 JSPs precompiled** with Jasper (`JspC`) — 0 errors, so no view fails at
  first request;
- schema loads from scratch and seeds ~800 demo tokens;
- the WAR deploys and connects: `DigiQ: database pool ready`;
- **~60 functional checks pass** across four suites — public pages, the full
  customer journey (book → QR → PDF → live status), the staff console (call,
  scan, complete), every admin screen, role isolation, registration, priority
  ordering, notifications, cancellation, the WebSocket event stream, and two
  counters draining one queue concurrently;
- redeploy is clean: no classloader leak, no driver or thread warnings.

### Fixed during that run

Three real defects surfaced only under load or at runtime:

1. **Deadlock on simultaneous "Call next".** Two counters calling at the same
   instant both update `tokens` and its queue index; InnoDB picks one as a victim
   and rolls it back, which surfaced to staff as *"could not reach the database"*.
   `TokenDAO` now retries a rolled-back transaction (error 1213/1205) up to four
   times with a jittered backoff, which is MySQL's own prescribed remedy.

2. **A counter being told "nobody is waiting" while customers were queued.**
   The far more serious one. `ORDER BY priority DESC, issued_at ASC` did not match
   the ascending index, so MySQL used a **filesort** — and a filesort must read,
   and therefore lock, *every* matching row before returning one. The first
   counter locked the whole queue; the second's `SKIP LOCKED` skipped all of it
   and reported an empty queue. Reproduced deterministically: the second caller
   came back empty in **8 of 8 trials**. Making `idx_token_queue` descending on
   `priority` removes the filesort (`EXPLAIN` drops `Using filesort`) and the
   second caller now gets the next token in **0 of 8** empty trials.

3. **Classloader leak on redeploy.** Connector/J's cleanup thread and its
   registered driver outlived the stopped context, and Tomcat logged a memory-leak
   warning on every redeploy. `AppContextListener` now stops that thread and
   deregisters only this application's drivers.

A fourth, cosmetic one: `CONNECTED` and `PONG` were sent outside the
`{type, payload}` envelope every other WebSocket event uses.

### Reproducing the verification

The suites live in [`tests/`](tests/) and drive the running application, not mocks.
Start the app, then:

```powershell
.\run.ps1
.\tests\run-all.ps1 -Reset
```

See [`tests/README.md`](tests/README.md) for what each suite covers and why
`05-lock-behaviour.js` exists.

The toolchain was installed portably under `%USERPROFILE%\toolchain` (no admin
rights needed) — JDK 17, Maven 3.9.9, Tomcat 9.0.84 and MySQL 8.0.45. To rebuild
by hand:

```bash
export JAVA_HOME=$HOME/toolchain/jdk-17.0.20.1+1
export PATH="$JAVA_HOME/bin:$HOME/toolchain/apache-maven-3.9.9/bin:$PATH"
mvn clean package
```
