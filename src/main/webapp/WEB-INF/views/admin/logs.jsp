<%@ include file="../layout/header.jsp" %>

<div class="page-head">
  <div class="page-head__text">
    <h1 class="h1">Service logs</h1>
    <p class="muted small">Every status change, with the counter and staff member responsible.</p>
  </div>
</div>

<form class="filterbar" method="get" action="${ctx}/admin/logs">
  <div class="field">
    <label class="field__label" for="lService">Service</label>
    <select class="select" id="lService" name="serviceId" data-autosubmit>
      <option value="0">All services</option>
      <c:forEach var="s" items="${services}">
        <option value="${s.id}" <c:if test="${selectedService == s.id}">selected</c:if>>
          <c:out value="${s.name}" />
        </option>
      </c:forEach>
    </select>
  </div>

  <div class="field">
    <label class="field__label" for="lAction">Action</label>
    <select class="select" id="lAction" name="action" data-autosubmit>
      <option value="">All actions</option>
      <option value="ISSUED"    <c:if test="${selectedAction == 'ISSUED'}">selected</c:if>>Issued</option>
      <option value="CALLED"    <c:if test="${selectedAction == 'CALLED'}">selected</c:if>>Called</option>
      <option value="COMPLETED" <c:if test="${selectedAction == 'COMPLETED'}">selected</c:if>>Completed</option>
      <option value="NO_SHOW"   <c:if test="${selectedAction == 'NO_SHOW'}">selected</c:if>>No show</option>
      <option value="CANCELLED" <c:if test="${selectedAction == 'CANCELLED'}">selected</c:if>>Cancelled</option>
    </select>
  </div>

  <a class="btn btn--ghost" href="${ctx}/admin/logs">Clear filters</a>
  <span class="push tiny dim">${logs.size()} entr<c:choose><c:when test="${logs.size() == 1}">y</c:when><c:otherwise>ies</c:otherwise></c:choose></span>
</form>

<div class="card">
  <div class="card__body--flush table-wrap">
    <c:choose>
      <c:when test="${empty logs}">
        <div class="empty">
          <div class="empty__icon" aria-hidden="true">&#128220;</div>
          <div class="empty__title">No log entries</div>
          <div class="small">Nothing matches that filter.</div>
        </div>
      </c:when>
      <c:otherwise>
        <table class="table">
          <thead>
            <tr><th>When</th><th>Token</th><th>Service</th><th>Action</th>
                <th>Transition</th><th>Counter</th><th>Staff</th><th>Note</th></tr>
          </thead>
          <tbody>
            <c:forEach var="log" items="${logs}">
              <tr>
                <td class="small muted nowrap"><fmt:formatDate value="${log.createdAt}" pattern="dd MMM, HH:mm:ss" /></td>
                <td class="table__token"><c:out value="${log.tokenNumber}" /></td>
                <td class="small muted"><c:out value="${log.serviceName}" /></td>
                <td>
                  <span class="badge ${log.toStatus == 'COMPLETED' ? 'badge--good' : (log.toStatus == 'IN_SERVICE' ? 'badge--info' : (log.toStatus == 'NO_SHOW' or log.toStatus == 'CANCELLED' ? 'badge--critical' : 'badge--muted'))}">
                    <c:out value="${log.action}" />
                  </span>
                </td>
                <td class="small muted nowrap">
                  <c:choose>
                    <c:when test="${empty log.fromStatus}"><span class="dim">new</span></c:when>
                    <c:otherwise><c:out value="${log.fromStatus}" /></c:otherwise>
                  </c:choose>
                  &rarr; <c:out value="${log.toStatus}" />
                </td>
                <td class="small"><c:out value="${log.counterName}" /></td>
                <td class="small"><c:out value="${log.staffName}" /></td>
                <td class="small muted"><c:out value="${log.note}" /></td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </c:otherwise>
    </c:choose>
  </div>
</div>

<%@ include file="../layout/footer.jsp" %>
