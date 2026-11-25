const API = window.CAMPUS_API_URL;
const token = localStorage.getItem("campusToken");
const account = JSON.parse(localStorage.getItem("campusUser") || "null");
const roles = account?.roles || [];
const headers = () => ({ Authorization: `Bearer ${token}` });
const displayDate = value => new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
let activeAttendeeEvent = null;
let activeInviteEvent = null;

function setMessage(id, text, isError = false) {
  const element = document.getElementById(id);
  element.textContent = text;
  element.classList.toggle("error", isError);
}

function apiError(data, fallback) {
  const details = data?.details;
  if (typeof details?.message === "string") return details.message;
  if (details && typeof details === "object") return Object.values(details).join(" ");
  return fallback;
}

async function requestJson(url, options = {}) {
  const response = await fetch(url, { ...options, headers: { ...headers(), ...options.headers } });
  if (response.status === 204) return null;
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) throw new Error(apiError(data, "The request could not be completed."));
  return data;
}

function requireOrganizer() {
  if (!token) {
    location.href = "/login?next=%2Forganizer";
    return false;
  }
  if (!roles.includes("ROLE_ORGANIZER") && !roles.includes("ROLE_ADMIN")) {
    document.querySelector(".organizer-wrap").innerHTML = '<div class="registration-empty">Organizer access is restricted. Contact a campus administrator if you need access. <a href="/events">Explore events ↗</a></div>';
    return false;
  }
  return true;
}

async function loadCategories() {
  const select = document.getElementById("eventCategory");
  try {
    const response = await fetch(`${API}/api/events/categories`);
    if (!response.ok) throw new Error("Could not load event categories.");
    const categories = await response.json();
    select.replaceChildren(new Option("Choose a category", ""));
    categories.forEach(category => select.add(new Option(category.name, category.id)));
  } catch (error) {
    select.replaceChildren(new Option("Categories unavailable", ""));
    setMessage("createMessage", error.message, true);
  }
}

async function loadOrganizerEvents() {
  const list = document.getElementById("organizerEvents");
  list.replaceChildren();
  const loading = document.createElement("div");
  loading.className = "registration-empty";
  loading.textContent = "Loading your events…";
  list.append(loading);
  try {
    const events = await requestJson(`${API}/api/organizer/events`);
    list.replaceChildren();
    if (!events.length) {
      const empty = document.createElement("div");
      empty.className = "registration-empty";
      empty.textContent = "No events yet. Create one above and get your people together.";
      list.append(empty);
      return;
    }
    events.forEach(event => list.append(eventCard(event)));
    const requestedEventId = new URLSearchParams(location.search).get("eventId");
    const requestedEvent = events.find(event => String(event.id) === requestedEventId);
    if (requestedEvent) await loadAttendees(requestedEvent);
  } catch (error) {
    list.replaceChildren();
    setMessage("manageMessage", error.message, true);
  }
}

function eventCard(event) {
  const card = document.createElement("article");
  card.className = "organizer-event-card";
  const details = document.createElement("div");
  const title = document.createElement("h3");
  title.textContent = event.title;
  const info = document.createElement("div");
  info.className = "organizer-event-info";
  info.textContent = `${event.category} · ${displayDate(event.startsAt)} · ${event.venue}`;
  details.append(title, info);
  const actions = document.createElement("div");
  actions.className = "organizer-event-actions";
  const roster = document.createElement("button");
  roster.type = "button";
  roster.className = "button button-outline";
  roster.textContent = "View attendance";
  roster.addEventListener("click", () => loadAttendees(event));
  const invite = document.createElement("button");
  invite.type = "button";
  invite.className = "button button-outline";
  invite.textContent = "Invite students";
  invite.addEventListener("click", () => loadInviteCandidates(event));
  const report = document.createElement("button");
  report.type = "button";
  report.className = "button button-primary";
  report.textContent = "Download report ↓";
  report.addEventListener("click", () => downloadReport(event.id));
  actions.append(roster, invite, report);
  card.append(details, actions);
  return card;
}

async function createEvent(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const message = document.getElementById("createMessage");
  const button = form.querySelector('button[type="submit"]');
  const values = new FormData(form);
  const startsAt = new Date(values.get("startsAt"));
  const endsAt = new Date(values.get("endsAt"));
  const deadline = new Date(values.get("registrationDeadline"));
  if (!startsAt || !endsAt || !deadline || Number.isNaN(startsAt.getTime()) || Number.isNaN(endsAt.getTime()) || Number.isNaN(deadline.getTime())) {
    setMessage("createMessage", "Add valid event dates and times.", true);
    return;
  }
  if (endsAt <= startsAt || deadline >= startsAt) {
    setMessage("createMessage", "The end time must be after the start, and registration must close before it starts.", true);
    return;
  }
  const payload = {
    title: values.get("title").trim(),
    description: values.get("description").trim(),
    categoryId: values.get("categoryId"),
    venue: values.get("venue").trim(),
    startsAt: startsAt.toISOString(),
    endsAt: endsAt.toISOString(),
    registrationDeadline: deadline.toISOString(),
    capacity: values.get("capacity") ? Number(values.get("capacity")) : null,
    status: "PUBLISHED"
  };
  button.disabled = true;
  button.textContent = "Publishing…";
  setMessage("createMessage", "");
  try {
    await requestJson(`${API}/api/events`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    form.reset();
    setMessage("createMessage", "It’s out there. Your plan is live!");
    await loadOrganizerEvents();
  } catch (error) {
    setMessage("createMessage", error.message, true);
  } finally {
    button.disabled = false;
    button.innerHTML = 'Put it out there <span aria-hidden="true">↗</span>';
  }
}

async function loadAttendees(event) {
  activeAttendeeEvent = event;
  const panel = document.getElementById("attendeePanel");
  const rows = document.getElementById("attendeeRows");
  const title = document.getElementById("attendeeTitle");
  const summary = document.getElementById("attendeeSummary");
  panel.hidden = false;
  title.textContent = event.title;
  summary.textContent = "Loading the guest list…";
  rows.replaceChildren();
  panel.scrollIntoView({ behavior: "smooth", block: "start" });
  try {
    const attendees = await requestJson(`${API}/api/organizer/events/${event.id}/registrations`);
    rows.replaceChildren();
    const confirmed = attendees.filter(person => person.status === "REGISTERED").length;
    const pending = attendees.filter(person => person.status === "PENDING_APPROVAL").length;
    const attended = attendees.filter(person => person.status === "ATTENDED").length;
    summary.textContent = `${confirmed} confirmed · ${pending} awaiting approval · ${attended} attended`;
    attendees.forEach(person => rows.append(attendeeRow(person)));
    if (!attendees.length) {
      const row = document.createElement("tr");
      const cell = document.createElement("td");
      cell.colSpan = 5;
      cell.textContent = "No seat requests yet.";
      row.append(cell);
      rows.append(row);
    }
  } catch (error) {
    summary.textContent = error.message;
  }
}

function attendeeRow(person) {
  const row = document.createElement("tr");
  const name = document.createElement("td");
  const nameText = document.createElement("span");
  nameText.textContent = person.fullName;
  const email = document.createElement("small");
  email.className = "attendee-email";
  email.textContent = person.email;
  name.append(nameText, document.createElement("br"), email);
  const student = document.createElement("td");
  student.textContent = person.studentNumber;
  const registered = document.createElement("td");
  registered.textContent = displayDate(person.registeredAt);
  const status = document.createElement("td");
  status.textContent = person.status === "REGISTERED" ? "Confirmed"
    : person.status === "PENDING_APPROVAL" ? "Awaiting approval" : person.status.toLowerCase();
  const action = document.createElement("td");
  if (person.status === "PENDING_APPROVAL") {
    const approve = document.createElement("button");
    approve.type = "button";
    approve.className = "button button-primary";
    approve.textContent = "Approve";
    approve.addEventListener("click", () => updateSeatRequest(person.id, "approve", approve));
    const reject = document.createElement("button");
    reject.type = "button";
    reject.className = "button button-outline";
    reject.textContent = "Decline";
    reject.addEventListener("click", () => updateSeatRequest(person.id, "reject", reject));
    action.append(approve, reject);
  } else if (person.status === "REGISTERED") {
    const checkIn = document.createElement("button");
    checkIn.type = "button";
    checkIn.className = "button button-outline";
    checkIn.textContent = "Mark attended";
    checkIn.addEventListener("click", () => markAttended(person.id, checkIn));
    action.append(checkIn);
  }
  row.append(name, student, registered, status, action);
  return row;
}

async function updateSeatRequest(id, action, button) {
  button.disabled = true;
  button.textContent = action === "approve" ? "Approving…" : "Declining…";
  try {
    await requestJson(`${API}/api/organizer/registrations/${id}/${action}`, { method: "POST" });
    if (activeAttendeeEvent) await loadAttendees(activeAttendeeEvent);
    await loadOrganizerNotifications();
  } catch (error) {
    button.disabled = false;
    button.textContent = action === "approve" ? "Approve" : "Decline";
    setMessage("manageMessage", error.message, true);
  }
}

async function markAttended(id, button) {
  button.disabled = true;
  button.textContent = "Saving…";
  try {
    await requestJson(`${API}/api/organizer/registrations/${id}/attendance`, { method: "POST" });
    if (activeAttendeeEvent) await loadAttendees(activeAttendeeEvent);
  } catch (error) {
    button.disabled = false;
    button.textContent = "Mark attended";
    setMessage("manageMessage", error.message, true);
  }
}

async function loadInviteCandidates(event) {
  activeInviteEvent = event;
  const panel = document.getElementById("invitePanel");
  const candidates = document.getElementById("inviteCandidates");
  document.getElementById("inviteEventTitle").textContent = event.title;
  document.getElementById("inviteMessage").textContent = "";
  panel.hidden = false;
  candidates.replaceChildren();
  const loading = document.createElement("p");
  loading.className = "registration-empty";
  loading.textContent = "Finding eligible students…";
  candidates.append(loading);
  panel.scrollIntoView({ behavior: "smooth", block: "start" });
  try {
    const people = await requestJson(`${API}/api/organizer/events/${event.id}/invite-candidates`);
    candidates.replaceChildren();
    if (!people.length) {
      const empty = document.createElement("p");
      empty.className = "registration-empty";
      empty.textContent = "No eligible students found. Students appear here after registering for one of your events.";
      candidates.append(empty);
      return;
    }
    people.forEach(person => {
      const label = document.createElement("label");
      label.className = "invite-candidate";
      const checkbox = document.createElement("input");
      checkbox.type = "checkbox";
      checkbox.name = "studentIds";
      checkbox.value = person.id;
      const details = document.createElement("span");
      const name = document.createElement("strong");
      name.textContent = person.fullName;
      const meta = document.createElement("small");
      meta.textContent = `${person.studentNumber} · ${person.email}`;
      details.append(name, meta);
      label.append(checkbox, details);
      candidates.append(label);
    });
  } catch (error) {
    candidates.replaceChildren();
    setMessage("inviteMessage", error.message, true);
  }
}

async function sendInvitations(event) {
  event.preventDefault();
  if (!activeInviteEvent) return;
  const button = event.currentTarget.querySelector('button[type="submit"]');
  const studentIds = [...document.querySelectorAll('#inviteCandidates input[name="studentIds"]:checked')]
    .map(input => input.value);
  if (!studentIds.length) {
    setMessage("inviteMessage", "Choose at least one student.", true);
    return;
  }
  button.disabled = true;
  button.textContent = "Sending…";
  try {
    const result = await requestJson(`${API}/api/organizer/events/${activeInviteEvent.id}/invitations`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ studentIds })
    });
    setMessage("inviteMessage", `${result.invited} invitation${result.invited === 1 ? "" : "s"} sent${result.unchanged ? ` · ${result.unchanged} already invited` : ""}.`);
    await loadOrganizerNotifications();
  } catch (error) {
    setMessage("inviteMessage", error.message, true);
  } finally {
    button.disabled = false;
    button.textContent = "Send invitations ↗";
  }
}

async function loadOrganizerNotifications() {
  const list = document.getElementById("organizerNotifications");
  list.replaceChildren();
  try {
    const notifications = await requestJson(`${API}/api/notifications`);
    if (!notifications.length) {
      const empty = document.createElement("div");
      empty.className = "registration-empty";
      empty.textContent = "No new activity yet.";
      list.append(empty);
      return;
    }
    notifications.forEach(notification => {
      const card = document.createElement("article");
      card.className = `notification-card${notification.readAt ? "" : " unread"}`;
      const title = document.createElement("strong");
      title.textContent = notification.title;
      const message = document.createElement("p");
      message.textContent = notification.message;
      const date = document.createElement("small");
      date.textContent = displayDate(notification.createdAt);
      card.append(title, message, date);
      if (!notification.readAt) {
        const read = document.createElement("button");
        read.type = "button";
        read.className = "button button-outline button-small";
        read.textContent = "Mark read";
        read.addEventListener("click", async () => {
          read.disabled = true;
          try {
            await requestJson(`${API}/api/notifications/${notification.id}/read`, { method: "POST" });
            await loadOrganizerNotifications();
          } catch (error) {
            read.disabled = false;
            setMessage("manageMessage", error.message, true);
          }
        });
        card.append(read);
      }
      list.append(card);
    });
  } catch (error) {
    const failure = document.createElement("div");
    failure.className = "registration-empty";
    failure.textContent = error.message;
    list.append(failure);
  }
}

async function downloadReport(eventId) {
  try {
    const response = await fetch(`${API}/api/organizer/events/${eventId}/report.csv`, { headers: headers() });
    if (!response.ok) {
      const data = await response.json();
      throw new Error(apiError(data, "Could not download the participation report."));
    }
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `event-${eventId}-participation.csv`;
    link.click();
    URL.revokeObjectURL(url);
    setMessage("manageMessage", "Participation report downloaded.");
  } catch (error) {
    setMessage("manageMessage", error.message, true);
  }
}

async function createAdministrator(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const submit = form.querySelector('button[type="submit"]');
  const values = new FormData(form);
  setMessage("adminCreateMessage", "");
  submit.disabled = true;
  submit.textContent = "Creating…";
  try {
    const result = await requestJson(`${API}/api/admin/users`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        fullName: values.get("fullName").trim(),
        email: values.get("email").trim()
      })
    });
    form.reset();
    setMessage("adminCreateMessage", result.message);
  } catch (error) {
    setMessage("adminCreateMessage", error.message || "Could not create the administrator.", true);
  } finally {
    submit.disabled = false;
    submit.innerHTML = 'Create administrator <span aria-hidden="true">↗</span>';
  }
}

document.getElementById("logout").addEventListener("click", () => {
  localStorage.removeItem("campusToken");
  localStorage.removeItem("campusUser");
  location.href = "/events";
});
document.getElementById("closeAttendees").addEventListener("click", () => {
  document.getElementById("attendeePanel").hidden = true;
});
document.getElementById("closeInvites").addEventListener("click", () => {
  document.getElementById("invitePanel").hidden = true;
});
document.getElementById("inviteForm").addEventListener("submit", sendInvitations);

if (requireOrganizer()) {
  document.getElementById("eventForm").addEventListener("submit", createEvent);
  if (roles.includes("ROLE_ADMIN")) {
    document.getElementById("adminManagement").hidden = false;
    document.getElementById("adminCreateForm").addEventListener("submit", createAdministrator);
  }
  loadCategories();
  loadOrganizerEvents();
  loadOrganizerNotifications();
}
