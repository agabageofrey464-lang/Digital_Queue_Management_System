<%@ include file="layout/public-header.jsp" %>

<section class="hero">
  <div>
    <span class="eyebrow">Digital Queue Management System</span>
    <h1 class="hero__title">Stop standing in line.<br>Take your place from anywhere.</h1>
    <p class="hero__lead">
      DigiQ replaces paper tickets and verbal announcements with a live digital queue.
      Book a token, watch your position update in real time, and walk up only when
      your number is called.
    </p>

    <div class="hero__actions">
      <a class="btn btn--primary btn--lg" href="${ctx}/register">Book a token</a>
      <a class="btn btn--lg" href="${ctx}/board">View the display board</a>
    </div>

    <ul class="hero__points">
      <li><span class="hero__check" aria-hidden="true">&#10003;</span>
        <span><b>Live position tracking</b> &mdash; your place updates the moment a counter calls the next customer.</span></li>
      <li><span class="hero__check" aria-hidden="true">&#10003;</span>
        <span><b>QR ticket, on screen or printed</b> &mdash; staff scan it to move you through the queue.</span></li>
      <li><span class="hero__check" aria-hidden="true">&#10003;</span>
        <span><b>Turn-approaching alerts</b> &mdash; you are told before you are called, not after.</span></li>
    </ul>
  </div>

  <div class="stack">
    <div class="card">
      <div class="card__head">
        <div>
          <div class="h3">Queues right now</div>
          <div class="tiny dim">
            <c:choose>
              <c:when test="${openCounters > 0}">${openCounters} counter<c:if test="${openCounters != 1}">s</c:if> serving</c:when>
              <c:otherwise>No counters are open</c:otherwise>
            </c:choose>
          </div>
        </div>
      </div>
      <div class="card__body--flush">
        <c:choose>
          <c:when test="${empty services}">
            <div class="empty">
              <div class="empty__icon" aria-hidden="true">&#128203;</div>
              <div class="empty__title">No services published yet</div>
              <div class="small">An administrator has not set up any services.</div>
            </div>
          </c:when>
          <c:otherwise>
            <ul class="queue-list">
              <c:forEach var="s" items="${services}">
                <li>
                  <span class="service-card__code" style="width:38px;height:38px;font-size:11px;">
                    <c:out value="${s.code}" />
                  </span>
                  <span style="min-width:0;">
                    <span class="queue-list__token"><c:out value="${s.name}" /></span>
                    <span class="queue-list__meta" style="display:block;">
                      ~${s.estimatedWaitMinutes} min wait
                    </span>
                  </span>
                  <span class="push">
                    <span class="badge ${s.waitingCount == 0 ? 'badge--good' : (s.waitingCount > 8 ? 'badge--critical' : 'badge--warning')}">
                      ${s.waitingCount} waiting
                    </span>
                  </span>
                </li>
              </c:forEach>
            </ul>
          </c:otherwise>
        </c:choose>
      </div>
      <div class="card__foot tiny dim">
        Figures refresh as counters serve customers.
      </div>
    </div>
  </div>
</section>

<%@ include file="layout/public-footer.jsp" %>
