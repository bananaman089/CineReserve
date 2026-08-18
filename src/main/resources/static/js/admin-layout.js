const ADMIN_NAV_ITEMS = [
    { key: 'home', href: '/admin.html', label: 'Табло', title: 'Админ табло · CineReserve', scripts: [], init: 'initAdminHome' },
    { key: 'cinemas', href: '/admin-cinemas.html', label: 'Кина и зали', title: 'Кина и зали · Админ', scripts: ['/js/admin-cinemas.js'], init: 'initAdminCinemas' },
    { key: 'movies', href: '/admin-movies.html', label: 'Филми и прожекции', title: 'Филми и прожекции · Админ', scripts: ['/js/admin-movies.js'], init: 'initAdminMovies' },
    { key: 'program', href: '/admin-program.html', label: 'Програма', title: 'Програма · Админ', scripts: ['/js/program.js', '/js/admin-program.js'], init: 'initAdminProgram' },
    { key: 'bar', href: '/admin-bar.html', label: 'Бар продукти', title: 'Бар · Админ', scripts: ['/js/admin-bar.js'], init: 'initAdminBar' },
    { key: 'staff', href: '/admin-staff.html', label: 'Акаунти', title: 'Акаунти · Админ', scripts: ['/js/admin-staff.js'], init: 'initAdminStaff' },
    { key: 'reports', href: '/admin-reports.html', label: 'Отчети', title: 'Отчети · Админ', scripts: ['/js/admin-reports.js'], init: 'initAdminReports' }
];

let adminActiveKey = 'home';
let adminNavigating = false;

function prefersReducedMotion() {
    return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

function wait(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function waitAnimation(el, fallbackMs) {
    return new Promise(resolve => {
        if (!el || prefersReducedMotion()) {
            resolve();
            return;
        }
        let done = false;
        const finish = () => {
            if (done) return;
            done = true;
            resolve();
        };
        el.addEventListener('animationend', finish, { once: true });
        setTimeout(finish, fallbackMs);
    });
}

function revealAdminPage() {
    const main = document.querySelector('.admin-main');
    if (!main) return;

    main.classList.remove('is-leaving', 'is-loading');

    if (main.classList.contains('is-entering') || main.classList.contains('is-ready')) {
        return;
    }

    if (prefersReducedMotion()) {
        main.classList.add('is-ready');
        return;
    }

    requestAnimationFrame(() => {
        requestAnimationFrame(() => {
            main.classList.add('is-entering');
        });
    });
}

function hideAdminPageForSwap() {
    const main = document.querySelector('.admin-main');
    if (!main) return Promise.resolve();
    main.classList.remove('is-entering', 'is-ready');
    if (prefersReducedMotion()) {
        main.style.opacity = '0';
        return Promise.resolve();
    }
    main.classList.add('is-leaving');
    return waitAnimation(main, 420).then(() => {
        main.classList.remove('is-leaving');
        main.style.opacity = '0';
    });
}

function loadScriptOnce(src) {
    return new Promise((resolve, reject) => {
        const existing = document.querySelector(`script[data-admin-src="${src}"]`)
            || Array.from(document.scripts).find(s => s.getAttribute('src') === src);
        if (existing) {
            existing.dataset.loaded = '1';
            resolve();
            return;
        }
        const script = document.createElement('script');
        script.src = src;
        script.async = false;
        script.dataset.adminSrc = src;
        script.addEventListener('load', () => {
            script.dataset.loaded = '1';
            resolve();
        }, { once: true });
        script.addEventListener('error', () => reject(new Error('Неуспешно зареждане: ' + src)), { once: true });
        document.body.appendChild(script);
    });
}

async function ensureAdminScripts(scripts) {
    for (const src of scripts) {
        await loadScriptOnce(src);
    }
}

function setAdminNavActive(activeKey) {
    adminActiveKey = activeKey;
    document.querySelectorAll('[data-admin-nav]').forEach(link => {
        link.classList.toggle('is-active', link.getAttribute('data-admin-nav') === activeKey);
    });
}

function showAdminLoadingSkeleton() {
    const main = document.querySelector('.admin-main');
    if (!main) return;
    main.className = 'admin-main admin-main--page is-loading is-ready';
    main.style.removeProperty('opacity');
    main.innerHTML = `
        <div class="admin-skeleton" aria-hidden="true">
            <div class="sk-line sk-title"></div>
            <div class="sk-line sk-sub"></div>
            <div class="sk-grid">
                <div class="sk-card"></div>
                <div class="sk-card"></div>
            </div>
        </div>
    `;
}

function escapeHtml(value) {
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function setAdminGreeting(name) {
    const greeting = document.getElementById('adminGreeting');
    if (!greeting) return;

    const username = String(name || '').trim();
    if (!username) {
        greeting.textContent = 'Здравей!';
        return;
    }

    sessionStorage.setItem('cineUsername', username);
    greeting.innerHTML = `Здравей, <span class="admin-greeting-name">${escapeHtml(username)}</span>!`;
}

async function initAdminHome() {
    let user = null;
    for (let i = 0; i < 3; i++) {
        try {
            user = typeof loadCurrentUser === 'function'
                ? await loadCurrentUser()
                : await api('/api/account/me');
            if (user && user.username) break;
        } catch {
            /* retry */
        }
        await wait(150);
    }

    if (user && user.username) {
        setAdminGreeting(user.username);
    } else {
        const cached = sessionStorage.getItem('cineUsername');
        if (cached) setAdminGreeting(cached);
    }
}

async function runAdminInit(page) {
    const fn = window[page.init];
    if (typeof fn === 'function') {
        await fn();
    }
}

async function softNavigate(activeKey, { push = true } = {}) {
    const page = ADMIN_NAV_ITEMS.find(item => item.key === activeKey);
    if (!page || adminNavigating) return;
    if (activeKey === adminActiveKey && push) return;

    adminNavigating = true;
    window.__adminSoftNavBoot = true;
    setAdminNavActive(activeKey);

    try {
        await hideAdminPageForSwap();
        showAdminLoadingSkeleton();

        const [response] = await Promise.all([
            fetch(page.href, { credentials: 'same-origin' }),
            ensureAdminScripts(page.scripts),
            wait(220)
        ]);

        if (!response.ok) throw new Error('Страницата не можа да се зареди.');

        const html = await response.text();
        const doc = new DOMParser().parseFromString(html, 'text/html');
        const newMain = doc.querySelector('.admin-main');
        if (!newMain) throw new Error('Липсва съдържание.');

        await hideAdminPageForSwap();

        const main = document.querySelector('.admin-main');
        main.className = newMain.className;
        main.classList.remove('is-entering', 'is-ready', 'is-leaving', 'is-loading');
        main.style.opacity = '0';
        main.innerHTML = newMain.innerHTML;

        document.title = page.title;
        if (push) {
            history.pushState({ adminKey: activeKey }, page.title, page.href);
        }

        await runAdminInit(page);
        main.style.removeProperty('opacity');
        revealAdminPage();
    } catch (err) {
        const main = document.querySelector('.admin-main');
        if (main) {
            main.className = 'admin-main admin-main--page is-ready';
            main.style.removeProperty('opacity');
            main.innerHTML = `<h1>Грешка</h1><p class="muted">${err.message || 'Неуспешно зареждане'}</p>`;
        }
    } finally {
        adminNavigating = false;
    }
}

function goAdminPage(href) {
    const page = ADMIN_NAV_ITEMS.find(item => item.href === href || href.endsWith(item.href));
    if (page) {
        softNavigate(page.key);
        return;
    }
    window.location.href = href;
}

function mountAdminSidebar(activeKey) {
    const aside = document.getElementById('adminSidebar');
    if (!aside) return;

    adminActiveKey = activeKey;

    if (!document.getElementById('adminPlayfair')) {
        const font = document.createElement('link');
        font.id = 'adminPlayfair';
        font.rel = 'stylesheet';
        font.href = 'https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,500;0,700;1,500&display=swap';
        document.head.appendChild(font);
    }

    const links = ADMIN_NAV_ITEMS.map(item => {
        const active = item.key === activeKey ? ' is-active' : '';
        return `<a class="admin-nav-link${active}" href="${item.href}" data-admin-nav="${item.key}">${item.label}</a>`;
    }).join('');

    aside.innerHTML = `
        <p class="admin-sidebar-label">Управление</p>
        <nav class="admin-nav">${links}</nav>
        <a class="admin-nav-link admin-nav-secondary" href="/client.html">Клиентски изглед</a>
    `;

    aside.querySelectorAll('[data-admin-nav]').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            softNavigate(link.getAttribute('data-admin-nav'));
        });
    });

    if (!window.__adminPopstateBound) {
        window.__adminPopstateBound = true;
        window.addEventListener('popstate', (event) => {
            const key = event.state?.adminKey
                || ADMIN_NAV_ITEMS.find(item => location.pathname.endsWith(item.href.replace(/^\//, '')) || location.pathname.endsWith(item.href))?.key
                || 'home';
            softNavigate(key, { push: false });
        });
    }

    history.replaceState({ adminKey: activeKey }, document.title, location.href);
    setTimeout(revealAdminPage, 2500);
}

window.initAdminHome = initAdminHome;
