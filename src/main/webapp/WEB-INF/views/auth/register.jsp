<%@ include file="../layout/public-header.jsp" %>

<div class="auth-wrap">
  <div class="auth-card">
    <div class="auth-card__head">
      <span class="brandmark" style="margin:0 auto 12px;">Q</span>
      <h1 class="h1" style="font-size:21px;">Create your account</h1>
      <p class="small muted" style="margin-top:4px;">One account books tokens for every service at this branch.</p>
    </div>

    <form class="auth-card__body" method="post" action="${ctx}/register">
      <c:if test="${not empty error}">
        <div class="flash flash--error" role="alert" style="margin-bottom:0;">
          <span class="flash__icon" aria-hidden="true">!</span>
          <span><c:out value="${error}" /></span>
        </div>
      </c:if>

      <div class="field">
        <label class="field__label" for="fullName">Full name</label>
        <input class="input" type="text" id="fullName" name="fullName" required autocomplete="name"
               value="<c:out value='${fullName}' />" placeholder="Joseph Mugisha">
      </div>

      <div class="field">
        <label class="field__label" for="email">Email address</label>
        <input class="input" type="email" id="email" name="email" required autocomplete="username"
               value="<c:out value='${email}' />" placeholder="you@example.com">
      </div>

      <div class="field">
        <label class="field__label" for="phone">Phone number <span class="dim">(optional)</span></label>
        <input class="input" type="tel" id="phone" name="phone" autocomplete="tel"
               value="<c:out value='${phone}' />" placeholder="+256 700 000 000">
      </div>

      <div class="field">
        <label class="field__label" for="password">Password</label>
        <input class="input" type="password" id="password" name="password" required minlength="8"
               autocomplete="new-password" placeholder="At least 8 characters">
        <span class="field__hint">Use at least 8 characters.</span>
      </div>

      <div class="field">
        <label class="field__label" for="confirmPassword">Confirm password</label>
        <input class="input" type="password" id="confirmPassword" name="confirmPassword" required
               minlength="8" autocomplete="new-password" placeholder="Repeat your password">
      </div>

      <button class="btn btn--primary btn--block btn--lg" type="submit">Create account</button>
    </form>

    <div class="auth-card__foot">
      Already registered? <a href="${ctx}/login">Sign in instead</a>
    </div>
  </div>
</div>

<%@ include file="../layout/public-footer.jsp" %>
