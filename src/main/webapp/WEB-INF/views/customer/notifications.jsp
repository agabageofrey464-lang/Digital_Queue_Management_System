<%@ include file="../layout/header.jsp" %>

<div class="page-head">
  <div class="page-head__text">
    <h1 class="h1">Notifications</h1>
    <p class="muted small">Alerts raised as your tokens move through the queue.</p>
  </div>
  <div class="page-head__actions">
    <form method="post" action="${ctx}/customer/notifications">
      <button class="btn" type="submit">Mark all as read</button>
    </form>
  </div>
</div>

<div class="card">
  <div class="card__body--flush">
    <c:choose>
      <c:when test="${empty notifications}">
        <div class="empty">
          <div class="empty__icon" aria-hidden="true">&#128276;</div>
          <div class="empty__title">Nothing to show</div>
          <div class="small">You will be alerted here when your turn approaches.</div>
        </div>
      </c:when>
      <c:otherwise>
        <ul class="queue-list" id="alert-feed">
          <c:forEach var="n" items="${notifications}">
            <c:set var="unreadStyle" value="${n.read ? '' : 'background:var(--info-wash);'}" />
            <li style="${unreadStyle}">
              <span class="queue-list__pos" aria-hidden="true">
                <c:choose>
                  <c:when test="${n.type == 'CALLED'}">&#9654;</c:when>
                  <c:when test="${n.type == 'APPROACHING'}">!</c:when>
                  <c:when test="${n.type == 'COMPLETED'}">&#10003;</c:when>
                  <c:otherwise>&#8226;</c:otherwise>
                </c:choose>
              </span>
              <span style="min-width:0;flex:1;">
                <span class="queue-list__token"><c:out value="${n.title}" /></span>
                <span class="queue-list__meta" style="display:block;"><c:out value="${n.message}" /></span>
              </span>
              <span class="push small dim nowrap">
                <fmt:formatDate value="${n.createdAt}" pattern="dd MMM, HH:mm" />
              </span>
            </li>
          </c:forEach>
        </ul>
      </c:otherwise>
    </c:choose>
  </div>
</div>

<script>
  document.addEventListener("DOMContentLoaded", function () {
    // A queue change may have produced a new alert - reload the feed quietly.
    function pull() {
      DigiQ.get("${ctx}/customer/notifications").then(function (data) {
        if (!data || !data.ok) { return; }
        var badge = document.querySelector("[data-unread-badge]");
        if (badge) { badge.textContent = data.unread; }
      });
    }
    DigiQ.live.on("TOKEN_CALLED", pull);
    DigiQ.live.on("QUEUE_CHANGED", pull);
  });
</script>

<%@ include file="../layout/footer.jsp" %>
