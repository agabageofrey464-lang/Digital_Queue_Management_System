<%@ include file="../layout/header.jsp" %>

<div class="page-head">
  <div class="page-head__text">
    <h1 class="h1">Hello, <c:out value="${me.fullName}" /></h1>
    <p class="muted small">Pick a service to join its queue. Your position updates live.</p>
  </div>
  <div class="page-head__actions">
    <a class="btn" href="${ctx}/board" target="_blank" rel="noopener">Open display board</a>
  </div>
</div>

<%-- ---------- Live tokens ---------- --%>
<c:if test="${not empty activeTokens}">
  <div class="stack" style="margin-bottom:20px;">
    <c:forEach var="t" items="${activeTokens}">
      <div class="card" data-token-card data-token-id="${t.id}" data-service-id="${t.serviceId}">
        <div class="card__body">
          <div class="row" style="align-items:flex-start;">
            <div style="min-width:0;flex:1;">
              <span class="eyebrow">Active token</span>
              <div class="row row--tight" style="margin-top:4px;">
                <span style="font-size:32px;font-weight:700;letter-spacing:-0.03em;" class="tabular">
                  <c:out value="${t.tokenNumber}" />
                </span>
                <span class="badge badge--${t.status.tone}" data-token-status>${t.status.label}</span>
                <c:if test="${t.priority}"><span class="badge badge--priority">Priority</span></c:if>
              </div>
              <div class="small muted" style="margin-top:2px;"><c:out value="${t.serviceName}" /></div>
            </div>

            <div class="position" style="flex:none;">
              <div>
                <div class="position__num" data-token-position>
                  <c:choose>
                    <c:when test="${t.status == 'IN_SERVICE'}">&#9654;</c:when>
                    <c:otherwise>${t.positionInQueue}</c:otherwise>
                  </c:choose>
                </div>
              </div>
              <div class="position__text" data-token-hint>
                <c:choose>
                  <c:when test="${t.status == 'IN_SERVICE'}">
                    <strong>Go to <c:out value="${t.counterName}" /></strong><br>You are being served now.
                  </c:when>
                  <c:when test="${t.positionInQueue == 1}">
                    <strong>You are next</strong><br>Stay close to the service area.
                  </c:when>
                  <c:otherwise>
                    <strong>in the queue</strong><br>About ${t.estimatedWaitMinutes} minutes to go.
                  </c:otherwise>
                </c:choose>
              </div>
            </div>
          </div>
        </div>
        <div class="card__foot row">
          <a class="btn btn--sm" href="${ctx}/customer/token?id=${t.id}">View ticket &amp; QR</a>
          <a class="btn btn--sm" href="${ctx}/customer/token/pdf?id=${t.id}">Download PDF</a>
          <c:if test="${t.status == 'PENDING'}">
            <form method="post" action="${ctx}/customer/cancel" class="push"
                  data-confirm="Cancel token ${t.tokenNumber}? You will lose your place in the queue.">
              <input type="hidden" name="tokenId" value="${t.id}">
              <button class="btn btn--sm btn--ghost" type="submit">Cancel token</button>
            </form>
          </c:if>
        </div>
      </div>
    </c:forEach>
  </div>
</c:if>

<%-- ---------- Bookable services ---------- --%>
<h2 class="h2" style="margin-bottom:12px;">Available services</h2>

<c:choose>
  <c:when test="${empty services}">
    <div class="card"><div class="empty">
      <div class="empty__icon" aria-hidden="true">&#128203;</div>
      <div class="empty__title">Nothing to book yet</div>
      <div class="small">An administrator has not published any services.</div>
    </div></div>
  </c:when>
  <c:otherwise>
    <div class="grid grid--3">
      <c:forEach var="s" items="${services}">
        <form class="service-card" method="post" action="${ctx}/customer/book">
          <input type="hidden" name="serviceId" value="${s.id}">

          <div class="service-card__top">
            <div class="service-card__code"><c:out value="${s.code}" /></div>
            <div style="min-width:0;">
              <div class="service-card__name"><c:out value="${s.name}" /></div>
              <div class="service-card__desc"><c:out value="${s.description}" /></div>
            </div>
          </div>

          <div class="service-card__stats">
            <div class="service-card__stat">
              <b class="tabular" data-waiting-for="${s.id}">${s.waitingCount}</b>
              <span>Waiting</span>
            </div>
            <div class="service-card__stat">
              <b class="tabular">~${s.estimatedWaitMinutes}m</b>
              <span>Est. wait</span>
            </div>
            <div class="service-card__stat">
              <b class="tabular">${s.openCounters}</b>
              <span>Counters</span>
            </div>
          </div>

          <label class="checkline small">
            <input type="checkbox" name="priority" value="on">
            Priority assistance (elderly, disability, expectant)
          </label>

          <button class="btn btn--primary btn--block" type="submit"
                  <c:if test="${s.openCounters == 0}">title="No counter is open for this service yet"</c:if>>
            Book a token
          </button>
        </form>
      </c:forEach>
    </div>
  </c:otherwise>
</c:choose>

<%-- ---------- Recent history ---------- --%>
<c:if test="${not empty recentTokens}">
  <div class="card" style="margin-top:20px;">
    <div class="card__head">
      <div class="h3">Recent tokens</div>
      <div class="card__actions"><a class="btn btn--sm btn--ghost" href="${ctx}/customer/tokens">See all</a></div>
    </div>
    <div class="card__body--flush table-wrap">
      <table class="table">
        <thead>
          <tr><th>Token</th><th>Service</th><th>Issued</th><th>Status</th><th></th></tr>
        </thead>
        <tbody>
          <c:forEach var="t" items="${recentTokens}">
            <tr>
              <td class="table__token"><c:out value="${t.tokenNumber}" /></td>
              <td><c:out value="${t.serviceName}" /></td>
              <td class="small muted"><fmt:formatDate value="${t.issuedAt}" pattern="dd MMM, HH:mm" /></td>
              <td><span class="badge badge--${t.status.tone}">${t.status.label}</span></td>
              <td class="num"><a class="btn btn--sm btn--ghost" href="${ctx}/customer/token?id=${t.id}">Open</a></td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </div>
  </div>
</c:if>

<script>
  /* Any queue movement on a service this customer is waiting in refreshes the
     position from the database - the socket only says that something changed. */
  document.addEventListener("DOMContentLoaded", function () {
    var cards = Array.prototype.slice.call(document.querySelectorAll("[data-token-card]"));
    if (!cards.length) { return; }

    function refresh(card) {
      var id = card.getAttribute("data-token-id");
      DigiQ.get("${ctx}/customer/status?id=" + id).then(function (data) {
        if (!data || !data.ok) { return; }

        var badge = card.querySelector("[data-token-status]");
        if (badge) {
          badge.textContent = data.statusLabel;
          badge.className = "badge badge--" + data.tone;
        }

        var pos = card.querySelector("[data-token-position]");
        var hint = card.querySelector("[data-token-hint]");
        if (data.status === "IN_SERVICE") {
          if (pos) { pos.innerHTML = "&#9654;"; }
          if (hint) { hint.innerHTML = "<strong>Go to " + data.counterName + "</strong><br>You are being served now."; }
          DigiQ.toast("It is your turn", "Please proceed to " + data.counterName + ".", "good");
        } else if (data.status === "PENDING") {
          if (pos) { pos.textContent = data.position; }
          if (hint) {
            hint.innerHTML = data.position === 1
              ? "<strong>You are next</strong><br>Stay close to the service area."
              : "<strong>in the queue</strong><br>About " + data.estimatedWait + " minutes to go.";
          }
        } else {
          window.location.reload();
        }
      });
    }

    function onChange(payload) {
      cards.forEach(function (card) {
        if (!payload.serviceId || String(payload.serviceId) === card.getAttribute("data-service-id")) {
          refresh(card);
        }
      });
    }

    DigiQ.live.on("TOKEN_CALLED", onChange);
    DigiQ.live.on("TOKEN_UPDATED", onChange);
    DigiQ.live.on("QUEUE_CHANGED", onChange);
    DigiQ.live.on("TOKEN_ISSUED", function (payload) {
      var counter = document.querySelector('[data-waiting-for="' + payload.serviceId + '"]');
      if (counter && payload.waiting !== undefined) { counter.textContent = payload.waiting; }
    });
  });
</script>

<%@ include file="../layout/footer.jsp" %>
