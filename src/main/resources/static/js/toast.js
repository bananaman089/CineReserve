function showToast(message, type = 'info') {
    let host = document.getElementById('toastHost');
    if (!host) {
        host = document.createElement('div');
        host.id = 'toastHost';
        host.className = 'toast-host';
        document.body.appendChild(host);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    host.appendChild(toast);

    requestAnimationFrame(() => toast.classList.add('is-visible'));

    setTimeout(() => {
        toast.classList.remove('is-visible');
        setTimeout(() => toast.remove(), 280);
    }, 3500);
}

function clearUrlParams(keys) {
    const url = new URL(window.location.href);
    let changed = false;
    keys.forEach(key => {
        if (url.searchParams.has(key)) {
            url.searchParams.delete(key);
            changed = true;
        }
    });
    if (changed) {
        const clean = url.pathname + (url.searchParams.toString() ? `?${url.searchParams}` : '') + url.hash;
        history.replaceState({}, '', clean);
    }
}

function handleAuthToasts() {
    const params = new URLSearchParams(window.location.search);
    if (params.has('error')) {
        showToast('Грешно потребителско име или парола', 'error');
        clearUrlParams(['error']);
    }
    if (params.get('login') === 'ok') {
        showToast('Влязохте успешно', 'success');
        clearUrlParams(['login']);
    }
}

handleAuthToasts();
