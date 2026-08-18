async function loadAdminProducts() {
    const body = document.getElementById('productAdminBody');
    const products = await api('/api/admin/products');
    body.innerHTML = products.map(p => `
        <tr data-id="${p.id}">
            <td>${p.name}<div class="muted">${p.category}</div></td>
            <td>${p.sizeLabel || '—'}</td>
            <td>
                <input type="number" step="0.01" min="0.1" value="${p.price}" class="price-input" style="width:90px;">
            </td>
            <td>${p.active ? 'Да' : 'Не'}</td>
            <td>
                <button type="button" class="btn btn-secondary btn-save-price">Цена</button>
                <button type="button" class="btn btn-danger btn-deactivate">${p.active ? 'Скрий' : 'Скрит'}</button>
            </td>
        </tr>
    `).join('') || '<tr><td colspan="5">Няма продукти</td></tr>';

    body.querySelectorAll('tr[data-id]').forEach(row => {
        const id = Number(row.dataset.id);
        row.querySelector('.btn-save-price').addEventListener('click', async () => {
            const price = Number(row.querySelector('.price-input').value);
            try {
                await api('/api/admin/products/' + id, {
                    method: 'PUT',
                    body: JSON.stringify({ price, active: true })
                });
                showMsg(document.getElementById('productAdminMsg'), 'Цената е обновена.', true);
                await loadAdminProducts();
            } catch (err) {
                showMsg(document.getElementById('productAdminMsg'), err.message, false);
            }
        });
        row.querySelector('.btn-deactivate').addEventListener('click', async () => {
            try {
                await api('/api/admin/products/' + id, { method: 'DELETE' });
                showMsg(document.getElementById('productAdminMsg'), 'Продуктът е деактивиран.', true);
                await loadAdminProducts();
            } catch (err) {
                showMsg(document.getElementById('productAdminMsg'), err.message, false);
            }
        });
    });
}

async function initAdminBar() {
    document.getElementById('productForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const msg = document.getElementById('productAdminMsg');
        try {
            await api('/api/admin/products', {
                method: 'POST',
                body: JSON.stringify({
                    name: document.getElementById('productName').value.trim(),
                    category: document.getElementById('productCategory').value,
                    sizeLabel: document.getElementById('productSize').value.trim(),
                    price: Number(document.getElementById('productPrice').value),
                    allowsSauce: document.getElementById('productSauce').checked,
                    active: true
                })
            });
            e.target.reset();
            showMsg(msg, 'Продуктът е добавен.', true);
            await loadAdminProducts();
        } catch (err) {
            showMsg(msg, err.message, false);
        }
    });

    await loadAdminProducts();
}

window.initAdminBar = initAdminBar;

if (!window.__adminSoftNavBoot) {
    initAdminBar()
        .catch(err => showMsg(document.getElementById('productAdminMsg'), err.message, false))
        .finally(() => revealAdminPage());
}
