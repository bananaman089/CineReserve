function selectedCinemaCity() {
    const select = document.getElementById('cinemaCitySelect');
    const value = select.value;
    if (value === '__new__' || !value) {
        return document.getElementById('cinemaCityNew').value.trim();
    }
    return value;
}

function toggleNewCityField() {
    const isNew = document.getElementById('cinemaCitySelect').value === '__new__';
    const wrap = document.getElementById('cinemaCityNewWrap');
    const input = document.getElementById('cinemaCityNew');
    wrap.hidden = !isNew;
    input.required = isNew;
    if (!isNew) input.value = '';
}

function fillCitySelect(cinemas) {
    const select = document.getElementById('cinemaCitySelect');
    const current = select.value;
    const cities = uniqueCities(cinemas);
    select.innerHTML =
        '<option value="">Избери град</option>' +
        cities.map(city => `<option value="${city}">${city}</option>`).join('') +
        '<option value="__new__">+ Нов град</option>';
    if ([...select.options].some(o => o.value === current)) {
        select.value = current;
    } else if (!cities.length) {
        select.value = '__new__';
    }
    toggleNewCityField();
}

async function refreshCinemasPage() {
    const [cinemas] = await Promise.all([api('/api/cinemas')]);

    fillCitySelect(cinemas);

    document.getElementById('hallCinemaId').innerHTML = cinemas.map(c =>
        `<option value="${c.id}">${c.name} (${c.city})</option>`
    ).join('') || '<option value="">Няма кина</option>';

    document.getElementById('deleteCinemaId').innerHTML = cinemas.map(c =>
        `<option value="${c.id}">${c.name} (${c.city})</option>`
    ).join('') || '<option value="">Няма</option>';
}

async function initAdminCinemas() {
    document.getElementById('cinemaCitySelect').addEventListener('change', toggleNewCityField);

    document.getElementById('cinemaForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const msg = document.getElementById('cinemaMsg');
        const city = selectedCinemaCity();
        if (!city) {
            showMsg(msg, 'Избери град или въведи нов.', false);
            return;
        }
        try {
            await api('/api/admin/cinemas', {
                method: 'POST',
                body: JSON.stringify({
                    name: document.getElementById('cinemaName').value.trim(),
                    city,
                    address: document.getElementById('cinemaAddress').value.trim()
                })
            });
            e.target.reset();
            showMsg(msg, 'Киното е създадено.', true);
            await refreshCinemasPage();
        } catch (err) {
            showMsg(msg, err.message, false);
        }
    });

    document.getElementById('hallForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const msg = document.getElementById('hallMsg');
        try {
            const data = await api('/api/admin/halls', {
                method: 'POST',
                body: JSON.stringify({
                    cinemaId: Number(document.getElementById('hallCinemaId').value),
                    name: document.getElementById('hallName').value.trim(),
                    rowsCount: Number(document.getElementById('hallRows').value),
                    seatsPerRow: Number(document.getElementById('hallSeats').value)
                })
            });
            showMsg(msg, `Залата е създадена. Генерирани са ${data.rowsCount * data.seatsPerRow} места.`, true);
            await refreshCinemasPage();
        } catch (err) {
            showMsg(msg, err.message, false);
        }
    });

    document.getElementById('btnDeleteCinema').addEventListener('click', async () => {
        const id = document.getElementById('deleteCinemaId').value;
        const msg = document.getElementById('deleteMsg');
        if (!id) return;
        try {
            await api('/api/admin/cinemas/' + id, { method: 'DELETE' });
            showMsg(msg, 'Киното е изтрито.', true);
            await refreshCinemasPage();
        } catch (err) {
            showMsg(msg, err.message, false);
        }
    });

    await refreshCinemasPage();
}

window.initAdminCinemas = initAdminCinemas;

if (!window.__adminSoftNavBoot) {
    initAdminCinemas()
        .catch(err => showMsg(document.getElementById('cinemaMsg'), err.message, false))
        .finally(() => revealAdminPage());
}
