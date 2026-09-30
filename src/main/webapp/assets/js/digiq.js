/* ===================================================================
   DigiQ - shared client runtime
   Theme, navigation, toasts, modals, fetch helper and the live socket.
   =================================================================== */
(function (window, document) {
  "use strict";

  var DigiQ = window.DigiQ || {};
  window.DigiQ = DigiQ;

  DigiQ.context = document.body ? (document.body.getAttribute("data-context") || "") : "";

  /* ---------------- Storage (never assume it works) ---------------- */
  function readStore(key) {
    try {
      return window.localStorage.getItem(key);
    } catch (e) {
      return null;
    }
  }

  function writeStore(key, value) {
    try {
      window.localStorage.setItem(key, value);
    } catch (e) {
      /* Private window or blocked site data - the page must still work. */
    }
  }

  /* ---------------- Theme ---------------- */
  DigiQ.theme = {
    apply: function (mode) {
      if (mode === "dark" || mode === "light") {
        document.documentElement.setAttribute("data-theme", mode);
      } else {
        document.documentElement.removeAttribute("data-theme");
      }
    },
    current: function () {
      var explicit = document.documentElement.getAttribute("data-theme");
      if (explicit) {
        return explicit;
      }
      return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
    },
    toggle: function () {
      var next = DigiQ.theme.current() === "dark" ? "light" : "dark";
      DigiQ.theme.apply(next);
      writeStore("digiq.theme", next);
      document.dispatchEvent(new CustomEvent("digiq:themechange", { detail: { theme: next } }));
      return next;
    },
    restore: function () {
      var saved = readStore("digiq.theme");
      if (saved) {
        DigiQ.theme.apply(saved);
      }
    }
  };
  DigiQ.theme.restore();

  /* ---------------- Toasts ---------------- */
  DigiQ.toast = function (title, body, tone) {
    var host = document.getElementById("toasts");
    if (!host) {
      host = document.createElement("div");
      host.id = "toasts";
      document.body.appendChild(host);
    }
    var el = document.createElement("div");
    el.className = "toast" + (tone ? " toast--" + tone : "");
    el.setAttribute("role", "status");

    var t = document.createElement("div");
    t.className = "toast__title";
    t.textContent = title;
    el.appendChild(t);

    if (body) {
      var b = document.createElement("div");
      b.className = "toast__body";
      b.textContent = body;
      el.appendChild(b);
    }

    host.appendChild(el);
    window.setTimeout(function () {
      el.style.opacity = "0";
      el.style.transition = "opacity 220ms ease";
      window.setTimeout(function () {
        if (el.parentNode) {
          el.parentNode.removeChild(el);
        }
      }, 240);
    }, 5200);
  };

  /* ---------------- Fetch helper ---------------- */
  DigiQ.post = function (url, data) {
    var body = new URLSearchParams();
    Object.keys(data || {}).forEach(function (k) {
      if (data[k] !== null && data[k] !== undefined) {
        body.append(k, data[k]);
      }
    });
    return fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
        "X-Requested-With": "fetch",
        "Accept": "application/json"
      },
      body: body.toString(),
      credentials: "same-origin"
    }).then(function (res) {
      return res.json().catch(function () {
        return { ok: false, error: "The server returned an unexpected response." };
      });
    });
  };

  DigiQ.get = function (url) {
    return fetch(url, {
      headers: { "X-Requested-With": "fetch", "Accept": "application/json" },
      credentials: "same-origin"
    }).then(function (res) {
      return res.json().catch(function () {
        return { ok: false, error: "The server returned an unexpected response." };
      });
    });
  };

  /* ---------------- Live socket ---------------- */
  /*
   * One socket per page. Reconnects with a capped backoff so a display board left
   * running overnight recovers on its own, and reports its state through the
   * "live" pill in the top bar.
   */
  DigiQ.live = (function () {
    var socket = null;
    var handlers = {};
    var attempts = 0;
    var keepAlive = null;
    var closedByUs = false;

    function indicator(state, label) {
      var el = document.querySelector("[data-live-indicator]");
      if (!el) {
        return;
      }
      el.setAttribute("data-state", state);
      var text = el.querySelector("[data-live-label]");
      if (text) {
        text.textContent = label;
      }
    }

    function url() {
      var proto = window.location.protocol === "https:" ? "wss:" : "ws:";
      var base = document.body.getAttribute("data-context") || "";
      return proto + "//" + window.location.host + base + "/ws/queue";
    }

    function connect() {
      if (!("WebSocket" in window)) {
        indicator("down", "Live updates unavailable");
        return;
      }
      indicator("connecting", "Connecting");
      try {
        socket = new WebSocket(url());
      } catch (e) {
        schedule();
        return;
      }

      socket.onopen = function () {
        attempts = 0;
        indicator("up", "Live");
        keepAlive = window.setInterval(function () {
          if (socket && socket.readyState === 1) {
            socket.send("ping");
          }
        }, 25000);
        emit("__open", {});
      };

      socket.onmessage = function (event) {
        var data;
        try {
          data = JSON.parse(event.data);
        } catch (e) {
          return;
        }
        if (!data || !data.type) {
          return;
        }
        emit(data.type, data.payload || {});
        emit("*", data);
      };

      socket.onclose = function () {
        window.clearInterval(keepAlive);
        if (closedByUs) {
          return;
        }
        indicator("down", "Reconnecting");
        schedule();
      };

      socket.onerror = function () {
        if (socket) {
          socket.close();
        }
      };
    }

    function schedule() {
      attempts += 1;
      var delay = Math.min(1000 * Math.pow(1.6, attempts), 20000);
      window.setTimeout(connect, delay);
    }

    function emit(type, payload) {
      (handlers[type] || []).forEach(function (fn) {
        try {
          fn(payload);
        } catch (e) {
          if (window.console) {
            window.console.error("DigiQ handler failed for " + type, e);
          }
        }
      });
    }

    return {
      start: function () {
        if (!socket) {
          connect();
        }
        return this;
      },
      on: function (type, fn) {
        (handlers[type] = handlers[type] || []).push(fn);
        return this;
      },
      stop: function () {
        closedByUs = true;
        if (socket) {
          socket.close();
        }
      }
    };
  }());

  /* ---------------- Modals ---------------- */
  DigiQ.modal = {
    open: function (id, fields) {
      var el = document.getElementById(id);
      if (!el) {
        return;
      }
      if (fields) {
        // The browser lower-cases data-* attribute names, so match control names
        // case-insensitively - otherwise data-field-serviceId misses name="serviceId".
        var controls = el.querySelectorAll("[name]");
        Object.keys(fields).forEach(function (name) {
          var wanted = name.toLowerCase();
          for (var i = 0; i < controls.length; i += 1) {
            var input = controls[i];
            if (input.name.toLowerCase() !== wanted) {
              continue;
            }
            if (input.type === "checkbox") {
              input.checked = !!fields[name] && fields[name] !== "false" && fields[name] !== "0";
            } else {
              input.value = fields[name] === null || fields[name] === undefined ? "" : fields[name];
            }
            break;
          }
        });
      }
      el.hidden = false;
      var title = el.querySelector("[data-modal-title]");
      var mode = el.querySelector('[name="id"]');
      if (title && mode) {
        title.textContent = (mode.value && mode.value !== "0")
          ? title.getAttribute("data-edit-label")
          : title.getAttribute("data-create-label");
      }
      var first = el.querySelector("input:not([type=hidden]), select, textarea");
      if (first) {
        first.focus();
      }
    },
    close: function (id) {
      var el = document.getElementById(id);
      if (el) {
        el.hidden = true;
      }
    }
  };

  /* ---------------- Wiring ---------------- */
  document.addEventListener("DOMContentLoaded", function () {
    // Theme toggle
    document.querySelectorAll("[data-theme-toggle]").forEach(function (btn) {
      btn.addEventListener("click", function () {
        var mode = DigiQ.theme.toggle();
        btn.setAttribute("aria-label", mode === "dark" ? "Switch to light theme" : "Switch to dark theme");
      });
    });

    // Mobile navigation
    document.querySelectorAll("[data-nav-toggle]").forEach(function (btn) {
      btn.addEventListener("click", function () {
        var open = document.body.getAttribute("data-nav") === "open";
        document.body.setAttribute("data-nav", open ? "closed" : "open");
      });
    });

    // Modal triggers and dismissal
    document.querySelectorAll("[data-modal-open]").forEach(function (btn) {
      btn.addEventListener("click", function () {
        var fields = {};
        Array.prototype.forEach.call(btn.attributes, function (attr) {
          if (attr.name.indexOf("data-field-") === 0) {
            fields[attr.name.slice(11)] = attr.value;
          }
        });
        DigiQ.modal.open(btn.getAttribute("data-modal-open"), fields);
      });
    });

    document.querySelectorAll("[data-modal-close]").forEach(function (btn) {
      btn.addEventListener("click", function () {
        DigiQ.modal.close(btn.getAttribute("data-modal-close"));
      });
    });

    document.querySelectorAll(".modal").forEach(function (modal) {
      modal.addEventListener("click", function (event) {
        if (event.target === modal) {
          modal.hidden = true;
        }
      });
    });

    document.addEventListener("keydown", function (event) {
      if (event.key === "Escape") {
        document.querySelectorAll(".modal:not([hidden])").forEach(function (m) {
          m.hidden = true;
        });
      }
    });

    // Destructive forms ask first.
    document.querySelectorAll("form[data-confirm]").forEach(function (form) {
      form.addEventListener("submit", function (event) {
        if (!window.confirm(form.getAttribute("data-confirm"))) {
          event.preventDefault();
        }
      });
    });

    // Auto-submit filter selects.
    document.querySelectorAll("[data-autosubmit]").forEach(function (el) {
      el.addEventListener("change", function () {
        if (el.form) {
          el.form.submit();
        }
      });
    });

    // Chart card table/graph switch.
    document.querySelectorAll("[data-view-toggle]").forEach(function (btn) {
      btn.addEventListener("click", function () {
        var card = btn.closest(".chart-card");
        if (!card) {
          return;
        }
        var showTable = card.getAttribute("data-view") !== "table";
        card.setAttribute("data-view", showTable ? "table" : "chart");
        btn.textContent = showTable ? "Show chart" : "Show table";
        var table = card.querySelector(".chart-table");
        if (table) {
          table.setAttribute("data-visible", showTable ? "true" : "false");
        }
      });
    });
  });

}(window, document));
