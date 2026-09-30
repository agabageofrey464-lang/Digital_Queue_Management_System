<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${pageTitle} &middot; DigiQ</title>
  <meta name="description" content="DigiQ - book a service token, track your place in the queue and skip the physical line.">
  <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 32 32'><rect width='32' height='32' rx='8' fill='%23184f95'/><text x='16' y='22' font-size='17' font-family='system-ui' font-weight='700' fill='white' text-anchor='middle'>Q</text></svg>">
  <link rel="stylesheet" href="${ctx}/assets/css/digiq.css">
  <script>
    (function () {
      try {
        var t = localStorage.getItem("digiq.theme");
        if (t) { document.documentElement.setAttribute("data-theme", t); }
      } catch (e) {}
    }());
  </script>
  <script src="${ctx}/assets/js/digiq.js" defer></script>
</head>
<body data-context="${ctx}">

<div class="shell-public">
  <nav class="pubnav">
    <a class="pubnav__brand" href="${ctx}/">
      <span class="brandmark">Q</span>
      <span>DigiQ</span>
    </a>
    <div class="pubnav__links">
      <a class="btn btn--ghost btn--sm" href="${ctx}/board">Now serving</a>
      <button class="btn btn--ghost btn--icon" data-theme-toggle type="button" aria-label="Switch theme">
        <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/></svg>
      </button>
      <a class="btn btn--sm" href="${ctx}/login">Sign in</a>
      <a class="btn btn--primary btn--sm" href="${ctx}/register">Create account</a>
    </div>
  </nav>
