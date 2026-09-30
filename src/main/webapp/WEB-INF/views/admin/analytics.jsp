<%@ include file="../layout/header.jsp" %>

<div class="page-head">
  <div class="page-head__text">
    <h1 class="h1">Analytics</h1>
    <p class="muted small">Demand, waiting time and counter throughput over the selected window.</p>
  </div>
  <div class="page-head__actions">
    <%-- Range filter sits in one row above the charts it controls. --%>
    <div class="segmented" role="group" aria-label="Date range">
      <a href="${ctx}/admin/analytics?days=7"  <c:if test="${days == 7}">aria-current="true"</c:if>>7 days</a>
      <a href="${ctx}/admin/analytics?days=14" <c:if test="${days == 14}">aria-current="true"</c:if>>14 days</a>
      <a href="${ctx}/admin/analytics?days=30" <c:if test="${days == 30}">aria-current="true"</c:if>>30 days</a>
    </div>
  </div>
</div>

<%-- ---------- Headline figures ---------- --%>
<div class="grid grid--4" style="margin-bottom:18px;">
  <div class="stat stat--brand">
    <div class="stat__accent"></div>
    <div class="stat__label">Issued today</div>
    <div class="stat__value tabular">${stats.issuedToday}</div>
    <div class="stat__meta">${stats.completionRate}% completed</div>
  </div>
  <div class="stat stat--good">
    <div class="stat__accent"></div>
    <div class="stat__label">Average wait today</div>
    <div class="stat__value tabular">${stats.avgWaitMinutes}<span style="font-size:16px;font-weight:560;"> min</span></div>
    <div class="stat__meta">From booking to being called</div>
  </div>
  <div class="stat">
    <div class="stat__accent"></div>
    <div class="stat__label">Average service today</div>
    <div class="stat__value tabular">${stats.avgServiceMinutes}<span style="font-size:16px;font-weight:560;"> min</span></div>
    <div class="stat__meta">Time spent at the counter</div>
  </div>
  <div class="stat stat--critical">
    <div class="stat__accent"></div>
    <div class="stat__label">No-shows today</div>
    <div class="stat__value tabular">${stats.noShowToday}</div>
    <div class="stat__meta">Called but never presented</div>
  </div>
</div>

<%-- ---------- Charts ---------- --%>
<div class="stack">

  <div class="card chart-card" data-view="chart">
    <div class="chart-card__head">
      <div>
        <div class="chart-card__title">Demand over time</div>
        <div class="chart-card__sub">Tokens issued against tokens completed, per day</div>
      </div>
      <button class="btn btn--sm btn--ghost chart-card__toggle" type="button" data-view-toggle>Show table</button>
    </div>
    <div class="chart-holder chart-holder--tall"><canvas id="chartVolume" aria-label="Tokens issued and completed per day"></canvas></div>
    <div class="chart-table table-wrap" data-visible="false">
      <table class="table">
        <thead><tr><th>Date</th><th class="num">Issued</th><th class="num">Completed</th></tr></thead>
        <tbody data-rows="volume"></tbody>
      </table>
    </div>
  </div>

  <div class="grid grid--split">
    <div class="card chart-card" data-view="chart">
      <div class="chart-card__head">
        <div>
          <div class="chart-card__title">How tokens ended</div>
          <div class="chart-card__sub">Outcome mix over the window</div>
        </div>
        <button class="btn btn--sm btn--ghost chart-card__toggle" type="button" data-view-toggle>Show table</button>
      </div>
      <div class="chart-holder"><canvas id="chartStatus" aria-label="Token outcomes"></canvas></div>
      <div class="chart-table table-wrap" data-visible="false">
        <table class="table">
          <thead><tr><th>Outcome</th><th class="num">Tokens</th></tr></thead>
          <tbody data-rows="status"></tbody>
        </table>
      </div>
    </div>

    <div class="card chart-card" data-view="chart">
      <div class="chart-card__head">
        <div>
          <div class="chart-card__title">When people arrive</div>
          <div class="chart-card__sub">Tokens issued by hour of day</div>
        </div>
        <button class="btn btn--sm btn--ghost chart-card__toggle" type="button" data-view-toggle>Show table</button>
      </div>
      <div class="chart-holder"><canvas id="chartPeak" aria-label="Tokens issued by hour"></canvas></div>
      <div class="chart-table table-wrap" data-visible="false">
        <table class="table">
          <thead><tr><th>Hour</th><th class="num">Tokens</th></tr></thead>
          <tbody data-rows="peak"></tbody>
        </table>
      </div>
    </div>
  </div>

  <div class="card chart-card" data-view="chart">
    <div class="chart-card__head">
      <div>
        <div class="chart-card__title">Where the time goes, by service</div>
        <div class="chart-card__sub">Average minutes waiting against average minutes being served</div>
      </div>
      <button class="btn btn--sm btn--ghost chart-card__toggle" type="button" data-view-toggle>Show table</button>
    </div>
    <div class="chart-holder"><canvas id="chartWaitService" aria-label="Average wait and service time per service"></canvas></div>
    <div class="chart-table table-wrap" data-visible="false">
      <table class="table">
        <thead><tr><th>Service</th><th class="num">Avg wait (min)</th><th class="num">Avg service (min)</th></tr></thead>
        <tbody>
          <c:forEach var="r" items="${serviceRows}">
            <tr>
              <td><c:out value="${r.service}" /></td>
              <td class="num tabular">${r.avgWait}</td>
              <td class="num tabular">${r.avgService}</td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </div>
  </div>

  <div class="card chart-card" data-view="chart">
    <div class="chart-card__head">
      <div>
        <div class="chart-card__title">Counter throughput</div>
        <div class="chart-card__sub">Tokens completed per counter over the window</div>
      </div>
      <button class="btn btn--sm btn--ghost chart-card__toggle" type="button" data-view-toggle>Show table</button>
    </div>
    <div class="chart-holder"><canvas id="chartCounters" aria-label="Tokens completed per counter"></canvas></div>
    <div class="chart-table table-wrap" data-visible="false">
      <table class="table">
        <thead><tr><th>Counter</th><th>Service</th><th class="num">Completed</th><th class="num">Avg service (min)</th></tr></thead>
        <tbody>
          <c:forEach var="r" items="${counterRows}">
            <tr>
              <td class="strong"><c:out value="${r.counter}" /></td>
              <td class="small muted"><c:out value="${r.service}" /></td>
              <td class="num tabular">${r.served}</td>
              <td class="num tabular">${r.avgService}</td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </div>
  </div>
</div>

<script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.min.js"
        integrity="sha512-CQBWl4fJHWbryGE+Pc7UAxWMUMNMWzWxF4SQo9CgkJIN1kx6djDQZjh3Y8SZ1d+6I+1zze6Z7kHXO7q3UyZAWw=="
        crossorigin="anonymous" referrerpolicy="no-referrer"></script>
<script src="${ctx}/assets/js/charts.js" defer></script>
<script>
document.addEventListener("DOMContentLoaded", function () {
  var data = ${analyticsJson};
  DigiQ.analytics.render(data);

  function fill(key, rows, cells) {
    var body = document.querySelector('[data-rows="' + key + '"]');
    if (!body) { return; }
    body.innerHTML = "";
    rows.forEach(function (row) {
      var tr = document.createElement("tr");
      cells(row).forEach(function (value, i) {
        var td = document.createElement("td");
        if (i > 0) { td.className = "num tabular"; }
        td.textContent = value;
        tr.appendChild(td);
      });
      body.appendChild(tr);
    });
  }

  function label(raw) {
    return String(raw).replace("_", " ").toLowerCase().replace(/\b\w/g, function (c) { return c.toUpperCase(); });
  }

  fill("volume", data.tokensPerDay, function (r) { return [r.date, r.issued, r.completed]; });
  fill("status", data.statusBreakdown, function (r) { return [label(r.status), r.count]; });
  fill("peak", data.peakHours, function (r) { return [r.label, r.count]; });
});
</script>

<%@ include file="../layout/footer.jsp" %>
