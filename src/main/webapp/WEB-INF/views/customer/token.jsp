<%@ include file="../layout/header.jsp" %>

<div class="page-head">
  <div class="page-head__text">
    <h1 class="h1">Your ticket</h1>
    <p class="muted small">Show the QR code at the counter, or download it to print.</p>
  </div>
  <div class="page-head__actions">
    <a class="btn" href="${ctx}/customer/tokens">All my tokens</a>
    <a class="btn btn--primary" href="${ctx}/customer/token/pdf?id=${token.id}">Download PDF</a>
  </div>
</div>

<div class="grid grid--main" data-token-card data-token-id="${token.id}" data-service-id="${token.serviceId}">

  <%-- ---------- The ticket ---------- --%>
  <div class="ticket">
    <div class="ticket__band">
      <span class="brandmark" style="background:rgba(255,255,255,.18);">Q</span>
      <div>
        <div style="font-weight:660;">DigiQ</div>
        <div class="eyebrow">Digital Queue Management System</div>
      </div>
      <span class="push badge badge--${token.status.tone}" data-token-status>${token.status.label}</span>
    </div>

    <div class="ticket__body">
      <span class="eyebrow">Your token number</span>
      <div class="ticket__number"><c:out value="${token.tokenNumber}" /></div>
      <div class="ticket__service"><c:out value="${token.serviceName}" /></div>
      <c:if test="${token.priority}">
        <div style="margin-top:8px;"><span class="badge badge--priority">Priority service</span></div>
      </c:if>

      <figure class="ticket__qr">
        <img src="${ctx}/customer/token/qr?id=${token.id}&amp;size=380"
             alt="QR code for token ${token.tokenNumber}" width="190" height="190">
      </figure>
      <p class="tiny dim" style="margin-top:8px;">Reference <span class="mono"><c:out value="${token.qrPayload}" /></span></p>
    </div>

    <div class="ticket__perf"></div>

    <dl class="ticket__meta">
      <div>
        <dt>Customer</dt>
        <dd><c:out value="${token.customerName}" /></dd>
      </div>
      <div>
        <dt>Issued</dt>
        <dd><fmt:formatDate value="${token.issuedAt}" pattern="dd MMM yyyy, HH:mm" /></dd>
      </div>
      <div>
        <dt>Counter</dt>
        <dd data-token-counter>
          <c:choose>
            <c:when test="${empty token.counterName}">To be assigned</c:when>
            <c:otherwise><c:out value="${token.counterName}" /></c:otherwise>
          </c:choose>
        </dd>
      </div>
      <div>
        <dt>Status</dt>
        <dd data-token-status-text>${token.status.label}</dd>
      </div>
    </dl>
  </div>

  <%-- ---------- Live position + timeline ---------- --%>
  <div class="stack">
    <div class="card">
      <div class="card__head"><div class="h3">Where you stand</div></div>
      <div class="card__body">
        <c:choose>
          <c:when test="${token.status == 'PENDING'}">
            <div class="position">
              <div class="position__num" data-token-position>${token.positionInQueue}</div>
              <div class="position__text" data-token-hint>
                <c:choose>
                  <c:when test="${token.positionInQueue == 1}">
                    <strong>You are next</strong><br>Please stay close to the service area.
                  </c:when>
                  <c:otherwise>
                    <strong>in the queue</strong><br>Roughly ${token.estimatedWaitMinutes} minutes to go.
                  </c:otherwise>
                </c:choose>
              </div>
            </div>
          </c:when>
          <c:when test="${token.status == 'IN_SERVICE'}">
            <div class="position">
              <div class="position__num" data-token-position>&#9654;</div>
              <div class="position__text" data-token-hint>
                <strong>Go to <c:out value="${token.counterName}" /></strong><br>You are being served now.
              </div>
            </div>
          </c:when>
          <c:otherwise>
            <div class="empty" style="padding:24px 8px;">
              <div class="empty__title">${token.status.label}</div>
              <div class="small">
                <c:if test="${token.status == 'COMPLETED'}">
                  Served in ${token.serviceMinutes} minutes after a ${token.waitMinutes} minute wait.
                </c:if>
                <c:if test="${token.status != 'COMPLETED'}">This token is closed.</c:if>
              </div>
            </div>
          </c:otherwise>
        </c:choose>

        <c:if test="${token.status == 'PENDING'}">
          <form method="post" action="${ctx}/customer/cancel" style="margin-top:14px;"
                data-confirm="Cancel token ${token.tokenNumber}? You will lose your place in the queue.">
            <input type="hidden" name="tokenId" value="${token.id}">
            <button class="btn btn--block" type="submit">Cancel this token</button>
          </form>
        </c:if>
      </div>
    </div>

    <div class="card">
      <div class="card__head"><div class="h3">Progress</div></div>
      <div class="card__body">
        <ul class="timeline">
          <c:forEach var="log" items="${timeline}">
            <li data-tone="${log.toStatus == 'COMPLETED' ? 'good' : (log.toStatus == 'IN_SERVICE' ? 'info' : (log.toStatus == 'NO_SHOW' || log.toStatus == 'CANCELLED' ? 'critical' : ''))}">
              <div class="timeline__title">
                <c:choose>
                  <c:when test="${log.action == 'ISSUED'}">Token booked</c:when>
                  <c:when test="${log.action == 'CALLED'}">Called to <c:out value="${log.counterName}" /></c:when>
                  <c:when test="${log.action == 'COMPLETED'}">Service completed</c:when>
                  <c:when test="${log.action == 'NO_SHOW'}">Marked as no-show</c:when>
                  <c:when test="${log.action == 'CANCELLED'}">Cancelled</c:when>
                  <c:otherwise><c:out value="${log.action}" /></c:otherwise>
                </c:choose>
              </div>
              <div class="timeline__meta">
                <fmt:formatDate value="${log.createdAt}" pattern="dd MMM, HH:mm:ss" />
                <c:if test="${not empty log.staffName}"> &middot; <c:out value="${log.staffName}" /></c:if>
              </div>
            </li>
          </c:forEach>
          <c:if test="${token.status == 'PENDING'}">
            <li><div class="timeline__title dim">Waiting to be called</div>
                <div class="timeline__meta">Updates automatically</div></li>
          </c:if>
        </ul>
      </div>
    </div>
  </div>
</div>

<script>
  document.addEventListener("DOMContentLoaded", function () {
    var card = document.querySelector("[data-token-card]");
    if (!card) { return; }
    var serviceId = card.getAttribute("data-service-id");
    var tokenId = card.getAttribute("data-token-id");
    var lastStatus = "${token.status}";

    function refresh() {
      DigiQ.get("${ctx}/customer/status?id=" + tokenId).then(function (data) {
        if (!data || !data.ok) { return; }

        card.querySelectorAll("[data-token-status]").forEach(function (el) {
          el.textContent = data.statusLabel;
          el.className = "badge badge--" + data.tone;
        });
        var text = card.querySelector("[data-token-status-text]");
        if (text) { text.textContent = data.statusLabel; }
        var counter = card.querySelector("[data-token-counter]");
        if (counter) { counter.textContent = data.counterName || "To be assigned"; }

        var pos = card.querySelector("[data-token-position]");
        var hint = card.querySelector("[data-token-hint]");
        if (data.status === "PENDING" && pos) {
          pos.textContent = data.position;
          if (hint) {
            hint.innerHTML = data.position === 1
              ? "<strong>You are next</strong><br>Please stay close to the service area."
              : "<strong>in the queue</strong><br>Roughly " + data.estimatedWait + " minutes to go.";
          }
        }

        if (data.status !== lastStatus) {
          if (data.status === "IN_SERVICE") {
            DigiQ.toast("It is your turn", "Please proceed to " + data.counterName + ".", "good");
          }
          lastStatus = data.status;
          // A finished token changes the whole page, so re-render it server-side.
          if (data.status !== "PENDING" && data.status !== "IN_SERVICE") {
            window.setTimeout(function () { window.location.reload(); }, 1200);
          } else if (data.status === "IN_SERVICE") {
            window.setTimeout(function () { window.location.reload(); }, 1500);
          }
        }
      });
    }

    function maybe(payload) {
      if (!payload.serviceId || String(payload.serviceId) === serviceId) { refresh(); }
    }

    DigiQ.live.on("TOKEN_CALLED", maybe);
    DigiQ.live.on("TOKEN_UPDATED", maybe);
    DigiQ.live.on("QUEUE_CHANGED", maybe);
  });
</script>

<%@ include file="../layout/footer.jsp" %>
