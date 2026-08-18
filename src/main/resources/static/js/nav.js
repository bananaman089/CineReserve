async function loadCurrentUser() {
    try {
        return await api('/api/account/me');
    } catch {
        return null;
    }
}

function closeAllMenus() {
    document.querySelectorAll('.burger-menu.open').forEach(m => m.classList.remove('open'));
    document.querySelectorAll('.burger-btn[aria-expanded="true"]').forEach(b => b.setAttribute('aria-expanded', 'false'));
}

function moneyEur(value) {
    return Number(value || 0).toFixed(2) + ' €';
}

/**
 * Меню според ролята:
 * CLIENT  -> акаунт, билети, баланс, бар меню (цени), програма
 * CASHIER -> каса, програма, изход (без баланс/акаунт)
 * ADMIN   -> админ панел, клиентски изглед, изход (без каса, без баланс)
 */
async function mountAppNav(options = {}) {
    const user = await loadCurrentUser();
    const header = document.querySelector('header.topbar');
    if (!header || !user) return user;

    if (user.username) {
        sessionStorage.setItem('cineUsername', user.username);
    }

    const role = user.role;
    const isAdmin = role === 'ADMIN';
    const isCashier = role === 'CASHIER';
    const isClient = role === 'CLIENT';

    let brandHref = '/client.html';
    if (isAdmin) brandHref = '/admin.html';
    if (isCashier) brandHref = '/cashier.html';

    const topButtons = [];
    const onAdminPage = /\/admin(-[\w-]+)?\.html$/.test(window.location.pathname);
    const onCashierPage = /\/cashier(-[\w-]+)?\.html$/.test(window.location.pathname);
    if (isAdmin && !onAdminPage) {
        topButtons.push(`<a class="btn btn-secondary admin-return-btn" href="/admin.html">← Админ панел</a>`);
    }
    if (isCashier && !onCashierPage) {
        topButtons.push(`<a class="btn btn-secondary" href="/cashier.html">Каса</a>`);
    }

    let menuItems = '';
    if (isClient) {
        menuItems = `
            <div class="burger-user">
                <strong>${user.username}</strong>
                <span>Баланс: ${moneyEur(user.balance)}</span>
            </div>
            <a href="/account.html">Акаунт</a>
            <a href="/tickets.html">Моите билети</a>
            <a href="/balance.html">Баланс / зареждане</a>
            <a href="/menu.html">Бар меню (цени)</a>
            <a href="/client.html">Програма</a>
        `;
    } else if (isCashier) {
        menuItems = `
            <div class="burger-user">
                <strong>${user.username}</strong>
                <span>Роля: Касиер</span>
            </div>
            <a href="/cashier.html">Касиерско табло</a>
            <a href="/cashier-program.html">Програма</a>
            <a href="/cashier-bar.html">Храни и напитки</a>
        `;
    } else if (isAdmin) {
        menuItems = `
            <div class="burger-user">
                <strong>${user.username}</strong>
                <span>Роля: Администратор</span>
            </div>
            <a href="/admin.html">Админ табло</a>
            <a href="/client.html">Клиентски изглед</a>
        `;
    }

    header.innerHTML = `
        <a class="brand" href="${brandHref}">CineReserve</a>
        <div class="nav-actions">
            ${topButtons.join('')}
            <div class="burger-wrap">
                <button type="button" class="burger-btn" aria-label="Меню" aria-expanded="false">
                    <span></span><span></span><span></span>
                </button>
                <div class="burger-menu">
                    ${menuItems}
                    <form action="/logout" method="post" id="logoutForm">
                        <button type="submit">Изход</button>
                    </form>
                </div>
            </div>
        </div>
    `;

    const btn = header.querySelector('.burger-btn');
    const menu = header.querySelector('.burger-menu');
    btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const open = menu.classList.toggle('open');
        btn.setAttribute('aria-expanded', open ? 'true' : 'false');
    });

    document.addEventListener('click', () => closeAllMenus());
    menu.addEventListener('click', (e) => e.stopPropagation());

    const logoutForm = header.querySelector('#logoutForm');
    if (logoutForm) {
        logoutForm.addEventListener('submit', () => {
            sessionStorage.removeItem('cineUsername');
        });
    }

    if (typeof setAdminGreeting === 'function' && user.username) {
        setAdminGreeting(user.username);
    }

    return user;
}
