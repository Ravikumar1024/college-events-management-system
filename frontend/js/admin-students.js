const API = window.CAMPUS_API_URL;
const token = localStorage.getItem("campusToken");
const account = JSON.parse(localStorage.getItem("campusUser") || "null");
const roles = Array.isArray(account?.roles) ? account.roles : [];
const isAdmin = roles.includes("ROLE_ADMIN");
const authHeaders = () => ({ Authorization: `Bearer ${token}` });
const displayDate = value => new Intl.DateTimeFormat(undefined, { dateStyle: "medium" }).format(new Date(value));

function setupDashboardShell() {
  const name = account?.fullName?.trim() || "Campus member";
  const email = account?.email?.trim() || "";
  const role = roles.includes("ROLE_ADMIN") ? "Administrator"
    : roles.includes("ROLE_ORGANIZER") ? "Organizer"
      : roles.includes("ROLE_STUDENT") ? "Student" : "Member";
  const initials = name.split(/\s+/).slice(0, 2).map(part => part[0]).join("").toUpperCase();
  for (const id of ["profileName", "profileMenuName"]) {
    document.getElementById(id).textContent = name;
  }
  for (const id of ["profileAvatar", "profileMenuAvatar"]) {
    document.getElementById(id).textContent = initials;
  }
  for (const id of ["profileEmail", "profileMenuEmail"]) {
    document.getElementById(id).textContent = email;
  }
  document.getElementById("profileRole").textContent = role;
  document.getElementById("profileMenuRole").textContent = role;
  document.getElementById("sidebarPlansLink").hidden = !roles.includes("ROLE_STUDENT");
  document.getElementById("sidebarHostLink").hidden =
    !roles.includes("ROLE_ORGANIZER") && !roles.includes("ROLE_ADMIN");
  document.getElementById("adminStudentsLink").hidden = !isAdmin;

  const sidebar = document.getElementById("dashboardSidebar");
  const sidebarToggle = document.getElementById("sidebarToggle");
  const backdrop = document.getElementById("sidebarBackdrop");
  const setSidebarOpen = open => {
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
    sidebarToggle.classList.toggle("is-open", open);
    document.body.classList.toggle("sidebar-open", open);
    backdrop.hidden = !open;
    sidebarToggle.setAttribute("aria-expanded", String(open));
    sidebarToggle.setAttribute("aria-label", open ? "Close navigation" : "Open navigation");
  };
  sidebarToggle.addEventListener("click", () => setSidebarOpen(!sidebar.classList.contains("is-open")));
  backdrop.addEventListener("click", () => setSidebarOpen(false));
  sidebar.querySelectorAll(".sidebar-link").forEach(link => {
    link.addEventListener("click", () => setSidebarOpen(false));
  });

  const profileWrap = document.getElementById("profileMenuWrap");
  const profileToggle = document.getElementById("profileMenuToggle");
  const profileMenu = document.getElementById("profileMenu");
  const setProfileOpen = open => {
    profileMenu.hidden = !open;
    profileToggle.setAttribute("aria-expanded", String(open));
    profileToggle.setAttribute("aria-label", open ? "Close profile details" : "Open profile details");
  };
  profileToggle.addEventListener("click", () => setProfileOpen(profileMenu.hidden));
  document.addEventListener("click", event => {
    if (!profileWrap.contains(event.target)) setProfileOpen(false);
  });
  document.addEventListener("keydown", event => {
    if (event.key === "Escape") {
      setSidebarOpen(false);
      if (!profileMenu.hidden) {
        setProfileOpen(false);
        profileToggle.focus();
      }
    }
  });

  const logout = () => {
    localStorage.removeItem("campusToken");
    localStorage.removeItem("campusUser");
    location.href = "/events";
  };
  document.getElementById("logout").addEventListener("click", logout);
  document.getElementById("profileLogout").addEventListener("click", logout);
}

function showMessage(text, isError = false) {
  const message = document.getElementById("adminMessage");
  message.textContent = text;
  message.classList.toggle("error", isError);
}

function errorText(data, fallback) {
  return typeof data?.details?.message === "string" ? data.details.message : fallback;
}

async function loadStudents() {
  const rows = document.getElementById("studentRows");
  rows.replaceChildren();
  const loading = document.createElement("tr");
  const loadingCell = document.createElement("td");
  loadingCell.colSpan = 6;
  loadingCell.textContent = "Loading student accounts…";
  loading.append(loadingCell);
  rows.append(loading);
  try {
    const response = await fetch(`${API}/api/admin/students`, { headers: authHeaders() });
    const data = await response.json();
    if (!response.ok) throw new Error(errorText(data, "Could not load student accounts."));
    rows.replaceChildren();
    if (!data.length) {
      const empty = document.createElement("tr");
      const cell = document.createElement("td");
      cell.colSpan = 6;
      cell.textContent = "No student accounts found.";
      empty.append(cell);
      rows.append(empty);
      return;
    }
    data.forEach(student => rows.append(studentRow(student)));
  } catch (error) {
    rows.replaceChildren();
    showMessage(error.message, true);
  }
}

function studentRow(student) {
  const row = document.createElement("tr");
  const person = document.createElement("td");
  const name = document.createElement("strong");
  name.textContent = student.fullName;
  person.append(name);
  const number = document.createElement("td");
  number.textContent = student.studentNumber;
  const joined = document.createElement("td");
  joined.textContent = displayDate(student.createdAt);
  const address = document.createElement("td");
  address.textContent = student.email;
  const status = document.createElement("td");
  status.textContent = student.active
    ? student.emailVerified ? "Active · verified" : "Active · unverified"
    : "Deactivated";
  const action = document.createElement("td");
  if (student.active) {
    const deactivate = document.createElement("button");
    deactivate.type = "button";
    deactivate.className = "button button-outline";
    deactivate.textContent = "Deactivate";
    deactivate.addEventListener("click", () => deactivateStudent(student, deactivate));
    action.append(deactivate);
  }
  row.append(person, number, joined, address, status, action);
  return row;
}

async function deactivateStudent(student, button) {
  if (!window.confirm(`Deactivate ${student.fullName}'s account? Their event history will be kept.`)) return;
  button.disabled = true;
  button.textContent = "Deactivating…";
  try {
    const response = await fetch(`${API}/api/admin/students/${student.id}`, {
      method: "DELETE",
      headers: authHeaders()
    });
    const data = response.status === 204 ? null : await response.json();
    if (!response.ok) throw new Error(errorText(data, "Could not deactivate this student."));
    showMessage(`${student.fullName}'s account is deactivated.`);
    await loadStudents();
  } catch (error) {
    button.disabled = false;
    button.textContent = "Deactivate";
    showMessage(error.message, true);
  }
}

setupDashboardShell();

if (!token) {
  location.replace(`/login?next=${encodeURIComponent("/admin/students")}`);
} else if (!isAdmin) {
  showMessage("Administrator access is required to manage student accounts.", true);
} else {
  loadStudents();
}
