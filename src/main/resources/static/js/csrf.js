// Same-origin state-changing requests must carry the session CSRF token.
function csrfFetch(url, init = {}) {
    const requestUrl = new URL(url, document.baseURI);
    const method = (init.method || "GET").toUpperCase();
    const unsafe = ["POST", "PUT", "PATCH", "DELETE"].includes(method);

    if (!unsafe) {
        return fetch(url, init);
    }

    if (requestUrl.origin !== window.location.origin) {
        return Promise.reject(new Error("csrfFetch only supports same-origin unsafe requests"));
    }

    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const headerName = document.querySelector('meta[name="_csrf_header"]')?.content;
    if (!token || !headerName) {
        return Promise.reject(new Error("CSRF metadata is missing"));
    }

    const headers = new Headers(init.headers);
    headers.set(headerName, token);
    return fetch(url, { ...init, method, headers });
}

window.csrfFetch = csrfFetch;
