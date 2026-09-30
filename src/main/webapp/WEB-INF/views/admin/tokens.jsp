<%@ include file="../layout/header.jsp" %>

<div class="page-head">
  <div class="page-head__text">
    <h1 class="h1">Live queue</h1>
    <p class="muted small">Every token issued today.</p>
  </div>
</div>

<form class="filterbar" method="get" action="${ctx}/admin/tokens">
  <div class="field">
    <label class="field__label" for="fService">Service</label>
    <select class="select" id="fService" name="serviceId" data-autosubmit>
      <option value="0">All services</option>
      <c:forEach var="s" items="${services}">
        <option value="${s.id}" <c:if test="${selectedService == s.id}">selected</c:if>>
          <c:out value="${s.name}" />
        </option>
      </c:forEach>
    </select>
  </div>

  <div class="field">
    <label class="field__label" for="fStatus">Status</label>
    <select class="select" id="fStatus" name="status" data-autosubmit>
      <option value="">All statuses</option>
      <c:forEach var="st" items="${statuses}">
        <option value="${st}" <c:if test="${selectedStatus == st}">selected</c:if>>${st.label}</option>
      </c:forEach>
    </select>
  </div>

  <a class="btn btn--ghost" href="${ctx}/admin/tokens">Clear filters</a>
  <span class="push tiny dim">${tokens.size()} token<c:if test="${tokens.size() != 1}">s</c:if></span>
</form>

<div class="card">
  <div class="card__body--flush table-wrap">
    <c:choose>
      <c:when test="${empty tokens}">
        <div class="empty">
          <div class="empty__icon" aria-hidden="true">&#128203;</div>
          <div class="empty__title">Nothing matches</div>
          <div class="small">No tokens today for that filter.</div>
        </div>
      </c:when>
      <c:otherwise>
        <table class="table">
          <thead>
            <tr>
              <th>Token</th><th>Customer</th><th>Service</th><th>Counter</th>
              <th>Issued</th><th>Called</th><th class="num">Wait</th><th>Status</th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="t" items="${tokens}">
              <tr>
                <td class="table__token">
                  <c:out value="${t.tokenNumber}" />
                  <c:if test="${t.priority}"> <span class="badge badge--priority">P</span></c:if>
                </td>
                <td class="small"><c:out value="${t.customerName}" /></td>
                <td class="small muted"><c:out value="${t.serviceName}" /></td>
                <td class="small"><c:out value="${t.counterName}" /></td>
                <td class="small muted nowrap"><fmt:formatDate value="${t.issuedAt}" pattern="HH:mm" /></td>
                <td class="small muted nowrap">
                  <c:choose>
                    <c:when test="${t.calledAt == null}"><span class="dim">&ndash;</span></c:when>
                    <c:otherwise><fmt:formatDate value="${t.calledAt}" pattern="HH:mm" /></c:otherwise>
                  </c:choose>
                </td>
                <td class="num small tabular">
                  <c:choose>
                    <c:when test="${t.calledAt == null}"><span class="dim">&ndash;</span></c:when>
                    <c:otherwise>${t.waitMinutes}m</c:otherwise>
                  </c:choose>
                </td>
                <td><span class="badge badge--${t.status.tone}">${t.status.label}</span></td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </c:otherwise>
    </c:choose>
  </div>
</div>

<script>
  document.addEventListener("DOMContentLoaded", function () {
    // The table is a server-rendered snapshot; reload it when the queue moves.
    var pending = null;
    function scheduleReload() {
      window.clearTimeout(pending);
      pending = window.setTimeout(function () { window.location.reload(); }, 2500);
    }
    DigiQ.live.on("TOKEN_ISSUED", scheduleReload);
    DigiQ.live.on("TOKEN_CALLED", scheduleReload);
    DigiQ.live.on("TOKEN_UPDATED", scheduleReload);
  });
</script>

<%@ include file="../layout/footer.jsp" %>
