/**
 * Групира прожекциите:
 * Филм -> град + кино -> зали -> часове
 */
function groupProjectionsByMovie(projections) {
    const byMovie = new Map();

    const sorted = [...projections].sort((a, b) => {
        const ta = parseDate(a.startTime)?.getTime() || 0;
        const tb = parseDate(b.startTime)?.getTime() || 0;
        return ta - tb;
    });

    for (const p of sorted) {
        const movieId = p.movie.id;
        if (!byMovie.has(movieId)) {
            byMovie.set(movieId, {
                movie: p.movie,
                venues: new Map()
            });
        }

        const cinema = p.hall.cinema || {};
        const cinemaKey = cinema.id != null
            ? String(cinema.id)
            : `${cinema.city || ''}|${cinema.name || ''}`;

        const movieEntry = byMovie.get(movieId);
        if (!movieEntry.venues.has(cinemaKey)) {
            movieEntry.venues.set(cinemaKey, {
                city: cinema.city || '—',
                cinemaName: cinema.name || '—',
                showtimes: []
            });
        }

        movieEntry.venues.get(cinemaKey).showtimes.push({
            id: p.id,
            startTime: p.startTime,
            hallName: p.hall.name || 'Зала',
            format: formatLabel(p.format),
            basePrice: p.basePrice
        });
    }

    return [...byMovie.values()].map(entry => ({
        movie: entry.movie,
        venues: [...entry.venues.values()]
    }));
}

function hallsOfVenue(venue) {
    const halls = new Map();
    for (const st of venue.showtimes) {
        if (!halls.has(st.hallName)) halls.set(st.hallName, []);
        halls.get(st.hallName).push(st);
    }
    return [...halls.entries()].sort((a, b) => a[0].localeCompare(b[0], 'bg'));
}

function lowestPrice(venue) {
    const prices = venue.showtimes.map(st => Number(st.basePrice)).filter(n => !Number.isNaN(n));
    if (!prices.length) return null;
    return Math.min(...prices);
}

function ensureProgramFont() {
    if (document.getElementById('programPlayfair')) return;
    const pre = document.createElement('link');
    pre.rel = 'preconnect';
    pre.href = 'https://fonts.googleapis.com';
    document.head.appendChild(pre);
    const font = document.createElement('link');
    font.id = 'programPlayfair';
    font.rel = 'stylesheet';
    font.href = 'https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,600;1,600&display=swap';
    document.head.appendChild(font);
}

function renderShowtime(st, mode, selectedId) {
    const time = formatTime24(st.startTime);
    const date = formatDateOnly(st.startTime);
    const selectedClass = String(selectedId) === String(st.id) ? ' is-selected' : '';
    const formatClass = st.format === '3D' ? ' is-3d' : ' is-2d';
    const inner = `
        <span class="showtime-hour">${time}</span>
        <span class="showtime-date">${date}</span>
        <span class="showtime-fmt">${st.format}</span>
    `;
    if (mode === 'button') {
        return `<button type="button" class="showtime-btn${formatClass}${selectedClass}" data-projection-id="${st.id}" title="${st.hallName} · ${date} · ${st.format}">${inner}</button>`;
    }
    return `<a class="showtime-btn${formatClass}${selectedClass}" href="/booking.html?projectionId=${st.id}" title="${st.hallName} · ${date} · ${st.format}">${inner}</a>`;
}

/**
 * mode: 'link' -> <a href="/booking.html?projectionId=...">
 * mode: 'button' -> <button data-projection-id="...">
 * selectedId: маркира избран час (за каса)
 */
function renderMovieProgram(container, projections, options = {}) {
    const mode = options.mode || 'link';
    const selectedId = options.selectedId || null;
    const onSelect = options.onSelect || null;
    ensureProgramFont();

    if (!projections.length) {
        container.innerHTML = `
            <article class="movie-card">
                <p class="muted">Все още няма прожекции. Админът трябва да добави такива.</p>
            </article>`;
        return;
    }

    const grouped = groupProjectionsByMovie(projections);

    container.innerHTML = grouped.map((entry, index) => {
        const venuesHtml = entry.venues.map(venue => {
            const price = lowestPrice(venue);
            const hallsHtml = hallsOfVenue(venue).map(([hallName, showtimes]) => `
                <div class="hall-row">
                    <span class="hall-row-name">${hallName}</span>
                    <div class="showtimes">${showtimes.map(st => renderShowtime(st, mode, selectedId)).join('')}</div>
                </div>
            `).join('');

            return `
                <div class="venue-block">
                    <div class="venue-meta">
                        <div class="venue-place">
                            <span class="venue-city">${venue.city}</span>
                            <strong>${venue.cinemaName}</strong>
                        </div>
                        ${price != null ? `<span class="badge badge-price">от ${price} €</span>` : ''}
                    </div>
                    <div class="hall-list">${hallsHtml}</div>
                </div>`;
        }).join('');

        return `
            <article class="movie-card" style="animation-delay:${index * 0.04}s">
                <div class="movie-card-head">
                    <h3>${entry.movie.title}</h3>
                    <p class="movie-meta">${entry.movie.duration || '—'} мин</p>
                </div>
                <p class="movie-desc">${entry.movie.description || ''}</p>
                ${venuesHtml}
            </article>`;
    }).join('');

    if (mode === 'button' && typeof onSelect === 'function') {
        container.querySelectorAll('.showtime-btn').forEach(btn => {
            btn.addEventListener('click', () => onSelect(Number(btn.dataset.projectionId)));
        });
    }
}
