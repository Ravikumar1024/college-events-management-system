const API = window.CAMPUS_API_URL;
const allowedNext = new Set(["/events", "/my-events", "/organizer", "/dashboard"]);
const routeRoles = {
  "/events": null,
  "/my-events": ["ROLE_STUDENT"],
  "/organizer": ["ROLE_ORGANIZER", "ROLE_ADMIN"],
  "/dashboard": ["ROLE_STUDENT", "ROLE_ORGANIZER", "ROLE_ADMIN"]
};

async function messageFrom(response) {
  let data;
  try {
    data = await response.json();
  } catch {
    return response.status === 401
      ? "Your session has expired. Please log in again."
      : response.status === 403
        ? "You don’t have permission for this action. Check that you selected a role assigned to your account."
        : `Request failed (${response.status}). Please try again.`;
  }
  if (typeof data?.message === "string") return data.message;
  const details = data?.details;
  if (typeof details?.message === "string") return details.message;
  if (details && typeof details === "object") return Object.values(details).join(" ");
  if (response.status === 401) return "Email or password is incorrect.";
  if (response.status === 403) return "You don’t have permission for this action. Check that you selected a role assigned to your account.";
  return "Unable to complete your request. Please try again.";
}

function requestedPage() {
  const requested = new URLSearchParams(location.search).get("next") || "/events";
  const path = requested.split("#", 1)[0];
  return allowedNext.has(path) ? requested : "/events";
}

function setRoleFromRequestedPage(form) {
  const path = requestedPage().split("#", 1)[0];
  const role = path === "/organizer" ? "ROLE_ORGANIZER"
    : path === "/my-events" ? "ROLE_STUDENT" : null;
  const select = form.elements.namedItem("loginRole");
  if (role && select) select.value = role;
}

function bindAuth(formId, path, fields) {
  const form = document.getElementById(formId);
  if (!form) return;
  setRoleFromRequestedPage(form);
  form.addEventListener("submit", async event => {
    event.preventDefault();
    const message = document.getElementById("message");
    const submit = form.querySelector('button[type="submit"]');
    message.textContent = "";
    submit.disabled = true;
    submit.textContent = "One moment…";
    const values = new FormData(form);
    const body = Object.fromEntries(fields.map(field => [field, values.get(field).trim()]));
    if (body.password) body.password = values.get("password");
    try {
      const response = await fetch(`${API}/api/auth/${path}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
      });
      if (!response.ok) throw new Error(await messageFrom(response));
      const data = await response.json();
      if (path === "register") {
        location.href = `/verify-email?email=${encodeURIComponent(data.email)}`;
        return;
      }
      const destination = requestedPage();
      const destinationPath = destination.split("#", 1)[0];
      const roles = Array.isArray(data.roles) ? data.roles : [];
      const selectedRole = values.get("loginRole");
      const roleIsValidForDestination = !routeRoles[destinationPath]
        || routeRoles[destinationPath].some(role => roles.includes(role));
      const selectedRoleIsValid = destinationPath === "/dashboard" || destinationPath === "/organizer"
        ? roles.some(role => routeRoles[destinationPath].includes(role))
        : roles.includes(selectedRole);
      if (path === "login" && (!selectedRoleIsValid || !roleIsValidForDestination)) {
        const label = selectedRole === "ROLE_ADMIN" ? "Administrator"
          : selectedRole === "ROLE_ORGANIZER" ? "Event organizer" : "Student";
        const accessMessage = `This account doesn’t have ${label} access. Select a role assigned to your account, or contact your campus administrator.`;
        message.textContent = accessMessage;
        localStorage.removeItem("campusToken");
        localStorage.removeItem("campusUser");
        submit.disabled = false;
        submit.innerHTML = '<span>Sign in</span><span class="auth-submit-arrow" aria-hidden="true">→</span>';
        return;
      }
      const finalDestination = path === "register" && !roleIsValidForDestination ? "/events" : destination;
      localStorage.setItem("campusToken", data.token);
      localStorage.setItem("campusUser", JSON.stringify(data));
      location.href = finalDestination;
    } catch (error) {
      message.textContent = error instanceof TypeError
        ? "Could not reach Offbeat. Check that the server is running and try again."
        : error.message || "Unable to authenticate. Please try again.";
      submit.disabled = false;
      submit.innerHTML = path === "login"
        ? '<span>Sign in</span><span class="auth-submit-arrow" aria-hidden="true">→</span>'
        : '<span>Create student account</span><span class="auth-submit-arrow" aria-hidden="true">→</span>';
    }
  });
}

bindAuth("loginForm", "login", ["email", "password"]);
bindAuth("registerForm", "register", ["fullName", "email", "password", "studentNumber"]);

document.querySelectorAll("[data-password-toggle]").forEach(toggle => {
  const input = document.getElementById(toggle.getAttribute("aria-controls"));
  if (!input) return;
  toggle.addEventListener("click", () => {
    const visible = input.type === "password";
    input.type = visible ? "text" : "password";
    toggle.setAttribute("aria-label", visible ? "Hide password" : "Show password");
    toggle.setAttribute("aria-pressed", String(visible));
  });
});
