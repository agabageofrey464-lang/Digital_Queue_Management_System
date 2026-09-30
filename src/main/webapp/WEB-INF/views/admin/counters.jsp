<%@ include file="../layout/header.jsp" %>

<div class="page-head">
  <div class="page-head__text">
    <h1 class="h1">Counters</h1>
    <p class="muted small">Each counter serves one service. Link a staff member to give them the console.</p>
  </div>
  <div class="page-head__actions">
    <button class="btn btn--primary" type="button" data-modal-open="counterModal"
            data-field-id="0" data-field-name="" data-field-staffId="0" data-field-status="CLOSED">
      New counter
    </button>
  </div>
</div>

<div class="card">
  <div class="card__body--flush table-wrap">
    <c:choose>
      <c:when test="${empty counters}">
        <div class="empty">
          <div class="empty__icon" aria-hidden="true">&#127978;</div>
          <div class="empty__title">No counters configured</div>
          <div class="small">Add a counter and assign it to a service.</div>
        </div>
      </c:when>
      <c:otherwise>
        <table class="table">
          <thead>
            <tr><th>Counter</th><th>Service</th><th>Staff</th><th>Now serving</th>
                <th class="num">Completed today</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            <c:forEach var="c" items="${counters}">
              <tr>
                <td class="strong"><c:out value="${c.name}" /></td>
                <td class="small"><c:out value="${c.serviceName}" /></td>
                <td class="small muted">
                  <c:choose>
                    <c:when test="${empty c.staffName}"><span class="dim">Unassigned</span></c:when>
                    <c:otherwise><c:out value="${c.staffName}" /></c:otherwise>
                  </c:choose>
                </td>
                <td class="table__token">
                  <c:choose>
                    <c:when test="${empty c.currentTokenNumber}"><span class="dim">&ndash;</span></c:when>
                    <c:otherwise><c:out value="${c.currentTokenNumber}" /></c:otherwise>
                  </c:choose>
                </td>
                <td class="num tabular">${c.servedToday}</td>
                <td><span class="badge badge--${c.status.tone}">${c.status.label}</span></td>
                <td class="num nowrap">
                  <button class="btn btn--sm" type="button" data-modal-open="counterModal"
                          data-field-id="${c.id}"
                          data-field-name="<c:out value='${c.name}' />"
                          data-field-serviceId="${c.serviceId}"
                          data-field-staffId="${empty c.staffId ? 0 : c.staffId}"
                          data-field-status="${c.status}">
                    Edit
                  </button>
                  <form method="post" action="${ctx}/admin/counters" style="display:inline;"
                        data-confirm="Delete '${c.name}'?">
                    <input type="hidden" name="action" value="delete">
                    <input type="hidden" name="id" value="${c.id}">
                    <button class="btn btn--sm btn--ghost" type="submit">Delete</button>
                  </form>
                </td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </c:otherwise>
    </c:choose>
  </div>
</div>

<div class="modal" id="counterModal" hidden role="dialog" aria-modal="true" aria-labelledby="counterModalTitle">
  <div class="modal__panel">
    <form method="post" action="${ctx}/admin/counters">
      <div class="modal__head">
        <h2 class="h2" id="counterModalTitle" data-modal-title
            data-create-label="New counter" data-edit-label="Edit counter">New counter</h2>
      </div>

      <div class="modal__body">
        <input type="hidden" name="id" value="0">
        <div class="form-grid">
          <div class="field">
            <label class="field__label" for="ctrName">Counter name</label>
            <input class="input" id="ctrName" name="name" required placeholder="Counter 1">
          </div>

          <div class="field">
            <label class="field__label" for="ctrService">Service</label>
            <select class="select" id="ctrService" name="serviceId" required>
              <c:forEach var="s" items="${services}">
                <option value="${s.id}"><c:out value="${s.name}" /> (<c:out value="${s.code}" />)</option>
              </c:forEach>
            </select>
          </div>

          <div class="field">
            <label class="field__label" for="ctrStaff">Assigned staff</label>
            <select class="select" id="ctrStaff" name="staffId">
              <option value="0">Unassigned</option>
              <c:forEach var="u" items="${staff}">
                <option value="${u.id}"><c:out value="${u.fullName}" /></option>
              </c:forEach>
            </select>
            <span class="field__hint">Only this person sees the counter console.</span>
          </div>

          <div class="field">
            <label class="field__label" for="ctrStatus">Status</label>
            <select class="select" id="ctrStatus" name="status">
              <option value="OPEN">Open</option>
              <option value="PAUSED">Paused</option>
              <option value="CLOSED">Closed</option>
            </select>
          </div>
        </div>
      </div>

      <div class="modal__foot">
        <button class="btn" type="button" data-modal-close="counterModal">Cancel</button>
        <button class="btn btn--primary" type="submit">Save counter</button>
      </div>
    </form>
  </div>
</div>

<%@ include file="../layout/footer.jsp" %>
