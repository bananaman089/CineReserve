function roleLabel(role) {
    if (role === 'ADMIN') return 'Администратор';
    if (role === 'CASHIER') return 'Касиер';
    if (role === 'CLIENT') return 'Клиент';
    return role;
}

async function logoutNow() {
    sessionStorage.removeItem('cineUsername');
    try {
        await fetch('/logout', {
            method: 'POST',
            credentials: 'same-origin',
            redirect: 'follow'
        });
    } catch {
        /* ignore */
    }
    window.location.href = '/index.html?logout';
}

function userRow(u, myName) {
    const isMe = u.username === myName;
    return `
        <tr>
            <td>${u.username}${isMe ? ' <span class="muted">(ти)</span>' : ''}</td>
            <td>
                <button type="button" class="btn btn-danger btn-delete-user" data-id="${u.id}" data-username="${u.username}" data-self="${isMe ? '1' : '0'}">
                    Изтрий
                </button>
            </td>
        </tr>
    `;
}

function bindDeleteButtons(root) {
    root.querySelectorAll('.btn-delete-user').forEach(btn => {
        btn.addEventListener('click', async () => {
            const id = btn.dataset.id;
            const username = btn.dataset.username;
            const isSelf = btn.dataset.self === '1';
            const msg = document.getElementById('staffMsg');

            const confirmText = isSelf
                ? `Сигурен ли си, че искаш да изтриеш СОБСТВЕНИЯ си акаунт "${username}"? Ще бъдеш изведен от системата.`
                : `Изтриване на акаунт "${username}"?`;

            if (!confirm(confirmText)) return;

            try {
                const result = await api('/api/admin/staff/' + id, { method: 'DELETE' });
                if (result.selfDeleted) {
                    showMsg(msg, 'Акаунтът е изтрит. Излизане...', true);
                    await logoutNow();
                    return;
                }
                showMsg(msg, `Акаунтът "${username}" е изтрит.`, true);
                await loadStaff();
            } catch (err) {
                showMsg(msg, err.message, false);
            }
        });
    });
}

function fillAccountColumn(bodyId, countId, users, myName) {
    const body = document.getElementById(bodyId);
    const count = document.getElementById(countId);
    if (!body) return;
    if (count) count.textContent = `(${users.length})`;
    body.innerHTML = users.length
        ? users.map(u => userRow(u, myName)).join('')
        : '<tr><td colspan="2">Няма акаунти</td></tr>';
    bindDeleteButtons(body);
}

async function loadStaff() {
    const me = typeof loadCurrentUser === 'function' ? await loadCurrentUser() : null;
    const myName = me && me.username ? me.username : '';
    const staff = await api('/api/admin/staff');

    fillAccountColumn('adminBody', 'adminCount', staff.filter(u => u.role === 'ADMIN'), myName);
    fillAccountColumn('cashierBody', 'cashierCount', staff.filter(u => u.role === 'CASHIER'), myName);
    fillAccountColumn('clientBody', 'clientCount', staff.filter(u => u.role === 'CLIENT'), myName);
}

async function initAdminStaff() {
    document.getElementById('staffForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const msg = document.getElementById('staffMsg');
        try {
            await api('/api/admin/staff', {
                method: 'POST',
                body: JSON.stringify({
                    username: document.getElementById('staffUsername').value.trim(),
                    password: document.getElementById('staffPassword').value,
                    role: document.getElementById('staffRole').value
                })
            });
            e.target.reset();
            showMsg(msg, 'Акаунтът е създаден.', true);
            await loadStaff();
        } catch (err) {
            showMsg(msg, err.message, false);
        }
    });

    await loadStaff();
}

window.initAdminStaff = initAdminStaff;

if (!window.__adminSoftNavBoot) {
    initAdminStaff()
        .catch(err => showMsg(document.getElementById('staffMsg'), err.message, false))
        .finally(() => revealAdminPage());
}
