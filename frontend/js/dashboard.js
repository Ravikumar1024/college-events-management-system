const API = window.CAMPUS_API_URL;
const token = localStorage.getItem("campusToken");
const account = JSON.parse(localStorage.getItem("campusUser") || "null");
const roles = Array.isArray(account?.roles) ? account.roles : [];
const authHeaders = () => ({ Authorization: `Bearer ${token}` });
const displayDate = value => new Intl.DateTimeFormat(undefined, {
  weekday: "short", month: "short", day: "numeric", hour: "numeric", minute: "2-digit"
}).format(new Date(value));
const dashboardRoles = [
  { role: "ROLE_STUDENT", label: "Student" },
  { role: "ROLE_ORGANIZER", label: "Organizer" },
  { role: "ROLE_ADMIN", label: "Admin" }
];
let activeRole = roles.includes("ROLE_ADMIN") ? "ROLE_ADMIN"
  : roles.includes("ROLE_ORGANIZER") ? "ROLE_ORGANIZER" : "ROLE_STUDENT";
let renderGeneration = 0;

function showMessage(text, isError = false) {
  const message = document.getElementById("dashboardMessage");
  message.textContent = text;
  message.classList.toggle("error", isError);
}

function apiMessage(data, fallback) {
  const details = data?.details;
  if (typeof details?.message === "string") return details.message;
  if (details && typeof details === "object") return Object.values(details).join(" ");
  return fallback;
}

async function requestJson(url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: { ...authHeaders(), ...options.headers }
  });
  let data;
  try {
    data = response.status === 204 ? null : await response.json();
  } catch {
    data = null;
  }
  if (!response.ok) throw new Error(apiMessage(data, `Request failed (${response.status}).`));
  return data;
}

function setupDashboard() {
  if (!token) {
    location.replace(`/login?next=${encodeURIComponent("/dashboard")}`);
    return false;
  }
  if (!dashboardRoles.some(item => roles.includes(item.role))) {
    showMessage("This account doesn’t have a dashboard role. Contact your campus administrator.", true);
    document.getElementById("dashboardList").replaceChildren();
    return false;
  }

  document.getElementById("sidebarPlansLink").hidden = !roles.includes("ROLE_STUDENT");
  document.getElementById("sidebarHostLink").hidden =
    !roles.includes("ROLE_ORGANIZER") && !roles.includes("ROLE_ADMIN");
  document.getElementById("adminStudentsLink").hidden = !roles.includes("ROLE_ADMIN");
  setupSidebar();
  setupProfileMenu();
  updateProfile();
  const logout = () => {
    localStorage.removeItem("campusToken");
    localStorage.removeItem("campusUser");
    location.href = "/events";
  };
  document.getElementById("logout").addEventListener("click", logout);
  document.getElementById("profileLogout").addEventListener("click", logout);
  renderRoleSwitcher();
  renderDashboard();
  return true;
}

function setupSidebar() {
  const sidebar = document.getElementById("dashboardSidebar");
  const toggle = document.getElementById("sidebarToggle");
  const backdrop = document.getElementById("sidebarBackdrop");
  const setOpen = open => {
    if (open) {
      const notificationPanel = document.querySelector(".dashboard-notifications");
      const notificationToggle = document.querySelector(".notification-toggle");
      if (notificationPanel) notificationPanel.hidden = true;
      notificationToggle?.setAttribute("aria-expanded", "false");
      notificationToggle?.setAttribute("aria-label", "Open notifications");
      const profileMenu = document.getElementById("profileMenu");
      const profileToggle = document.getElementById("profileMenuToggle");
      profileMenu.hidden = true;
      profileToggle.setAttribute("aria-expanded", "false");
      profileToggle.setAttribute("aria-label", "Open profile details");
    }
    sidebar.classList.toggle("is-open", open);
    toggle.classList.toggle("is-open", open);
    document.body.classList.toggle("sidebar-open", open);
    backdrop.hidden = !open;
    toggle.setAttribute("aria-expanded", String(open));
    toggle.setAttribute("aria-label", open ? "Close navigation" : "Open navigation");
  };
  toggle.addEventListener("click", () => setOpen(!sidebar.classList.contains("is-open")));
  backdrop.addEventListener("click", () => setOpen(false));
  sidebar.querySelectorAll(".sidebar-link").forEach(link => {
    link.addEventListener("click", () => setOpen(false));
  });
  document.addEventListener("keydown", event => {
    if (event.key === "Escape") setOpen(false);
  });
}

function setupProfileMenu() {
  const wrap = document.getElementById("profileMenuWrap");
  const toggle = document.getElementById("profileMenuToggle");
  const menu = document.getElementById("profileMenu");
  const setOpen = open => {
    menu.hidden = !open;
    toggle.setAttribute("aria-expanded", String(open));
    toggle.setAttribute("aria-label", open ? "Close profile details" : "Open profile details");
  };
  toggle.addEventListener("click", () => setOpen(menu.hidden));
  document.addEventListener("click", event => {
    if (!wrap.contains(event.target)) setOpen(false);
  });
  document.addEventListener("keydown", event => {
    if (event.key === "Escape" && !menu.hidden) {
      setOpen(false);
      toggle.focus();
    }
  });
}

function updateProfile() {
  const name = account.fullName?.trim() || "Campus member";
  const email = account.email?.trim() || "";
  const role = dashboardRoles.find(item => item.role === activeRole)?.label || "Member";
  const initials = name.split(/\s+/).slice(0, 2).map(part => part[0]).join("").toUpperCase();

  document.getElementById("profileName").textContent = name;
  document.getElementById("profileEmail").textContent = email;
  document.getElementById("profileRole").textContent = role;
  document.getElementById("profileAvatar").textContent = initials || "O";
  document.getElementById("profileMenuName").textContent = name;
  document.getElementById("profileMenuEmail").textContent = email || "No email on file";
  document.getElementById("profileMenuRole").textContent = role;
  document.getElementById("profileMenuAvatar").textContent = initials || "O";
}

function renderRoleSwitcher() {
  const container = document.getElementById("dashboardRoleSwitch");
  container.replaceChildren();
  const availableRoles = dashboardRoles.filter(item => roles.includes(item.role));
  container.hidden = availableRoles.length < 2;
  availableRoles.forEach(item => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "role-switch-button";
    button.textContent = item.label;
    button.setAttribute("aria-pressed", String(item.role === activeRole));
    button.addEventListener("click", () => {
      activeRole = item.role;
      updateProfile();
      renderRoleSwitcher();
      renderDashboard();
    });
    container.append(button);
  });
}

function setHeading(eyebrow, firstLine, secondLine, intro, roleLabel) {
  document.getElementById("dashboardEyebrow").textContent = eyebrow;
  const heading = document.getElementById("dashboardHeading");
  heading.replaceChildren(document.createTextNode(`${firstLine} `));
  const second = document.createElement("span");
  second.textContent = secondLine;
  heading.append(document.createElement("br"), second);
  document.getElementById("dashboardIntro").textContent = intro;
  document.getElementById("dashboardRoleLabel").textContent = roleLabel;
}

async function renderDashboard() {
  const generation = ++renderGeneration;
  showMessage("");
  document.getElementById("dashboardStats").replaceChildren();
  document.getElementById("dashboardList").innerHTML =
    '<div class="dashboard-loading">Gathering your plans…</div>';

  try {
    if (activeRole === "ROLE_STUDENT") await renderStudentDashboard(generation);
    else await renderEventDashboard(activeRole === "ROLE_ADMIN", generation);
  } catch (error) {
    if (generation !== renderGeneration) return;
    document.getElementById("dashboardList").replaceChildren();
    showMessage(error.message || "Could not load your dashboard.", true);
  }
}

async function renderStudentDashboard(generation) {
  const firstName = account.fullName?.trim().split(/\s+/)[0] || "there";
  setHeading("YOUR OFFBEAT ITINERARY", `Hi ${firstName},`, "what’s next?",
    "Your saved seats, upcoming plans, and the events you’ve bookmarked—all together.",
    "STUDENT DASHBOARD");
  document.getElementById("listEyebrow").textContent = "YOUR YES LIST";
  document.getElementById("listHeading").textContent = "Coming up";
  document.getElementById("listSubtitle").textContent = "The next good things on your calendar.";
  document.getElementById("listAction").href = "/my-events";
  document.getElementById("listAction").innerHTML = 'All my plans <span aria-hidden="true">↗</span>';
  document.getElementById("dashboardNote").textContent =
    "A seat saved is a story waiting to happen. We’ll remind you before the event.";

  const [registrations, events] = await Promise.all([
    requestJson(`${API}/api/students/me/registrations`),
    requestJson(`${API}/api/events`)
  ]);
  if (generation !== renderGeneration) return;
  const eventById = new Map(events.map(event => [String(event.id), event]));
  const active = registrations.filter(registration => registration.status === "REGISTERED");
  const attended = registrations.filter(registration => registration.status === "ATTENDED");
  const now = Date.now();
  const upcoming = active.map(registration => ({
    registration,
    event: eventById.get(String(registration.eventId))
  })).filter(item => item.event && new Date(item.event.startsAt).getTime() > now)
    .sort((a, b) => new Date(a.event.startsAt) - new Date(b.event.startsAt));

  const savedIds = getSavedEventIds();
  const savedEvents = savedIds.map(id => eventById.get(id)).filter(Boolean);
  renderStats([
    { label: "UP NEXT", value: upcoming.length, note: "future plans" },
    { label: "SEATS SAVED", value: active.length, note: "confirmed registrations" },
    { label: "SAVED FOR LATER", value: savedEvents.length, note: "bookmarked events" },
    { label: "SHOWED UP", value: attended.length, note: "events attended" }
  ]);
  renderStudentPlans(upcoming);
  renderSavedEvents(savedEvents);
  renderActions([
    { label: "Find your next thing", href: "/events#events", mark: "↗" },
    { label: "Manage registrations", href: "/my-events", mark: "→" }
  ]);
}

async function renderEventDashboard(isAdmin, generation) {
  const firstName = account.fullName?.trim().split(/\s+/)[0] || "there";
  setHeading(isAdmin ? "CAMPUS-WIDE OVERVIEW" : "YOUR EVENTS, IN MOTION",
    isAdmin ? "Campus life," : `Hi ${firstName},`,
    isAdmin ? "at a glance." : "you’ve got this.",
    isAdmin ? "A clear view of events, registrations, and attendance across campus."
      : "See what’s coming up, who’s joining, and how each plan is going.",
    isAdmin ? "ADMIN DASHBOARD" : "ORGANIZER DASHBOARD");
  document.getElementById("listEyebrow").textContent = isAdmin ? "ACROSS CAMPUS" : "YOUR EVENT LINEUP";
  document.getElementById("listHeading").textContent = isAdmin ? "Event overview" : "Your events";
  document.getElementById("listSubtitle").textContent = isAdmin
    ? "Every event and its participation at a glance."
    : "A quick pulse on registrations and check-ins.";
  document.getElementById("listAction").href = "/organizer";
  document.getElementById("listAction").innerHTML = isAdmin
    ? 'Manage campus events <span aria-hidden="true">↗</span>'
    : 'Open host desk <span aria-hidden="true">↗</span>';
  document.getElementById("dashboardNote").textContent = isAdmin
    ? "Good events happen when the details are taken care of. Use the host desk to manage rosters and reports."
    : "Your host desk has the full roster, attendance check-in, and downloadable participation reports.";

  const events = await requestJson(`${API}/api/organizer/events`);
  const eventData = await Promise.all(events.map(async event => {
    const attendees = await requestJson(`${API}/api/organizer/events/${event.id}/registrations`);
    return { event, attendees };
  }));
  if (generation !== renderGeneration) return;
  const registered = eventData.reduce((total, item) =>
    total + item.attendees.filter(person => person.status === "REGISTERED").length, 0);
  const attended = eventData.reduce((total, item) =>
    total + item.attendees.filter(person => person.status === "ATTENDED").length, 0);
  const published = eventData.filter(item => item.event.status === "PUBLISHED").length;
  const upcoming = eventData.filter(item =>
    item.event.status === "PUBLISHED" && new Date(item.event.startsAt).getTime() > Date.now()).length;

  renderStats(isAdmin ? [
    { label: "ALL EVENTS", value: events.length, note: "across the platform" },
    { label: "PUBLISHED", value: published, note: "live for students" },
    { label: "SEATS CLAIMED", value: registered, note: "active registrations" },
    { label: "CHECKED IN", value: attended, note: "attendance recorded" }
  ] : [
    { label: "YOUR EVENTS", value: events.length, note: "created by you" },
    { label: "COMING UP", value: upcoming, note: "published events" },
    { label: "SEATS CLAIMED", value: registered, note: "active registrations" },
    { label: "CHECKED IN", value: attended, note: "attendance recorded" }
  ]);
  renderEventList(eventData, isAdmin);
  renderSavedEvents([]);
  document.getElementById("secondaryEyebrow").textContent = "A QUICK TIP";
  document.getElementById("secondaryHeading").textContent = "Make check-in easy";
  document.getElementById("secondaryList").textContent =
    "Open an event roster at the host desk to mark attendance as people arrive.";
  renderActions([
    { label: "Create or manage events", href: "/organizer#eventForm", mark: "↗" },
    ...(isAdmin ? [{ label: "Manage student accounts", href: "/admin/students", mark: "♙" }] : []),
    { label: "Browse the event calendar", href: "/events#events", mark: "→" }
  ]);
}

function renderStats(stats) {
  const container = document.getElementById("dashboardStats");
  container.replaceChildren();
  stats.forEach((stat, index) => {
    const card = document.createElement("article");
    card.className = `dashboard-stat${index === 0 ? " featured" : ""}`;
    const label = document.createElement("p");
    label.className = "dashboard-stat-label";
    label.textContent = stat.label;
    const value = document.createElement("strong");
    value.className = "dashboard-stat-value";
    value.textContent = String(stat.value);
    const note = document.createElement("span");
    note.className = "dashboard-stat-note";
    note.textContent = stat.note;
    card.append(label, value, note);
    container.append(card);
  });
}

function renderStudentPlans(items) {
  const list = document.getElementById("dashboardList");
  list.replaceChildren();
  if (!items.length) {
    list.append(emptyState("Nothing booked just yet. Find a workshop, talk, or team to join."));
    return;
  }
  items.slice(0, 5).forEach(({ event }) => {
    const card = document.createElement("article");
    card.className = "dashboard-plan-card";
    const date = document.createElement("div");
    date.className = "dashboard-date-block";
    const dateParts = new Intl.DateTimeFormat(undefined, { month: "short", day: "numeric" })
      .formatToParts(new Date(event.startsAt));
    const month = document.createElement("span");
    month.textContent = dateParts.find(part => part.type === "month")?.value || "";
    const day = document.createElement("strong");
    day.textContent = dateParts.find(part => part.type === "day")?.value || "";
    date.append(month, day);
    const info = document.createElement("div");
    info.className = "dashboard-plan-info";
    const category = document.createElement("span");
    category.className = "dashboard-kicker";
    category.textContent = event.category || "CAMPUS EVENT";
    const title = document.createElement("h3");
    title.textContent = event.title;
    const meta = document.createElement("p");
    meta.textContent = `${displayDate(event.startsAt)} · ${event.venue}`;
    info.append(category, title, meta);
    const status = document.createElement("span");
    status.className = "dashboard-attending";
    status.textContent = "SEAT SAVED";
    card.append(date, info, status);
    list.append(card);
  });
}

function renderEventList(eventData, isAdmin) {
  const list = document.getElementById("dashboardList");
  list.replaceChildren();
  if (!eventData.length) {
    list.append(emptyState("No events to show yet. Create the first plan from the host desk."));
    return;
  }
  const now = Date.now();
  eventData.sort((a, b) => {
    const aUpcoming = a.event.status === "PUBLISHED" && new Date(a.event.startsAt).getTime() > now;
    const bUpcoming = b.event.status === "PUBLISHED" && new Date(b.event.startsAt).getTime() > now;
    return Number(bUpcoming) - Number(aUpcoming) || new Date(a.event.startsAt) - new Date(b.event.startsAt);
  });
  eventData.slice(0, 8).forEach(({ event, attendees }) => {
    const card = document.createElement("article");
    card.className = "dashboard-event-card";
    const top = document.createElement("div");
    top.className = "dashboard-event-top";
    const status = document.createElement("span");
    status.className = `dashboard-event-status ${event.status.toLowerCase()}`;
    status.textContent = event.status.replaceAll("_", " ");
    const date = document.createElement("span");
    date.className = "dashboard-event-date";
    date.textContent = displayDate(event.startsAt);
    top.append(status, date);
    const title = document.createElement("h3");
    title.textContent = event.title;
    const venue = document.createElement("p");
    venue.className = "dashboard-event-venue";
    venue.textContent = `${event.category} · ${event.venue}${isAdmin ? ` · hosted by ${event.organizer}` : ""}`;
    const activeCount = attendees.filter(person => person.status === "REGISTERED").length;
    const attendedCount = attendees.filter(person => person.status === "ATTENDED").length;
    const stats = document.createElement("div");
    stats.className = "dashboard-event-metrics";
    stats.append(metric("REGISTERED", activeCount), metric("ATTENDED", attendedCount),
      metric("CAPACITY", event.capacity || "OPEN"));
    const progress = document.createElement("div");
    progress.className = "dashboard-progress";
    const bar = document.createElement("span");
    const progressPercent = event.capacity
      ? Math.min(100, activeCount / event.capacity * 100)
      : activeCount + attendedCount ? attendedCount / (activeCount + attendedCount) * 100 : 0;
    bar.style.width = `${progressPercent}%`;
    progress.append(bar);
    const footer = document.createElement("div");
    footer.className = "dashboard-event-footer";
    const manage = makeLink("/organizer", isAdmin ? "Manage event" : "Open host desk");
    manage.className = "dashboard-text-link";
    const report = document.createElement("button");
    report.type = "button";
    report.className = "dashboard-text-link";
    report.textContent = "Download CSV ↓";
    report.addEventListener("click", () => downloadReport(event.id, report));
    footer.append(manage, report);
    card.append(top, title, venue, stats, progress, footer);
    list.append(card);
  });
}

function metric(label, value) {
  const item = document.createElement("span");
  item.className = "dashboard-event-metric";
  const caption = document.createElement("small");
  caption.textContent = label;
  const amount = document.createElement("strong");
  amount.textContent = String(value);
  item.append(caption, amount);
  return item;
}

function renderSavedEvents(events) {
  const list = document.getElementById("secondaryList");
  list.replaceChildren();
  if (!events.length) {
    list.append(emptyState("No events saved for later."));
    return;
  }
  events.slice(0, 4).forEach(event => {
    const link = makeLink("/events#events", event.title);
    link.className = "saved-event-link";
    const meta = document.createElement("span");
    meta.textContent = `${event.category} · ${displayDate(event.startsAt)}`;
    link.append(meta);
    list.append(link);
  });
}

function renderActions(actions) {
  const container = document.getElementById("dashboardActions");
  container.replaceChildren();
  actions.forEach(action => {
    const link = makeLink(action.href, action.label);
    link.className = "dashboard-action-link";
    const mark = document.createElement("span");
    mark.setAttribute("aria-hidden", "true");
    mark.textContent = action.mark;
    link.append(mark);
    container.append(link);
  });
}

function getSavedEventIds() {
  try {
    const ids = JSON.parse(localStorage.getItem("campusSavedEvents") || "[]");
    return Array.isArray(ids) ? ids.map(String) : [];
  } catch {
    return [];
  }
}

function emptyState(text) {
  const empty = document.createElement("div");
  empty.className = "dashboard-empty";
  empty.textContent = text;
  return empty;
}

function makeLink(href, text) {
  const link = document.createElement("a");
  link.href = href;
  link.textContent = text;
  return link;
}

async function downloadReport(eventId, button) {
  button.disabled = true;
  try {
    const response = await fetch(`${API}/api/organizer/events/${eventId}/report.csv`, {
      headers: authHeaders()
    });
    if (!response.ok) {
      let data;
      try {
        data = await response.json();
      } catch {
        data = null;
      }
      throw new Error(apiMessage(data, "Could not download the event report."));
    }
    const url = URL.createObjectURL(await response.blob());
    const link = document.createElement("a");
    link.href = url;
    link.download = `event-${eventId}-participation.csv`;
    link.click();
    URL.revokeObjectURL(url);
    showMessage("Participation report downloaded.");
  } catch (error) {
    showMessage(error.message, true);
  } finally {
    button.disabled = false;
  }
}

setupDashboard();
