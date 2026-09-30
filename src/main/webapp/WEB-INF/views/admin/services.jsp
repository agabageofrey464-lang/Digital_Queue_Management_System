<%@ include file="../layout/header.jsp" %>

<div class="page-head">
  <div class="page-head__text">
    <h1 class="h1">Services</h1>
    <p class="muted small">What customers can queue for. The token code becomes the ticket prefix.</p>
  </div>
  <div class="page-head__actions">
    <button class="btn btn--primary" type="button" data-modal-open="serviceModal"
            data-field-id="0" data-field-name="" data-field-code="" data-field-description=""
            data-field-avgServiceMinutes="10" data-field-active="true">
      New service
    </button>
  </div>
</div>

<div class="card">
  <div class="card__body--flush table-wrap">
    <c:choose>
      <c:when test="${empty services}">
        <div class="empty">
          <div class="empty__icon" aria-hidden="true">&#128203;</div>
          <div class="empty__title">No services yet</div>
          <div class="small">Create one so customers have something to book.</div>
        </div>
      </c:when>
      <c:otherwise>
        <table class="table">
          <thead>
            <tr><th>Code</th><th>Service</th><th>Description</th>
                <th class="num">Avg service</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            <c:forEach var="s" items="${services}">
              <tr>
                <td><span class="badge badge--info"><c:out value="${s.code}" /></span></td>
                <td class="strong"><c:out value="${s.name}" /></td>
                <td class="small muted"><c:out value="${s.description}" /></td>
                <td class="num tabular">${s.avgServiceMinutes} min</td>
                <td>
                  <span class="badge ${s.active ? 'badge--good' : 'badge--muted'}">
                    ${s.active ? 'Active' : 'Retired'}
                  </span>
                </td>
                <td class="num nowrap">
                  <button class="btn btn--sm" type="button" data-modal-open="serviceModal"
                          data-field-id="${s.id}"
                          data-field-name="<c:out value='${s.name}' />"
                          data-field-code="<c:out value='${s.code}' />"
                          data-field-description="<c:out value='${s.description}' />"
                          data-field-avgServiceMinutes="${s.avgServiceMinutes}"
                          data-field-active="${s.active}">
                    Edit
                  </button>
                  <form method="post" action="${ctx}/admin/services" style="display:inline;"
                        data-confirm="Delete '${s.name}'? Its counters and token history go with it.">
                    <input type="hidden" name="action" value="delete">
                    <input type="hidden" name="id" value="${s.id}">
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

<%-- ---------- Create / edit ---------- --%>
<div class="modal" id="serviceModal" hidden role="dialog" aria-modal="true" aria-labelledby="serviceModalTitle">
  <div class="modal__panel">
    <form method="post" action="${ctx}/admin/services">
      <div class="modal__head">
        <h2 class="h2" id="serviceModalTitle" data-modal-title
            data-create-label="New service" data-edit-label="Edit service">New service</h2>
      </div>

      <div class="modal__body">
        <input type="hidden" name="id" value="0">
        <div class="form-grid">
          <div class="field span-2">
            <label class="field__label" for="svcName">Service name</label>
            <input class="input" id="svcName" name="name" required placeholder="Account Opening">
          </div>

          <div class="field">
            <label class="field__label" for="svcCode">Token code</label>
            <input class="input" id="svcCode" name="code" required maxlength="8"
                   style="text-transform:uppercase;" placeholder="ACC">
            <span class="field__hint">Ticket numbers read ACC-0001.</span>
          </div>

          <div class="field">
            <label class="field__label" for="svcMinutes">Average service time</label>
            <input class="input" id="svcMinutes" name="avgServiceMinutes" type="number" min="1" max="240" value="10">
            <span class="field__hint">Minutes. Drives the wait estimate.</span>
          </div>

          <div class="field span-2">
            <label class="field__label" for="svcDesc">Description</label>
            <textarea class="textarea" id="svcDesc" name="description"
                      placeholder="Shown on the customer booking card."></textarea>
          </div>

          <label class="checkline span-2">
            <input type="checkbox" name="active" value="on" checked>
            Active &mdash; customers can book this service
          </label>
        </div>
      </div>

      <div class="modal__foot">
        <button class="btn" type="button" data-modal-close="serviceModal">Cancel</button>
        <button class="btn btn--primary" type="submit">Save service</button>
      </div>
    </form>
  </div>
</div>

<%@ include file="../layout/footer.jsp" %>
