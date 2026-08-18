const selected = new Set();
let seats = [];
let currentProjectionId = null;
let allProjections = [];
let currentOccupied = new Set();

async function loadProgram() {
    const msg = document.getElementById('cashierMsg');
    try {
        allProjections = await api('/api/projections');
        renderMovieProgram(document.getElementById('programList'), allProjections, {
            mode: 'button',
            selectedId: currentProjectionId,
            onSelect: selectProjection
        });
    } catch (err) {
        showMsg(msg, err.message, false);
    }
}

function redrawSeats() {
    renderSeatMap(
        document.getElementById('seatMap'),
        seats,
        currentOccupied,
        selected,
        (seatId) => {
            if (selected.has(seatId)) selected.delete(seatId);
            else selected.add(seatId);
            redrawSeats();
            updateSeatSelectionText();
        }
    );
    updateSeatSelectionText();
}

function updateSeatSelectionText() {
    const el = document.getElementById('seatSelectionInfo');
    if (el) {
        el.textContent = describeSelectedSeats(seats, selected);
    }
}

async function selectProjection(projectionId) {
    const msg = document.getElementById('cashierMsg');
    currentProjectionId = projectionId;
    selected.clear();

    renderMovieProgram(document.getElementById('programList'), allProjections, {
        mode: 'button',
        selectedId: currentProjectionId,
        onSelect: selectProjection
    });

    try {
        const projection = await api('/api/projections/' + projectionId);
        const cinema = projection.hall.cinema || {};
        document.getElementById('selectedInfo').innerHTML =
            `Избрано: <b>${projection.movie.title}</b> · ${cinema.city || ''} · ${cinema.name || ''} · ` +
            `${projection.hall.name} · <b>${formatLabel(projection.format)}</b> · ${formatDateTime(projection.startTime)}`;

        seats = await api('/api/halls/' + projection.hall.id + '/seats');
        currentOccupied = new Set(await api('/api/projections/' + projectionId + '/occupied-seats'));
        redrawSeats();
        showMsg(msg, 'Схемата е заредена.', true);
    } catch (err) {
        showMsg(msg, err.message, false);
    }
}

document.getElementById('btnSell').addEventListener('click', async () => {
    const msg = document.getElementById('cashierMsg');
    if (!currentProjectionId) {
        showMsg(msg, 'Първо избери час от програмата.', false);
        return;
    }
    if (!selected.size) {
        showMsg(msg, 'Избери места.', false);
        return;
    }

    try {
        const sold = await api('/api/cashier/sell', {
            method: 'POST',
            body: JSON.stringify({
                projectionId: currentProjectionId,
                seatIds: [...selected],
                ticketType: document.getElementById('ticketType').value
            })
        });
        document.getElementById('sellOut').textContent = sold
            .map(t => `Билет: Ред ${t.seat.rowNum} / място ${t.seat.seatNum} · ${Number(t.price).toFixed(2)} €`)
            .join('\n');
        showMsg(msg, 'Продажбата на билети е успешна.', true);
        selected.clear();
        await selectProjection(currentProjectionId);
    } catch (err) {
        showMsg(msg, err.message, false);
    }
});

loadProgram();

let selectedProductId = null;
let products = [];

async function loadProducts() {
    const grid = document.getElementById('productGrid');
    const msg = document.getElementById('productMsg');
    try {
        products = await api('/api/products');
        grid.innerHTML = products.map(p => `
            <button type="button" class="product-card" data-id="${p.id}">
                <strong>${p.name}</strong>
                <span>${p.sizeLabel || ''} · ${p.category}</span>
                <span class="price">${Number(p.price).toFixed(2)} €</span>
                ${p.allowsSauce ? '<em>с избор на сос</em>' : ''}
            </button>
        `).join('') || '<p class="muted">Няма активни продукти.</p>';

        grid.querySelectorAll('.product-card').forEach(card => {
            card.addEventListener('click', () => {
                grid.querySelectorAll('.product-card').forEach(c => c.classList.remove('selected'));
                card.classList.add('selected');
                selectedProductId = Number(card.dataset.id);
                document.getElementById('btnSellProduct').disabled = false;
            });
        });
    } catch (err) {
        showMsg(msg, err.message, false);
    }
}

document.getElementById('btnSellProduct').addEventListener('click', async () => {
    const msg = document.getElementById('productMsg');
    if (!selectedProductId) {
        showMsg(msg, 'Избери продукт.', false);
        return;
    }
    const product = products.find(p => p.id === selectedProductId);
    try {
        const sale = await api('/api/cashier/products/sell', {
            method: 'POST',
            body: JSON.stringify({
                productId: selectedProductId,
                quantity: Number(document.getElementById('productQty').value) || 1,
                sauce: product && product.allowsSauce ? document.getElementById('sauceSelect').value : null
            })
        });
        document.getElementById('productOut').textContent =
            `Продадено: ${sale.product.name}` +
            (sale.product.sizeLabel ? ` (${sale.product.sizeLabel})` : '') +
            ` × ${sale.quantity}` +
            (sale.sauce ? ` · сос ${sale.sauce === 'CHEDDAR' ? 'Чедър' : 'Барбекю'}` : '') +
            ` · общо ${Number(sale.totalPrice).toFixed(2)} €`;
        showMsg(msg, 'Продуктът е продаден.', true);
    } catch (err) {
        showMsg(msg, err.message, false);
    }
});

loadProducts();
