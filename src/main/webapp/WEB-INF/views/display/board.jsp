<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Now serving &middot; DigiQ</title>
  <meta name="description" content="Live queue display board.">
  <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 32 32'><rect width='32' height='32' rx='8' fill='%23184f95'/><text x='16' y='22' font-size='17' font-family='system-ui' font-weight='700' fill='white' text-anchor='middle'>Q</text></svg>">
  <link rel="stylesheet" href="${ctx}/assets/css/digiq.css">
  <script src="${ctx}/assets/js/digiq.js" defer></script>
</head>
<%-- No names on this page: it hangs on a wall where anyone can read it. --%>
<body data-context="${ctx}">

<div class="board">
  <header class="board__head">
    <div class="board__brand">
      <span class="brandmark" style="width:42px;height:42px;font-size:18px;">Q</span>
      <div>
        <div class="board__title">Now Serving</div>
        <div class="board__sub">
          DigiQ &middot; <span class="livedot" data-live-indicator data-state="connecting"
                               style="background:rgba(255,255,255,.1);border-color:rgba(255,255,255,.2);color:rgba(255,255,255,.75);">
            <span data-live-label>Connecting</span>
          </span>
        </div>
      </div>
    </div>
    <div class="board__clock">
      <div class="board__time" id="clock">--:--</div>
      <div class="board__date" id="today"></div>
    </div>
  </header>

  <div class="board__grid" id="boardGrid">
    <c:choose>
      <c:when test="${empty counters}">
        <div class="board-counter board-counter--idle">
          <div class="board-counter__name">No counters open</div>
          <div class="board-counter__token">&mdash;</div>
          <div class="board-counter__service">Please wait for a counter to open</div>
        </div>
      </c:when>
      <c:otherwise>
        <c:forEach var="c" items="${counters}">
          <div class="board-counter ${empty c.currentTokenNumber ? 'board-counter--idle' : ''}"
               data-counter="${c.id}" data-status="${c.status}">
            <div class="board-counter__name"><c:out value="${c.name}" /></div>
            <div class="board-counter__service"><c:out value="${c.serviceName}" /></div>
            <div class="board-counter__token" data-token>
              <c:choose>
                <c:when test="${empty c.currentTokenNumber}">&mdash;</c:when>
                <c:otherwise><c:out value="${c.currentTokenNumber}" /></c:otherwise>
              </c:choose>
            </div>
            <div class="board-counter__state">${c.status.label}</div>
          </div>
        </c:forEach>
      </c:otherwise>
    </c:choose>
  </div>

  <section class="board__recent">
    <h2>Recently called</h2>
    <ul id="recentList">
      <c:forEach var="t" items="${recent}">
        <li><c:out value="${t.tokenNumber}" /> <span><c:out value="${t.counterName}" /></span></li>
      </c:forEach>
    </ul>
  </section>
</div>

<script>
document.addEventListener("DOMContentLoaded", function () {
  var base = "${ctx}";

  /* ---------------- Clock ---------------- */
  function tick() {
    var now = new Date();
    document.getElementById("clock").textContent =
      String(now.getHours()).padStart(2, "0") + ":" + String(now.getMinutes()).padStart(2, "0");
    document.getElementById("today").textContent =
      now.toLocaleDateString(undefined, { weekday: "long", day: "numeric", month: "long" });
  }
  tick();
  window.setInterval(tick, 10000);

  /* ---------------- Announce a call ---------------- */
  function announce(counterId, tokenNumber) {
    var card = document.querySelector('[data-counter="' + counterId + '"]');
    if (!card) { return; }
    var slot = card.querySelector("[data-token]");
    if (slot) { slot.textContent = tokenNumber; }
    card.classList.remove("board-counter--idle", "board-counter--called");
    // Reflow so the animation restarts even on a repeat call to the same counter.
    void card.offsetWidth;
    card.classList.add("board-counter--called");
    window.setTimeout(function () { card.classList.remove("board-counter--called"); }, 4000);

    var list = document.getElementById("recentList");
    if (list) {
      var li = document.createElement("li");
      li.textContent = tokenNumber + " ";
      var span = document.createElement("span");
      span.textContent = card.querySelector(".board-counter__name").textContent;
      li.appendChild(span);
      list.insertBefore(li, list.firstChild);
      while (list.children.length > 6) { list.removeChild(list.lastChild); }
    }
  }

  /* ---------------- Full redraw ---------------- */
  function redraw() {
    DigiQ.get(base + "/board/data").then(function (data) {
      if (!data || !data.ok) { return; }
      data.counters.forEach(function (c) {
        var card = document.querySelector('[data-counter="' + c.id + '"]');
        if (!card) { return; }
        card.setAttribute("data-status", c.status);
        var slot = card.querySelector("[data-token]");
        if (slot) { slot.textContent = c.token || "—"; }
        card.classList.toggle("board-counter--idle", !c.token);
        var state = card.querySelector(".board-counter__state");
        if (state) { state.textContent = c.status.charAt(0) + c.status.slice(1).toLowerCase(); }
      });
    });
  }

  DigiQ.live.on("TOKEN_CALLED", function (payload) {
    announce(payload.counterId, payload.token.tokenNumber);
  });
  DigiQ.live.on("TOKEN_UPDATED", redraw);
  DigiQ.live.on("COUNTER_UPDATED", function () {
    // A counter opening or closing changes the tile set - re-render server-side.
    window.setTimeout(function () { window.location.reload(); }, 800);
  });
  // A dropped connection may have missed calls; resync whenever it comes back.
  DigiQ.live.on("__open", redraw);

  DigiQ.live.start();
});
</script>
</body>
</html>
