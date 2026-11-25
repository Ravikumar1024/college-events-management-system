const notificationApi = window.CAMPUS_API_URL;
const notificationToken = localStorage.getItem("campusToken");
const notificationAccount = JSON.parse(localStorage.getItem("campusUser") || "null");
const notificationRoles = Array.isArray(notificationAccount?.roles) ? notificationAccount.roles : [];
const notificationHeaders = () => ({ Authorization: `Bearer ${notificationToken}` });

async function notificationRequest(url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: { ...notificationHeaders(), ...options.headers }
  });
  let data;
  try {
    data = response.status === 204 ? null : await response.json();
  } catch {
    data = null;
  }
  if (!response.ok) {
    const message = data?.details?.message || `Request failed (${response.status}).`;
    throw new Error(message);
  }
  return data;
}

function notificationLink(title, description, date, href) {
  const link = document.createElement("a");
  link.className = "dashboard-notice-link";
  link.href = href;
  const heading = document.createElement("strong");
  heading.textContent = title;
  const message = document.createElement("p");
  message.textContent = description;
  const timestamp = document.createElement("small");
  timestamp.textContent = date;
  link.append(heading, message, timestamp);
  return link;
}

function notificationRoute(notification) {
  if (notification.type === "EVENT_INVITATION") return "/my-events#invitations";
  if (notificationRoles.includes("ROLE_STUDENT")
      && ["SEAT_APPROVED", "SEAT_REJECTED"].includes(notification.type)) {
    return "/my-events";
  }
  if (notificationRoles.includes("ROLE_ORGANIZER") || notificationRoles.includes("ROLE_ADMIN")) {
    if (notification.eventId) {
      return `/organizer?eventId=${encodeURIComponent(notification.eventId)}#attendeePanel`;
    }
    return "/organizer#organizerActivity";
  }
  return "/my-events";
}

function displayNotificationDate(value) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" })
    .format(new Date(value));
}

function renderInvitationList(container, invitations, pendingIds) {
  container.replaceChildren();
  if (!notificationRoles.includes("ROLE_STUDENT")) {
    const empty = document.createElement("div");
    empty.className = "notification-empty";
    empty.textContent = "Invitations are available on student accounts.";
    container.append(empty);
    return;
  }
  const pending = invitations.filter(invitation => invitation.status === "PENDING");
  if (!pending.length) {
    const empty = document.createElement("div");
    empty.className = "notification-empty";
    empty.textContent = "No invitations waiting for a response.";
    container.append(empty);
    return;
  }
  pending.forEach(invitation => {
    pendingIds.add(String(invitation.id));
    const card = document.createElement("article");
    card.className = "dashboard-notice unread";
    card.append(notificationLink(
      invitation.eventTitle,
      "You’re invited. Open your plans to accept or decline.",
      displayNotificationDate(invitation.createdAt),
      "/my-events#invitations"
    ));
    container.append(card);
  });
}

function renderUpdates(container, notifications, pendingInvitationIds, markRead) {
  container.replaceChildren();
  const updates = notifications.filter(notification =>
    notification.type !== "EVENT_INVITATION" || !pendingInvitationIds.has(String(notification.referenceId)));
  if (!updates.length) {
    const empty = document.createElement("div");
    empty.className = "notification-empty";
    empty.textContent = "You’re all caught up.";
    container.append(empty);
    return;
  }
  updates.forEach(notification => {
    const card = document.createElement("article");
    card.className = `dashboard-notice${notification.readAt ? "" : " unread"}`;
    card.append(notificationLink(
      notification.title,
      notification.message,
      displayNotificationDate(notification.createdAt),
      notificationRoute(notification)
    ));
    if (!notification.readAt) {
      const read = document.createElement("button");
      read.type = "button";
      read.className = "button button-outline button-small";
      read.textContent = "Mark read";
      read.addEventListener("click", () => markRead(notification.id, read));
      card.append(read);
    }
    container.append(card);
  });
}

function setupNotificationCenter(wrapper) {
  const toggle = wrapper.querySelector(".notification-toggle");
  const panel = wrapper.querySelector(".dashboard-notifications");
  const badge = wrapper.querySelector(".notification-count");
  const invitationList = wrapper.querySelector(".dashboard-invitation-list");
  const updateList = wrapper.querySelector(".dashboard-notification-list");
  const message = wrapper.querySelector(".notification-panel-message");
  const setOpen = open => {
    panel.hidden = !open;
    toggle.setAttribute("aria-expanded", String(open));
    toggle.setAttribute("aria-label", open ? "Close notifications" : "Open notifications");
    if (open) loadNotifications();
  };

  async function markRead(id, button) {
    button.disabled = true;
    try {
      await notificationRequest(`${notificationApi}/api/notifications/${id}/read`, { method: "POST" });
      await loadNotifications();
    } catch (error) {
      button.disabled = false;
      message.textContent = error.message || "Could not update this notification.";
      message.classList.add("error");
    }
  }

  async function loadNotifications() {
    message.textContent = "";
    message.classList.remove("error");
    try {
      const [notifications, invitations] = await Promise.all([
        notificationRequest(`${notificationApi}/api/notifications`),
        notificationRoles.includes("ROLE_STUDENT")
          ? notificationRequest(`${notificationApi}/api/students/me/invitations`)
          : Promise.resolve([])
      ]);
      const pendingInvitationIds = new Set();
      renderInvitationList(invitationList, invitations, pendingInvitationIds);
      const unread = notifications.filter(notification =>
        !notification.readAt
        && (notification.type !== "EVENT_INVITATION"
          || !pendingInvitationIds.has(String(notification.referenceId)))).length;
      const count = unread + pendingInvitationIds.size;
      badge.hidden = count === 0;
      badge.textContent = count > 9 ? "9+" : String(count);
      renderUpdates(updateList, notifications, pendingInvitationIds, markRead);
    } catch (error) {
      badge.hidden = true;
      invitationList.replaceChildren();
      updateList.replaceChildren();
      const failure = document.createElement("div");
      failure.className = "notification-empty error";
      failure.textContent = error.message || "Could not load notifications.";
      updateList.append(failure);
    }
  }

  toggle.addEventListener("click", () => setOpen(panel.hidden));
  wrapper.querySelector(".notification-refresh").addEventListener("click", loadNotifications);
  document.addEventListener("click", event => {
    if (!wrapper.contains(event.target)) setOpen(false);
  });
  document.addEventListener("keydown", event => {
    if (event.key === "Escape" && !panel.hidden) {
      setOpen(false);
      toggle.focus();
    }
  });
  loadNotifications();
}

document.querySelectorAll("[data-notification-center]").forEach(wrapper => {
  if (notificationToken) {
    wrapper.hidden = false;
    setupNotificationCenter(wrapper);
  } else {
    wrapper.hidden = true;
  }
});
