/**
 * Рисува схема на местата с етикети "Ред N" и номер върху всяка седалка.
 * selected: Set от seatId
 * onToggle(seatId): клик върху свободно място
 */
function renderSeatMap(mapEl, seats, occupiedSet, selectedSet, onToggle) {
    const byRow = new Map();
    seats.forEach(seat => {
        if (!byRow.has(seat.rowNum)) byRow.set(seat.rowNum, []);
        byRow.get(seat.rowNum).push(seat);
    });

    const rows = [...byRow.keys()].sort((a, b) => a - b);
    mapEl.innerHTML = '';

    // горен ред с номера на колоните (местата)
    const maxSeats = Math.max(0, ...seats.map(s => s.seatNum));
    if (maxSeats > 0) {
        const header = document.createElement('div');
        header.className = 'seat-row seat-row-header';

        const spacer = document.createElement('div');
        spacer.className = 'row-label';
        spacer.setAttribute('aria-hidden', 'true');
        header.appendChild(spacer);

        for (let n = 1; n <= maxSeats; n++) {
            const col = document.createElement('div');
            col.className = 'seat-col-num';
            col.textContent = n;
            header.appendChild(col);
        }
        mapEl.appendChild(header);
    }

    rows.forEach(rowNum => {
        const rowEl = document.createElement('div');
        rowEl.className = 'seat-row';

        const label = document.createElement('div');
        label.className = 'row-label';
        label.textContent = 'Ред ' + rowNum;
        label.title = 'Ред ' + rowNum;
        rowEl.appendChild(label);

        byRow.get(rowNum)
            .sort((a, b) => a.seatNum - b.seatNum)
            .forEach(seat => {
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'seat';
                btn.textContent = String(seat.seatNum);
                btn.title = `Ред ${seat.rowNum}, място ${seat.seatNum}`;
                btn.setAttribute('aria-label', `Ред ${seat.rowNum}, място ${seat.seatNum}`);
                btn.dataset.seatId = seat.id;

                if (occupiedSet.has(seat.id)) {
                    btn.classList.add('busy');
                    btn.disabled = true;
                } else if (selectedSet.has(seat.id)) {
                    btn.classList.add('picked');
                } else {
                    btn.classList.add('free');
                }

                btn.addEventListener('click', () => {
                    if (occupiedSet.has(seat.id)) return;
                    onToggle(seat.id);
                });
                rowEl.appendChild(btn);
            });

        mapEl.appendChild(rowEl);
    });
}

/** Текст от рода на: Ред 3 / място 5, Ред 3 / място 6 */
function describeSelectedSeats(seats, selectedSet) {
    const chosen = seats
        .filter(s => selectedSet.has(s.id))
        .sort((a, b) => (a.rowNum - b.rowNum) || (a.seatNum - b.seatNum));

    if (!chosen.length) {
        return 'Избрани места: няма';
    }

    const parts = chosen.map(s => `Ред ${s.rowNum} / място ${s.seatNum}`);
    return 'Избрани места (' + chosen.length + '): ' + parts.join(', ');
}
