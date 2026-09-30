/* ===================================================================
   DigiQ - analytics charts (Chart.js v4)

   Rules held to throughout:
     - one value axis per chart, never a second y-scale
     - categorical hues assigned in fixed slot order, never cycled
     - a legend whenever two series are on screen, none for one
     - single-series bars carry direct value labels, so the chart still
       reads if the colour does not
     - grid and axes stay recessive; the marks carry the meaning
     - dark mode uses its own steps, re-read whenever the theme changes
   =================================================================== */
(function (window, document) {
  "use strict";

  var DigiQ = window.DigiQ || (window.DigiQ = {});
  var charts = [];

  function token(name, fallback) {
    var value = getComputedStyle(document.documentElement).getPropertyValue(name);
    value = (value || "").trim();
    return value || fallback;
  }

  /** Re-read on every build so a theme switch repaints from the right steps. */
  function palette() {
    return {
      s1: token("--series-1", "#2a78d6"),
      s2: token("--series-2", "#eb6834"),
      surface: token("--surface-1", "#fcfcfb"),
      ink: token("--ink-1", "#0b0b0b"),
      ink2: token("--ink-2", "#52514e"),
      muted: token("--ink-3", "#898781"),
      grid: token("--hairline", "#e1e0d9"),
      axis: token("--hairline-strong", "#c3c2b7")
    };
  }

  /* Draws the value at the end of each bar - the secondary encoding that keeps
     a single-series chart readable without relying on its fill colour. */
  var valueLabels = {
    id: "digiqValueLabels",
    afterDatasetsDraw: function (chart, args, opts) {
      if (!opts || opts.enabled === false) {
        return;
      }
      var ctx = chart.ctx;
      var p = palette();
      ctx.save();
      ctx.font = "600 11px " + token("--font", "system-ui, sans-serif");
      ctx.fillStyle = p.ink2;

      chart.data.datasets.forEach(function (dataset, di) {
        var meta = chart.getDatasetMeta(di);
        if (meta.hidden) {
          return;
        }
        meta.data.forEach(function (bar, i) {
          var raw = dataset.data[i];
          if (raw === null || raw === undefined || raw === 0) {
            return;
          }
          var text = opts.suffix ? raw + opts.suffix : String(raw);
          if (chart.options.indexAxis === "y") {
            ctx.textAlign = "left";
            ctx.textBaseline = "middle";
            ctx.fillText(text, bar.x + 7, bar.y);
          } else {
            ctx.textAlign = "center";
            ctx.textBaseline = "bottom";
            ctx.fillText(text, bar.x, bar.y - 5);
          }
        });
      });
      ctx.restore();
    }
  };

  function baseOptions(p, opts) {
    opts = opts || {};
    return {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: opts.indexAxis === "y" ? "nearest" : "index", intersect: false },
      layout: { padding: { top: 8, right: opts.indexAxis === "y" ? 40 : 10, bottom: 0, left: 0 } },
      plugins: {
        legend: {
          display: !!opts.legend,
          position: "top",
          align: "end",
          labels: {
            boxWidth: 10,
            boxHeight: 10,
            usePointStyle: true,
            pointStyle: "rectRounded",
            color: p.ink2,
            font: { size: 12 },
            padding: 14
          }
        },
        tooltip: {
          backgroundColor: p.ink,
          titleColor: "#fff",
          bodyColor: "#fff",
          padding: 10,
          cornerRadius: 8,
          displayColors: true,
          boxWidth: 9,
          boxHeight: 9,
          usePointStyle: true,
          callbacks: opts.tooltip || {}
        },
        digiqValueLabels: opts.valueLabels || { enabled: false }
      },
      scales: {
        x: {
          grid: {
            display: opts.indexAxis === "y",
            color: p.grid,
            drawTicks: false,
            drawBorder: false
          },
          border: { display: false },
          ticks: { color: p.muted, font: { size: 11 }, padding: 6, maxRotation: 0, autoSkipPadding: 14 }
        },
        y: {
          beginAtZero: true,
          grid: {
            display: opts.indexAxis !== "y",
            color: p.grid,
            drawTicks: false,
            drawBorder: false
          },
          border: { display: false },
          ticks: { color: p.muted, font: { size: 11 }, padding: 8, precision: 0 }
        }
      }
    };
  }

  function make(canvasId, config) {
    var el = document.getElementById(canvasId);
    if (!el || typeof window.Chart === "undefined") {
      return null;
    }
    var chart = new window.Chart(el.getContext("2d"), config);
    charts.push({ id: canvasId, chart: chart });
    return chart;
  }

  function shortDate(iso) {
    var parts = String(iso).split("-");
    if (parts.length !== 3) {
      return iso;
    }
    var months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
    return parseInt(parts[2], 10) + " " + months[parseInt(parts[1], 10) - 1];
  }

  function statusLabel(raw) {
    return String(raw).replace("_", " ").toLowerCase().replace(/\b\w/g, function (c) {
      return c.toUpperCase();
    });
  }

  /* ---------------- The five charts ---------------- */

  function buildAll(data) {
    var p = palette();

    /* 1. Volume over time - two series, so a legend is present.
          Lines at 2px, markers appear on hover at 8px+. */
    make("chartVolume", {
      type: "line",
      data: {
        labels: data.tokensPerDay.map(function (r) { return shortDate(r.date); }),
        datasets: [
          {
            label: "Issued",
            data: data.tokensPerDay.map(function (r) { return r.issued; }),
            borderColor: p.s1,
            backgroundColor: "transparent",
            borderWidth: 2,
            tension: 0.32,
            pointRadius: 0,
            pointHoverRadius: 5,
            pointHoverBorderWidth: 2,
            pointHoverBorderColor: p.surface,
            pointHoverBackgroundColor: p.s1,
            pointHitRadius: 18
          },
          {
            label: "Completed",
            data: data.tokensPerDay.map(function (r) { return r.completed; }),
            borderColor: p.s2,
            backgroundColor: "transparent",
            borderWidth: 2,
            borderDash: [5, 4],
            tension: 0.32,
            pointRadius: 0,
            pointHoverRadius: 5,
            pointHoverBorderWidth: 2,
            pointHoverBorderColor: p.surface,
            pointHoverBackgroundColor: p.s2,
            pointHitRadius: 18
          }
        ]
      },
      options: baseOptions(p, { legend: true })
    });

    /* 2. Outcome mix - one series, so no legend; the axis names each bar and
          the direct labels carry the value. */
    make("chartStatus", {
      type: "bar",
      data: {
        labels: data.statusBreakdown.map(function (r) { return statusLabel(r.status); }),
        datasets: [{
          label: "Tokens",
          data: data.statusBreakdown.map(function (r) { return r.count; }),
          backgroundColor: p.s1,
          borderColor: p.surface,
          borderWidth: 2,
          borderRadius: 4,
          borderSkipped: "start",
          barThickness: 20
        }]
      },
      options: Object.assign(baseOptions(p, {
        indexAxis: "y",
        valueLabels: { enabled: true }
      }), { indexAxis: "y" })
    });

    /* 3. Where the time goes - two measures in the same unit (minutes), so they
          legitimately share one axis. */
    make("chartWaitService", {
      type: "bar",
      data: {
        labels: data.waitVsService.map(function (r) { return r.code; }),
        datasets: [
          {
            label: "Avg wait (min)",
            data: data.waitVsService.map(function (r) { return r.avgWait; }),
            backgroundColor: p.s1,
            borderColor: p.surface,
            borderWidth: 2,
            borderRadius: 4,
            borderSkipped: "bottom"
          },
          {
            label: "Avg service (min)",
            data: data.waitVsService.map(function (r) { return r.avgService; }),
            backgroundColor: p.s2,
            borderColor: p.surface,
            borderWidth: 2,
            borderRadius: 4,
            borderSkipped: "bottom"
          }
        ]
      },
      options: baseOptions(p, {
        legend: true,
        tooltip: {
          title: function (items) {
            var i = items[0].dataIndex;
            return data.waitVsService[i].service;
          },
          label: function (item) {
            return item.dataset.label + ": " + item.formattedValue + " min";
          }
        }
      })
    });

    /* 4. Arrivals by hour - one series. */
    make("chartPeak", {
      type: "bar",
      data: {
        labels: data.peakHours.map(function (r) { return r.label; }),
        datasets: [{
          label: "Tokens issued",
          data: data.peakHours.map(function (r) { return r.count; }),
          backgroundColor: p.s1,
          borderColor: p.surface,
          borderWidth: 2,
          borderRadius: 4,
          borderSkipped: "bottom"
        }]
      },
      options: baseOptions(p, {
        tooltip: {
          label: function (item) {
            return item.formattedValue + " tokens issued";
          }
        }
      })
    });

    /* 5. Counter throughput - one series, direct labels. */
    make("chartCounters", {
      type: "bar",
      data: {
        labels: data.counterPerformance.map(function (r) { return r.counter; }),
        datasets: [{
          label: "Completed",
          data: data.counterPerformance.map(function (r) { return r.served; }),
          backgroundColor: p.s1,
          borderColor: p.surface,
          borderWidth: 2,
          borderRadius: 4,
          borderSkipped: "start",
          barThickness: 18
        }]
      },
      options: Object.assign(baseOptions(p, {
        indexAxis: "y",
        valueLabels: { enabled: true },
        tooltip: {
          afterLabel: function (item) {
            var row = data.counterPerformance[item.dataIndex];
            return row.service + " - avg " + row.avgService + " min";
          }
        }
      }), { indexAxis: "y" })
    });
  }

  function destroyAll() {
    charts.forEach(function (entry) {
      entry.chart.destroy();
    });
    charts = [];
  }

  /* ---------------- Public entry point ---------------- */
  DigiQ.analytics = {
    render: function (data) {
      if (typeof window.Chart === "undefined") {
        return;
      }
      window.Chart.register(valueLabels);
      window.Chart.defaults.font.family = token("--font", "system-ui, sans-serif");
      destroyAll();
      buildAll(data);
      DigiQ.analytics.data = data;
    },

    /** Pulls a new window without a page reload. */
    load: function (days) {
      var base = document.body.getAttribute("data-context") || "";
      return DigiQ.get(base + "/admin/analytics/data?days=" + days).then(function (data) {
        if (data && data.tokensPerDay) {
          DigiQ.analytics.render(data);
        }
        return data;
      });
    }
  };

  // Dark mode is a different set of steps, not an inversion - rebuild on switch.
  document.addEventListener("digiq:themechange", function () {
    if (DigiQ.analytics.data) {
      DigiQ.analytics.render(DigiQ.analytics.data);
    }
  });

}(window, document));
