const API = window.CAMPUS_API_URL;
const token = localStorage.getItem("campusToken");
const account = JSON.parse(localStorage.getItem("campusUser") || "null");
const isStudent = Array.isArray(account?.roles) && account.roles.includes("ROLE_STUDENT");
const authHeaders = () => token ? { Authorization: `Bearer ${token}` } : {};
const dateTime = value => new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
let registeredEventStatuses = new Map();
let registrationCheckFailed = false;

function showMessage(element, text, isError = false) {
  if (!element) return;
  element.textContent = text;
  element.classList.toggle("error", isError);
}

function errorText(data, fallback) {
  const details = data?.details;
  if (typeof details?.message === "string") return details.message;
  if (details && typeof details === "object") return Object.values(details).join(" ");
  return fallback;
}

function getSavedEventIds() {
  try {
    const saved = JSON.parse(localStorage.getItem("campusSavedEvents") || "[]");
    return new Set(Array.isArray(saved) ? saved.map(String) : []);
  } catch {
    return new Set();
  }
}

function toggleSavedEvent(event, events) {
  const saved = getSavedEventIds();
  const id = String(event.id);
  if (saved.has(id)) saved.delete(id);
  else saved.add(id);
  localStorage.setItem("campusSavedEvents", JSON.stringify([...saved]));
  renderEvents(events);
}

function setupNavigation() {
  const roles = account?.roles || [];
  const loggedIn = Boolean(token);
  const isStudent = roles.includes("ROLE_STUDENT");
  const dashboard = document.getElementById("sidebarDashboardLink") || document.getElementById("dashboardLink");
  const login = document.getElementById("sidebarLoginLink") || document.getElementById("loginLink");
  const myEvents = document.getElementById("sidebarPlansLink") || document.getElementById("myEvents");
  const organizer = document.getElementById("sidebarHostLink") || document.getElementById("organizerLink");
  const logout = document.getElementById("logout");
  if (dashboard) dashboard.hidden = !loggedIn;
  if (login) login.hidden = loggedIn;
  if (myEvents) myEvents.hidden = !isStudent;
  if (organizer) organizer.hidden = !roles.includes("ROLE_ORGANIZER") && !roles.includes("ROLE_ADMIN");
  if (logout) {
    logout.hidden = !loggedIn;
    logout.addEventListener("click", () => {
      localStorage.removeItem("campusToken");
      localStorage.removeItem("campusUser");
      location.href = "/events";
    });
  }
}

async function loadEvents() {
  const grid = document.getElementById("eventGrid");
  if (!grid) return;
  const message = document.getElementById("message");
  grid.replaceChildren();
  grid.innerHTML = '<div class="empty-state">Finding what’s happening…</div>';
  try {
    const registrationRequest = isStudent ? loadStudentRegistrationStatuses() : Promise.resolve();
    const [eventsResponse, categoriesResponse] = await Promise.all([
      fetch(`${API}/api/events`),
      fetch(`${API}/api/events/categories`),
      registrationRequest
    ]);
    if (!eventsResponse.ok || !categoriesResponse.ok) throw new Error("Could not load campus events.");
    const [events, categories] = await Promise.all([eventsResponse.json(), categoriesResponse.json()]);
    populateCategories(categories);
    renderEvents(events);
    bindFilters(events);
  } catch (error) {
    showMessage(message, `${error.message} Check that the API and database are running.`, true);
    grid.replaceChildren();
  }
}

async function loadStudentRegistrationStatuses() {
  registeredEventStatuses = new Map();
  registrationCheckFailed = false;
  try {
    const response = await fetch(`${API}/api/students/me/registrations`, { headers: authHeaders() });
    let data;
    try {
      data = await response.json();
    } catch {
      data = null;
    }
    if (!response.ok) {
      if (response.status === 401) {
        localStorage.removeItem("campusToken");
        localStorage.removeItem("campusUser");
        throw new Error("Your session has expired. Log in again to check your saved seats.");
      }
      if (response.status === 403) {
        throw new Error("Student access is required to check saved seats. Sign in with a student account.");
      }
      throw new Error(errorText(data, "Could not check your saved seats."));
    }
    if (!Array.isArray(data)) throw new Error("The server returned an invalid registrations list.");
    registeredEventStatuses = new Map(data.map(registration => [
      String(registration.eventId),
      registration.status
    ]));
  } catch (error) {
    registrationCheckFailed = true;
    showMessage(document.getElementById("message"), error.message || "Could not check your saved seats.", true);
  }
}

function populateCategories(categories) {
  const select = document.getElementById("categoryFilter");
  if (!select) return;
  categories.forEach(category => {
    const option = document.createElement("option");
    option.value = category.name;
    option.textContent = category.name;
    select.append(option);
  });
}

function bindFilters(events) {
  ["searchInput", "categoryFilter", "savedFilter", "sortFilter"].forEach(id => {
    const filter = document.getElementById(id);
    filter?.addEventListener(filter instanceof HTMLSelectElement ? "change" : "input", () => renderEvents(events));
  });
}

function renderEvents(events) {
  const grid = document.getElementById("eventGrid");
  if (!grid) return;
  const query = document.getElementById("searchInput")?.value.trim().toLowerCase() || "";
  const category = document.getElementById("categoryFilter")?.value || "";
  const savedOnly = document.getElementById("savedFilter")?.value === "saved";
  const savedIds = getSavedEventIds();
  const sort = document.getElementById("sortFilter")?.value || "soonest";
  const filtered = events.filter(event => {
    const text = `${event.title} ${event.description} ${event.venue} ${event.category}`.toLowerCase();
    return (!query || text.includes(query)) && (!category || event.category === category)
      && (!savedOnly || savedIds.has(String(event.id)));
  });
  filtered.sort((a, b) => sort === "az" ? a.title.localeCompare(b.title)
    : sort === "latest" ? new Date(b.startsAt) - new Date(a.startsAt)
      : new Date(a.startsAt) - new Date(b.startsAt));
  const counter = document.getElementById("eventCount");
  if (counter) counter.textContent = `${filtered.length} ${filtered.length === 1 ? "event" : "events"}`;
  grid.replaceChildren();
  if (!filtered.length) {
    const empty = document.createElement("div");
    empty.className = "empty-state";
    empty.textContent = savedOnly && events.length ? "No saved events yet. Tap ☆ Save on an event to keep it here."
      : events.length ? "No events match that search. Try another category or keyword."
        : "Nothing on the calendar just yet. Check back soon.";
    grid.append(empty);
    return;
  }
  filtered.forEach(event => grid.append(createEventCard(event, events)));
}

function createEventCard(event, events) {
  const card = document.createElement("article");
  card.className = "event-card";
  const deadlinePassed = new Date(event.registrationDeadline) <= new Date();
  const category = document.createElement("span");
  category.className = "event-category";
  category.textContent = event.category;
  const deadline = document.createElement("span");
  deadline.className = "event-deadline";
  deadline.textContent = deadlinePassed ? "Registration closed" : "Registration open";
  const topline = document.createElement("div");
  topline.className = "event-topline";
  topline.append(category, deadline);
  const title = document.createElement("h3");
  title.textContent = event.title;
  const description = document.createElement("p");
  description.className = "event-description";
  description.textContent = event.description;
  const meta = document.createElement("div");
  meta.className = "event-meta";
  meta.append(makeMeta("◷", dateTime(event.startsAt)), makeMeta("⌖", event.venue));
  const footer = document.createElement("div");
  footer.className = "event-footer";
  const capacity = document.createElement("span");
  capacity.className = "event-capacity";
  capacity.textContent = event.capacity ? `Limited to ${event.capacity} seats` : "Everyone’s welcome";
  const button = document.createElement("button");
  button.className = "button button-primary";
  button.type = "button";
  const registrationStatus = registeredEventStatuses.get(String(event.id));
  const activeRegistration = registrationStatus === "REGISTERED" || registrationStatus === "ATTENDED";
  const pendingRequest = registrationStatus === "PENDING_APPROVAL";
  button.textContent = registrationStatus === "ATTENDED" ? "You attended ✓"
    : activeRegistration ? "Seat saved ✓"
      : pendingRequest ? "Request pending"
      : registrationCheckFailed ? "Seat status unavailable"
        : deadlinePassed ? "Registration closed"
          : !token ? "Log in to join"
            : !isStudent ? "Student access required"
              : "Save my seat";
  button.disabled = activeRegistration || pendingRequest || registrationCheckFailed || deadlinePassed || (Boolean(token) && !isStudent);
  button.addEventListener("click", () => registerForEvent(event.id, button));
  const saved = getSavedEventIds().has(String(event.id));
  const saveButton = document.createElement("button");
  saveButton.className = "event-save";
  saveButton.type = "button";
  saveButton.setAttribute("aria-pressed", String(saved));
  saveButton.setAttribute("aria-label", saved ? `Remove ${event.title} from saved events` : `Save ${event.title}`);
  saveButton.textContent = saved ? "Saved ★" : "☆ Save";
  saveButton.addEventListener("click", () => toggleSavedEvent(event, events));
  topline.append(saveButton);
  footer.append(capacity, button);
  card.append(topline, title, description, meta, footer);
  return card;
}

function makeMeta(icon, text) {
  const row = document.createElement("span");
  row.className = "meta-item";
  const glyph = document.createElement("span");
  glyph.className = "meta-icon";
  glyph.setAttribute("aria-hidden", "true");
  glyph.textContent = icon;
  row.append(glyph, document.createTextNode(text));
  return row;
}

async function registerForEvent(eventId, button) {
  if (!token) {
    location.href = `/login?next=${encodeURIComponent("/events#events")}`;
    return;
  }
  const message = document.getElementById("message");
  button.disabled = true;
  button.textContent = "Saving your seat…";
  try {
    const response = await fetch(`${API}/api/events/${eventId}/registrations`, {
      method: "POST",
      headers: authHeaders()
    });
    const data = response.status === 204 ? null : await response.json();
    if (!response.ok) throw new Error(errorText(data, "Could not register for this event."));
    const status = data?.status || "PENDING_APPROVAL";
    registeredEventStatuses.set(String(eventId), status);
    button.textContent = status === "REGISTERED" ? "Seat saved ✓" : "Request pending";
    button.disabled = true;
    showMessage(message, status === "REGISTERED"
      ? "Your seat is saved. A confirmation email is on its way."
      : "Your request is with the event host. We’ll notify you when they respond.");
  } catch (error) {
    button.disabled = false;
    button.textContent = "Save my seat";
    showMessage(message, error.message, true);
  }
}

async function loadMine() {
  const list = document.getElementById("registrations");
  if (!list) return;
  if (!token) {
    list.innerHTML = '<div class="registration-empty">Log in to see your registrations. <a href="/login">Log in ↗</a></div>';
    document.getElementById("invitations")?.replaceChildren();
    document.getElementById("studentNotifications")?.replaceChildren();
    return;
  }
  list.innerHTML = '<div class="registration-empty">Loading your plans…</div>';
  await Promise.all([loadInvitations(), loadStudentNotifications()]);
  try {
    const response = await fetch(`${API}/api/students/me/registrations`, { headers: authHeaders() });
    const data = await response.json();
    if (!response.ok) {
      if (response.status === 401) {
        localStorage.removeItem("campusToken");
        localStorage.removeItem("campusUser");
        throw new Error("Your session has expired. Log in again to view your registrations.");
      }
      if (response.status === 403) {
        throw new Error("Student access is required to view personal registrations. Sign in with a student account or ask an administrator to assign ROLE_STUDENT.");
      }
      throw new Error(errorText(data, "Could not load your registrations."));
    }
    list.replaceChildren();
    if (!data.length) {
      const empty = document.createElement("div");
      empty.className = "registration-empty";
      empty.append(document.createTextNode("No registrations yet. "), makeLink("/events#events", "Find something to do ↗"));
      list.append(empty);
      return;
    }
    data.forEach(registration => list.append(registrationCard(registration)));
  } catch (error) {
    list.replaceChildren();
    showMessage(document.getElementById("message"), error.message, true);
  }
}

function registrationCard(registration) {
  const card = document.createElement("article");
  card.className = "registration-card";
  const details = document.createElement("div");
  details.className = "registration-details";
  const title = document.createElement("h3");
  title.textContent = registration.eventTitle;
  const meta = document.createElement("div");
  meta.className = "registration-meta";
  meta.append(document.createTextNode(`Registered ${dateTime(registration.registeredAt)}`));
  details.append(title, meta);
  const actions = document.createElement("div");
  actions.className = "registration-actions";
  const status = document.createElement("span");
  status.className = `status-pill ${registration.status.toLowerCase()}`;
  status.textContent = registration.status === "REGISTERED" ? "You’re going"
    : registration.status === "ATTENDED" ? "Attended"
      : registration.status === "PENDING_APPROVAL" ? "Awaiting host"
        : registration.status.toLowerCase();
  actions.append(status);
  if (registration.status === "REGISTERED" || registration.status === "PENDING_APPROVAL") {
    const cancel = document.createElement("button");
    cancel.className = "button button-outline";
    cancel.type = "button";
    cancel.textContent = registration.status === "REGISTERED" ? "Cancel seat" : "Withdraw request";
    cancel.addEventListener("click", () => cancelRegistration(registration.id, cancel));
    actions.append(cancel);
  }
  card.append(details, actions);
  return card;
}

async function cancelRegistration(id, button) {
  if (!window.confirm("Withdraw this request or release your saved seat?")) return;
  button.disabled = true;
  button.textContent = "Cancelling…";
  try {
    const response = await fetch(`${API}/api/registrations/${id}`, { method: "DELETE", headers: authHeaders() });
    if (!response.ok) {
      const data = await response.json();
      throw new Error(errorText(data, "Could not cancel this registration."));
    }
    showMessage(document.getElementById("message"), "Your request was cancelled.");
    loadMine();
  } catch (error) {
    button.disabled = false;
    button.textContent = "Try again";
    showMessage(document.getElementById("message"), error.message, true);
  }
}

async function loadInvitations() {
  const list = document.getElementById("invitations");
  if (!list) return;
  list.replaceChildren();
  try {
    const response = await fetch(`${API}/api/students/me/invitations`, { headers: authHeaders() });
    const data = await response.json();
    if (!response.ok) throw new Error(errorText(data, "Could not load invitations."));
    if (!Array.isArray(data)) throw new Error("The server returned an invalid invitations list.");
    const pending = data.filter(invitation => invitation.status === "PENDING");
    if (!pending.length) {
      const empty = document.createElement("div");
      empty.className = "registration-empty";
      empty.textContent = data.length ? "You’ve responded to all your invitations." : "No invitations yet. Keep an eye out for a host’s note.";
      list.append(empty);
      return;
    }
    pending.forEach(invitation => list.append(invitationCard(invitation)));
  } catch (error) {
    showMessage(document.getElementById("invitationMessage"), error.message, true);
  }
}

function invitationCard(invitation) {
  const card = document.createElement("article");
  card.className = "registration-card invitation-card";
  const details = document.createElement("div");
  details.className = "registration-details";
  const title = document.createElement("h3");
  title.textContent = invitation.eventTitle;
  const meta = document.createElement("div");
  meta.className = "registration-meta";
  meta.textContent = `Invited ${dateTime(invitation.createdAt)}`;
  details.append(title, meta);
  const actions = document.createElement("div");
  actions.className = "registration-actions";
  const accept = document.createElement("button");
  accept.className = "button button-primary";
  accept.type = "button";
  accept.textContent = "Accept & save seat";
  accept.addEventListener("click", () => respondToInvitation(invitation.id, "accept", accept));
  const decline = document.createElement("button");
  decline.className = "button button-outline";
  decline.type = "button";
  decline.textContent = "Decline";
  decline.addEventListener("click", () => respondToInvitation(invitation.id, "decline", decline));
  actions.append(accept, decline);
  card.append(details, actions);
  return card;
}

async function respondToInvitation(id, action, button) {
  button.disabled = true;
  try {
    const response = await fetch(`${API}/api/invitations/${id}/${action}`, {
      method: "POST",
      headers: authHeaders()
    });
    const data = await response.json();
    if (!response.ok) throw new Error(errorText(data, "Could not update this invitation."));
    showMessage(document.getElementById("invitationMessage"),
      action === "accept" ? "Invitation accepted. Your seat is confirmed." : "Invitation declined.");
    await loadMine();
  } catch (error) {
    button.disabled = false;
    showMessage(document.getElementById("invitationMessage"), error.message, true);
  }
}

async function loadStudentNotifications() {
  const list = document.getElementById("studentNotifications");
  if (!list) return;
  list.replaceChildren();
  try {
    const response = await fetch(`${API}/api/notifications`, { headers: authHeaders() });
    const data = await response.json();
    if (!response.ok) throw new Error(errorText(data, "Could not load notifications."));
    if (!Array.isArray(data) || !data.length) {
      const empty = document.createElement("div");
      empty.className = "registration-empty";
      empty.textContent = "You’re all caught up.";
      list.append(empty);
      return;
    }
    data.forEach(notification => {
      const card = document.createElement("article");
      card.className = `notification-card${notification.readAt ? "" : " unread"}`;
      const heading = document.createElement("strong");
      heading.textContent = notification.title;
      const message = document.createElement("p");
      message.textContent = notification.message;
      const date = document.createElement("small");
      date.textContent = dateTime(notification.createdAt);
      card.append(heading, message, date);
      if (!notification.readAt) {
        const read = document.createElement("button");
        read.type = "button";
        read.className = "button button-outline button-small";
        read.textContent = "Mark read";
        read.addEventListener("click", () => markStudentNotificationRead(notification.id, read));
        card.append(read);
      }
      list.append(card);
    });
  } catch (error) {
    const empty = document.createElement("div");
    empty.className = "registration-empty error";
    empty.textContent = error.message;
    list.append(empty);
  }
}

async function markStudentNotificationRead(id, button) {
  button.disabled = true;
  try {
    const response = await fetch(`${API}/api/notifications/${id}/read`, {
      method: "POST",
      headers: authHeaders()
    });
    if (!response.ok) {
      const data = await response.json();
      throw new Error(errorText(data, "Could not update notification."));
    }
    loadStudentNotifications();
  } catch (error) {
    button.disabled = false;
    button.textContent = error.message;
  }
}

function makeLink(href, text) {
  const link = document.createElement("a");
  link.href = href;
  link.textContent = text;
  return link;
}

setupNavigation();
loadEvents();
loadMine();
