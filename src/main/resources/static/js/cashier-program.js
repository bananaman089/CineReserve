const selected = new Set();
let seats = [];
let currentProjectionId = null;
let allProjections = [];
let currentOccupied = new Set();
let selectedCinemaId = '';

function cinemaFromProjection(p) {
    return p.hall && p.hall.cinema ? p.hall.cinema : {};
}

function uniqueCinemas(projections) {
    const map = new Map();
    projections.forEach(p => {
        const cinema = cinemaFromProjection(p);
        if (cinema.id && !map.has(cinema.id)) {
            map.set(cinema.id, cinema);
        }
    });
    return [...map.values()].sort((a, b) =>
        `${a.city} ${a.name}`.localeCompare(`${b.city} ${b.name}`, 'bg')
    );
}

function filteredProjections() {
    if (!selectedCinemaId) return [];
    return allProjections.filter(p => String(cinemaFromProjection(p).id) === String(selectedCinemaId));
}

function renderProgram() {
    const list = document.getElementById('programList');
    if (!selectedCinemaId) {
        list.innerHTML = '<p class="muted">Първо избери кино от падащото меню.</p>';
        return;
    }
    const filtered = filteredProjections();
    if (!filtered.length) {
        list.innerHTML = '<p class="muted">Няма прожекции за това кино.</p>';
        return;
    }
    renderMovieProgram(list, filtered, {
        mode: 'button',
        selectedId: currentProjectionId,
        onSelect: selectProjection
    });
}

async function loadProgram() {
    const msg = document.getElementById('cashierMsg') || document.getElementById('cashierMsgTop');
    try {
        allProjections = await api('/api/projections');
        const select = document.getElementById('cinemaSelect');
        const cinemas = uniqueCinemas(allProjections);
        select.innerHTML = '<option value="">Избери кино</option>' + cinemas.map(c =>
            `<option value="${c.id}">${c.city || '—'} · ${c.name}</option>`
        ).join('');
        renderProgram();
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
    if (el) el.textContent = describeSelectedSeats(seats, selected);
}

async function selectProjection(projectionId) {
    const msg = document.getElementById('cashierMsg') || document.getElementById('cashierMsgTop');
    currentProjectionId = projectionId;
    selected.clear();
    document.getElementById('ticketPanel').hidden = false;
    renderProgram();

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

document.getElementById('cinemaSelect').addEventListener('change', (e) => {
    selectedCinemaId = e.target.value;
    currentProjectionId = null;
    selected.clear();
    document.getElementById('ticketPanel').hidden = true;
    document.getElementById('selectedInfo').textContent = 'Няма избран час.';
    renderProgram();
});

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
