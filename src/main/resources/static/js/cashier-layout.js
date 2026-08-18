const CASHIER_NAV_ITEMS = [
    { key: 'home', href: '/cashier.html', label: 'Табло' },
    { key: 'program', href: '/cashier-program.html', label: 'Програма' },
    { key: 'bar', href: '/cashier-bar.html', label: 'Храни и напитки' }
];

function escapeCashierHtml(value) {
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function setCashierGreeting(name) {
    const greeting = document.getElementById('cashierGreeting');
    if (!greeting) return;
    const username = String(name || '').trim();
    if (!username) {
        greeting.textContent = 'Здравей!';
        return;
    }
    greeting.innerHTML = `Здравей, <span class="admin-greeting-name">${escapeCashierHtml(username)}</span>!`;
}

function mountCashierSidebar(activeKey) {
    const aside = document.getElementById('cashierSidebar');
    if (!aside) return;

    const links = CASHIER_NAV_ITEMS.map(item => {
        const active = item.key === activeKey ? ' is-active' : '';
        return `<a class="admin-nav-link${active}" href="${item.href}">${item.label}</a>`;
    }).join('');

    aside.innerHTML = `
        <p class="admin-sidebar-label">Каса</p>
        <nav class="admin-nav">${links}</nav>
        <a class="admin-nav-link admin-nav-secondary" href="/client.html">Клиентски изглед</a>
    `;
}
