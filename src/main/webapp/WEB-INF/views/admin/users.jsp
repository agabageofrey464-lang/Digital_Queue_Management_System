<%@ include file="../layout/header.jsp" %>

<div class="page-head">
  <div class="page-head__text">
    <h1 class="h1">Users</h1>
    <p class="muted small">
      ${roleCounts['ADMIN']} administrator<c:if test="${roleCounts['ADMIN'] != 1}">s</c:if> &middot;
      ${roleCounts['STAFF']} counter staff &middot;
      ${roleCounts['CUSTOMER']} customer<c:if test="${roleCounts['CUSTOMER'] != 1}">s</c:if>
    </p>
  </div>
  <div class="page-head__actions">
    <button class="btn btn--primary" type="button" data-modal-open="userModal"
            data-field-id="0" data-field-fullname="" data-field-email="" data-field-phone=""
            data-field-role="STAFF" data-field-active="true">
      New user
    </button>
  </div>
</div>

<div class="card">
  <div class="card__body--flush table-wrap">
    <table class="table">
      <thead>
        <tr><th>Name</th><th>Email</th><th>Phone</th><th>Role</th><th>Status</th><th>Joined</th><th></th></tr>
      </thead>
      <tbody>
        <c:forEach var="u" items="${users}">
          <tr>
            <td>
              <div class="row row--tight" style="flex-wrap:nowrap;">
                <span class="avatar" style="width:28px;height:28px;font-size:11px;">${u.initials}</span>
                <span class="strong"><c:out value="${u.fullName}" /></span>
              </div>
            </td>
            <td class="small muted"><c:out value="${u.email}" /></td>
            <td class="small muted"><c:out value="${u.phone}" /></td>
            <td>
              <span class="badge ${u.role == 'ADMIN' ? 'badge--critical' : (u.role == 'STAFF' ? 'badge--info' : 'badge--muted')}">
                ${u.role.label}
              </span>
            </td>
            <td>
              <span class="badge ${u.active ? 'badge--good' : 'badge--muted'}">${u.active ? 'Active' : 'Disabled'}</span>
            </td>
            <td class="small muted nowrap"><fmt:formatDate value="${u.createdAt}" pattern="dd MMM yyyy" /></td>
            <td class="num nowrap">
              <button class="btn btn--sm" type="button" data-modal-open="userModal"
                      data-field-id="${u.id}"
                      data-field-fullname="<c:out value='${u.fullName}' />"
                      data-field-email="<c:out value='${u.email}' />"
                      data-field-phone="<c:out value='${u.phone}' />"
                      data-field-role="${u.role}"
                      data-field-active="${u.active}">
                Edit
              </button>
              <button class="btn btn--sm btn--ghost" type="button" data-modal-open="passwordModal"
                      data-field-id="${u.id}">
                Reset password
              </button>
              <c:if test="${u.id != me.id}">
                <form method="post" action="${ctx}/admin/users" style="display:inline;"
                      data-confirm="Delete the account for ${u.fullName}?">
                  <input type="hidden" name="action" value="delete">
                  <input type="hidden" name="id" value="${u.id}">
                  <button class="btn btn--sm btn--ghost" type="submit">Delete</button>
                </form>
              </c:if>
            </td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </div>
</div>

<%-- ---------- Create / edit ---------- --%>
<div class="modal" id="userModal" hidden role="dialog" aria-modal="true" aria-labelledby="userModalTitle">
  <div class="modal__panel">
    <form method="post" action="${ctx}/admin/users">
      <div class="modal__head">
        <h2 class="h2" id="userModalTitle" data-modal-title
            data-create-label="New user" data-edit-label="Edit user">New user</h2>
      </div>

      <div class="modal__body">
        <input type="hidden" name="id" value="0">
        <div class="form-grid">
          <div class="field span-2">
            <label class="field__label" for="usrName">Full name</label>
            <input class="input" id="usrName" name="fullName" required placeholder="Grace Nakato">
          </div>

          <div class="field">
            <label class="field__label" for="usrEmail">Email address</label>
            <input class="input" id="usrEmail" name="email" type="email" required placeholder="grace@digiq.com">
          </div>

          <div class="field">
            <label class="field__label" for="usrPhone">Phone</label>
            <input class="input" id="usrPhone" name="phone" type="tel" placeholder="+256 700 000 000">
          </div>

          <div class="field">
            <label class="field__label" for="usrRole">Role</label>
            <select class="select" id="usrRole" name="role">
              <option value="CUSTOMER">Customer</option>
              <option value="STAFF">Counter staff</option>
              <option value="ADMIN">Administrator</option>
            </select>
          </div>

          <div class="field">
            <label class="field__label" for="usrPassword">Password</label>
            <input class="input" id="usrPassword" name="password" type="password"
                   minlength="8" placeholder="New accounts only">
            <span class="field__hint">Leave blank when editing.</span>
          </div>

          <label class="checkline span-2">
            <input type="checkbox" name="active" value="on" checked>
            Active &mdash; the account can sign in
          </label>
        </div>
      </div>

      <div class="modal__foot">
        <button class="btn" type="button" data-modal-close="userModal">Cancel</button>
        <button class="btn btn--primary" type="submit">Save user</button>
      </div>
    </form>
  </div>
</div>

<%-- ---------- Reset password ---------- --%>
<div class="modal" id="passwordModal" hidden role="dialog" aria-modal="true" aria-labelledby="passwordModalTitle">
  <div class="modal__panel">
    <form method="post" action="${ctx}/admin/users">
      <div class="modal__head">
        <h2 class="h2" id="passwordModalTitle">Reset password</h2>
      </div>
      <div class="modal__body">
        <input type="hidden" name="action" value="resetPassword">
        <input type="hidden" name="id" value="0">
        <div class="field">
          <label class="field__label" for="resetPw">New password</label>
          <input class="input" id="resetPw" name="password" type="password" required minlength="8"
                 placeholder="At least 8 characters">
          <span class="field__hint">The user is not notified &mdash; tell them yourself.</span>
        </div>
      </div>
      <div class="modal__foot">
        <button class="btn" type="button" data-modal-close="passwordModal">Cancel</button>
        <button class="btn btn--primary" type="submit">Reset password</button>
      </div>
    </form>
  </div>
</div>

<%@ include file="../layout/footer.jsp" %>
