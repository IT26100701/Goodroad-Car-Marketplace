(() => {
  "use strict";

  const $ = (selector, parent = document) => parent.querySelector(selector);
  const appMain = $("#main-content");
  const header = $("#site-header");
  const footer = $("#site-footer");
  const toastRegion = $("#toast-region");
  const state = {
    user: null,
    userLoaded: false,
    csrfToken: null,
    csrfHeader: "X-XSRF-TOKEN",
    routeKey: "",
    routeVersion: 0,
    filters: { q: "", vehicleType: "", minPrice: "", maxPrice: "", minYear: "", maxYear: "", minMileage: "", maxMileage: "", sort: "newest" },
    wishlistIds: new Set(),
    listingDraft: null,
    photoDraft: null,
    completedReviews: {},
    lastBrowseQuery: ""
  };

  const icon = {
    car: '<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="m4 11 1.7-4.3A2 2 0 0 1 7.56 5.5h8.88a2 2 0 0 1 1.86 1.2L20 11l1 1.6v5.1h-2.5v-2H5.5v2H3v-5.1L4 11Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M4 11h16M6.5 13.2h.01m11 0h.01" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>',
    heart: '<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M20.4 5.6a5.2 5.2 0 0 0-7.4 0l-1 1-1-1a5.2 5.2 0 0 0-7.4 7.4l1 1L12 21l7.4-7 1-1a5.2 5.2 0 0 0 0-7.4Z" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/></svg>',
    search: '<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="10.8" cy="10.8" r="6.7" stroke="currentColor" stroke-width="1.8"/><path d="m16 16 4.2 4.2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>',
    arrow: '<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M4.5 12h15m-6-6 6 6-6 6" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    shield: '<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M12 3 20 6v5.3c0 4.6-3.1 8.1-8 9.7-4.9-1.6-8-5.1-8-9.7V6l8-3Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="m8.8 11.8 2.1 2.1 4.5-4.7" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    menu: '<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M4 7h16M4 12h16M4 17h16" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>',
    close: '<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="m6 6 12 12M18 6 6 18" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>',
    plus: '<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>',
    check: '<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="m5 12.5 4.2 4.2L19 7" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    pin: '<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 1 1 14 0Z" stroke="currentColor" stroke-width="1.6"/><circle cx="12" cy="10" r="2.2" stroke="currentColor" stroke-width="1.6"/></svg>',
    sun: '<svg class="i-sun" viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="12" cy="12" r="4.2" stroke="currentColor" stroke-width="1.7"/><path d="M12 2.8v2.4M12 18.8v2.4M2.8 12h2.4M18.8 12h2.4M5.5 5.5l1.7 1.7M16.8 16.8l1.7 1.7M5.5 18.5l1.7-1.7M16.8 7.2l1.7-1.7" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg>',
    moon: '<svg class="i-moon" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M20 14.2A8.2 8.2 0 0 1 9.8 4 8.2 8.2 0 1 0 20 14.2Z" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/></svg>'
  };

  const escapeHTML = (value = "") => String(value).replace(/[&<>"']/g, (char) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  })[char]);
  const money = (value) => {
    const amount = Number(value);
    return Number.isFinite(amount) ? `Rs. ${new Intl.NumberFormat("en-US", { maximumFractionDigits: 0 }).format(amount)}` : "Price unavailable";
  };
  const number = (value) => new Intl.NumberFormat("en-US").format(Number(value) || 0);
  const dateText = (value, options = { year: "numeric", month: "short", day: "numeric" }) => {
    if (!value) return "—";
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? "—" : new Intl.DateTimeFormat(undefined, options).format(date);
  };
  const initials = (text = "") => String(text).trim().split(/\s+/).slice(0, 2).map((part) => part[0] || "").join("").toUpperCase() || "G";
  const titleCase = (value = "") => String(value).toLowerCase().replace(/(^|[_\s-])\S/g, (letter) => letter.toUpperCase()).replaceAll("_", " ");
  const hashPath = () => {
    const raw = location.hash.replace(/^#\/?/, "");
    return raw || "browse";
  };
  const routeHref = (path) => `/#${path}`;

  function toast(message, kind = "") {
    const element = document.createElement("div");
    element.className = `toast ${kind}`.trim();
    element.setAttribute("role", kind === "error" ? "alert" : "status");
    element.textContent = message;
    toastRegion.append(element);
    window.setTimeout(() => element.remove(), 4600);
  }

  function errorText(body, status, fallback = "The request could not be completed.") {
    if (typeof body === "string" && body.trim()) return body;
    if (body && typeof body === "object") {
      if (typeof body.message === "string") return body.message;
      if (typeof body.detail === "string") return body.detail;
      if (typeof body.error === "string") return body.error;
      if (Array.isArray(body.errors) && body.errors.length) {
        return body.errors.map((entry) => typeof entry === "string" ? entry : entry.defaultMessage || entry.message || "").filter(Boolean).join(" ");
      }
      if (body.errors && typeof body.errors === "object") {
        return Object.entries(body.errors).map(([key, value]) => `${key}: ${value}`).join(" · ");
      }
    }
    return `${fallback} (HTTP ${status})`;
  }

  async function readResponse(response) {
    if (response.status === 204) return null;
    const text = await response.text();
    if (!text) return null;
    try { return JSON.parse(text); } catch { return text; }
  }

  async function getCsrf() {
    const response = await fetch("/api/auth/csrf", { method: "GET", credentials: "same-origin", headers: { Accept: "application/json" } });
    const body = await readResponse(response);
    if (!response.ok) throw new Error(errorText(body, response.status, "Could not prepare a secure request."));
    state.csrfToken = body?.token || null;
    state.csrfHeader = body?.headerName || "X-XSRF-TOKEN";
    if (!state.csrfToken) throw new Error("The security token was missing from the server response.");
    return state.csrfToken;
  }

  async function api(path, options = {}) {
    const method = (options.method || "GET").toUpperCase();
    const headers = { Accept: "application/json", ...(options.headers || {}) };
    const request = { method, credentials: "same-origin", headers };
    if (["POST", "PUT", "PATCH", "DELETE"].includes(method)) {
      await getCsrf();
      headers[state.csrfHeader || "X-XSRF-TOKEN"] = state.csrfToken;
    }
    if (options.body instanceof FormData) {
      // The browser adds the multipart boundary itself, so no Content-Type here.
      request.body = options.body;
    } else if (options.body !== undefined) {
      headers["Content-Type"] = "application/json";
      request.body = JSON.stringify(options.body);
    }
    const response = await fetch(path, request);
    const body = await readResponse(response);
    if (!response.ok) throw new Error(errorText(body, response.status));
    return body;
  }

  function statusPill(value) {
    const text = String(value || "unknown").toUpperCase();
    const className = `status-${text.toLowerCase()}`;
    return `<span class="status-pill ${className}">${escapeHTML(titleCase(text))}</span>`;
  }

  function carIllustration(type = "SEDAN") {
    const wide = ["SUV", "VAN", "TRUCK"].includes(type);
    const roof = wide
      ? "M5.4 13.5 7.8 8.3c.4-.9 1.3-1.5 2.3-1.5h5.3c1.1 0 1.9.5 2.5 1.5l3 5.2"
      : "M5.4 13.5 8.1 9c.5-.9 1.3-1.4 2.3-1.4h3.9c1 0 1.9.5 2.5 1.4l3.2 4.5";
    return `<svg viewBox="0 0 28 20" fill="none" aria-hidden="true">
      <path d="${roof}M3.4 13.5c.4-.9 1.2-1.3 2.2-1.3h16.8c1.2 0 2.1.7 2.3 1.9l.3 2.1c.1.8-.5 1.4-1.2 1.4H4.1c-.8 0-1.3-.6-1.2-1.4l.5-2.7Z" fill="currentColor" opacity=".94"/>
      <path d="M8 9.4h8.7M5 14.5h2m14 0h2" stroke="hsl(42 42% 99%)" stroke-width=".7" stroke-linecap="round" opacity=".82"/>
      ${wheel(7.4)}${wheel(21)}
      <path d="M3.7 14h1.8m18.5 0h.8" stroke="hsl(12 69% 61%)" stroke-width=".8" stroke-linecap="round"/>
    </svg>`;
  }

  function heroCarIllustration() {
    return `<svg viewBox="0 0 760 420" fill="none" aria-hidden="true" focusable="false">
      <circle class="hero-sun" cx="581" cy="124" r="73"/>
      <path class="hero-horizon" d="M0 287c112-27 203-39 319-31 116 9 202-7 441-57"/>
      <ellipse class="hero-ground-shadow" cx="383" cy="356" rx="283" ry="17"/>
      <path class="hero-road" d="M0 370h760M48 396h117m86 0h156m88 0h117"/>
      <path class="hero-car-body" d="M92 274c9-12 29-20 57-23l65-7 49-75c11-17 29-27 51-28l141-2c26 0 45 10 60 30l61 75 68 9c29 4 46 17 52 38l7 29c3 14-7 25-21 25H102c-15 0-25-11-23-25l4-29c1-7 4-12 9-17Z"/>
      <path class="hero-car-roof" d="m226 240 41-65c11-17 26-25 47-26l140-2c22 0 40 9 53 26l54 68"/>
      <path class="hero-window" d="m260 237 31-56c5-9 14-14 25-14h73v70H260Zm146-70h46c15 0 27 6 36 18l39 52H406v-70Z"/>
      <path class="hero-window-glint" d="m303 179-18 34m137-32h49"/>
      <path class="hero-car-detail" d="M397 241v80m-166-78-41 5m380-2 51 7M104 285h568M296 250h34m123 0h35"/>
      <path class="hero-car-sill" d="M134 320h514"/>
      <path class="hero-tail-light" d="M88 282h32c7 0 11 4 11 10v7H82l2-13c1-3 2-4 4-4Z"/>
      <path class="hero-head-light" d="M660 273c16 3 27 10 32 20l3 7h-44c-5 0-8-4-7-8l3-10c2-7 6-10 13-9Z"/>
      <g class="hero-wheel" transform="translate(208 331)"><circle class="hero-tire" r="46"/><circle class="hero-rim" r="32"/><circle class="hero-wheel-inner" r="22"/><path d="M0-20v40M-20 0h40M-14-14l28 28M14-14l-28 28"/><circle class="hero-hub" r="7"/></g>
      <g class="hero-wheel" transform="translate(565 331)"><circle class="hero-tire" r="46"/><circle class="hero-rim" r="32"/><circle class="hero-wheel-inner" r="22"/><path d="M0-20v40M-20 0h40M-14-14l28 28M14-14l-28 28"/><circle class="hero-hub" r="7"/></g>
    </svg>`;
  }

  // A wheel with spokes so it visibly spins when theme.css animates .wheel.
  function wheel(cx) {
    const rim = "hsl(39 40% 90%)";
    return `<g class="wheel"><circle cx="${cx}" cy="17" r="2.1" fill="hsl(172 25% 14%)" stroke="${rim}" stroke-opacity=".55" stroke-width=".25"/>
      <circle cx="${cx}" cy="17" r=".8" fill="${rim}"/>
      <path d="M${cx} 15.4v3.2M${cx - 1.6} 17h3.2M${cx - 1.13} 15.87l2.26 2.26M${cx + 1.13} 15.87l-2.26 2.26" stroke="${rim}" stroke-width=".28" stroke-linecap="round" opacity=".8"/></g>`;
  }

  function artColors(car) {
    const palettes = [
      ["161 24% 88%", "164 31% 31%"], ["38 46% 87%", "164 27% 32%"],
      ["12 41% 89%", "171 30% 29%"], ["192 22% 87%", "12 54% 47%"],
      ["48 35% 87%", "169 29% 27%"], ["24 34% 88%", "157 28% 31%"]
    ];
    const seed = String(car?.id || car?.brand || "car").split("").reduce((sum, char) => sum + char.charCodeAt(0), 0);
    const [bg, fg] = palettes[seed % palettes.length];
    return `--art-bg:${bg};--art-ink:${fg}`;
  }

  const MAX_PHOTOS = 6;
  const MAX_PHOTO_BYTES = 5 * 1024 * 1024;
  const PHOTO_TYPES = ["image/jpeg", "image/png", "image/webp"];

  const photoFileName = (url) => String(url).split("/").pop();

  function clearPhotoDraft() {
    (state.photoDraft?.added || []).forEach((item) => URL.revokeObjectURL(item.preview));
    state.photoDraft = null;
  }

  function startPhotoDraft(car = null) {
    clearPhotoDraft();
    state.photoDraft = { existing: Array.isArray(car?.images) ? [...car.images] : [], removed: [], added: [] };
  }

  function photoCount() {
    const draft = state.photoDraft;
    return draft ? draft.existing.length + draft.added.length : 0;
  }

  function photoGridMarkup() {
    const draft = state.photoDraft || { existing: [], added: [] };
    const tiles = [
      ...draft.existing.map((url) => ({ src: url, kind: "existing", key: photoFileName(url) })),
      ...draft.added.map((item, index) => ({ src: item.preview, kind: "new", key: String(index) }))
    ];
    return tiles.map((tile, index) => `<figure class="photo-tile">
      ${index === 0 ? '<span class="photo-cover">Cover</span>' : ""}
      <img src="${escapeHTML(tile.src)}" alt="Photo ${index + 1} of your car">
      <button class="photo-remove" type="button" data-action="remove-photo" data-kind="${tile.kind}" data-key="${escapeHTML(tile.key)}" aria-label="Remove photo ${index + 1}">${icon.close}</button>
    </figure>`).join("");
  }

  function refreshPhotoGrid() {
    const grid = $("#photo-grid");
    if (grid) grid.innerHTML = photoGridMarkup();
    const note = $("#photo-count");
    if (note) note.textContent = `${photoCount()} of ${MAX_PHOTOS} photos`;
  }

  function addPhotoFiles(files) {
    if (!state.photoDraft) startPhotoDraft();
    const draft = state.photoDraft;
    let problem = "";
    for (const file of files) {
      if (!PHOTO_TYPES.includes(file.type)) { problem = `${file.name} is not a JPG, PNG or WebP image.`; continue; }
      if (file.size > MAX_PHOTO_BYTES) { problem = `${file.name} is larger than 5 MB.`; continue; }
      if (photoCount() >= MAX_PHOTOS) { problem = `A listing can have up to ${MAX_PHOTOS} photos.`; break; }
      draft.added.push({ file, preview: URL.createObjectURL(file) });
    }
    if (problem) toast(problem, "error");
    refreshPhotoGrid();
  }

  function removePhoto(kind, key) {
    const draft = state.photoDraft;
    if (!draft) return;
    if (kind === "existing") {
      draft.existing = draft.existing.filter((url) => photoFileName(url) !== key);
      draft.removed.push(key);
    } else {
      const [item] = draft.added.splice(Number(key), 1);
      if (item) URL.revokeObjectURL(item.preview);
    }
    refreshPhotoGrid();
  }

  function galleryMarkup(car) {
    const images = Array.isArray(car.images) ? car.images : [];
    if (!images.length) {
      return `<div class="detail-art" style="${artColors(car)}"><div class="car-illustration">${carIllustration(car.vehicleType)}</div></div>`;
    }
    const label = `${car.year} ${car.brand} ${car.model}`;
    return `<div class="gallery">
      <div class="gallery-main"><img id="gallery-main" src="${escapeHTML(images[0])}" alt="${escapeHTML(label)}"></div>
      ${images.length > 1 ? `<div class="gallery-thumbs">${images.map((src, index) => `<button type="button" class="gallery-thumb" data-action="gallery-pick" data-src="${escapeHTML(src)}" aria-label="Show photo ${index + 1}" aria-pressed="${index === 0}"><img src="${escapeHTML(src)}" alt=""></button>`).join("")}</div>` : ""}
    </div>`;
  }

  function carCard(car) {
    const saved = state.wishlistIds.has(String(car.id));
    const available = car.status === "ACTIVE";
    const carHref = routeHref(`listing/${encodeURIComponent(car.id)}`);
    const cover = Array.isArray(car.images) && car.images.length ? car.images[0] : null;
    const visual = `<div class="car-visual ${cover ? "has-photo" : ""}" style="${artColors(car)}">
      ${cover ? `<img class="car-photo" src="${escapeHTML(cover)}" alt="" loading="lazy">` : ""}
      <div class="car-visual-top"><span class="tag">${escapeHTML(titleCase(car.vehicleType || "car"))}</span>${car.status && car.status !== "ACTIVE" ? statusPill(car.status) : ""}</div>
      ${cover ? "" : `<div class="car-illustration">${carIllustration(car.vehicleType)}</div>`}
      ${car.images?.length > 1 ? `<span class="photo-count">${car.images.length} photos</span>` : ""}
    </div>`;
    const title = `${escapeHTML(car.year)} ${escapeHTML(car.brand)} ${escapeHTML(car.model)}`;
    return `<article class="car-card">
      ${available ? `<a class="car-card-link" href="${carHref}" aria-label="View ${title}">${visual}</a>` : `<div class="car-card-link">${visual}</div>`}
      <div class="card-body">
        <div class="card-title-line">
          ${available ? `<a class="car-card-link" href="${carHref}"><h3 class="card-title">${title}</h3></a>` : `<span class="car-card-link"><h3 class="card-title">${title}</h3></span>`}
          ${state.user?.role === "BUYER" ? `<button class="icon-button ${saved ? "is-saved" : ""}" type="button" data-action="toggle-wishlist" data-car-id="${escapeHTML(car.id)}" aria-label="${saved ? "Remove from" : "Save to"} saved cars" aria-pressed="${saved}">${icon.heart}</button>` : ""}
        </div>
        <div class="car-meta"><span>${number(car.mileage)} km</span><span>${escapeHTML(titleCase(car.vehicleType || "vehicle"))}</span></div>
        <div class="price-row"><strong class="price">${money(car.price)}</strong><span class="seller-caption">Listed by ${escapeHTML(car.sellerName || "independent seller")}</span></div>
      </div>
    </article>`;
  }

  function renderHeader(active = "") {
    const links = [
      ["browse", "Browse cars"],
      ...(state.user?.role === "SELLER" ? [["sell", "Seller desk"], ["profile", "Your profile"]] : []),
      ...(state.user?.role === "BUYER" ? [["wishlist", "Saved cars"]] : []),
      ...(state.user?.role === "BUYER" ? [["orders", "Purchases"]] : []),
      ...(state.user?.role === "ADMIN" ? [["admin", "Admin desk"], ["profile", "Your profile"]] : []),
      ...(state.user && state.user.role !== "SELLER" && state.user.role !== "ADMIN" ? [["profile", "Your profile"]] : [])
    ];
    header.innerHTML = `<div class="header-inner">
      <a class="brand" href="${routeHref("browse")}" aria-label="Goodroad home">
        <span class="brand-mark">${icon.car}</span><span>goodroad<small>cars, described honestly</small></span>
      </a>
      <nav class="primary-nav" id="primary-nav" aria-label="Main navigation">
        ${links.map(([path, label]) => `<a href="${routeHref(path)}" ${active === path ? 'aria-current="page"' : ""}>${label}</a>`).join("")}
      </nav>
      <div class="header-account">
        <button class="icon-button theme-toggle" type="button" data-theme-toggle aria-label="Switch theme">${icon.sun}${icon.moon}</button>
        ${state.user
          ? `<a class="account-name" href="${routeHref("profile")}">${escapeHTML(state.user.fullName || state.user.email)}</a><button class="button button-outline button-small header-logout" type="button" data-action="logout">Sign out</button>`
          : `<a class="button button-outline button-small" href="${routeHref("login")}">Sign in</a><a class="button button-primary button-small" href="${routeHref("register")}">Join</a>`}
      </div>
      <button class="mobile-menu-toggle" type="button" data-action="toggle-menu" aria-label="Open navigation" aria-expanded="false" aria-controls="primary-nav">${icon.menu}</button>
    </div>`;
    footer.innerHTML = `<div class="footer-inner"><span>Goodroad is a straightforward place to meet your next car.</span><span class="footer-mark">A little more road, a little less guesswork.</span></div>`;
  }

  function loadingMarkup() {
    return `<section class="container page load-state" aria-label="Loading content">
      <div class="loading-skeleton" aria-hidden="true">
        <div class="skeleton-line skeleton-block" style="width:32%"></div>
        <div class="skeleton-line skeleton-block" style="width:75%;height:27px"></div>
        <div class="skeleton-line skeleton-block" style="width:51%"></div>
        <div class="skeleton-line skeleton-block" style="width:100%;height:85px;margin-top:25px"></div>
      </div>
    </section>`;
  }

  function renderError(message, retry = true) {
    appMain.innerHTML = `<section class="container page error-state" role="alert">
      <span class="eyebrow">A small detour</span><h2>We couldn't load this view.</h2>
      <p>${escapeHTML(message)}</p>
      <div class="field-actions" style="justify-content:center">${retry ? '<button class="button button-primary" type="button" data-action="retry">Try again</button>' : ""}<a class="button button-outline" href="${routeHref("browse")}">Back to browsing</a></div>
    </section>`;
  }

  function showInlineError(container, message, selector = "[data-form-error]") {
    const form = typeof container === "string" ? $(container) : container;
    if (!form) { toast(message, "error"); return; }
    let node = $(selector, form);
    if (!node) {
      node = document.createElement("div");
      node.className = "inline-error";
      node.setAttribute("role", "alert");
      form.prepend(node);
    }
    node.textContent = message;
    node.classList.remove("hidden");
  }

  async function ensureUser() {
    if (state.userLoaded) return state.user;
    try {
      state.user = await api("/api/users/me");
    } catch {
      state.user = null;
    }
    state.userLoaded = true;
    return state.user;
  }

  async function requireRole(role) {
    const user = await ensureUser();
    if (!user) {
      location.hash = "#login";
      return false;
    }
    if (role && user.role !== role) {
      appMain.innerHTML = `<section class="container page error-state"><span class="eyebrow">Not your road</span><h2>This area is for ${escapeHTML(titleCase(role))} accounts.</h2><p>Your signed-in account does not have access to this screen.</p><a class="button button-primary" href="${routeHref("browse")}">Return to browsing</a></section>`;
      return false;
    }
    return true;
  }

  function renderBrowseForm() {
    const f = state.filters;
    return `<form class="search-panel" id="browse-form" data-form="browse" aria-label="Search available cars">
      <div class="search-intro"><div><span class="eyebrow">Start your search</span><h2>Find a car that fits.</h2></div><p>Choose what matters most, then explore the details.</p></div>
      <div class="search-row">
        <div class="field search-query"><label for="filter-q">What are you looking for?</label><input class="input" id="filter-q" name="q" type="search" placeholder="e.g. Toyota Aqua, hybrid, 2019" value="${escapeHTML(f.q)}"></div>
        <div class="field"><label for="filter-min-price">Price from (Rs.)</label><input class="input" id="filter-min-price" name="minPrice" type="number" min="0" step="100" placeholder="Any" value="${escapeHTML(f.minPrice)}"></div>
        <div class="field"><label for="filter-max-price">Price to (Rs.)</label><input class="input" id="filter-max-price" name="maxPrice" type="number" min="0" step="100" placeholder="Any" value="${escapeHTML(f.maxPrice)}"></div>
        <div class="field search-sort"><label for="filter-sort">Sort by</label><select class="select" id="filter-sort" name="sort">
          ${[["newest","Recently listed"],["price_asc","Price: low to high"],["price_desc","Price: high to low"],["year_desc","Newest model year"],["mileage_asc","Lowest mileage"]].map(([v,l]) => `<option value="${v}" ${f.sort === v ? "selected" : ""}>${l}</option>`).join("")}
        </select></div>
        <button class="button button-primary search-submit" type="submit">${icon.search} Find a car</button>
      </div>
      <div class="search-more">
        <div class="field"><label for="filter-type">Body style</label><select class="select" id="filter-type" name="vehicleType">
          ${[["","Any style"],["SEDAN","Sedan"],["HATCHBACK","Hatchback"],["SUV","SUV"],["TRUCK","Truck"],["VAN","Van"],["OTHER","Other"]].map(([v,l]) => `<option value="${v}" ${f.vehicleType === v ? "selected" : ""}>${l}</option>`).join("")}
        </select></div>
        <div class="field"><label for="filter-min-year">Year from</label><input class="input" id="filter-min-year" name="minYear" type="number" min="1886" placeholder="Any year" value="${escapeHTML(f.minYear)}"></div>
        <div class="field"><label for="filter-max-year">Year to</label><input class="input" id="filter-max-year" name="maxYear" type="number" min="1886" placeholder="Any year" value="${escapeHTML(f.maxYear)}"></div>
        <div class="field"><label for="filter-min-mileage">Mileage from (km)</label><input class="input" id="filter-min-mileage" name="minMileage" type="number" min="0" placeholder="Any" value="${escapeHTML(f.minMileage)}"></div>
        <div class="field"><label for="filter-max-mileage">Mileage to (km)</label><input class="input" id="filter-max-mileage" name="maxMileage" type="number" min="0" placeholder="Any" value="${escapeHTML(f.maxMileage)}"></div>
      </div>
      <div class="search-lower"><span>Start with a few details. Refine when you need to.</span><button class="button button-outline button-small" type="button" data-action="clear-filters">Clear filters</button></div>
    </form>`;
  }

  async function renderBrowse(version) {
    renderHeader("browse");
    appMain.innerHTML = `<div class="container page">
      <section class="hero" aria-labelledby="browse-title">
        <div class="hero-copy"><span class="eyebrow">The road starts here</span>
          <h1 id="browse-title">Good cars.<br><em>Clear stories.</em></h1>
          <p class="lede">Browse cars from independent sellers, compare the details, and find one worth the drive.</p>
          <div class="hero-actions"><a class="button button-primary" href="#browse" data-action="scroll-inventory">Browse cars ${icon.arrow}</a>${state.user?.role === "SELLER" ? `<a class="button button-outline" href="${routeHref("sell")}">Manage your cars</a>` : ""}</div>
          <div class="hero-points"><span>${icon.search} Search the details</span><span>${icon.shield} Connect with sellers</span></div>
        </div>
        <div class="hero-art" aria-hidden="true"><div class="hero-art-label">A clearer path to your next car <span>↗</span></div><div class="hero-car">${heroCarIllustration()}</div></div>
      </section>
      ${renderBrowseForm()}
      <section id="inventory" aria-labelledby="inventory-title">
        <div class="inventory-head"><div><span class="eyebrow">Explore the marketplace</span><h2 id="inventory-title" style="margin:7px 0 0">Cars worth a closer look</h2></div><span class="inventory-count" id="inventory-count">Finding the right cars…</span></div>
        <div id="browse-results"><div class="car-grid">${Array.from({length: 6}, () => `<div class="skeleton-card" aria-hidden="true"><div class="skeleton-art skeleton-block"></div><div class="skeleton-lines"><div class="skeleton-line skeleton-block"></div><div class="skeleton-line short skeleton-block"></div><div class="skeleton-line skeleton-block"></div></div></div>`).join("")}</div></div>
      </section>
    </div>`;
    const params = new URLSearchParams();
    Object.entries(state.filters).forEach(([key, value]) => { if (value !== "" && value != null) params.set(key, value); });
    const query = params.toString();
    state.lastBrowseQuery = query;
    try {
      const cars = await api(`/api/cars${query ? `?${query}` : ""}`);
      if (version !== state.routeVersion || hashPath() !== "browse") return;
      if (state.user?.role === "BUYER" && state.wishlistIds.size === 0) {
        try {
          const saved = await api("/api/wishlist");
          state.wishlistIds = new Set(saved.map((item) => String(item.car?.id)).filter(Boolean));
        } catch { /* Public inventory remains available if saved items cannot be read. */ }
      }
      const results = $("#browse-results");
      const count = $("#inventory-count");
      if (!results || version !== state.routeVersion) return;
      count.textContent = `${cars.length} ${cars.length === 1 ? "listing" : "listings"}`;
      results.innerHTML = cars.length
        ? `<div class="car-grid">${cars.map(carCard).join("")}</div>`
        : `<div class="empty-state"><div class="empty-mark">${icon.search}</div><h3>No cars match just yet.</h3><p>Try widening the year, mileage or price range. The right car might be one filter away.</p><button class="button button-outline" type="button" data-action="clear-filters">Reset your search</button></div>`;
    } catch (error) {
      if (version !== state.routeVersion) return;
      $("#browse-results").innerHTML = `<div class="empty-state" role="alert"><div class="empty-mark">${icon.car}</div><h3>Inventory is taking a breather.</h3><p>${escapeHTML(error.message)}</p><button class="button button-primary" type="button" data-action="retry">Try again</button></div>`;
      $("#inventory-count").textContent = "Unavailable";
    }
  }

  async function renderListing(id, version) {
    renderHeader("browse");
    appMain.innerHTML = loadingMarkup();
    const [carResult, reviewsResult] = await Promise.allSettled([
      api(`/api/cars/${encodeURIComponent(id)}`),
      api(`/api/cars/${encodeURIComponent(id)}/reviews`)
    ]);
    if (version !== state.routeVersion) return;
    if (carResult.status === "rejected") { renderError(carResult.reason.message); return; }
    const car = carResult.value;
    const reviews = reviewsResult.status === "fulfilled" ? reviewsResult.value : [];
    let hasCompletedPurchase = false;
    let reviewLookupError = "";
    if (state.user?.role === "BUYER") {
      try {
        const orders = await api("/api/orders");
        hasCompletedPurchase = orders.some((order) => String(order.carId) === String(id) && order.status === "COMPLETED");
      } catch (error) { reviewLookupError = error.message; }
    }
    let saved = false;
    if (state.user?.role === "BUYER") {
      if (!state.wishlistIds.size) {
        try {
          const wishlist = await api("/api/wishlist");
          state.wishlistIds = new Set(wishlist.map((item) => String(item.car?.id)));
        } catch { /* Wishlist status is optional on a public listing. */ }
      }
      saved = state.wishlistIds.has(String(car.id));
    }
    if (version !== state.routeVersion) return;
    const canBuy = car.status === "ACTIVE" && state.user?.role === "BUYER";
    const purchaseActions = state.user?.role === "BUYER"
      ? `<button class="button button-primary button-block" type="button" data-action="create-order" data-car-id="${escapeHTML(car.id)}" ${canBuy ? "" : "disabled"}>${car.status === "ACTIVE" ? "Request to purchase" : "This car is not available"}</button>`
      : state.user
        ? `<p class="notice">Buying is available to buyer accounts.</p>`
        : `<a class="button button-primary button-block" href="${routeHref("login")}">Sign in to make an offer</a>`;
    const ownReview = state.user?.role === "BUYER" ? reviews.find((review) => String(review.buyerId) === String(state.user.id)) : null;
    const reviewSection = state.user?.role === "BUYER"
      ? hasCompletedPurchase
        ? ownReview
          ? `<div class="notice">You’ve shared your experience for this car. <a href="${routeHref("orders")}">Edit or remove your review in purchase history.</a></div>`
          : `<section class="review-form" aria-labelledby="review-title"><h3 id="review-title">Share your experience</h3>
            ${reviewLookupError ? `<div class="inline-error" role="alert">${escapeHTML(reviewLookupError)}</div>` : ""}
            <form data-form="review" data-car-id="${escapeHTML(car.id)}">
              <div class="rating-control" role="radiogroup" aria-label="Rating from one to five">
                ${[1,2,3,4,5].map((rating) => `<button type="button" data-action="rating" data-rating="${rating}" aria-label="${rating} out of 5" aria-pressed="${rating === 5}">${rating}</button>`).join("")}
              </div><input type="hidden" name="rating" value="5">
              <div class="field"><label for="review-comment">Your review</label><textarea class="textarea" id="review-comment" name="comment" maxlength="2000" required placeholder="What should another buyer know?"></textarea></div>
              <div class="field-actions"><button class="button button-primary" type="submit">Post review</button><span class="field-hint">Reviews are tied to completed purchases.</span></div>
              <div data-form-error class="inline-error hidden" role="alert"></div>
            </form>
          </section>`
        : `<div class="notice">Reviews are open to buyers after a completed purchase.</div>`
      : state.user ? `<div class="notice">Reviews are shared by verified buyers.</div>` : `<p class="muted" style="font-size:12px">Sign in as a buyer to leave a review after purchase.</p>`;
    appMain.innerHTML = `<div class="container page">
      <div class="page-title-row"><div><a class="eyebrow" href="${routeHref("browse")}">← Back to all cars</a><h1>${escapeHTML(car.year)} ${escapeHTML(car.brand)} ${escapeHTML(car.model)}</h1><p>Listed by ${escapeHTML(car.sellerName || "independent seller")} · Added ${dateText(car.createdAt)}</p></div>${statusPill(car.status)}</div>
      <div class="detail-layout">
        <div>
          ${galleryMarkup(car)}
          <div class="detail-facts">
            <div class="fact"><span>Model year</span><strong>${escapeHTML(car.year)}</strong></div>
            <div class="fact"><span>Mileage</span><strong>${number(car.mileage)} km</strong></div>
            <div class="fact"><span>Body style</span><strong>${escapeHTML(titleCase(car.vehicleType || "Other"))}</strong></div>
          </div>
          <section style="margin-top:34px"><span class="eyebrow">The details</span><h2 style="font-size:25px;margin:8px 0 0">A clear picture, in the seller's words.</h2><p class="detail-description">${escapeHTML(car.description || "The seller has not added a description yet.")}</p></section>
          <section class="admin-section" aria-labelledby="reviews-heading">
            <div class="section-heading"><div><span class="eyebrow">Buyer notes</span><h2 id="reviews-heading">Reviews <span class="muted" style="font-size:15px;font-family:var(--font-body);letter-spacing:0">(${reviews.length})</span></h2></div></div>
            ${reviewsResult.status === "rejected" ? `<div class="notice notice-warm" role="status">Reviews are not available right now: ${escapeHTML(reviewsResult.reason.message)}</div>` : ""}
            ${reviews.length ? `<div class="review-list">${reviews.map((review) => `<article class="review-card">
              <div class="review-head"><div><strong>${escapeHTML(review.buyerName || "Buyer")}</strong><span class="review-date">${dateText(review.createdAt)}</span></div><span class="stars" aria-label="${review.rating} out of 5">${Number(review.rating)} / 5</span></div>
              <p>${escapeHTML(review.comment)}</p>
              ${state.user?.role === "BUYER" && String(review.buyerId) === String(state.user.id) ? `<div class="field-actions"><button class="button button-outline button-small" type="button" data-action="edit-review" data-review-id="${escapeHTML(review.id)}" data-car-id="${escapeHTML(car.id)}">Edit your review</button><button class="button button-danger button-small" type="button" data-action="delete-review" data-review-id="${escapeHTML(review.id)}" data-car-id="${escapeHTML(car.id)}">Delete</button></div>` : ""}
            </article>`).join("")}</div>` : reviewsResult.status === "fulfilled" ? `<div class="empty-state"><div class="empty-mark">${icon.check}</div><h3>No reviews yet.</h3><p>When a buyer has a story to share, it will live here.</p></div>` : ""}
            <div style="margin-top:16px">${reviewSection}</div>
          </section>
        </div>
        <aside class="purchase-card" aria-label="Purchase details">
          <span class="eyebrow">Asking price</span><strong class="price">${money(car.price)}</strong>
          ${purchaseActions}
          ${state.user?.role === "BUYER" ? `<button class="button button-outline button-block" style="margin-top:9px" type="button" data-action="toggle-wishlist" data-car-id="${escapeHTML(car.id)}" aria-pressed="${saved}">${icon.heart}${saved ? "Remove from saved cars" : "Save this car"}</button>` : ""}
          <div class="seller-byline"><span class="seller-avatar">${escapeHTML(initials(car.sellerName || "Goodroad seller"))}</span><span><strong>${escapeHTML(car.sellerName || "Independent seller")}</strong><span>Seller on Goodroad</span></span></div>
          <p class="muted" style="font-size:11px;margin:0">A good decision starts with a good conversation. Review the details and ask the seller what matters to you.</p>
        </aside>
      </div>
    </div>`;
  }

  function authShell(title, heading, description, form, switchText) {
    appMain.innerHTML = `<div class="container auth-wrap">
      <section class="auth-story"><span class="eyebrow">Welcome to Goodroad</span><h1>${title}</h1><p class="lede">${description}</p>
        <div class="auth-aside-note">${icon.shield} <strong>Good decisions start with clear details.</strong> Our marketplace is built around listings you can actually understand.</div>
      </section>
      <section class="auth-card" aria-labelledby="auth-heading"><span class="eyebrow">Your next step</span><h2 id="auth-heading">${heading}</h2><p class="subline">A few details and you’re on your way.</p>${form}${switchText}</section>
    </div>`;
  }

  function renderLogin() {
    renderHeader("login");
    if (state.user) {
      appMain.innerHTML = `<div class="container page"><div class="empty-state"><div class="empty-mark">${icon.check}</div><h2>You're already signed in.</h2><p>Continue to the cars, or head to your account area.</p><a class="button button-primary" href="${routeHref(state.user.role === "SELLER" ? "sell" : state.user.role === "ADMIN" ? "admin" : "browse")}">Continue</a></div></div>`;
      return;
    }
    const form = `<form data-form="login"><div data-form-error class="inline-error hidden" role="alert"></div>
      <div class="field"><label for="login-email">Email address</label><input class="input" id="login-email" name="email" type="email" maxlength="254" autocomplete="email" required></div>
      <div class="field"><label for="login-password">Password</label><input class="input" id="login-password" name="password" type="password" maxlength="72" autocomplete="current-password" required></div>
      <button class="button button-primary button-block" type="submit">Sign in to Goodroad ${icon.arrow}</button></form>`;
    authShell("Find a car you can feel good about.", "Good to see you.", "Pick up where you left off—your saved cars, your account, and the next step toward a better drive.", form, `<p class="auth-switch">New around here? <a href="${routeHref("register")}">Create an account</a></p>`);
  }

  function renderRegister() {
    renderHeader("register");
    if (state.user) {
      appMain.innerHTML = `<div class="container page"><div class="empty-state"><div class="empty-mark">${icon.check}</div><h2>You already have a Goodroad account.</h2><p>Your marketplace is ready when you are.</p><a class="button button-primary" href="${routeHref("browse")}">Browse cars</a></div></div>`;
      return;
    }
    const form = `<form data-form="register"><div data-form-error class="inline-error hidden" role="alert"></div>
      <div class="field"><label for="register-name">Your name</label><input class="input" id="register-name" name="fullName" maxlength="120" autocomplete="name" required></div>
      <div class="field"><label for="register-email">Email address</label><input class="input" id="register-email" name="email" type="email" maxlength="254" autocomplete="email" required></div>
      <div class="field"><label for="register-phone">Phone <span class="muted">(optional)</span></label><input class="input" id="register-phone" name="phone" type="tel" maxlength="30" autocomplete="tel"></div>
      <div class="field"><label for="register-password">Password</label><input class="input" id="register-password" name="password" type="password" minlength="8" maxlength="72" autocomplete="new-password" required><div class="field-hint">Use at least 8 characters.</div></div>
      <div class="field"><span class="field-label" style="display:block;margin-bottom:6px;font-size:11px;font-weight:700">I’m here to</span><div class="role-choice">
        <label><input type="radio" name="role" value="BUYER" checked> Find a car</label><label><input type="radio" name="role" value="SELLER"> Sell a car</label>
      </div></div>
      <button class="button button-primary button-block" type="submit">Create your account ${icon.arrow}</button></form>`;
    authShell("A good car starts with a good conversation.", "Join Goodroad.", "Choose the road that fits you. Buyers can save and purchase cars; independent sellers can write thoughtful listings and manage orders.", form, `<p class="auth-switch">Already have an account? <a href="${routeHref("login")}">Sign in</a></p>`);
  }

  async function renderProfile(version) {
    renderHeader("profile");
    if (!await requireRole()) return;
    appMain.innerHTML = loadingMarkup();
    try {
      const user = await api("/api/users/me");
      if (version !== state.routeVersion) return;
      state.user = user;
      appMain.innerHTML = `<div class="container page">
        <div class="page-title-row"><div><span class="eyebrow">Your Goodroad account</span><h1>Account details</h1><p>Keep the essentials current, so your conversations stay simple.</p></div>${statusPill(user.active ? "ACTIVE" : "INACTIVE")}</div>
        <section class="profile-card">
          <div class="profile-summary"><div class="profile-monogram">${escapeHTML(initials(user.fullName))}</div><div><h2>${escapeHTML(user.fullName)}</h2><p>${escapeHTML(titleCase(user.role))} account · Member since ${dateText(user.createdAt)}</p></div></div>
          <form data-form="profile"><div data-form-error class="inline-error hidden" role="alert"></div>
            <div class="form-grid">
              <div class="field"><label for="profile-name">Full name</label><input class="input" id="profile-name" name="fullName" maxlength="120" value="${escapeHTML(user.fullName)}" required></div>
              <div class="field"><label for="profile-email">Email address</label><input class="input" id="profile-email" name="email" type="email" maxlength="254" value="${escapeHTML(user.email)}" required></div>
              <div class="field field-full"><label for="profile-phone">Phone <span class="muted">(optional)</span></label><input class="input" id="profile-phone" name="phone" type="tel" maxlength="30" value="${escapeHTML(user.phone || "")}"></div>
            </div>
            <div class="field-actions"><button class="button button-primary" type="submit">Save account details</button><span class="field-hint">Your email is used for signing in.</span></div>
          </form>
        </section>
      </div>`;
    } catch (error) {
      if (version === state.routeVersion) renderError(error.message);
    }
  }

  async function renderWishlist(version) {
    renderHeader("wishlist");
    if (!await requireRole("BUYER")) return;
    appMain.innerHTML = loadingMarkup();
    try {
      const items = await api("/api/wishlist");
      if (version !== state.routeVersion) return;
      state.wishlistIds = new Set(items.map((item) => String(item.car?.id)).filter(Boolean));
      appMain.innerHTML = `<div class="container page">
        <div class="page-title-row"><div><span class="eyebrow">Your shortlist</span><h1>Saved cars</h1><p>Keep the interesting ones close while you compare.</p></div><span class="tag">${items.length} saved</span></div>
        ${items.length ? `<div class="car-grid">${items.filter((item) => item.car).map((item) => carCard(item.car)).join("")}</div>` : `<div class="empty-state"><div class="empty-mark">${icon.heart}</div><h2>Your shortlist starts here.</h2><p>Save a car you want to think about. It’ll stay here while you compare the details.</p><a class="button button-primary" href="${routeHref("browse")}">Browse the inventory ${icon.arrow}</a></div>`}
      </div>`;
    } catch (error) {
      if (version === state.routeVersion) renderError(error.message);
    }
  }

  function reviewFormForOrder(order, review) {
    const editing = Boolean(review);
    const rating = review?.rating || 5;
    return `<div class="review-form" style="margin-top:13px">
      <h3>${editing ? "Your review" : "Tell other buyers how it went"}</h3>
      <form data-form="order-review" data-car-id="${escapeHTML(order.carId)}" data-review-id="${escapeHTML(review?.id || "")}">
        <div data-form-error class="inline-error hidden" role="alert"></div>
        <div class="rating-control" role="radiogroup" aria-label="Rating from one to five">${[1,2,3,4,5].map((value) => `<button type="button" class="${value <= rating ? "selected" : ""}" data-action="rating" data-rating="${value}" aria-label="${value} out of 5" aria-pressed="${value === rating}">${value}</button>`).join("")}</div>
        <input type="hidden" name="rating" value="${rating}">
        <div class="field"><label for="order-review-${escapeHTML(order.id)}">Your review</label><textarea class="textarea" id="order-review-${escapeHTML(order.id)}" name="comment" maxlength="2000" required placeholder="What should the next buyer know?">${escapeHTML(review?.comment || "")}</textarea></div>
        <div class="field-actions"><button class="button button-primary button-small" type="submit">${editing ? "Update review" : "Post review"}</button>${editing ? `<button class="button button-danger button-small" type="button" data-action="delete-review" data-review-id="${escapeHTML(review.id)}" data-car-id="${escapeHTML(order.carId)}">Delete review</button>` : ""}<a class="button button-outline button-small" href="${routeHref(`listing/${encodeURIComponent(order.carId)}`)}">View car</a></div>
      </form>
    </div>`;
  }

  async function renderOrders(version) {
    renderHeader("orders");
    if (!await requireRole("BUYER")) return;
    appMain.innerHTML = loadingMarkup();
    try {
      const orders = await api("/api/orders");
      if (version !== state.routeVersion) return;
      const completed = orders.filter((order) => order.status === "COMPLETED");
      const reviewsByOrder = {};
      await Promise.all(completed.map(async (order) => {
        try {
          const reviews = await api(`/api/cars/${encodeURIComponent(order.carId)}/reviews`);
          reviewsByOrder[order.id] = reviews.find((review) => String(review.buyerId) === String(state.user.id)) || null;
        } catch { reviewsByOrder[order.id] = null; }
      }));
      if (version !== state.routeVersion) return;
      state.completedReviews = reviewsByOrder;
      appMain.innerHTML = `<div class="container page">
        <div class="page-title-row"><div><span class="eyebrow">Your buying journey</span><h1>Purchase history</h1><p>Every request and completed handover, in one place.</p></div><span class="tag">${orders.length} ${orders.length === 1 ? "order" : "orders"}</span></div>
        ${orders.length ? `<div class="order-list">${orders.map((order) => `<article class="order-card">
          <div><span class="mono muted">Order ${escapeHTML(String(order.id).slice(0, 10))}</span><h3>${escapeHTML(order.carLabel || "Vehicle")}</h3><p>From ${escapeHTML(order.sellerName || "seller")} · ${dateText(order.createdAt)}</p>
            <div class="order-meta"><span>${money(order.amount)}</span><span>Updated ${dateText(order.updatedAt)}</span>${statusPill(order.status)}</div>
          </div>
          <div class="order-actions">
            ${order.status === "PENDING" ? `<button class="button button-outline button-small" type="button" data-action="order-status" data-order-id="${escapeHTML(order.id)}" data-status="CANCELLED">Cancel request</button>` : ""}
          </div>
          ${order.status === "COMPLETED" ? `<div class="field-full">${reviewFormForOrder(order, reviewsByOrder[order.id])}</div>` : ""}
        </article>`).join("")}</div>` : `<div class="empty-state"><div class="empty-mark">${icon.car}</div><h2>No purchase requests yet.</h2><p>When a car catches your eye, send the seller a purchase request. Your order updates will appear here.</p><a class="button button-primary" href="${routeHref("browse")}">Find a car ${icon.arrow}</a></div>`}
      </div>`;
    } catch (error) {
      if (version === state.routeVersion) renderError(error.message);
    }
  }

  function carForm(car = null) {
    const c = car || {};
    const editing = Boolean(c.id);
    return `<form class="panel-card" data-form="car" data-car-id="${escapeHTML(c.id || "")}">
      <div data-form-error class="inline-error hidden" role="alert"></div>
      <div class="section-heading"><div><span class="eyebrow">${editing ? "Update a listing" : "Make a thoughtful listing"}</span><h2>${editing ? "Edit your car" : "Add a car"}</h2><p>Specific details make for a better first conversation.</p></div><button class="icon-button" type="button" data-action="close-car-form" aria-label="Close listing form">${icon.close}</button></div>
      <div class="form-grid">
        <div class="field"><label for="car-brand">Make</label><input class="input" id="car-brand" name="brand" maxlength="80" value="${escapeHTML(c.brand || "")}" required></div>
        <div class="field"><label for="car-model">Model</label><input class="input" id="car-model" name="model" maxlength="100" value="${escapeHTML(c.model || "")}" required></div>
        <div class="field"><label for="car-type">Body style</label><select class="select" id="car-type" name="vehicleType" required>${["SEDAN","SUV","TRUCK","HATCHBACK","VAN","OTHER"].map((type) => `<option value="${type}" ${c.vehicleType === type ? "selected" : ""}>${titleCase(type)}</option>`).join("")}</select></div>
        <div class="field"><label for="car-year">Year</label><input class="input" id="car-year" name="year" type="number" min="1886" max="${new Date().getFullYear() + 1}" value="${escapeHTML(c.year || "")}" required></div>
        <div class="field"><label for="car-mileage">Mileage (km)</label><input class="input" id="car-mileage" name="mileage" type="number" min="0" value="${escapeHTML(c.mileage ?? "")}" required></div>
        <div class="field"><label for="car-price">Asking price (Rs.)</label><input class="input" id="car-price" name="price" type="number" min="0.01" step="0.01" value="${escapeHTML(c.price ?? "")}" required></div>
        <div class="field field-full"><label for="car-description">Description</label><textarea class="textarea" id="car-description" name="description" maxlength="2000" required placeholder="Tell buyers what is worth knowing: condition, recent work, and what you appreciate about the car.">${escapeHTML(c.description || "")}</textarea><div class="field-hint">Up to 2,000 characters.</div></div>
        <div class="field field-full photo-field">
          <label for="car-photos">Photos</label>
          <div class="photo-grid" id="photo-grid">${photoGridMarkup()}</div>
          <input class="visually-hidden" id="car-photos" type="file" accept="image/jpeg,image/png,image/webp" multiple>
          <label class="photo-drop" for="car-photos">${icon.plus}<span>Add photos</span></label>
          <div class="field-hint"><span id="photo-count">${photoCount()} of ${MAX_PHOTOS} photos</span> · JPG, PNG or WebP, up to 5 MB each. The first photo is the cover.</div>
        </div>
      </div>
      <div class="field-actions"><button class="button button-primary" type="submit">${editing ? "Save listing" : "Publish listing"}</button><button class="button button-outline" type="button" data-action="close-car-form">Keep browsing your inventory</button></div>
    </form>`;
  }

  function sellerCarRow(car) {
    const canManage = car.status === "ACTIVE";
    return `<tr><td><div class="row-vehicle">${car.images?.length ? `<img class="row-thumb" src="${escapeHTML(car.images[0])}" alt="">` : ""}<div><strong>${escapeHTML(car.year)} ${escapeHTML(car.brand)} ${escapeHTML(car.model)}</strong><span class="subtext">${number(car.mileage)} km · ${titleCase(car.vehicleType)}</span></div></div></td><td>${money(car.price)}</td><td>${statusPill(car.status)}</td><td>${dateText(car.updatedAt)}</td><td><div class="table-actions">
      ${canManage ? `<button class="button button-outline button-small" type="button" data-action="edit-car" data-car-id="${escapeHTML(car.id)}">Edit</button>
      <button class="button button-danger button-small" type="button" data-action="delete-car" data-car-id="${escapeHTML(car.id)}">Remove</button>
      <a class="button button-outline button-small" href="${routeHref(`listing/${encodeURIComponent(car.id)}`)}">View</a>` : `<span class="muted">No actions available</span>`}
    </div></td></tr>`;
  }

  async function renderSell(version) {
    renderHeader("sell");
    if (!await requireRole("SELLER")) return;
    appMain.innerHTML = loadingMarkup();
    const [carsResult, ordersResult] = await Promise.allSettled([api("/api/users/me/cars"), api("/api/orders")]);
    if (version !== state.routeVersion) return;
    if (carsResult.status === "rejected") { renderError(carsResult.reason.message); return; }
    const cars = carsResult.value;
    const orders = ordersResult.status === "fulfilled" ? ordersResult.value : [];
    const openOrders = orders.filter((order) => order.status === "PENDING" || order.status === "APPROVED");
    appMain.innerHTML = `<div class="container page">
      <div class="page-title-row"><div><span class="eyebrow">Independent seller desk</span><h1>Your cars, your conversations.</h1><p>Keep listings clear and move purchase requests along at your pace.</p></div><button class="button button-primary" type="button" data-action="new-car">${icon.plus} Add a car</button></div>
      ${state.listingDraft !== null ? `<div id="car-form-wrap" style="margin-bottom:28px">${carForm(state.listingDraft || null)}</div>` : ""}
      <section aria-labelledby="seller-inventory"><div class="section-heading"><div><span class="eyebrow">Your inventory</span><h2 id="seller-inventory">Active and past listings</h2><p>${cars.length} cars in your inventory</p></div></div>
        ${cars.length ? `<div class="table-wrap"><table><thead><tr><th>Vehicle</th><th>Price</th><th>Status</th><th>Updated</th><th>Actions</th></tr></thead><tbody>${cars.map(sellerCarRow).join("")}</tbody></table></div>` : `<div class="empty-state"><div class="empty-mark">${icon.car}</div><h3>Your seller story starts here.</h3><p>Make your first listing a good one. Clear details help the right buyer find you.</p><button class="button button-primary" type="button" data-action="new-car">Create your first listing ${icon.arrow}</button></div>`}
      </section>
      <section class="admin-section" aria-labelledby="seller-orders"><div class="section-heading"><div><span class="eyebrow">Buyer activity</span><h2 id="seller-orders">Purchase requests</h2><p>Approve a pending request, then mark the handover complete.</p></div><span class="tag">${openOrders.length} open</span></div>
        ${ordersResult.status === "rejected" ? `<div class="inline-error" role="alert">${escapeHTML(ordersResult.reason.message)}</div>` : ""}
        ${orders.length ? `<div class="order-list">${orders.map((order) => `<article class="order-card"><div><span class="mono muted">Order ${escapeHTML(String(order.id).slice(0,10))}</span><h3>${escapeHTML(order.carLabel || "Vehicle")}</h3><p>Buyer ${escapeHTML(order.buyerName || "Goodroad buyer")} · ${dateText(order.createdAt)}</p><div class="order-meta"><span>${money(order.amount)}</span><span>${dateText(order.updatedAt)}</span>${statusPill(order.status)}</div></div>
          <div class="order-actions">
            ${order.status === "PENDING" ? `<button class="button button-primary button-small" type="button" data-action="order-status" data-order-id="${escapeHTML(order.id)}" data-status="APPROVED">Approve</button><button class="button button-danger button-small" type="button" data-action="order-status" data-order-id="${escapeHTML(order.id)}" data-status="CANCELLED">Decline</button>` : ""}
            ${order.status === "APPROVED" ? `<button class="button button-primary button-small" type="button" data-action="order-status" data-order-id="${escapeHTML(order.id)}" data-status="COMPLETED">Mark complete</button><button class="button button-danger button-small" type="button" data-action="order-status" data-order-id="${escapeHTML(order.id)}" data-status="CANCELLED">Cancel order</button>` : ""}
          </div></article>`).join("")}</div>` : ordersResult.status === "fulfilled" ? `<div class="empty-state"><div class="empty-mark">${icon.check}</div><h3>No purchase requests yet.</h3><p>When a buyer requests one of your cars, their order will be ready for you here.</p></div>` : ""}
      </section>
    </div>`;
  }

  function adminUsersTable(allUsers) {
    const users = allUsers.filter((user) => user.role !== "ADMIN");
    return users.length ? `<div class="table-wrap"><table><thead><tr><th>Account</th><th>Role</th><th>Joined</th><th>Status</th><th>Action</th></tr></thead><tbody>${users.map((user) => `<tr>
      <td><strong>${escapeHTML(user.fullName)}</strong><span class="subtext">${escapeHTML(user.email)}</span></td><td>${escapeHTML(titleCase(user.role))}</td><td>${dateText(user.createdAt)}</td><td>${statusPill(user.active ? "ACTIVE" : "INACTIVE")}</td>
      <td>${user.active ? `<button class="button button-danger button-small" type="button" data-action="deactivate-user" data-user-id="${escapeHTML(user.id)}">Deactivate</button>` : `<span class="muted">Deactivated</span>`}</td>
    </tr>`).join("")}</tbody></table></div>` : `<div class="table-empty">No customer accounts to show.</div>`;
  }

  function adminsTable(admins) {
    return admins.length ? `<div class="table-wrap"><table><thead><tr><th>Administrator</th><th>Joined</th><th>Action</th></tr></thead><tbody>${admins.map((user) => `<tr><td><strong>${escapeHTML(user.fullName)}</strong><span class="subtext">${escapeHTML(user.email)}</span></td><td>${dateText(user.createdAt)}</td><td><button class="button button-danger button-small" type="button" data-action="delete-admin" data-admin-id="${escapeHTML(user.id)}">Remove access</button></td></tr>`).join("")}</tbody></table></div>` : `<div class="table-empty">No administrator accounts found.</div>`;
  }

  function adminReviewsTable(reviews) {
    return reviews.length ? `<div class="table-wrap"><table><thead><tr><th>Review</th><th>Rating</th><th>Status</th><th>Submitted</th><th>Moderation</th></tr></thead><tbody>${reviews.map((review) => `<tr><td><strong>${escapeHTML(review.buyerName || "Buyer")}</strong><span class="subtext">${escapeHTML(String(review.comment).slice(0,125))}${String(review.comment).length > 125 ? "…" : ""}</span></td><td>${Number(review.rating)} / 5</td><td>${statusPill(review.status)}</td><td>${dateText(review.createdAt)}</td><td><button class="button ${review.status === "PUBLISHED" ? "button-danger" : "button-soft"} button-small" type="button" data-action="moderate-review" data-review-id="${escapeHTML(review.id)}" data-status="${review.status === "PUBLISHED" ? "HIDDEN" : "PUBLISHED"}">${review.status === "PUBLISHED" ? "Hide review" : "Publish review"}</button></td></tr>`).join("")}</tbody></table></div>` : `<div class="table-empty">No reviews are available for moderation.</div>`;
  }

  async function renderAdmin(version) {
    renderHeader("admin");
    if (!await requireRole("ADMIN")) return;
    appMain.innerHTML = loadingMarkup();
    const resources = await Promise.allSettled([
      api("/api/admin/overview"), api("/api/admin/users"), api("/api/admin/admins"), api("/api/admin/reviews"), api("/api/admin/audit")
    ]);
    if (version !== state.routeVersion) return;
    const [overviewR, usersR, adminsR, reviewsR, auditR] = resources;
    if (overviewR.status === "rejected") { renderError(overviewR.reason.message); return; }
    const overview = overviewR.value;
    const users = usersR.status === "fulfilled" ? usersR.value : [];
    const admins = adminsR.status === "fulfilled" ? adminsR.value : [];
    const reviews = reviewsR.status === "fulfilled" ? reviewsR.value : [];
    const audit = auditR.status === "fulfilled" ? auditR.value : [];
    const stat = (label, value, detail) => `<div class="stat-card"><span>${label}</span><strong>${number(value)}</strong><small>${detail}</small></div>`;
    const adminForm = `<form class="panel-card" data-form="admin-create"><span class="eyebrow">Trusted access</span><h2 style="margin:8px 0 17px;font-size:21px">Add an administrator</h2><div data-form-error class="inline-error hidden" role="alert"></div>
      <div class="field"><label for="admin-name">Full name</label><input class="input" id="admin-name" name="fullName" maxlength="120" required></div>
      <div class="field"><label for="admin-email">Email</label><input class="input" id="admin-email" name="email" type="email" maxlength="254" required></div>
      <div class="field"><label for="admin-phone">Phone <span class="muted">(optional)</span></label><input class="input" id="admin-phone" name="phone" maxlength="30"></div>
      <div class="field"><label for="admin-password">Temporary password</label><input class="input" id="admin-password" name="password" type="password" minlength="12" maxlength="72" required><div class="field-hint">At least 12 characters.</div></div>
      <button class="button button-primary button-block" type="submit">Create administrator</button>
    </form>`;
    appMain.innerHTML = `<div class="container page">
      <div class="page-title-row"><div><span class="eyebrow">Goodroad operations</span><h1>Marketplace overview</h1><p>Accounts, activity, reviews and the history behind them.</p></div><span class="tag">Administrator</span></div>
      <section class="stats-grid" aria-label="Marketplace metrics">
        ${stat("Total accounts", overview.totalUsers, `${number(overview.activeUsers)} active`)}
        ${stat("Car listings", overview.totalCars, `${number(overview.activeCars)} active`)}
        ${stat("Transactions", overview.totalTransactions, `${number(overview.pendingTransactions)} pending`)}
        ${stat("Buyer reviews", overview.totalReviews, `${number(overview.publishedReviews)} published`)}
      </section>
      ${usersR.status === "rejected" ? `<div class="inline-error" role="alert">Accounts: ${escapeHTML(usersR.reason.message)}</div>` : ""}
      ${adminsR.status === "rejected" ? `<div class="inline-error" role="alert">Administrator accounts: ${escapeHTML(adminsR.reason.message)}</div>` : ""}
      ${reviewsR.status === "rejected" ? `<div class="inline-error" role="alert">Review queue: ${escapeHTML(reviewsR.reason.message)}</div>` : ""}
      ${auditR.status === "rejected" ? `<div class="inline-error" role="alert">Audit history: ${escapeHTML(auditR.reason.message)}</div>` : ""}
      <div class="management-grid">
        <section aria-labelledby="accounts-title"><div class="section-heading"><div><span class="eyebrow">Community</span><h2 id="accounts-title">Buyer and seller accounts</h2></div></div>${adminUsersTable(users)}</section>
        ${adminForm}
      </div>
      <section class="admin-section" aria-labelledby="admin-list-title"><div class="section-heading"><div><span class="eyebrow">Stewardship</span><h2 id="admin-list-title">Administrator accounts</h2></div></div>${adminsTable(admins)}</section>
      <section class="admin-section" aria-labelledby="moderation-title"><div class="section-heading"><div><span class="eyebrow">Community standards</span><h2 id="moderation-title">Review moderation</h2><p>Hide a review from public listings or publish it again.</p></div></div>${adminReviewsTable(reviews)}</section>
      <section class="admin-section" aria-labelledby="audit-title"><div class="section-heading"><div><span class="eyebrow">The paper trail</span><h2 id="audit-title">Audit history</h2><p>Recent actions recorded across the marketplace.</p></div></div>
        ${audit.length ? `<div class="audit-feed">${audit.map((event) => `<article class="audit-row"><time class="audit-time" datetime="${escapeHTML(event.createdAt)}">${dateText(event.createdAt, {month:"short",day:"numeric",year:"numeric"})}<br>${dateText(event.createdAt,{hour:"numeric",minute:"2-digit"})}</time><div><strong>${escapeHTML(event.actorName || "Marketplace")} · ${escapeHTML(titleCase(event.action))}</strong><p>${escapeHTML(event.details || `${event.targetType || "Record"} ${event.targetId || ""}`)}</p></div></article>`).join("")}</div>` : `<div class="empty-state"><h3>No recorded actions.</h3><p>Audit events will appear here as the marketplace is used.</p></div>`}
      </section>
    </div>`;
  }

  async function renderRoute() {
    const version = ++state.routeVersion;
    const path = hashPath();
    state.routeKey = path;
    if (path === "browse") {
      await ensureUser();
      if (version === state.routeVersion) renderBrowse(version);
      return;
    }
    if (path === "login") { await ensureUser(); if (version === state.routeVersion) renderLogin(); return; }
    if (path === "register") { await ensureUser(); if (version === state.routeVersion) renderRegister(); return; }
    if (path === "profile") { await renderProfile(version); return; }
    if (path === "wishlist") { await renderWishlist(version); return; }
    if (path === "orders") { await renderOrders(version); return; }
    if (path === "sell") { await renderSell(version); return; }
    if (path === "admin") { await renderAdmin(version); return; }
    const listingMatch = path.match(/^listing\/(.+)$/);
    if (listingMatch) { await ensureUser(); if (version === state.routeVersion) await renderListing(decodeURIComponent(listingMatch[1]), version); return; }
    renderHeader("");
    appMain.innerHTML = `<section class="container page error-state"><span class="eyebrow">Wrong turn</span><h2>That page isn’t on this map.</h2><p>The address may have changed. You can find your way back to the current inventory.</p><a class="button button-primary" href="${routeHref("browse")}">Browse cars</a></section>`;
  }

  function formValues(form) {
    return Object.fromEntries(new FormData(form).entries());
  }

  function setBusy(form, busy, label = "Working…") {
    const submit = $('button[type="submit"]', form);
    if (!submit) return;
    if (busy) {
      submit.dataset.originalLabel = submit.textContent.trim();
      submit.disabled = true;
      submit.setAttribute("aria-busy", "true");
      submit.textContent = label;
    } else {
      submit.disabled = false;
      submit.removeAttribute("aria-busy");
      if (submit.dataset.originalLabel) submit.textContent = submit.dataset.originalLabel;
    }
  }

  async function handleSubmit(event) {
    const form = event.target.closest("form[data-form]");
    if (!form) return;
    event.preventDefault();
    const kind = form.dataset.form;
    const values = formValues(form);
    setBusy(form, true);
    const fail = (error) => showInlineError(form, error.message || String(error));
    try {
      if (kind === "browse") {
        state.filters = Object.fromEntries(["q","vehicleType","minPrice","maxPrice","minYear","maxYear","minMileage","maxMileage","sort"].map((key) => [key, String(values[key] || "").trim()]));
        await renderRoute();
      } else if (kind === "login") {
        const user = await api("/api/auth/login", { method: "POST", body: { email: values.email.trim(), password: values.password } });
        state.user = user;
        state.userLoaded = true;
        await getCsrf();
        toast(`Welcome back, ${user.fullName || "you"}.`);
        location.hash = user.role === "SELLER" ? "#sell" : user.role === "ADMIN" ? "#admin" : "#browse";
      } else if (kind === "register") {
        await api("/api/auth/register", { method: "POST", body: { email: values.email.trim(), password: values.password, fullName: values.fullName.trim(), phone: values.phone.trim(), role: values.role } });
        toast("Your account is ready. Sign in to continue.");
        location.hash = "#login";
      } else if (kind === "profile") {
        const user = await api("/api/users/me", { method: "PUT", body: { email: values.email.trim(), fullName: values.fullName.trim(), phone: values.phone.trim() } });
        state.user = user;
        renderHeader("profile");
        toast("Your account details are up to date.");
        await renderRoute();
      } else if (kind === "car") {
        const payload = {
          brand: values.brand.trim(), model: values.model.trim(), vehicleType: values.vehicleType,
          year: Number(values.year), mileage: Number(values.mileage), price: Number(values.price), description: values.description.trim()
        };
        const carId = form.dataset.carId;
        const saved = await api(carId ? `/api/cars/${encodeURIComponent(carId)}` : "/api/cars", { method: carId ? "PUT" : "POST", body: payload });
        const targetId = saved?.id || carId;
        const draft = state.photoDraft;
        let photoProblem = "";
        if (draft && targetId) {
          try {
            // Remove first so the six-photo limit is checked against what will remain.
            for (const fileName of draft.removed) {
              await api(`/api/cars/${encodeURIComponent(targetId)}/images/${encodeURIComponent(fileName)}`, { method: "DELETE" });
            }
            if (draft.added.length) {
              const upload = new FormData();
              draft.added.forEach((item) => upload.append("files", item.file));
              await api(`/api/cars/${encodeURIComponent(targetId)}/images`, { method: "POST", body: upload });
            }
          } catch (photoError) {
            photoProblem = photoError.message;
          }
        }
        clearPhotoDraft();
        state.listingDraft = null;
        if (photoProblem) toast(`Your listing was saved, but the photos were not: ${photoProblem}`, "error");
        else toast(carId ? "Your listing has been updated." : "Your car is now listed.");
        await renderRoute();
      } else if (kind === "review" || kind === "order-review") {
        const reviewId = form.dataset.reviewId;
        const carId = form.dataset.carId;
        const payload = { rating: Number(values.rating), comment: values.comment.trim() };
        await api(reviewId ? `/api/reviews/${encodeURIComponent(reviewId)}` : `/api/cars/${encodeURIComponent(carId)}/reviews`, { method: reviewId ? "PUT" : "POST", body: payload });
        toast(reviewId ? "Your review has been updated." : "Thanks for sharing your experience.");
        await renderRoute();
      } else if (kind === "admin-create") {
        await api("/api/admin/admins", { method: "POST", body: { email: values.email.trim(), password: values.password, fullName: values.fullName.trim(), phone: values.phone.trim() } });
        toast("Administrator account created.");
        await renderRoute();
      }
    } catch (error) {
      fail(error);
    } finally {
      if (form.isConnected) setBusy(form, false);
    }
  }

  async function toggleWishlist(carId) {
    if (!state.user) {
      location.hash = "#login";
      return;
    }
    if (state.user.role !== "BUYER") {
      toast("Saved cars are available to buyer accounts.", "error");
      return;
    }
    const saved = state.wishlistIds.has(String(carId));
    try {
      await api(`/api/wishlist/${encodeURIComponent(carId)}`, { method: saved ? "DELETE" : "POST" });
      if (saved) state.wishlistIds.delete(String(carId)); else state.wishlistIds.add(String(carId));
      toast(saved ? "Removed from your saved cars." : "Added to your saved cars.");
      await renderRoute();
    } catch (error) { toast(error.message, "error"); }
  }

  async function updateOrder(orderId, status) {
    const verb = status === "CANCELLED" ? "cancel this order" : status === "COMPLETED" ? "mark this purchase complete" : "approve this request";
    if (!window.confirm(`Are you sure you want to ${verb}?`)) return;
    try {
      await api(`/api/orders/${encodeURIComponent(orderId)}/status`, { method: "PATCH", body: { status } });
      toast(`Order marked ${titleCase(status).toLowerCase()}.`);
      await renderRoute();
    } catch (error) { toast(error.message, "error"); }
  }

  async function handleClick(event) {
    const action = event.target.closest("[data-action]");
    if (!action) return;
    const name = action.dataset.action;
    if (name === "remove-photo") {
      removePhoto(action.dataset.kind, action.dataset.key);
      return;
    }
    if (name === "gallery-pick") {
      const main = $("#gallery-main");
      if (main) main.src = action.dataset.src;
      $$(".gallery-thumb").forEach((thumb) => thumb.setAttribute("aria-pressed", String(thumb === action)));
      return;
    }
    if (name === "scroll-inventory") {
      event.preventDefault();
      const target = $("#inventory");
      if (target) target.scrollIntoView({ behavior: "smooth", block: "start" });
      return;
    }
    if (name === "toggle-menu") {
      const nav = $("#primary-nav");
      const open = nav.classList.toggle("is-open");
      action.setAttribute("aria-expanded", String(open));
      action.setAttribute("aria-label", open ? "Close navigation" : "Open navigation");
      action.innerHTML = open ? icon.close : icon.menu;
    } else if (name === "toggle-wishlist") {
      await toggleWishlist(action.dataset.carId);
    } else if (name === "clear-filters") {
      state.filters = { q: "", vehicleType: "", minPrice: "", maxPrice: "", minYear: "", maxYear: "", minMileage: "", maxMileage: "", sort: "newest" };
      if (hashPath() !== "browse") location.hash = "#browse"; else await renderRoute();
    } else if (name === "retry") {
      await renderRoute();
    } else if (name === "logout") {
      try {
        await api("/api/auth/logout", { method: "POST" });
        state.user = null;
        state.userLoaded = true;
        state.wishlistIds.clear();
        state.csrfToken = null;
        toast("You’re signed out.");
        location.hash = "#browse";
        await renderRoute();
      } catch (error) { toast(error.message, "error"); }
    } else if (name === "create-order") {
      if (!window.confirm("Send a purchase request for this car?")) return;
      try {
        await api("/api/orders", { method: "POST", body: { carId: action.dataset.carId } });
        toast("Your purchase request has been sent.");
        location.hash = "#orders";
      } catch (error) { toast(error.message, "error"); }
    } else if (name === "order-status") {
      await updateOrder(action.dataset.orderId, action.dataset.status);
    } else if (name === "new-car") {
      state.listingDraft = {};
      startPhotoDraft();
      await renderRoute();
      $("#car-brand")?.focus();
    } else if (name === "close-car-form") {
      clearPhotoDraft();
      state.listingDraft = null;
      await renderRoute();
    } else if (name === "edit-car") {
      try {
        const cars = await api("/api/users/me/cars");
        state.listingDraft = cars.find((car) => String(car.id) === String(action.dataset.carId)) || null;
        if (!state.listingDraft) { toast("That listing could not be found.", "error"); return; }
        startPhotoDraft(state.listingDraft);
        await renderRoute();
        $("#car-brand")?.focus();
      } catch (error) { toast(error.message, "error"); }
    } else if (name === "delete-car") {
      if (!window.confirm("Remove this car listing? This action cannot be undone.")) return;
      try {
        await api(`/api/cars/${encodeURIComponent(action.dataset.carId)}`, { method: "DELETE" });
        toast("The listing has been removed.");
        await renderRoute();
      } catch (error) { toast(error.message, "error"); }
    } else if (name === "edit-review") {
      const reviewId = action.dataset.reviewId;
      const review = $(`[data-review-id="${CSS.escape(reviewId)}"]`, appMain);
      if (!review) return;
      toast("Edit your review in purchase history.");
      location.hash = "#orders";
      await renderRoute();
    } else if (name === "delete-review") {
      if (!window.confirm("Delete this review?")) return;
      try {
        await api(`/api/reviews/${encodeURIComponent(action.dataset.reviewId)}`, { method: "DELETE" });
        toast("Your review has been deleted.");
        await renderRoute();
      } catch (error) { toast(error.message, "error"); }
    } else if (name === "deactivate-user") {
      if (!window.confirm("Deactivate this account? The account will no longer be active.")) return;
      try {
        await api(`/api/admin/users/${encodeURIComponent(action.dataset.userId)}`, { method: "DELETE" });
        toast("Account deactivated.");
        await renderRoute();
      } catch (error) { toast(error.message, "error"); }
    } else if (name === "delete-admin") {
      if (!window.confirm("Remove this administrator's access?")) return;
      try {
        await api(`/api/admin/admins/${encodeURIComponent(action.dataset.adminId)}`, { method: "DELETE" });
        toast("Administrator access removed.");
        await renderRoute();
      } catch (error) { toast(error.message, "error"); }
    } else if (name === "moderate-review") {
      try {
        await api(`/api/admin/reviews/${encodeURIComponent(action.dataset.reviewId)}`, { method: "PATCH", body: { status: action.dataset.status } });
        toast(`Review ${action.dataset.status === "HIDDEN" ? "hidden" : "published"}.`);
        await renderRoute();
      } catch (error) { toast(error.message, "error"); }
    } else if (name === "rating") {
      const form = action.closest("form");
      const value = Number(action.dataset.rating);
      const input = $('input[name="rating"]', form);
      if (input) input.value = String(value);
      $$(".rating-control button", form).forEach((button) => {
        const selected = Number(button.dataset.rating) <= value;
        button.classList.toggle("selected", selected);
        button.setAttribute("aria-pressed", String(Number(button.dataset.rating) === value));
      });
    }
  }

  function $$(selector, parent = document) { return Array.from(parent.querySelectorAll(selector)); }

  function handleChange(event) {
    if (event.target.matches("#car-photos")) {
      addPhotoFiles(Array.from(event.target.files || []));
      event.target.value = "";
      return;
    }
    if (event.target.matches("#filter-sort, #filter-type")) {
      const form = $("#browse-form");
      if (form) form.requestSubmit();
    }
  }

  function initialize() {
    document.addEventListener("click", handleClick);
    document.addEventListener("submit", handleSubmit);
    document.addEventListener("change", handleChange);
    $(".skip-link").addEventListener("click", (event) => {
      event.preventDefault();
      appMain.focus();
      appMain.scrollIntoView({ behavior: "smooth", block: "start" });
    });
    window.addEventListener("hashchange", renderRoute);
    if (!location.hash) history.replaceState(null, "", `${location.pathname}${location.search}#browse`);
    renderRoute();
  }

  initialize();
})();
