(() => {
  const originalFetch = window.fetch.bind(window);
  let logoutInProgress = false;

  window.fetch = async (input, init) => {
    const requestUrl = typeof input === "string" || input instanceof URL ? input : input.url;
    const url = new URL(requestUrl, window.location.href);
    const headers = new Headers(init?.headers || (input instanceof Request ? input.headers : undefined));
    const authenticatedApiRequest = url.pathname.startsWith("/api/")
      && /^Bearer\s+\S+$/i.test(headers.get("Authorization") || "");
    const response = await originalFetch(input, init);

    if (response.status === 401 && authenticatedApiRequest && !logoutInProgress) {
      logoutInProgress = true;
      localStorage.removeItem("campusToken");
      localStorage.removeItem("campusUser");
      const returnTo = `${window.location.pathname}${window.location.hash}`;
      window.location.replace(`/login?reason=session-expired&next=${encodeURIComponent(returnTo)}`);
    }

    return response;
  };
})();
