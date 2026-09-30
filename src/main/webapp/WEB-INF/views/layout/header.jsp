<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="me" value="${sessionScope.authUser}" />
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${pageTitle} &middot; DigiQ</title>
  <meta name="description" content="DigiQ - Digital Queue Management System">
  <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 32 32'><rect width='32' height='32' rx='8' fill='%23184f95'/><text x='16' y='22' font-size='17' font-family='system-ui' font-weight='700' fill='white' text-anchor='middle'>Q</text></svg>">
  <link rel="stylesheet" href="${ctx}/assets/css/digiq.css">
  <script>
    /* Applied before first paint so a dark-mode user never sees a light flash. */
    (function () {
      try {
        var t = localStorage.getItem("digiq.theme");
        if (t) { document.documentElement.setAttribute("data-theme", t); }
      } catch (e) {}
    }());
  </script>
  <%-- Deferred, so it has run by the time each page's DOMContentLoaded handler fires. --%>
  <script src="${ctx}/assets/js/digiq.js" defer></script>
</head>
<body data-context="${ctx}" data-nav="closed">

<div class="app">
  <aside class="sidebar">
    <div class="sidebar__brand">
      <div class="sidebar__mark">Q</div>
      <div>
        <div class="sidebar__name">DigiQ</div>
        <div class="sidebar__role">${me.role.label}</div>
      </div>
    </div>

    <nav class="sidebar__nav" aria-label="Main">
      <c:choose>
        <%-- ---------------- Administrator ---------------- --%>
        <c:when test="${me.role == 'ADMIN'}">
          <div class="sidebar__section">Overview</div>
          <a class="navlink ${navActive == 'dashboard' ? 'navlink--active' : ''}" href="${ctx}/admin/dashboard">
            <svg class="navlink__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><rect x="3" y="3" width="7" height="9" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/><rect x="3" y="16" width="7" height="5" rx="1"/></svg>
            Dashboard
          </a>
          <a class="navlink ${navActive == 'analytics' ? 'navlink--active' : ''}" href="${ctx}/admin/analytics">
            <svg class="navlink__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M3 20h18M7 16V9M12 16V5M17 16v-4"/></svg>
            Analytics
          </a>
          <a class="navlink ${navActive == 'tokens' ? 'navlink--active' : ''}" href="${ctx}/admin/tokens">
            <svg class="navlink__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M4 7h16v4a2 2 0 0 0 0 4v3H4v-3a2 2 0 0 0 0-4z"/><path d="M12 7v11"/></svg>
            Live queue
          </a>

          <div class="sidebar__section">Configuration</div>
          <a class="navlink ${navActive == 'services' ? 'navlink--active' : ''}" href="${ctx}/admin/services">
            <svg class="navlink__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><rect x="3" y="4" width="18" height="6" rx="2"/><rect x="3" y="14" width="18" height="6" rx="2"/></svg>
            Services
          </a>
          <a class="navlink ${navActive == 'counters' ? 'navlink--active' : ''}" href="${ctx}/admin/counters">
            <svg class="navlink__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M3 10h18M5 10V6a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v4M4 10v10h16V10"/></svg>
            Counters
          </a>
          <a class="navlink ${navActive == 'users' ? 'navlink--active' : ''}" href="${ctx}/admin/users">
            <svg class="navlink__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><circle cx="9" cy="8" r="3"/><path d="M3 20a6 6 0 0 1 12 0"/><path d="M16 5.5a3 3 0 0 1 0 5M18 20a6 6 0 0 0-2-4.5"/></svg>
            Users
          </a>
          <a class="navlink ${navActive == 'logs' ? 'navlink--active' : ''}" href="${ctx}/admin/logs">
            <svg class="navlink__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M5 4h14v16H5z"/><path d="M9 8h6M9 12h6M9 16h3"/></svg>
            Service logs
          </a>
        </c:when>

        <%-- ---------------- Counter staff ---------------- --%>
        <c:when test="${me.role == 'STAFF'}">
          <div class="sidebar__section">Counter</div>
          <a class="navlink ${navActive == 'console' ? 'navlink--active' : ''}" href="${ctx}/staff/console">
            <svg class="navlink__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><rect x="3" y="4" width="18" height="13" rx="2"/><path d="M8 21h8M12 17v4"/></svg>
            Console
          </a>
          <a class="navlink" href="${ctx}/board" target="_blank" rel="noopener">
            <svg class="navlink__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><rect x="2" y="5" width="20" height="12" rx="2"/><path d="M8 21h8"/></svg>
            Display board
          </a>
        </c:when>

        <%-- ---------------- Customer ---------------- --%>
        <c:otherwise>
          <div class="sidebar__section">My queue</div>
          <a class="navlink ${navActive == 'home' ? 'navlink--active' : ''}" href="${ctx}/customer/home">
            <svg class="navlink__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M4 11l8-7 8 7"/><path d="M6 10v10h12V10"/></svg>
            Book a token
          </a>
          <a class="navlink ${navActive == 'tokens' ? 'navlink--active' : ''}" href="${ctx}/customer/tokens">
            <svg class="navlink__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M4 7h16v4a2 2 0 0 0 0 4v3H4v-3a2 2 0 0 0 0-4z"/><path d="M12 7v11"/></svg>
            My tokens
          </a>
          <a class="navlink ${navActive == 'alerts' ? 'navlink--active' : ''}" href="${ctx}/customer/notifications">
            <svg class="navlink__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M18 8a6 6 0 1 0-12 0c0 7-3 8-3 8h18s-3-1-3-8"/><path d="M13.7 21a2 2 0 0 1-3.4 0"/></svg>
            Notifications
            <c:if test="${unreadCount > 0}">
              <span class="navlink__badge" data-unread-badge>${unreadCount}</span>
            </c:if>
          </a>
          <a class="navlink" href="${ctx}/board" target="_blank" rel="noopener">
            <svg class="navlink__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><rect x="2" y="5" width="20" height="12" rx="2"/><path d="M8 21h8"/></svg>
            Now serving
          </a>
        </c:otherwise>
      </c:choose>
    </nav>

    <div class="sidebar__foot">
      <div>Signed in as</div>
      <div style="color:#fff;font-weight:600;"><c:out value="${me.fullName}" /></div>
    </div>
  </aside>

  <div class="main">
    <header class="topbar">
      <button class="btn btn--ghost btn--icon sidebar-toggle" data-nav-toggle type="button" aria-label="Open navigation">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M4 6h16M4 12h16M4 18h16"/></svg>
      </button>
      <div class="topbar__title">${pageTitle}</div>
      <div class="topbar__spacer"></div>
      <div class="topbar__actions">
        <span class="livedot" data-live-indicator data-state="connecting">
          <span data-live-label>Connecting</span>
        </span>
        <button class="btn btn--ghost btn--icon" data-theme-toggle type="button" aria-label="Switch theme">
          <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/></svg>
        </button>
        <span class="avatar" title="${me.fullName}">${me.initials}</span>
        <a class="btn btn--sm" href="${ctx}/logout">Sign out</a>
      </div>
    </header>

    <main class="content">
      <%@ include file="flash.jsp" %>
