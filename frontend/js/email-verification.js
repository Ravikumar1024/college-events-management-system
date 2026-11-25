const API = window.CAMPUS_API_URL;
const params = new URLSearchParams(location.search);
const emailInput = document.getElementById("verificationEmail");
const otpForm = document.getElementById("otpForm");
const message = document.getElementById("verificationMessage");
const activationLinkSection = document.getElementById("activationLinkSection");
const activationToken = params.get("token");

if (params.has("email")) emailInput.value = params.get("email");
activationLinkSection.hidden = !activationToken;

function showVerificationMessage(text, isError = false) {
  message.textContent = text;
  message.classList.toggle("error", isError);
}

function responseMessage(data, fallback) {
  const details = data?.details;
  if (typeof details?.message === "string") return details.message;
  if (data && typeof data.message === "string") return data.message;
  if (details && typeof details === "object") return Object.values(details).join(" ");
  return fallback;
}

async function postVerification(path, payload) {
  const response = await fetch(`${API}/api/auth/${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  let data;
  try {
    data = await response.json();
  } catch {
    data = null;
  }
  if (!response.ok) throw new Error(responseMessage(data, "Could not verify this email. Please try again."));
  return data;
}

function markVerified(text) {
  showVerificationMessage(text);
  otpForm.hidden = true;
  activationLinkSection.hidden = true;
  document.getElementById("resendVerification").hidden = true;
  document.getElementById("verificationSuccess").hidden = false;
}

otpForm.addEventListener("submit", async event => {
  event.preventDefault();
  showVerificationMessage("");
  const submit = otpForm.querySelector('button[type="submit"]');
  submit.disabled = true;
  submit.textContent = "Checking code…";
  try {
    const result = await postVerification("verify-otp", {
      email: emailInput.value.trim(),
      otp: document.getElementById("verificationOtp").value.trim()
    });
    if (result.verified) markVerified(result.message);
    else showVerificationMessage(result.message, true);
  } catch (error) {
    showVerificationMessage(error.message || "Could not verify this email.", true);
  } finally {
    if (!otpForm.hidden) {
      submit.disabled = false;
      submit.innerHTML = 'Verify email <span aria-hidden="true">→</span>';
    }
  }
});

document.getElementById("activateLinkButton").addEventListener("click", async event => {
  const button = event.currentTarget;
  showVerificationMessage("");
  button.disabled = true;
  button.textContent = "Activating…";
  try {
    const result = await postVerification("verify-link", { token: activationToken });
    if (result.verified) markVerified(result.message);
    else showVerificationMessage(result.message, true);
  } catch (error) {
    showVerificationMessage(error.message || "Could not activate this account.", true);
  } finally {
    if (!activationLinkSection.hidden) {
      button.disabled = false;
      button.innerHTML = 'Activate account <span aria-hidden="true">→</span>';
    }
  }
});

document.getElementById("resendVerification").addEventListener("click", async event => {
  const button = event.currentTarget;
  if (!emailInput.value.trim()) {
    showVerificationMessage("Enter your college email first.", true);
    emailInput.focus();
    return;
  }
  showVerificationMessage("");
  button.disabled = true;
  button.textContent = "Sending…";
  try {
    const result = await postVerification("resend-verification", { email: emailInput.value.trim() });
    showVerificationMessage(result.message);
  } catch (error) {
    showVerificationMessage(error.message || "Could not resend the verification email.", true);
  } finally {
    button.disabled = false;
    button.textContent = "Resend code and activation link";
  }
});
