<%@ include file="../layout/header.jsp" %>

<div class="page-head">
  <div class="page-head__text">
    <h1 class="h1">Operations dashboard</h1>
    <p class="muted small">Today at a glance, updating as counters serve customers.</p>
  </div>
  <div class="page-head__actions">
    <a class="btn" href="${ctx}/board" target="_blank" rel="noopener">Display board</a>
    <a class="btn btn--primary" href="${ctx}/admin/analytics">Full analytics</a>
  </div>
</div>

<%-- ---------- Stat tiles ---------- --%>
<div class="grid grid--4" style="margin-bottom:18px;">
  <div class="stat stat--brand">
    <div class="stat__accent"></div>
    <div class="stat__label">Tokens issued today</div>
    <div class="stat__value tabular" data-stat="issued">${stats.issuedToday}</div>
    <div class="stat__meta">${stats.completedToday} completed &middot; ${stats.completionRate}% completion</div>
  </div>

  <div class="stat stat--warning">
    <div class="stat__accent"></div>
    <div class="stat__label">Waiting now</div>
    <div class="stat__value tabular" data-stat="waiting">${stats.waitingNow}</div>
    <div class="stat__meta">${stats.inServiceNow} at a counter right now</div>
  </div>

  <div class="stat stat--good">
    <div class="stat__accent"></div>
    <div class="stat__label">Average wait</div>
    <div class="stat__value tabular">${stats.avgWaitMinutes}<span style="font-size:16px;font-weight:560;"> min</span></div>
    <div class="stat__meta">Average service time ${stats.avgServiceMinutes} min</div>
  </div>

  <div class="stat">
    <div class="stat__accent"></div>
    <div class="stat__label">Counters open</div>
    <div class="stat__value tabular">${stats.openCounters}<span style="font-size:16px;font-weight:560;color:var(--ink-3);"> / ${stats.totalCounters}</span></div>
    <div class="stat__meta">${stats.totalStaff} staff &middot; ${stats.totalServices} services</div>
  </div>
</div>

<div class="grid grid--main">
  <div class="stack">
    <%-- ---------- Volume trend ---------- --%>
    <div class="card chart-card" data-view="chart">
      <div class="chart-card__head">
        <div>
          <div class="chart-card__title">Tokens issued and completed</div>
          <div class="chart-card__sub">Last 14 days</div>
        </div>
        <button class="btn btn--sm btn--ghost chart-card__toggle" type="button" data-view-toggle>Show table</button>
      </div>
      <div class="chart-holder"><canvas id="chartVolume" aria-label="Tokens issued and completed per day"></canvas></div>
      <div class="chart-table table-wrap" data-visible="false">
        <table class="table">
          <thead><tr><th>Date</th><th class="num">Issued</th><th class="num">Completed</th></tr></thead>
          <tbody id="trendRows"></tbody>
        </table>
      </div>
    </div>

    <%-- ---------- Counters ---------- --%>
    <div class="card">
      <div class="card__head">
        <div class="h3">Counters</div>
        <div class="card__actions"><a class="btn btn--sm btn--ghost" href="${ctx}/admin/counters">Manage</a></div>
      </div>
      <div class="card__body--flush table-wrap">
        <table class="table">
          <thead>
            <tr><th>Counter</th><th>Service</th><th>Staff</th><th>Now serving</th>
                <th class="num">Completed today</th><th>Status</th></tr>
          </thead>
          <tbody>
            <c:forEach var="c" items="${counters}">
              <tr data-counter-row="${c.id}">
                <td class="strong"><c:out value="${c.name}" /></td>
                <td class="small"><c:out value="${c.serviceName}" /></td>
                <td class="small muted">
                  <c:choose>
                    <c:when test="${empty c.staffName}"><span class="dim">Unassigned</span></c:when>
                    <c:otherwise><c:out value="${c.staffName}" /></c:otherwise>
                  </c:choose>
                </td>
                <td class="table__token" data-counter-token>
                  <c:choose>
                    <c:when test="${empty c.currentTokenNumber}"><span class="dim">&ndash;</span></c:when>
                    <c:otherwise><c:out value="${c.currentTokenNumber}" /></c:otherwise>
                  </c:choose>
                </td>
                <td class="num tabular">${c.servedToday}</td>
                <td><span class="badge badge--${c.status.tone}" data-counter-badge>${c.status.label}</span></td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </div>
    </div>

    <%-- ---------- Today's tokens ---------- --%>
    <div class="card">
      <div class="card__head">
        <div class="h3">Latest tokens</div>
        <div class="card__actions"><a class="btn btn--sm btn--ghost" href="${ctx}/admin/tokens">See all</a></div>
      </div>
      <div class="card__body--flush table-wrap">
        <c:choose>
          <c:when test="${empty recentTokens}">
            <div class="empty" style="padding:30px;"><div class="small">No tokens issued today yet.</div></div>
          </c:when>
          <c:otherwise>
            <table class="table">
              <thead><tr><th>Token</th><th>Customer</th><th>Service</th><th>Issued</th><th>Status</th></tr></thead>
              <tbody>
                <c:forEach var="t" items="${recentTokens}">
                  <tr>
                    <td class="table__token"><c:out value="${t.tokenNumber}" /></td>
                    <td class="small"><c:out value="${t.customerName}" /></td>
                    <td class="small muted"><c:out value="${t.serviceName}" /></td>
                    <td class="small muted nowrap"><fmt:formatDate value="${t.issuedAt}" pattern="HH:mm" /></td>
                    <td><span class="badge badge--${t.status.tone}">${t.status.label}</span></td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
  </div>

  <%-- ---------- Side column ---------- --%>
  <div class="stack">
    <div class="card">
      <div class="card__head"><div class="h3">Queue depth by service</div></div>
      <div class="card__body--flush">
        <ul class="queue-list">
          <c:forEach var="s" items="${services}">
            <li>
              <span class="service-card__code" style="width:36px;height:36px;font-size:10px;"><c:out value="${s.code}" /></span>
              <span style="min-width:0;flex:1;">
                <span class="queue-list__token"><c:out value="${s.name}" /></span>
                <span class="queue-list__meta" style="display:block;">
                  ${s.openCounters} counter<c:if test="${s.openCounters != 1}">s</c:if> &middot; ~${s.estimatedWaitMinutes} min
                </span>
              </span>
              <span class="push badge ${s.waitingCount == 0 ? 'badge--good' : (s.waitingCount > 8 ? 'badge--critical' : 'badge--warning')}"
                    data-service-waiting="${s.id}">
                ${s.waitingCount}
              </span>
            </li>
          </c:forEach>
        </ul>
      </div>
    </div>

    <div class="card">
      <div class="card__head"><div class="h3">Live activity</div></div>
      <div class="card__body--flush">
        <ul class="queue-list" id="activityFeed">
          <c:forEach var="log" items="${activity}">
            <li>
              <span style="min-width:0;flex:1;">
                <span class="queue-list__token"><c:out value="${log.tokenNumber}" /></span>
                <span class="queue-list__meta" style="display:block;">
                  <c:out value="${log.action}" />
                  <c:if test="${not empty log.counterName}"> &middot; <c:out value="${log.counterName}" /></c:if>
                </span>
              </span>
              <span class="push tiny dim nowrap"><fmt:formatDate value="${log.createdAt}" pattern="HH:mm" /></span>
            </li>
          </c:forEach>
        </ul>
      </div>
    </div>
  </div>
</div>

<script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.min.js"
        integrity="sha512-CQBWl4fJHWbryGE+Pc7UAxWMUMNMWzWxF4SQo9CgkJIN1kx6djDQZjh3Y8SZ1d+6I+1zze6Z7kHXO7q3UyZAWw=="
        crossorigin="anonymous" referrerpolicy="no-referrer"></script>
<script src="${ctx}/assets/js/charts.js" defer></script>
<script>
document.addEventListener("DOMContentLoaded", function () {
  var trend = ${trendJson};

  DigiQ.analytics.render({
    tokensPerDay: trend,
    statusBreakdown: [],
    waitVsService: [],
    peakHours: [],
    counterPerformance: []
  });

  // Table view of the same numbers - the relief path when colour is not enough.
  var body = document.getElementById("trendRows");
  if (body) {
    trend.forEach(function (row) {
      var tr = document.createElement("tr");
      tr.innerHTML = "<td>" + row.date + '</td><td class="num tabular">' + row.issued +
                     '</td><td class="num tabular">' + row.completed + "</td>";
      body.appendChild(tr);
    });
  }

  /* ---------------- Live updates ---------------- */
  function setCounter(payload) {
    var row = document.querySelector('[data-counter-row="' + payload.counterId + '"]');
    if (!row) { return; }
    var cell = row.querySelector("[data-counter-token]");
    if (cell && payload.token) { cell.textContent = payload.token.tokenNumber; }
  }

  DigiQ.live.on("TOKEN_CALLED", function (payload) {
    setCounter(payload);
    var el = document.querySelector('[data-stat="waiting"]');
    if (el) { el.textContent = Math.max(0, parseInt(el.textContent, 10) - 1); }
    prepend(payload.token.tokenNumber, "Called - " + payload.counterName);
  });

  DigiQ.live.on("TOKEN_ISSUED", function (payload) {
    var issued = document.querySelector('[data-stat="issued"]');
    if (issued) { issued.textContent = parseInt(issued.textContent, 10) + 1; }
    var waiting = document.querySelector('[data-stat="waiting"]');
    if (waiting) { waiting.textContent = parseInt(waiting.textContent, 10) + 1; }
    var badge = document.querySelector('[data-service-waiting="' + payload.serviceId + '"]');
    if (badge && payload.waiting !== undefined) { badge.textContent = payload.waiting; }
    prepend(payload.token.tokenNumber, "Issued - " + payload.token.serviceName);
  });

  DigiQ.live.on("TOKEN_UPDATED", function (payload) {
    prepend(payload.token.tokenNumber, payload.token.statusLabel);
  });

  DigiQ.live.on("COUNTER_UPDATED", function (payload) {
    var row = document.querySelector('[data-counter-row="' + payload.counterId + '"]');
    if (!row) { return; }
    var badge = row.querySelector("[data-counter-badge]");
    if (!badge) { return; }
    var tone = payload.status === "OPEN" ? "good" : (payload.status === "PAUSED" ? "warning" : "muted");
    badge.className = "badge badge--" + tone;
    badge.textContent = payload.status.charAt(0) + payload.status.slice(1).toLowerCase();
  });

  function prepend(token, text) {
    var feed = document.getElementById("activityFeed");
    if (!feed) { return; }
    var li = document.createElement("li");
    var now = new Date();
    li.innerHTML = '<span style="min-width:0;flex:1;"><span class="queue-list__token"></span>' +
                   '<span class="queue-list__meta" style="display:block;"></span></span>' +
                   '<span class="push tiny dim nowrap">' +
                   String(now.getHours()).padStart(2, "0") + ":" + String(now.getMinutes()).padStart(2, "0") +
                   "</span>";
    li.querySelector(".queue-list__token").textContent = token;
    li.querySelector(".queue-list__meta").textContent = text;
    feed.insertBefore(li, feed.firstChild);
    while (feed.children.length > 12) { feed.removeChild(feed.lastChild); }
  }
});
</script>

<%@ include file="../layout/footer.jsp" %>
