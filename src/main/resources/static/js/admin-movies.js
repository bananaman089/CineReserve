let allCinemas = [];
let allHalls = [];
let selectedCity = '';
let selectedCinemaIds = new Set();
let selectedHallIds = new Set();

function fillMilitaryTimeSelects() {
    const hourSelect = document.getElementById('projHour');
    const minuteSelect = document.getElementById('projMinute');
    const dateInput = document.getElementById('projDate');

    hourSelect.innerHTML = Array.from({ length: 24 }, (_, h) => {
        const v = pad2(h);
        return `<option value="${v}">${v}</option>`;
    }).join('');

    minuteSelect.innerHTML = Array.from({ length: 12 }, (_, i) => {
        const v = pad2(i * 5);
        return `<option value="${v}">${v}</option>`;
    }).join('');

    hourSelect.value = '17';
    minuteSelect.value = '00';

    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    dateInput.value =
        `${tomorrow.getFullYear()}-${pad2(tomorrow.getMonth() + 1)}-${pad2(tomorrow.getDate())}`;

    dateInput.addEventListener('click', () => {
        try {
            if (typeof dateInput.showPicker === 'function') dateInput.showPicker();
        } catch (err) { /* ignore */ }
    });
    dateInput.addEventListener('focus', () => {
        try {
            if (typeof dateInput.showPicker === 'function') dateInput.showPicker();
        } catch (err) { /* ignore */ }
    });
}

function buildStartTimeLocal() {
    const date = document.getElementById('projDate').value;
    const hour = document.getElementById('projHour').value;
    const minute = document.getElementById('projMinute').value;
    if (!date || !hour || !minute) throw new Error('Попълни дата и час');
    return `${date}T${hour}:${minute}:00`;
}

function cinemasInCity() {
    return cinemasForCity(allCinemas, selectedCity);
}

function visibleHalls() {
    return allHalls.filter(h => selectedCinemaIds.has(Number((h.cinema || {}).id)));
}

function cinemaName(id) {
    const cinema = allCinemas.find(c => Number(c.id) === Number(id));
    return cinema ? cinema.name : 'Кино';
}

function updateSummary() {
    const summary = document.getElementById('projSummary');
    const halls = visibleHalls().filter(h => selectedHallIds.has(Number(h.id)));
    if (!selectedCity) {
        summary.textContent = 'Избери град, за да започнеш.';
        return;
    }
    if (!selectedCinemaIds.size) {
        summary.textContent = `Град: ${selectedCity}. Избери поне едно кино.`;
        return;
    }
    if (!halls.length) {
        summary.textContent = `Град: ${selectedCity} · ${selectedCinemaIds.size} кина. Избери залите.`;
        return;
    }
    const byCinema = new Map();
    halls.forEach(h => {
        const name = (h.cinema || {}).name || cinemaName((h.cinema || {}).id);
        if (!byCinema.has(name)) byCinema.set(name, []);
        byCinema.get(name).push(h.name);
    });
    const parts = [...byCinema.entries()].map(([name, list]) => `${name} (${list.join(', ')})`);
    summary.textContent = `${selectedCity} · ${halls.length} ${halls.length === 1 ? 'зала' : 'зали'}: ${parts.join(' · ')}`;
}

function setColDim(id, dim) {
    document.getElementById(id).classList.toggle('is-dim', dim);
}

function renderCities() {
    const box = document.getElementById('projCityBox');
    const cities = uniqueCities(allCinemas);
    document.getElementById('cityCount').textContent = selectedCity
        ? selectedCity
        : (cities.length ? `${cities.length} града` : 'Няма градове');

    if (!cities.length) {
        box.innerHTML = '<p class="muted">Няма добавени кина.</p>';
        return;
    }

    box.innerHTML = cities.map(city => `
        <button type="button" class="place-option${city === selectedCity ? ' is-on' : ''}" data-city="${city}">
            <span>${city}</span>
        </button>
    `).join('');

    box.querySelectorAll('[data-city]').forEach(btn => {
        btn.addEventListener('click', () => {
            const city = btn.getAttribute('data-city');
            if (selectedCity !== city) {
                selectedCity = city;
                selectedCinemaIds = new Set();
                selectedHallIds = new Set();
                document.getElementById('cinemaSearch').value = '';
            }
            renderCities();
            renderCinemas();
            renderHalls();
        });
    });
}

function renderCinemas() {
    const box = document.getElementById('projCinemaBox');
    const search = document.getElementById('cinemaSearch');
    const cinemas = cinemasInCity();
    const query = (search.value || '').trim().toLowerCase();
    const filtered = query
        ? cinemas.filter(c => (c.name || '').toLowerCase().includes(query))
        : cinemas;

    search.hidden = !selectedCity || cinemas.length < 6;
    setColDim('cinemaCol', !selectedCity);
    document.getElementById('cinemaCount').textContent = !selectedCity
        ? 'Първо избери град'
        : (selectedCinemaIds.size
            ? `${selectedCinemaIds.size} избрани`
            : `${cinemas.length} кина в града`);

    if (!selectedCity) {
        box.innerHTML = '<p class="muted">Първо избери град.</p>';
        return;
    }
    if (!cinemas.length) {
        box.innerHTML = '<p class="muted">Няма кина в този град.</p>';
        return;
    }
    if (!filtered.length) {
        box.innerHTML = '<p class="muted">Няма съвпадение.</p>';
        return;
    }

    box.innerHTML = filtered.map(c => `
        <button type="button" class="chip${selectedCinemaIds.has(Number(c.id)) ? ' is-on' : ''}" data-cinema-id="${c.id}">
            <span>${c.name}</span>
        </button>
    `).join('');

    box.querySelectorAll('[data-cinema-id]').forEach(btn => {
        btn.addEventListener('click', () => {
            const id = Number(btn.getAttribute('data-cinema-id'));
            if (selectedCinemaIds.has(id)) {
                selectedCinemaIds.delete(id);
                allHalls
                    .filter(h => Number((h.cinema || {}).id) === id)
                    .forEach(h => selectedHallIds.delete(Number(h.id)));
            } else {
                selectedCinemaIds.add(id);
            }
            renderCinemas();
            renderHalls();
        });
    });
}

function renderHalls() {
    const box = document.getElementById('projHallBox');
    const halls = visibleHalls();
    const selectedVisible = halls.filter(h => selectedHallIds.has(Number(h.id)));

    setColDim('hallCol', !selectedCinemaIds.size);
    document.getElementById('hallCount').textContent = !selectedCinemaIds.size
        ? 'Избери кино, за да видиш залите'
        : (selectedVisible.length
            ? `${selectedVisible.length} избрани`
            : `${halls.length} зали`);

    if (!selectedCinemaIds.size) {
        box.innerHTML = '<p class="muted">Избери кино, за да видиш залите.</p>';
        updateSummary();
        return;
    }
    if (!halls.length) {
        box.innerHTML = '<p class="muted">Няма зали за избраните кина.</p>';
        updateSummary();
        return;
    }

    const groups = new Map();
    halls.forEach(h => {
        const cinemaId = Number((h.cinema || {}).id);
        if (!groups.has(cinemaId)) groups.set(cinemaId, []);
        groups.get(cinemaId).push(h);
    });

    box.innerHTML = [...groups.entries()].map(([cinemaId, group]) => `
        <div class="hall-group">
            <div class="hall-group-title">
                <span>${cinemaName(cinemaId)}</span>
                <button type="button" data-halls-for="${cinemaId}">Всички зали</button>
            </div>
            <div class="chip-grid">
                ${group.map(h => `
                    <button type="button" class="chip${selectedHallIds.has(Number(h.id)) ? ' is-on' : ''}" data-hall-id="${h.id}">
                        <span>${h.name}</span>
                    </button>
                `).join('')}
            </div>
        </div>
    `).join('');

    box.querySelectorAll('[data-hall-id]').forEach(btn => {
        btn.addEventListener('click', () => {
            const id = Number(btn.getAttribute('data-hall-id'));
            if (selectedHallIds.has(id)) selectedHallIds.delete(id);
            else selectedHallIds.add(id);
            renderHalls();
        });
    });

    box.querySelectorAll('[data-halls-for]').forEach(btn => {
        btn.addEventListener('click', () => {
            const cinemaId = Number(btn.getAttribute('data-halls-for'));
            const ids = allHalls
                .filter(h => Number((h.cinema || {}).id) === cinemaId)
                .map(h => Number(h.id));
            const allOn = ids.every(id => selectedHallIds.has(id));
            ids.forEach(id => allOn ? selectedHallIds.delete(id) : selectedHallIds.add(id));
            renderHalls();
        });
    });

    updateSummary();
}

function toggleAllCinemas() {
    const cinemas = cinemasInCity();
    if (!cinemas.length) return;
    const ids = cinemas.map(c => Number(c.id));
    const allOn = ids.every(id => selectedCinemaIds.has(id));
    if (allOn) {
        selectedCinemaIds = new Set();
        selectedHallIds = new Set();
    } else {
        selectedCinemaIds = new Set(ids);
    }
    renderCinemas();
    renderHalls();
}

function toggleAllHalls() {
    const halls = visibleHalls();
    if (!halls.length) return;
    const ids = halls.map(h => Number(h.id));
    const allOn = ids.every(id => selectedHallIds.has(id));
    selectedHallIds = allOn ? new Set() : new Set(ids);
    renderHalls();
}

async function refreshMoviesPage() {
    const [halls, movies, projections, cinemas] = await Promise.all([
        api('/api/halls'),
        api('/api/movies'),
        api('/api/projections'),
        api('/api/cinemas')
    ]);
    allHalls = halls;
    allCinemas = cinemas;

    document.getElementById('projMovieId').innerHTML = movies.map(m =>
        `<option value="${m.id}">${m.title}</option>`
    ).join('') || '<option value="">Няма филми</option>';

    const cities = uniqueCities(cinemas);
    if (selectedCity && !cities.includes(selectedCity)) {
        selectedCity = '';
        selectedCinemaIds = new Set();
        selectedHallIds = new Set();
    }

    renderCities();
    renderCinemas();
    renderHalls();

    document.getElementById('deleteMovieId').innerHTML = movies.map(m =>
        `<option value="${m.id}">${m.title}</option>`
    ).join('') || '<option value="">Няма</option>';

    document.getElementById('deleteProjectionId').innerHTML = projections.map(p => {
        const cinema = p.hall.cinema || {};
        return `<option value="${p.id}">${p.movie.title} · ${cinema.city || ''} · ${cinema.name || ''} · ${formatLabel(p.format)} · ${formatDateTime(p.startTime)}</option>`;
    }).join('') || '<option value="">Няма</option>';
}

async function initAdminMovies() {
    fillMilitaryTimeSelects();

    document.getElementById('btnAllCinemas').addEventListener('click', toggleAllCinemas);
    document.getElementById('btnAllHalls').addEventListener('click', toggleAllHalls);
    document.getElementById('cinemaSearch').addEventListener('input', renderCinemas);

    document.getElementById('movieForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const msg = document.getElementById('movieMsg');
        try {
            await api('/api/admin/movies', {
                method: 'POST',
                body: JSON.stringify({
                    title: document.getElementById('movieTitle').value.trim(),
                    description: document.getElementById('movieDescription').value.trim(),
                    duration: Number(document.getElementById('movieDuration').value)
                })
            });
            e.target.reset();
            showMsg(msg, 'Филмът е създаден.', true);
            await refreshMoviesPage();
        } catch (err) {
            showMsg(msg, err.message, false);
        }
    });

    document.getElementById('projectionForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const msg = document.getElementById('projMsg');
        const hallIds = [...selectedHallIds];
        if (!selectedCity) {
            showMsg(msg, 'Избери град.', false);
            return;
        }
        if (!hallIds.length) {
            showMsg(msg, 'Избери поне една зала.', false);
            return;
        }
        try {
            const created = await api('/api/admin/projections', {
                method: 'POST',
                body: JSON.stringify({
                    movieId: Number(document.getElementById('projMovieId').value),
                    hallIds,
                    startTime: buildStartTimeLocal(),
                    basePrice: Number(document.getElementById('projPrice').value),
                    format: document.getElementById('projFormat').value
                })
            });
            const count = Array.isArray(created) ? created.length : 1;
            showMsg(msg, `Добавени са ${count} прожекции.`, true);
            selectedCinemaIds = new Set();
            selectedHallIds = new Set();
            await refreshMoviesPage();
        } catch (err) {
            showMsg(msg, err.message, false);
        }
    });

    document.getElementById('btnDeleteMovie').addEventListener('click', async () => {
        const id = document.getElementById('deleteMovieId').value;
        const msg = document.getElementById('deleteMsg');
        if (!id) return;
        try {
            await api('/api/admin/movies/' + id, { method: 'DELETE' });
            showMsg(msg, 'Филмът е изтрит.', true);
            await refreshMoviesPage();
        } catch (err) {
            showMsg(msg, err.message, false);
        }
    });

    document.getElementById('btnDeleteProjection').addEventListener('click', async () => {
        const id = document.getElementById('deleteProjectionId').value;
        const msg = document.getElementById('deleteMsg');
        if (!id) return;
        try {
            await api('/api/admin/projections/' + id, { method: 'DELETE' });
            showMsg(msg, 'Прожекцията е изтрита.', true);
            await refreshMoviesPage();
        } catch (err) {
            showMsg(msg, err.message, false);
        }
    });

    await refreshMoviesPage();
}

window.initAdminMovies = initAdminMovies;

if (!window.__adminSoftNavBoot) {
    initAdminMovies()
        .catch(err => showMsg(document.getElementById('movieMsg'), err.message, false))
        .finally(() => revealAdminPage());
}
