(() => {
  const token = localStorage.getItem("campusToken");
  let account = null;
  try {
    account = JSON.parse(localStorage.getItem("campusUser") || "null");
  } catch {
    account = null;
  }
  const roles = Array.isArray(account?.roles) ? account.roles : [];
  document.body.classList.toggle("guest-shell", !token);
  const roleLabel = roles.includes("ROLE_ADMIN") ? "Administrator"
    : roles.includes("ROLE_ORGANIZER") ? "Organizer"
      : roles.includes("ROLE_STUDENT") ? "Student" : "Member";

  const sidebar = document.getElementById("dashboardSidebar");
  const toggle = document.getElementById("sidebarToggle");
  const backdrop = document.getElementById("sidebarBackdrop");
  const login = document.getElementById("sidebarLoginLink");
  const logout = document.getElementById("logout");
  const bell = document.querySelector("[data-notification-center]");
  const profileWrap = document.getElementById("profileMenuWrap");
  const profileToggle = document.getElementById("profileMenuToggle");
  const profileMenu = document.getElementById("profileMenu");

  document.getElementById("sidebarDashboardLink").hidden = !token;
  document.getElementById("sidebarPlansLink").hidden = !roles.includes("ROLE_STUDENT");
  document.getElementById("sidebarHostLink").hidden =
    !roles.includes("ROLE_ORGANIZER") && !roles.includes("ROLE_ADMIN");
  document.getElementById("adminStudentsLink").hidden = !roles.includes("ROLE_ADMIN");
  login.hidden = Boolean(token);
  logout.hidden = !token;
  bell.hidden = !token;
  profileWrap.hidden = !token;

  if (token) {
    const name = account?.fullName?.trim() || "Campus member";
    const email = account?.email?.trim() || "";
    const initials = name.split(/\s+/).slice(0, 2).map(part => part[0]).join("").toUpperCase() || "O";
    document.getElementById("profileName").textContent = name;
    document.getElementById("profileEmail").textContent = email;
    document.getElementById("profileRole").textContent = roleLabel;
    document.getElementById("profileAvatar").textContent = initials;
    document.getElementById("profileMenuName").textContent = name;
    document.getElementById("profileMenuEmail").textContent = email || "No email on file";
    document.getElementById("profileMenuRole").textContent = roleLabel;
    document.getElementById("profileMenuAvatar").textContent = initials;
  }

  const setSidebarOpen = open => {
    if (open) {
      const notificationPanel = document.querySelector(".dashboard-notifications");
      const notificationToggle = document.querySelector(".notification-toggle");
      if (notificationPanel) notificationPanel.hidden = true;
      notificationToggle?.setAttribute("aria-expanded", "false");
      notificationToggle?.setAttribute("aria-label", "Open notifications");
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
  toggle.addEventListener("click", () => setSidebarOpen(!sidebar.classList.contains("is-open")));
  backdrop.addEventListener("click", () => setSidebarOpen(false));
  sidebar.querySelectorAll(".sidebar-link").forEach(link => {
    link.addEventListener("click", () => setSidebarOpen(false));
  });

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

  document.getElementById("profileLogout").addEventListener("click", () => {
    localStorage.removeItem("campusToken");
    localStorage.removeItem("campusUser");
    location.href = "/events";
  });
})();
