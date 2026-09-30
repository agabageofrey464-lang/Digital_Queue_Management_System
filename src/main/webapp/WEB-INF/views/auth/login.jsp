<%@ include file="../layout/public-header.jsp" %>

<div class="auth-wrap">
  <div class="auth-card">
    <div class="auth-card__head">
      <span class="brandmark" style="margin:0 auto 12px;">Q</span>
      <h1 class="h1" style="font-size:21px;">Sign in to DigiQ</h1>
      <p class="small muted" style="margin-top:4px;">Customers, counter staff and administrators use the same form.</p>
    </div>

    <form class="auth-card__body" method="post" action="${ctx}/login">
      <c:if test="${not empty error}">
        <div class="flash flash--error" role="alert" style="margin-bottom:0;">
          <span class="flash__icon" aria-hidden="true">!</span>
          <span><c:out value="${error}" /></span>
        </div>
      </c:if>
      <c:if test="${not empty sessionScope.flashError}">
        <div class="flash flash--error" role="alert" style="margin-bottom:0;">
          <span class="flash__icon" aria-hidden="true">!</span>
          <span><c:out value="${sessionScope.flashError}" /></span>
        </div>
        <c:remove var="flashError" scope="session" />
      </c:if>
      <c:if test="${not empty sessionScope.flashSuccess}">
        <div class="flash flash--success" role="status" style="margin-bottom:0;">
          <span class="flash__icon" aria-hidden="true">&#10003;</span>
          <span><c:out value="${sessionScope.flashSuccess}" /></span>
        </div>
        <c:remove var="flashSuccess" scope="session" />
      </c:if>

      <%-- Deliberately type="text", not type="email": an account's sign-in name may
           be a plain username, and the browser's built-in email validation would
           refuse to submit the form before the server ever saw it. --%>
      <div class="field">
        <label class="field__label" for="email">Email or username</label>
        <input class="input" type="text" id="email" name="email" required autocomplete="username"
               value="<c:out value='${email}' />" placeholder="you@example.com or your username">
      </div>

      <div class="field">
        <label class="field__label" for="password">Password</label>
        <input class="input" type="password" id="password" name="password" required
               autocomplete="current-password" placeholder="Your password">
      </div>

      <button class="btn btn--primary btn--block btn--lg" type="submit">Sign in</button>

      <%-- Off unless digiq.showDemoAccounts is switched on in web.xml. This page is
           public, so listing the administrator password here would undo every role
           check in the application. --%>
      <c:if test="${initParam['digiq.showDemoAccounts'] == 'true'}">
        <div class="demo-creds">
          <div class="eyebrow" style="margin-bottom:6px;">Demo accounts &mdash; password <code>Digiq@123</code></div>
          <table>
            <tr><td>Administrator</td><td><code>admin@digiq.com</code></td></tr>
            <tr><td>Counter staff</td><td><code>grace@digiq.com</code></td></tr>
            <tr><td>Customer</td><td><code>joseph@mail.com</code></td></tr>
          </table>
        </div>
      </c:if>
    </form>

    <div class="auth-card__foot">
      New here? <a href="${ctx}/register">Create a customer account</a>
    </div>
  </div>
</div>

<%@ include file="../layout/public-footer.jsp" %>
