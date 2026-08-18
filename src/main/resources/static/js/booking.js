const projectionId = Number(qs('projectionId'));
const selected = new Set();
let pendingTicketIds = [];
let seats = [];
let currentOccupied = new Set();

if (!projectionId) {
    document.getElementById('subtitle').textContent = 'Липсва projectionId в адреса.';
} else {
    initBooking();
}

async function initBooking() {
    const msg = document.getElementById('bookingMsg');
    try {
        const [projection, me] = await Promise.all([
            api('/api/projections/' + projectionId),
            api('/api/account/me')
        ]);
        document.getElementById('title').textContent = projection.movie.title;
        const cinema = projection.hall.cinema || {};
        document.getElementById('subtitle').innerHTML =
            `${cinema.city || '—'} · ${cinema.name || '—'} · ${projection.hall.name} · ` +
            `<span class="badge badge-${formatLabel(projection.format) === '3D' ? '3d' : '2d'}">${formatLabel(projection.format)}</span> · ` +
            `${formatDateTime(projection.startTime)} · от ${projection.basePrice} €`;
        document.getElementById('balanceHint').textContent =
            `Твой баланс: ${Number(me.balance).toFixed(2)} € (ако нямаш достатъчно, плащането ще откаже)`;

        seats = await api('/api/halls/' + projection.hall.id + '/seats');
        currentOccupied = new Set(await api('/api/projections/' + projectionId + '/occupied-seats'));
        redraw();
    } catch (err) {
        showMsg(msg, err.message, false);
    }
}

function redraw() {
    renderSeatMap(
        document.getElementById('seatMap'),
        seats,
        currentOccupied,
        selected,
        (seatId) => {
            if (selected.has(seatId)) selected.delete(seatId);
            else selected.add(seatId);
            redraw();
        }
    );
    document.getElementById('selectionInfo').textContent = describeSelectedSeats(seats, selected);
}

document.getElementById('btnReserve').addEventListener('click', async () => {
    const msg = document.getElementById('bookingMsg');
    document.getElementById('ticketCodes').textContent = '';
    if (!selected.size) {
        showMsg(msg, 'Избери поне едно място.', false);
        return;
    }

    try {
        const tickets = await api('/api/tickets/reserve', {
            method: 'POST',
            body: JSON.stringify({
                projectionId,
                seatIds: [...selected],
                ticketType: document.getElementById('ticketType').value
            })
        });
        pendingTicketIds = tickets.map(t => t.id);
        document.getElementById('btnPay').disabled = false;
        showMsg(msg, `Резервирано! Имаш 5 минути да платиш. Билети: ${pendingTicketIds.join(', ')}`, true);

        currentOccupied = new Set(await api('/api/projections/' + projectionId + '/occupied-seats'));
        selected.clear();
        redraw();
    } catch (err) {
        showMsg(msg, err.message, false);
        currentOccupied = new Set(await api('/api/projections/' + projectionId + '/occupied-seats'));
        redraw();
    }
});

document.getElementById('btnPay').addEventListener('click', async () => {
    const msg = document.getElementById('bookingMsg');
    if (!pendingTicketIds.length) {
        showMsg(msg, 'Няма чакащи билети за плащане.', false);
        return;
    }

    try {
        const paid = await api('/api/tickets/pay', {
            method: 'POST',
            body: JSON.stringify({ ticketIds: pendingTicketIds })
        });
        const codes = paid.map(t =>
            `Билет: Ред ${t.seat.rowNum} / място ${t.seat.seatNum} · ${Number(t.price).toFixed(2)} €`
        ).join('\n');
        document.getElementById('ticketCodes').textContent = codes;
        showMsg(msg, 'Плащането е успешно!', true);
        pendingTicketIds = [];
        document.getElementById('btnPay').disabled = true;

        currentOccupied = new Set(await api('/api/projections/' + projectionId + '/occupied-seats'));
        redraw();
    } catch (err) {
        showMsg(msg, err.message, false);
    }
});
