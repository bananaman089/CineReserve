function money(value) {
    const num = Number(value || 0);
    return num.toLocaleString('bg-BG', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function categoryLabel(category) {
    if (category === 'POPCORN') return 'Пуканки';
    if (category === 'DRINK') return 'Напитка';
    if (category === 'NACHOS') return 'Начос';
    return category || '—';
}

function sauceLabel(sauce) {
    if (sauce === 'BBQ') return 'Барбекю';
    if (sauce === 'CHEDDAR') return 'Чедър';
    return sauce ? sauce : '—';
}

function fillCitySelect(select, cities) {
    select.innerHTML = '<option value="">Избери град</option>' +
        cities.map(city => `<option value="${city}">${city}</option>`).join('');
}

function fillCinemaSelect(select, cinemas, city) {
    const filtered = cinemasForCity(cinemas, city);
    select.disabled = !city;
    select.innerHTML = city
        ? '<option value="">Избери кино</option>' + filtered.map(c => `<option value="${c.id}">${c.name}</option>`).join('')
        : '<option value="">Първо избери град</option>';
}

async function loadRevenueReport(cinemaId) {
    const msg = document.getElementById('ticketMsg');
    const report = document.getElementById('revenueReport');
    if (!cinemaId) {
        report.hidden = true;
        return;
    }
    try {
        const data = await api('/api/admin/reports/revenue-by-movie?cinemaId=' + cinemaId);
        const body = document.getElementById('revenueBody');
        const summary = document.getElementById('revenueSummary');

        if (!data.length) {
            body.innerHTML = '<tr><td colspan="3">Няма платени билети за това кино.</td></tr>';
            summary.innerHTML = '';
        } else {
            const total = data.reduce((sum, row) => sum + Number(row.revenue || 0), 0);
            const tickets = data.reduce((sum, row) => sum + Number(row.tickets || 0), 0);
            summary.innerHTML = `
                <div class="stat-card"><span class="label">Филми с продажби</span><span class="value">${data.length}</span></div>
                <div class="stat-card"><span class="label">Продадени билети</span><span class="value">${tickets}</span></div>
                <div class="stat-card"><span class="label">Общ приход</span><span class="value">${money(total)} €</span></div>
            `;
            body.innerHTML = data
                .slice()
                .sort((a, b) => Number(b.revenue) - Number(a.revenue))
                .map(row => `
                    <tr>
                        <td>${row.movieTitle}</td>
                        <td>${row.tickets || 0}</td>
                        <td>${money(row.revenue)}</td>
                    </tr>
                `).join('');
        }
        report.hidden = false;
        showMsg(msg, 'Отчетът за билети е зареден.', true);
    } catch (err) {
        report.hidden = true;
        showMsg(msg, err.message, false);
    }
}

async function loadOccupancyReport() {
    const msg = document.getElementById('ticketMsg');
    const projectionId = Number(document.getElementById('reportProjectionId').value);
    if (!projectionId) {
        document.getElementById('occupancyReport').hidden = true;
        return;
    }

    try {
        const data = await api('/api/admin/reports/occupancy/' + projectionId);
        const paid = Number(data.paidSeats || 0);
        const pending = Number(data.pendingSeats || 0);
        const free = Number(data.freeSeats || 0);
        const total = Number(data.totalSeats || 0);
        const soldPercent = Number(data.soldPercent || 0);
        const occupiedPercent = Number(data.occupancyPercent || 0);

        document.getElementById('occupancySummary').innerHTML = `
            <div class="stat-card"><span class="label">Продадени</span><span class="value">${paid}</span></div>
            <div class="stat-card"><span class="label">Заключени (PENDING)</span><span class="value">${pending}</span></div>
            <div class="stat-card"><span class="label">Свободни</span><span class="value">${free}</span></div>
            <div class="stat-card"><span class="label">Общо места</span><span class="value">${total}</span></div>
            <div class="stat-card"><span class="label">% продадени</span><span class="value">${soldPercent}%</span></div>
            <div class="stat-card"><span class="label">% заети общо</span><span class="value">${occupiedPercent}%</span></div>
        `;
        document.getElementById('occupancyFill').style.width = Math.min(occupiedPercent, 100) + '%';
        document.getElementById('occupancyBody').innerHTML = `
            <tr><td class="key-col">Филм</td><td>${data.movieTitle}</td></tr>
            <tr><td class="key-col">Зала</td><td>${data.hallName}</td></tr>
            <tr><td class="key-col">Начало</td><td>${formatDateTime(data.startTime)}</td></tr>
            <tr><td class="key-col">Продадени места</td><td>${paid}</td></tr>
            <tr><td class="key-col">Свободни</td><td>${free}</td></tr>
        `;
        document.getElementById('occupancyReport').hidden = false;
    } catch (err) {
        showMsg(msg, err.message, false);
    }
}

function fillProjectionSelect(projections, cinemaId) {
    const select = document.getElementById('reportProjectionId');
    const wrap = document.getElementById('occupancyWrap');
    const filtered = projections.filter(p => String((p.hall.cinema || {}).id) === String(cinemaId));
    wrap.hidden = !cinemaId;
    select.innerHTML = filtered.map(p => {
        const cinema = p.hall.cinema || {};
        return `<option value="${p.id}">${p.movie.title} · ${p.hall.name} · ${formatLabel(p.format)} · ${formatDateTime(p.startTime)}</option>`;
    }).join('') || '<option value="">Няма прожекции</option>';
}

async function loadProductSalesReport(cinemaId) {
    const msg = document.getElementById('productMsg');
    const report = document.getElementById('productSalesReport');
    if (!cinemaId) {
        report.hidden = true;
        return;
    }
    try {
        const data = await api('/api/admin/reports/product-sales?cinemaId=' + cinemaId);
        const summary = data.summary || {};
        document.getElementById('productSalesSummary').innerHTML = `
            <div class="stat-card"><span class="label">Продажби</span><span class="value">${summary.totalOrders || 0}</span></div>
            <div class="stat-card"><span class="label">Продадени бройки</span><span class="value">${summary.totalQuantity || 0}</span></div>
            <div class="stat-card"><span class="label">Различни продукти</span><span class="value">${summary.uniqueProducts || 0}</span></div>
            <div class="stat-card"><span class="label">Приход от бар</span><span class="value">${money(summary.totalRevenue)} €</span></div>
        `;

        const byProduct = data.byProduct || [];
        document.getElementById('productByProductBody').innerHTML = byProduct.length
            ? byProduct.map(row => `
                <tr>
                    <td>${row.productName}</td>
                    <td>${categoryLabel(row.category)}</td>
                    <td>${row.sizeLabel || '—'}</td>
                    <td>${row.quantity}</td>
                    <td>${money(row.revenue)}</td>
                </tr>
            `).join('')
            : '<tr><td colspan="5">Няма продадени артикули за това кино.</td></tr>';

        const byCategory = data.byCategory || [];
        document.getElementById('productByCategoryBody').innerHTML = byCategory.length
            ? byCategory.map(row => `
                <tr>
                    <td>${categoryLabel(row.category)}</td>
                    <td>${row.quantity}</td>
                    <td>${money(row.revenue)}</td>
                </tr>
            `).join('')
            : '<tr><td colspan="3">Няма данни.</td></tr>';

        const byCashier = data.byCashier || [];
        document.getElementById('productByCashierBody').innerHTML = byCashier.length
            ? byCashier.map(row => `
                <tr>
                    <td>${row.cashier}</td>
                    <td>${row.orders}</td>
                    <td>${row.quantity}</td>
                    <td>${money(row.revenue)}</td>
                </tr>
            `).join('')
            : '<tr><td colspan="4">Няма данни.</td></tr>';

        const details = data.details || [];
        document.getElementById('productDetailsBody').innerHTML = details.length
            ? details.map(row => `
                <tr>
                    <td>${formatDateTime(row.soldAt)}</td>
                    <td>${row.productName}</td>
                    <td>${categoryLabel(row.category)}</td>
                    <td>${row.sizeLabel || '—'}</td>
                    <td>${sauceLabel(row.sauce)}</td>
                    <td>${row.quantity}</td>
                    <td>${money(row.unitPrice)}</td>
                    <td>${money(row.totalPrice)}</td>
                    <td>${row.cashier}</td>
                </tr>
            `).join('')
            : '<tr><td colspan="9">Няма продажби.</td></tr>';

        report.hidden = false;
        showMsg(msg, 'Отчетът за артикули е зареден.', true);
    } catch (err) {
        report.hidden = true;
        showMsg(msg, err.message, false);
    }
}

async function initAdminReports() {
    const [cinemas, projections] = await Promise.all([
        api('/api/cinemas'),
        api('/api/projections')
    ]);
    const cities = uniqueCities(cinemas);
    fillCitySelect(document.getElementById('ticketCity'), cities);
    fillCitySelect(document.getElementById('productCity'), cities);

    document.getElementById('ticketCity').addEventListener('change', (e) => {
        fillCinemaSelect(document.getElementById('ticketCinema'), cinemas, e.target.value);
        document.getElementById('revenueReport').hidden = true;
        document.getElementById('occupancyWrap').hidden = true;
        document.getElementById('occupancyReport').hidden = true;
    });

    document.getElementById('ticketCinema').addEventListener('change', async (e) => {
        const cinemaId = e.target.value;
        await loadRevenueReport(cinemaId);
        fillProjectionSelect(projections, cinemaId);
        if (document.getElementById('reportProjectionId').value) {
            await loadOccupancyReport();
        }
    });

    document.getElementById('reportProjectionId').addEventListener('change', () => loadOccupancyReport());

    document.getElementById('productCity').addEventListener('change', (e) => {
        fillCinemaSelect(document.getElementById('productCinema'), cinemas, e.target.value);
        document.getElementById('productSalesReport').hidden = true;
    });

    document.getElementById('productCinema').addEventListener('change', (e) => {
        loadProductSalesReport(e.target.value);
    });
}

window.initAdminReports = initAdminReports;

if (!window.__adminSoftNavBoot) {
    initAdminReports()
        .catch(err => showMsg(document.getElementById('ticketMsg'), err.message, false))
        .finally(() => revealAdminPage());
}
