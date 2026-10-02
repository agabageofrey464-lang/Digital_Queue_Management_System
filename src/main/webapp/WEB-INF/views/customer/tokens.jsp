<%@ include file="../layout/header.jsp" %>

<div class="page-head">
  <div class="page-head__text">
    <h1 class="h1">My tokens</h1>
    <p class="muted small">Everything you have booked, newest first.</p>
  </div>
  <div class="page-head__actions">
    <a class="btn btn--primary" href="${ctx}/customer/home">Book another token</a>
  </div>
</div>

<c:if test="${not empty activeTokens}">
  <div class="grid grid--2" style="margin-bottom:20px;">
    <c:forEach var="t" items="${activeTokens}">
      <div class="card">
        <div class="card__body">
          <span class="eyebrow">Active</span>
          <div class="row row--tight" style="margin-top:4px;">
            <span style="font-size:26px;font-weight:700;letter-spacing:-0.03em;" class="tabular"><c:out value="${t.tokenNumber}" /></span>
            <span class="badge badge--${t.status.tone}">${t.status.label}</span>
          </div>
          <div class="small muted"><c:out value="${t.serviceName}" /></div>
          <div class="small" style="margin-top:8px;">
            <c:choose>
              <c:when test="${t.status == 'IN_SERVICE'}">
                Being served at <strong><c:out value="${t.counterName}" /></strong>.
              </c:when>
              <c:otherwise>
                Position <strong>${t.positionInQueue}</strong> &middot; about ${t.estimatedWaitMinutes} minutes.
              </c:otherwise>
            </c:choose>
          </div>
        </div>
        <div class="card__foot row">
          <a class="btn btn--sm" href="${ctx}/customer/token?id=${t.id}">Open ticket</a>
          <a class="btn btn--sm btn--ghost" href="${ctx}/customer/token/pdf?id=${t.id}">PDF</a>
        </div>
      </div>
    </c:forEach>
  </div>
</c:if>

<div class="card">
  <div class="card__head"><div class="h3">Full history</div></div>
  <div class="card__body--flush table-wrap">
    <c:choose>
      <c:when test="${empty tokens}">
        <div class="empty">
          <div class="empty__icon" aria-hidden="true">&#128203;</div>
          <div class="empty__title">You have not booked anything yet</div>
          <div class="small">Choose a service to get your first token.</div>
          <a class="btn btn--primary" style="margin-top:14px;" href="${ctx}/customer/home">Book a token</a>
        </div>
      </c:when>
      <c:otherwise>
        <table class="table">
          <thead>
            <tr>
              <th>Token</th><th>Service</th><th>Issued</th><th>Counter</th>
              <th class="num">Wait</th><th class="num">Service</th><th>Status</th><th></th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="t" items="${tokens}">
              <tr>
                <td class="table__token">
                  <c:out value="${t.tokenNumber}" />
                  <c:if test="${t.priority}"> <span class="badge badge--priority tiny">P</span></c:if>
                </td>
                <td><c:out value="${t.serviceName}" /></td>
                <td class="small muted nowrap"><fmt:formatDate value="${t.issuedAt}" pattern="dd MMM yy, HH:mm" /></td>
                <td class="small"><c:out value="${t.counterName}" /></td>
                <td class="num small">
                  <c:if test="${t.calledAt != null}">${t.waitMinutes}m</c:if>
                  <c:if test="${t.calledAt == null}"><span class="dim">&ndash;</span></c:if>
                </td>
                <td class="num small">
                  <c:if test="${t.completedAt != null}">${t.serviceMinutes}m</c:if>
                  <c:if test="${t.completedAt == null}"><span class="dim">&ndash;</span></c:if>
                </td>
                <td><span class="badge badge--${t.status.tone}">${t.status.label}</span></td>
                <td class="num nowrap">
                  <a class="btn btn--sm btn--ghost" href="${ctx}/customer/token?id=${t.id}">Open</a>
                  <%-- Cancelling straight from the list saves opening the ticket first.
                       Testing for PENDING alone is now sufficient: the daily sweep moves
                       any unreached token from an earlier day to EXPIRED, so a PENDING
                       row here is always one that is genuinely still in today's queue. --%>
                  <c:if test="${t.status == 'PENDING'}">
                    <form method="post" action="${ctx}/customer/cancel" style="display:inline;"
                          data-confirm="Cancel token ${t.tokenNumber}? You will lose your place in the queue.">
                      <input type="hidden" name="tokenId" value="${t.id}">
                      <button class="btn btn--sm btn--ghost" type="submit">Cancel</button>
                    </form>
                  </c:if>
                </td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </c:otherwise>
    </c:choose>
  </div>
</div>

<%@ include file="../layout/footer.jsp" %>
