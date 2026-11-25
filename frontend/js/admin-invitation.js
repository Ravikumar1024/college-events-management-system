const API = window.CAMPUS_API_URL;
const invitationToken = new URLSearchParams(location.search).get("token");
const form = document.getElementById("adminSetupForm");
const message = document.getElementById("adminSetupMessage");
const success = document.getElementById("adminSetupSuccess");

if (!invitationToken) {
  message.textContent = "This administrator invitation link is invalid or expired.";
  message.classList.add("error");
  form.hidden = true;
}

form.addEventListener("submit", async event => {
  event.preventDefault();
  const password = document.getElementById("adminPassword").value;
  const confirmPassword = document.getElementById("adminPasswordConfirm").value;
  const submit = form.querySelector('button[type="submit"]');
  message.textContent = "";
  message.classList.remove("error");
  if (password !== confirmPassword) {
    message.textContent = "The passwords do not match.";
    message.classList.add("error");
    return;
  }

  submit.disabled = true;
  submit.textContent = "Saving…";
  try {
    const response = await fetch(`${API}/api/auth/admin-invitations/complete`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ token: invitationToken, password })
    });
    let data;
    try {
      data = await response.json();
    } catch {
      data = null;
    }
    const errorMessage = data?.details?.message || data?.message;
    if (!response.ok) throw new Error(errorMessage || "Could not set your password.");
    form.hidden = true;
    success.hidden = false;
    message.textContent = data.message;
  } catch (error) {
    message.textContent = error instanceof TypeError
      ? "Could not reach Offbeat. Please try again."
      : error.message || "Could not set your password.";
    message.classList.add("error");
  } finally {
    if (!form.hidden) {
      submit.disabled = false;
      submit.innerHTML = 'Save password <span aria-hidden="true">→</span>';
    }
  }
});
