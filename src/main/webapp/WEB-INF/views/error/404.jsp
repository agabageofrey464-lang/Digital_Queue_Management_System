<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Page not found &middot; DigiQ</title>
  <link rel="stylesheet" href="${ctx}/assets/css/digiq.css">
</head>
<body>
<div class="auth-wrap" style="min-height:100vh;">
  <div class="auth-card" style="text-align:center;">
    <div class="auth-card__body" style="padding:38px 28px;">
      <span class="brandmark" style="margin:0 auto 16px;width:44px;height:44px;font-size:18px;">Q</span>
      <div style="font-size:52px;font-weight:700;letter-spacing:-0.04em;color:var(--brand-700);line-height:1;">404</div>
      <h1 class="h2" style="margin-top:10px;">Page not found</h1>
      <p class="small muted" style="margin-top:6px;">The page you asked for does not exist or has moved.</p>
      <a class="btn btn--primary btn--block" style="margin-top:20px;" href="${ctx}/">Back to DigiQ</a>
    </div>
  </div>
</div>
</body>
</html>
