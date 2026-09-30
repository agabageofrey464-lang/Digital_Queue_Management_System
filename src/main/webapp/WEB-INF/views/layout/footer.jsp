<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
    </main>
  </div>
</div>

<div id="toasts" aria-live="polite" aria-atomic="false"></div>

<script>
  // digiq.js is deferred, so it has already run when this fires.
  document.addEventListener("DOMContentLoaded", function () {
    DigiQ.live.start();
  });
</script>
</body>
</html>
