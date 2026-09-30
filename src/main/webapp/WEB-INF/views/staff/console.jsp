<%@ include file="../layout/header.jsp" %>

<c:choose>
  <%-- ---------- No counter assigned ---------- --%>
  <c:when test="${empty counter}">
    <div class="card">
      <div class="empty">
        <div class="empty__icon" aria-hidden="true">&#128683;</div>
        <div class="empty__title">No counter is assigned to you yet</div>
        <div class="small">
          Ask an administrator to link your account to a counter under
          <strong>Counters</strong>. The console activates as soon as they do.
        </div>
      </div>
    </div>
  </c:when>

  <c:otherwise>
    <div class="page-head">
      <div class="page-head__text">
        <h1 class="h1"><c:out value="${counter.name}" /></h1>
        <p class="muted small">
          Serving <strong><c:out value="${counter.serviceName}" /></strong>
          &middot; <span data-waiting-count>${waitingCount}</span> waiting
          &middot; ${counter.servedToday} completed today
        </p>
      </div>
      <div class="page-head__actions">
        <span class="badge badge--${counter.status.tone}" data-counter-status>${counter.status.label}</span>
        <div class="segmented" role="group" aria-label="Counter status">
          <button type="button" data-counter-action="open"  <c:if test="${counter.status == 'OPEN'}">aria-current="true"</c:if>>Open</button>
          <button type="button" data-counter-action="pause" <c:if test="${counter.status == 'PAUSED'}">aria-current="true"</c:if>>Pause</button>
          <button type="button" data-counter-action="close" <c:if test="${counter.status == 'CLOSED'}">aria-current="true"</c:if>>Close</button>
        </div>
      </div>
    </div>

    <div class="grid grid--main">

      <%-- ---------- Serving panel ---------- --%>
      <div class="stack">
        <c:choose>
          <c:when test="${not empty currentToken}">
            <div class="now-serving" data-serving-panel>
              <span class="eyebrow">Now serving</span>
              <div class="now-serving__number" data-serving-number><c:out value="${currentToken.tokenNumber}" /></div>
              <div class="now-serving__who">
                <c:out value="${currentToken.customerName}" />
                <c:if test="${currentToken.priority}"> &middot; Priority</c:if>
              </div>
              <div class="row" style="justify-content:center;margin-top:18px;">
                <button class="btn btn--lg" style="background:#fff;color:var(--brand-900);border-color:#fff;"
                        data-token-action="complete" data-token-id="${currentToken.id}">
                  Complete service
                </button>
                <button class="btn btn--lg" style="background:transparent;color:#fff;border-color:rgba(255,255,255,.5);"
                        data-token-action="noshow" data-token-id="${currentToken.id}">
                  No show
                </button>
              </div>
            </div>
          </c:when>
          <c:otherwise>
            <div class="now-serving now-serving--idle" data-serving-panel>
              <span class="eyebrow">Counter idle</span>
              <div class="now-serving__number" data-serving-number>&mdash;</div>
              <div class="small">Call the next customer when you are ready.</div>
            </div>
          </c:otherwise>
        </c:choose>

        <button class="btn btn--primary btn--lg btn--block" data-counter-action="call"
                <c:if test="${counter.status != 'OPEN'}">disabled title="Open the counter first"</c:if>>
          Call next customer
        </button>

        <%-- ---------- Waiting queue ---------- --%>
        <div class="card">
          <div class="card__head">
            <div class="h3">Waiting queue</div>
            <div class="card__actions tiny dim"><span data-waiting-count>${waitingCount}</span> in line</div>
          </div>
          <div class="card__body--flush">
            <c:choose>
              <c:when test="${empty queue}">
                <div class="empty" style="padding:30px 16px;">
                  <div class="empty__title">Queue is clear</div>
                  <div class="small">No one is waiting for <c:out value="${counter.serviceName}" />.</div>
                </div>
              </c:when>
              <c:otherwise>
                <ul class="queue-list">
                  <c:forEach var="t" items="${queue}" varStatus="loop">
                    <li>
                      <span class="queue-list__pos">${loop.index + 1}</span>
                      <span style="min-width:0;flex:1;">
                        <span class="queue-list__token"><c:out value="${t.tokenNumber}" /></span>
                        <c:if test="${t.priority}"> <span class="badge badge--priority">Priority</span></c:if>
                        <span class="queue-list__meta" style="display:block;">
                          <c:out value="${t.customerName}" /> &middot; waiting since
                          <fmt:formatDate value="${t.issuedAt}" pattern="HH:mm" />
                        </span>
                      </span>
                    </li>
                  </c:forEach>
                </ul>
              </c:otherwise>
            </c:choose>
          </div>
        </div>
      </div>

      <%-- ---------- Scanner + activity ---------- --%>
      <div class="stack">
        <div class="card">
          <div class="card__head">
            <div class="h3">Verify a ticket</div>
            <div class="card__actions">
              <button class="btn btn--sm" type="button" id="scanToggle">Use camera</button>
            </div>
          </div>
          <div class="card__body stack" style="gap:12px;">
            <div id="qr-reader" hidden></div>

            <form id="scanForm" class="row row--tight" style="flex-wrap:nowrap;">
              <input class="input" id="scanInput" name="code" placeholder="Scan QR or type ACC-0001"
                     autocomplete="off" aria-label="Token code">
              <button class="btn btn--primary" type="submit">Check</button>
            </form>

            <div id="scanResult" hidden></div>

            <p class="tiny dim" style="margin:0;">
              The camera needs HTTPS or localhost. Typing the printed token number works anywhere.
            </p>
          </div>
        </div>

        <div class="card">
          <div class="card__head"><div class="h3">Recent activity</div></div>
          <div class="card__body--flush">
            <c:choose>
              <c:when test="${empty recentActivity}">
                <div class="empty" style="padding:26px 16px;"><div class="small">Nothing logged yet today.</div></div>
              </c:when>
              <c:otherwise>
                <ul class="queue-list">
                  <c:forEach var="log" items="${recentActivity}">
                    <li>
                      <span style="min-width:0;flex:1;">
                        <span class="queue-list__token"><c:out value="${log.tokenNumber}" /></span>
                        <span class="queue-list__meta" style="display:block;">
                          <c:out value="${log.action}" />
                          <c:if test="${not empty log.counterName}"> &middot; <c:out value="${log.counterName}" /></c:if>
                        </span>
                      </span>
                      <span class="push tiny dim nowrap">
                        <fmt:formatDate value="${log.createdAt}" pattern="HH:mm" />
                      </span>
                    </li>
                  </c:forEach>
                </ul>
              </c:otherwise>
            </c:choose>
          </div>
        </div>
      </div>
    </div>
  </c:otherwise>
</c:choose>

<script src="https://cdnjs.cloudflare.com/ajax/libs/html5-qrcode/2.3.8/html5-qrcode.min.js"
        integrity="sha512-r6rDA7W6ZeQhvl8S7yRVQUKVHdexq+GAlNkNNqVC7YyIV+NwqCTJe2hDWCiffTyRNOeGEzRRJ9ifvRm/HCzGYg=="
        crossorigin="anonymous" referrerpolicy="no-referrer"></script>
<script>
document.addEventListener("DOMContentLoaded", function () {
  var base = "${ctx}";

  /* ---------------- Counter + token actions ---------------- */
  function send(payload) {
    return DigiQ.post(base + "/staff/action", payload).then(function (res) {
      if (res.ok) {
        DigiQ.toast(res.message || "Done", null, "good");
        // The panel, queue and counts all move together - re-render server-side.
        window.setTimeout(function () { window.location.reload(); }, 700);
      } else {
        DigiQ.toast("Not done", res.message || res.error || "Try again.", "warning");
      }
      return res;
    });
  }

  document.querySelectorAll("[data-counter-action]").forEach(function (btn) {
    btn.addEventListener("click", function () {
      btn.disabled = true;
      send({ action: btn.getAttribute("data-counter-action") }).then(function () {
        btn.disabled = false;
      });
    });
  });

  document.querySelectorAll("[data-token-action]").forEach(function (btn) {
    btn.addEventListener("click", function () {
      var action = btn.getAttribute("data-token-action");
      if (action === "noshow" && !window.confirm("Mark this token as a no-show?")) { return; }
      btn.disabled = true;
      send({ action: action, tokenId: btn.getAttribute("data-token-id") }).then(function () {
        btn.disabled = false;
      });
    });
  });

  /* ---------------- Ticket lookup ---------------- */
  var form = document.getElementById("scanForm");
  var input = document.getElementById("scanInput");
  var result = document.getElementById("scanResult");

  function show(data) {
    if (!result) { return; }
    result.hidden = false;
    if (!data.ok) {
      result.className = "flash flash--error";
      result.textContent = data.message || data.error || "Ticket not found.";
      return;
    }
    result.className = "scan-result";
    var tone = data.status === "COMPLETED" ? "good"
             : data.status === "IN_SERVICE" ? "info"
             : data.status === "PENDING" ? "warning" : "muted";
    result.innerHTML =
      '<div class="scan-result__token">' + data.tokenNumber + "</div>" +
      '<div class="small muted">' + data.customerName + " &middot; " + data.serviceName + "</div>" +
      '<div style="margin-top:8px;"><span class="badge badge--' + tone + '">' + data.statusLabel + "</span>" +
      (data.priority ? ' <span class="badge badge--priority">Priority</span>' : "") + "</div>" +
      (data.message ? '<div class="small" style="margin-top:8px;color:var(--critical-ink);">' + data.message + "</div>" : "");
  }

  if (form) {
    form.addEventListener("submit", function (event) {
      event.preventDefault();
      var code = input.value.trim();
      if (!code) { return; }
      DigiQ.post(base + "/staff/scan", { code: code }).then(show);
    });
  }

  /* ---------------- Camera scanner ---------------- */
  var toggle = document.getElementById("scanToggle");
  var reader = document.getElementById("qr-reader");
  var scanner = null;
  var scanning = false;

  if (toggle && reader) {
    toggle.addEventListener("click", function () {
      if (typeof Html5Qrcode === "undefined") {
        DigiQ.toast("Scanner unavailable", "The camera library did not load. Type the token number instead.", "warning");
        return;
      }
      if (scanning) {
        scanner.stop().then(function () {
          scanning = false;
          reader.hidden = true;
          toggle.textContent = "Use camera";
        });
        return;
      }
      reader.hidden = false;
      scanner = scanner || new Html5Qrcode("qr-reader");
      scanner.start(
        { facingMode: "environment" },
        { fps: 10, qrbox: { width: 220, height: 220 } },
        function (text) {
          input.value = text;
          DigiQ.post(base + "/staff/scan", { code: text }).then(show);
        },
        function () { /* per-frame decode misses are normal - ignore them */ }
      ).then(function () {
        scanning = true;
        toggle.textContent = "Stop camera";
      }).catch(function () {
        reader.hidden = true;
        DigiQ.toast("Camera blocked", "Allow camera access, or type the token number.", "warning");
      });
    });
  }

  /* ---------------- Live queue depth ---------------- */
  var myService = "${counter.serviceId}";
  function bump(payload) {
    if (payload.serviceId && String(payload.serviceId) !== myService) { return; }
    if (payload.waiting !== undefined) {
      document.querySelectorAll("[data-waiting-count]").forEach(function (el) {
        el.textContent = payload.waiting;
      });
    }
  }
  DigiQ.live.on("TOKEN_ISSUED", function (payload) {
    bump(payload);
    if (!payload.serviceId || String(payload.serviceId) === myService) {
      DigiQ.toast("New ticket", payload.token.tokenNumber + " joined your queue.", "good");
    }
  });
});
</script>

<%@ include file="../layout/footer.jsp" %>
