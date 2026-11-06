/* Goodroad — interaction layer (theme switch + motion).
   Independent of app.js: it watches the DOM that app.js renders and
   decorates it. Safe to remove; the app works without it. */
(() => {
  "use strict";

  const root = document.documentElement;
  const reduceMq = matchMedia("(prefers-reduced-motion: reduce)");
  const fineMq = matchMedia("(hover: hover) and (pointer: fine)");
  const reduced = () => reduceMq.matches;
  const KEY = "goodroad-theme";
  const THEME_COLOR = { light: "#f5f2e9", dark: "#071015" };

  /* ---------------- Theme ---------------- */
  const stored = () => {
    try {
      const v = localStorage.getItem(KEY);
      return v === "light" || v === "dark" ? v : null;
    } catch {
      return null;
    }
  };
  const system = () => (matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");
  const current = () => (root.getAttribute("data-theme") === "dark" ? "dark" : "light");

  function syncToggles() {
    const dark = current() === "dark";
    document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
      button.setAttribute("aria-label", dark ? "Switch to light theme" : "Switch to dark theme");
      button.title = dark ? "Light theme" : "Dark theme";
      button.setAttribute("aria-pressed", dark ? "true" : "false");
      button.classList.toggle("is-dark", dark);
    });
  }

  function paint(theme) {
    const next = theme === "dark" ? "dark" : "light";
    root.setAttribute("data-theme", next);
    root.dataset.theme = next;
    try {
      root.style.colorScheme = next;
    } catch (_) {}
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute("content", THEME_COLOR[next]);
    syncToggles();
  }

  function setTheme(next, x, y) {
    const target = next === "dark" ? "dark" : "light";
    try {
      localStorage.setItem(KEY, target);
    } catch {
      /* private mode: theme just won't persist */
    }
    if (target === current()) {
      // Force a visual refresh in case CSS was out of sync
      paint(target);
      return;
    }

    if (reduced() || !document.startViewTransition) {
      root.classList.add("theme-anim");
      paint(target);
      setTimeout(() => root.classList.remove("theme-anim"), 480);
      return;
    }

    const cx = typeof x === "number" ? x : innerWidth / 2;
    const cy = typeof y === "number" ? y : 0;
    const radius = Math.hypot(Math.max(cx, innerWidth - cx), Math.max(cy, innerHeight - cy));

    try {
      const transition = document.startViewTransition(() => paint(target));
      transition.ready
        .then(() => {
          root.animate(
            {
              clipPath: [
                `circle(0px at ${cx}px ${cy}px)`,
                `circle(${radius}px at ${cx}px ${cy}px)`,
              ],
            },
            {
              duration: 720,
              easing: "cubic-bezier(.22,.8,.2,1)",
              pseudoElement: "::view-transition-new(root)",
            }
          );
        })
        .catch(() => {
          paint(target);
        });
    } catch (_) {
      root.classList.add("theme-anim");
      paint(target);
      setTimeout(() => root.classList.remove("theme-anim"), 480);
    }
  }

  // Capture-phase so the toggle always wins over other click handlers
  document.addEventListener(
    "click",
    (event) => {
      const button = event.target.closest("[data-theme-toggle]");
      if (!button) return;
      event.preventDefault();
      event.stopPropagation();
      const box = button.getBoundingClientRect();
      setTheme(
        current() === "dark" ? "light" : "dark",
        box.left + box.width / 2,
        box.top + box.height / 2
      );
    },
    true
  );

  // Follow the OS setting until the visitor picks a theme themselves.
  matchMedia("(prefers-color-scheme: dark)").addEventListener?.("change", () => {
    if (!stored()) paint(system());
  });

  // Always apply a definite theme on load (index.html may have set one already)
  paint(stored() || system());

  /* ---------------- Ambient layers ---------------- */
  const bg = document.createElement("div");
  bg.className = "fx-bg";
  bg.setAttribute("aria-hidden", "true");
  bg.innerHTML = "<i></i><i></i><i></i>";
  document.body.prepend(bg);

  const bar = document.createElement("div");
  bar.className = "fx-progress";
  bar.setAttribute("aria-hidden", "true");
  document.body.append(bar);

  let scrollQueued = false;
  function onScroll() {
    if (scrollQueued) return;
    scrollQueued = true;
    requestAnimationFrame(() => {
      scrollQueued = false;
      const max = root.scrollHeight - innerHeight;
      bar.style.setProperty("--p", max > 0 ? Math.min(1, scrollY / max).toFixed(4) : 0);
      document.getElementById("site-header")?.classList.toggle("is-scrolled", scrollY > 12);
    });
  }
  addEventListener("scroll", onScroll, { passive: true });
  addEventListener("resize", onScroll, { passive: true });

  /* ---------------- Scroll reveal ---------------- */
  const REVEAL =
    ".car-card,.stat-card,.review-card,.order-card,.panel-card,.profile-card,.section-heading,.inventory-head," +
    ".search-panel,.detail-facts,.table-wrap,.audit-feed,.empty-state,.auth-card,.auth-story,.purchase-card," +
    ".gallery,.detail-art,.page-title-row,.review-form";
  const canObserve = "IntersectionObserver" in window;
  if (canObserve && !reduced()) root.classList.add("fx-on");

  const revealIO = canObserve
    ? new IntersectionObserver(
        (entries) => {
          for (const entry of entries) {
            if (!entry.isIntersecting) continue;
            revealIO.unobserve(entry.target);
            entry.target.classList.add("is-in");
          }
        },
        { threshold: 0.1, rootMargin: "0px 0px -5% 0px" }
      )
    : null;

  function tagReveal(scope) {
    if (!revealIO || !root.classList.contains("fx-on")) return;
    scope.querySelectorAll(REVEAL).forEach((el) => {
      if (el.hasAttribute("data-reveal")) return;
      const siblings = el.parentElement
        ? Array.from(el.parentElement.children).filter((c) => c.matches(REVEAL))
        : [];
      el.style.setProperty("--i", String(Math.max(0, siblings.indexOf(el)) % 6));
      el.setAttribute("data-reveal", "");
      revealIO.observe(el);
    });
  }

  /* ---------------- Hero headline: word-by-word entrance ---------------- */
  function splitHeadline(scope) {
    scope.querySelectorAll(".hero h1:not([data-split])").forEach((h1) => {
      h1.dataset.split = "1";
      if (reduced()) return;
      let index = 0;
      const walk = (node) => {
        Array.from(node.childNodes).forEach((child) => {
          if (child.nodeType === Node.TEXT_NODE) {
            const frag = document.createDocumentFragment();
            child.textContent.split(/(\s+)/).forEach((part) => {
              if (!part) return;
              if (/^\s+$/.test(part)) {
                frag.append(part);
                return;
              }
              const span = document.createElement("span");
              span.className = "w";
              span.style.setProperty("--wi", String(index++));
              span.textContent = part;
              frag.append(span);
            });
            child.replaceWith(frag);
          } else if (child.nodeType === Node.ELEMENT_NODE && child.tagName !== "BR") {
            walk(child);
          }
        });
      };
      walk(h1);
    });
  }

  /* ---------------- Count-up numbers ---------------- */
  const countIO = canObserve
    ? new IntersectionObserver(
        (entries) => {
          for (const entry of entries) {
            if (!entry.isIntersecting) continue;
            countIO.unobserve(entry.target);
            animateCount(entry.target);
          }
        },
        { threshold: 0.4 }
      )
    : null;

  function animateCount(el) {
    const final = el.dataset.fxPending;
    const match = final && final.match(/^(\D*?)(\d[\d,]*)(?![\d.])(.*)$/);
    if (!match) return;
    const [, prefix, digits, suffix] = match;
    const target = parseInt(digits.replace(/,/g, ""), 10);
    const start = performance.now();
    const duration = 1100;
    el._counting = true;
    const tick = (now) => {
      const t = Math.min(1, (now - start) / duration);
      const eased = 1 - Math.pow(1 - t, 4);
      el.textContent =
        t < 1
          ? `${prefix}${Math.round(target * eased).toLocaleString("en-US")}${suffix}`
          : final;
      if (t < 1) requestAnimationFrame(tick);
      else {
        el._counting = false;
        el.dataset.fxDone = final;
      }
    };
    requestAnimationFrame(tick);
  }

  function scanCounters() {
    document.querySelectorAll(".stat-card strong,#inventory-count").forEach((el) => {
      if (el._counting) return;
      const text = el.textContent.trim();
      if (el.dataset.fxDone === text || el.dataset.fxPending === text) return;
      const countable =
        /^(\D*?)(\d[\d,]*)(?![\d.])(.*)$/.test(text) &&
        parseInt(text.replace(/[^\d]/g, ""), 10) > 1;
      if (!countable || reduced() || !countIO) {
        el.dataset.fxDone = text;
        return;
      }
      el.dataset.fxPending = text;
      countIO.observe(el);
    });
  }

  /* ---------------- Pointer effects ---------------- */
  let hot = null;
  let frame = 0;
  let lastMove = null;
  const resetCard = (card) => {
    ["--rx", "--ry"].forEach((name) => card.style.removeProperty(name));
  };
  document.addEventListener(
    "pointermove",
    (event) => {
      if (!fineMq.matches || event.pointerType === "touch") return;
      lastMove = event;
      if (frame) return;
      frame = requestAnimationFrame(() => {
        frame = 0;
        const e = lastMove;
        const card = e.target.closest?.(".car-card,.stat-card,.order-card,.panel-card") || null;
        if (hot && hot !== card) resetCard(hot);
        hot = card;
        if (card) {
          const box = card.getBoundingClientRect();
          const x = e.clientX - box.left;
          const y = e.clientY - box.top;
          card.style.setProperty("--mx", `${x}px`);
          card.style.setProperty("--my", `${y}px`);
          if (card.classList.contains("car-card") && !reduced()) {
            card.style.setProperty("--rx", `${(((y / box.height) - 0.5) * -7).toFixed(2)}deg`);
            card.style.setProperty("--ry", `${(((x / box.width) - 0.5) * 9).toFixed(2)}deg`);
          }
        }
        const hero = document.querySelector(".hero");
        const art = hero?.querySelector(".hero-art");
        if (art && !reduced()) {
          const box = hero.getBoundingClientRect();
          const px = Math.max(-1, Math.min(1, ((e.clientX - box.left) / box.width - 0.5) * 2));
          const py = Math.max(-1, Math.min(1, ((e.clientY - box.top) / box.height - 0.5) * 2));
          art.style.setProperty("--px", px.toFixed(3));
          art.style.setProperty("--py", py.toFixed(3));
        }
      });
    },
    { passive: true }
  );
  root.addEventListener("pointerleave", () => {
    if (hot) resetCard(hot);
    hot = null;
  });

  document.addEventListener(
    "pointerdown",
    (event) => {
      const button = event.target.closest?.(".button");
      if (!button || button.disabled || reduced()) return;
      const box = button.getBoundingClientRect();
      const size = Math.max(box.width, box.height) * 2;
      const ripple = document.createElement("span");
      ripple.className = "fx-ripple";
      ripple.style.cssText = `width:${size}px;height:${size}px;left:${event.clientX - box.left - size / 2}px;top:${event.clientY - box.top - size / 2}px`;
      button.append(ripple);
      ripple.addEventListener("animationend", () => ripple.remove());
    },
    { passive: true }
  );

  /* ---------------- Watch what app.js renders ---------------- */
  const main = document.getElementById("main-content");
  const header = document.getElementById("site-header");
  let queued = false;
  function process() {
    queued = false;
    if (main) {
      splitHeadline(main);
      tagReveal(main);
    }
    scanCounters();
    syncToggles();
  }
  const schedule = () => {
    if (!queued) {
      queued = true;
      requestAnimationFrame(process);
    }
  };
  if (main) new MutationObserver(schedule).observe(main, { childList: true, subtree: true });
  if (header) new MutationObserver(schedule).observe(header, { childList: true });
  process();
  onScroll();
})();
