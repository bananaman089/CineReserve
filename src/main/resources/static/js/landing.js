const modal = document.getElementById('authModal');
const loginForm = document.getElementById('loginForm');
const registerForm = document.getElementById('registerForm');

function unlockPageScroll() {
    document.body.classList.remove('modal-open');
    document.body.style.overflow = '';
    document.documentElement.style.overflow = '';
}

function lockPageScroll() {
    document.body.classList.add('modal-open');
}

function openModal(tab) {
    modal.hidden = false;
    lockPageScroll();
    switchTab(tab || 'login');
}

function closeModal() {
    modal.hidden = true;
    unlockPageScroll();
}

function switchTab(tab) {
    document.querySelectorAll('.modal-tabs .tab').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.tab === tab);
    });
    loginForm.hidden = tab !== 'login';
    registerForm.hidden = tab !== 'register';
}

// Ако модалът е затворен, скролът винаги трябва да е отключен
function syncScrollWithModal() {
    if (modal.hidden) {
        unlockPageScroll();
    }
}

document.getElementById('btnOpenLogin').addEventListener('click', () => openModal('login'));
document.getElementById('btnOpenRegister').addEventListener('click', () => openModal('register'));
document.getElementById('btnHeroLogin').addEventListener('click', () => openModal('login'));
document.getElementById('btnHeroRegister').addEventListener('click', () => openModal('register'));
document.getElementById('btnCloseModal').addEventListener('click', (e) => {
    e.preventDefault();
    e.stopPropagation();
    closeModal();
});

// Кликването извън картата НЕ затваря менюто — само бутонът ×.
modal.addEventListener('click', (e) => {
    e.stopPropagation();
});

document.querySelectorAll('.modal-tabs .tab').forEach(btn => {
    btn.addEventListener('click', () => switchTab(btn.dataset.tab));
});

registerForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const msg = document.getElementById('regMsg');
    try {
        await api('/api/auth/register', {
            method: 'POST',
            body: JSON.stringify({
                username: document.getElementById('regUsername').value.trim(),
                password: document.getElementById('regPassword').value
            })
        });
        showMsg(msg, 'Готово! Сега влез с новия акаунт.', true);
        switchTab('login');
    } catch (err) {
        showMsg(msg, err.message, false);
    }
});

loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const submitBtn = loginForm.querySelector('button[type="submit"]');
    if (submitBtn) submitBtn.disabled = true;

    try {
        const body = new URLSearchParams(new FormData(loginForm));
        const loginName = (body.get('username') || '').trim();

        // изчистваме стара сесия, за да не остане предишният user
        try {
            await fetch('/logout', {
                method: 'POST',
                credentials: 'same-origin',
                redirect: 'follow'
            });
        } catch {
            /* ignore */
        }
        sessionStorage.removeItem('cineUsername');

        const res = await fetch('/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body,
            credentials: 'same-origin',
            redirect: 'follow'
        });

        const finalUrl = res.url || '';
        const loggedIn = /\/(admin|cashier|client)\.html/.test(finalUrl);

        if (!loggedIn || finalUrl.includes('error')) {
            showToast('Грешно потребителско име или парола', 'error');
            return;
        }

        // взимаме текущия профил (с кратки опити след login)
        let me = null;
        for (let i = 0; i < 4; i++) {
            try {
                me = await api('/api/account/me');
                if (me && me.username) break;
            } catch {
                me = null;
            }
            await new Promise(r => setTimeout(r, 120));
        }

        const username = (me && me.username) ? me.username : loginName;
        if (!username) {
            showToast('Грешно потребителско име или парола', 'error');
            return;
        }

        sessionStorage.setItem('cineUsername', username);
        showToast('Влязохте успешно', 'success');
        closeModal();

        let target = finalUrl.split('?')[0];
        if (me && me.role === 'ADMIN') target = '/admin.html';
        else if (me && me.role === 'CASHIER') target = '/cashier.html';
        else if (me && me.role === 'CLIENT') target = '/client.html';
        else if (!target || target.includes('index')) target = '/client.html';

        setTimeout(() => {
            window.location.href = target;
        }, 900);
    } catch {
        showToast('Грешно потребителско име или парола', 'error');
    } finally {
        if (submitBtn) submitBtn.disabled = false;
    }
});

const params = new URLSearchParams(location.search);
if (params.has('error')) {
    openModal('login');
} else {
    unlockPageScroll();
}

async function loadLandingProgram() {
    const msg = document.getElementById('landingMsg');
    try {
        const projections = await api('/api/projections');
        const list = document.getElementById('landingProgram');
        renderMovieProgram(list, projections, {
            mode: 'button',
            onSelect: () => openModal('login')
        });
        if (!projections.length) {
            showMsg(msg, 'Все още няма публикувани прожекции.', true);
        }
    } catch (err) {
        showMsg(msg, err.message, false);
    }
}

// предпазна мрежа: ако модалът е скрит, отключи скрола
setInterval(syncScrollWithModal, 800);

loadLandingProgram();
