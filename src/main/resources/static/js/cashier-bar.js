const cart = new Map();
let products = [];
let lastReceipt = null;

function money(value) {
    return Number(value || 0).toFixed(2) + ' €';
}

function sauceLabel(sauce) {
    if (sauce === 'BBQ') return 'Барбекю';
    if (sauce === 'CHEDDAR') return 'Чедър';
    return '';
}

function categoryLabel(category) {
    if (category === 'POPCORN') return 'Пуканки';
    if (category === 'DRINK') return 'Напитка';
    if (category === 'NACHOS') return 'Начос';
    return category || '';
}

function cartItems() {
    return [...cart.values()].filter(item => item.qty > 0);
}

function cartTotal() {
    return cartItems().reduce((sum, item) => sum + item.qty * Number(item.product.price), 0);
}

function getOrCreateCartItem(product) {
    if (!cart.has(product.id)) {
        cart.set(product.id, {
            product,
            qty: 0,
            sauce: product.allowsSauce ? 'CHEDDAR' : null
        });
    }
    return cart.get(product.id);
}

function renderCart() {
    const list = document.getElementById('cartList');
    const items = cartItems();
    document.getElementById('cartTotal').textContent = money(cartTotal());
    document.getElementById('btnPay').disabled = items.length === 0;

    if (!items.length) {
        list.innerHTML = '<p class="muted">Все още няма избрани продукти.</p>';
        return;
    }

    list.innerHTML = items.map(item => {
        const p = item.product;
        const sauce = p.allowsSauce
            ? `<span class="muted">сос ${sauceLabel(item.sauce)}</span>`
            : '';
        return `
            <div class="cart-row">
                <div>
                    <strong>${p.name}</strong>
                    <div class="muted">${p.sizeLabel || categoryLabel(p.category)} ${sauce}</div>
                </div>
                <div class="cart-row-right">
                    <span>${item.qty} × ${money(p.price)}</span>
                    <strong>${money(item.qty * Number(p.price))}</strong>
                </div>
            </div>
        `;
    }).join('');
}

function renderProducts() {
    const grid = document.getElementById('productGrid');
    grid.innerHTML = products.map(p => {
        const item = cart.get(p.id);
        const qty = item ? item.qty : 0;
        const selectedClass = qty > 0 ? ' selected' : '';
        const sauce = p.allowsSauce ? `
            <select class="sauce-mini" data-sauce-id="${p.id}">
                <option value="CHEDDAR" ${!item || item.sauce === 'CHEDDAR' ? 'selected' : ''}>Чедър</option>
                <option value="BBQ" ${item && item.sauce === 'BBQ' ? 'selected' : ''}>Барбекю</option>
            </select>
        ` : '';
        return `
            <div class="product-card${selectedClass}" data-id="${p.id}">
                <strong>${p.name}</strong>
                <span>${p.sizeLabel || ''} · ${categoryLabel(p.category)}</span>
                <span class="price">${money(p.price)}</span>
                ${sauce}
                <div class="qty-row">
                    <button type="button" class="qty-btn" data-delta="-1" data-id="${p.id}">−</button>
                    <span class="qty-value" data-qty-id="${p.id}">${qty}</span>
                    <button type="button" class="qty-btn" data-delta="1" data-id="${p.id}">+</button>
                </div>
            </div>
        `;
    }).join('') || '<p class="muted">Няма активни продукти.</p>';

    grid.querySelectorAll('.qty-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const product = products.find(p => p.id === Number(btn.dataset.id));
            if (!product) return;
            const item = getOrCreateCartItem(product);
            item.qty = Math.max(0, item.qty + Number(btn.dataset.delta));
            renderProducts();
            renderCart();
        });
    });

    grid.querySelectorAll('.sauce-mini').forEach(select => {
        select.addEventListener('change', () => {
            const product = products.find(p => p.id === Number(select.dataset.sauceId));
            if (!product) return;
            const item = getOrCreateCartItem(product);
            item.sauce = select.value;
            renderCart();
        });
    });
}

function openModal(id) {
    document.getElementById(id).hidden = false;
}

function closeModal(id) {
    document.getElementById(id).hidden = true;
}

function receiptHtml(receipt) {
    const now = receipt.soldAt;
    const lines = receipt.items.map(item => `
        <div class="receipt-line">
            <span>${item.qty}× ${item.name}${item.sizeLabel ? ' (' + item.sizeLabel + ')' : ''}${item.sauce ? ' · ' + sauceLabel(item.sauce) : ''}</span>
            <span>${money(item.lineTotal)}</span>
        </div>
        <div class="receipt-sub">по ${money(item.unitPrice)}</div>
    `).join('');

    return `
        <p class="receipt-brand">CineReserve</p>
        <p class="receipt-title">Касов бон</p>
        <p class="muted">${now}</p>
        <p class="muted">Касиер: ${receipt.cashier}</p>
        <p class="muted">Плащане: ${receipt.paymentMethod === 'CARD' ? 'Карта' : 'Кеш'}</p>
        <hr class="receipt-hr">
        ${lines}
        <hr class="receipt-hr">
        <div class="receipt-line receipt-total">
            <span>ОБЩО</span>
            <span>${money(receipt.total)}</span>
        </div>
        <p class="muted receipt-thanks">Благодарим ви!</p>
    `;
}

function showReceipt(receipt) {
    lastReceipt = receipt;
    document.getElementById('receiptBody').innerHTML = receiptHtml(receipt);
    openModal('receiptModal');
}

function buildReceipt(paymentMethod, cashierName) {
    const items = cartItems().map(item => ({
        name: item.product.name,
        sizeLabel: item.product.sizeLabel || '',
        sauce: item.product.allowsSauce ? item.sauce : null,
        qty: item.qty,
        unitPrice: Number(item.product.price),
        lineTotal: item.qty * Number(item.product.price)
    }));
    const now = new Date();
    const stamp =
        `${pad2(now.getDate())}.${pad2(now.getMonth() + 1)}.${now.getFullYear()} ${pad2(now.getHours())}:${pad2(now.getMinutes())}`;
    return {
        cashier: cashierName || sessionStorage.getItem('cineUsername') || 'касиер',
        paymentMethod,
        soldAt: stamp,
        items,
        total: cartTotal()
    };
}

async function completeSale(paymentMethod) {
    const msg = document.getElementById('productMsg');
    const items = cartItems();
    if (!items.length) return;

    const payload = {
        paymentMethod,
        cinemaId: Number(document.getElementById('barCinema').value),
        items: items.map(item => ({
            productId: item.product.id,
            quantity: item.qty,
            sauce: item.product.allowsSauce ? item.sauce : null
        }))
    };

    const me = typeof loadCurrentUser === 'function' ? await loadCurrentUser() : null;
    const receipt = buildReceipt(paymentMethod, me && me.username);

    try {
        await api('/api/cashier/products/sell-batch', {
            method: 'POST',
            body: JSON.stringify(payload)
        });
        cart.clear();
        renderProducts();
        renderCart();
        showReceipt(receipt);
        showMsg(msg, 'Поръчката е платена.', true);
    } catch (err) {
        showMsg(msg, err.message, false);
        if (typeof showToast === 'function') showToast(err.message, 'error');
    }
}

async function loadProducts() {
    const msg = document.getElementById('productMsg');
    try {
        products = await api('/api/products');
        renderProducts();
        renderCart();
    } catch (err) {
        showMsg(msg, err.message, false);
    }
}

document.getElementById('btnPay').addEventListener('click', () => {
    const cinemaId = document.getElementById('barCinema').value;
    if (!cinemaId) {
        showMsg(document.getElementById('productMsg'), 'Първо избери град и кино.', false);
        return;
    }
    document.getElementById('payTotalLabel').textContent = money(cartTotal());
    openModal('payModal');
});

document.getElementById('btnClearCart').addEventListener('click', () => {
    cart.clear();
    renderProducts();
    renderCart();
});

document.getElementById('btnPayCash').addEventListener('click', async () => {
    closeModal('payModal');
    await completeSale('CASH');
});

document.getElementById('btnPayCard').addEventListener('click', () => {
    closeModal('payModal');
    document.getElementById('posAmount').textContent = money(cartTotal());
    openModal('posModal');
});

document.getElementById('btnPosConfirm').addEventListener('click', async () => {
    closeModal('posModal');
    await completeSale('CARD');
});

['btnClosePay', 'btnClosePos', 'btnCloseReceipt', 'btnCloseReceiptOk'].forEach(id => {
    document.getElementById(id).addEventListener('click', () => {
        closeModal('payModal');
        closeModal('posModal');
        closeModal('receiptModal');
    });
});

async function loadBarCinemas() {
    const cinemas = await api('/api/cinemas');
    const cities = uniqueCities(cinemas);
    const citySelect = document.getElementById('barCity');
    const cinemaSelect = document.getElementById('barCinema');
    citySelect.innerHTML = '<option value="">Избери град</option>' +
        cities.map(city => `<option value="${city}">${city}</option>`).join('');

    citySelect.addEventListener('change', () => {
        const city = citySelect.value;
        const filtered = cinemasForCity(cinemas, city);
        cinemaSelect.disabled = !city;
        cinemaSelect.innerHTML = city
            ? '<option value="">Избери кино</option>' + filtered.map(c => `<option value="${c.id}">${c.name}</option>`).join('')
            : '<option value="">Първо избери град</option>';
    });
}

loadBarCinemas().catch(err => showMsg(document.getElementById('productMsg'), err.message, false));
loadProducts();
