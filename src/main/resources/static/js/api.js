async function api(url, options = {}) {
    const res = await fetch(url, {
        credentials: 'same-origin',
        headers: {
            'Content-Type': 'application/json',
            ...(options.headers || {})
        },
        ...options
    });

    const text = await res.text();
    let data = null;
    if (text) {
        try {
            data = JSON.parse(text);
        } catch {
            data = text;
        }
    }

    if (!res.ok) {
        const message = (data && data.message) ? data.message : ('Грешка ' + res.status);
        throw new Error(message);
    }
    return data;
}

function showMsg(el, text, ok = true) {
    if (!el) return;
    el.textContent = text;
    el.className = 'msg show ' + (ok ? 'ok' : 'err');
}

function qs(name) {
    return new URLSearchParams(window.location.search).get(name);
}

function pad2(n) {
    return String(n).padStart(2, '0');
}

/**
 * Чете дата/час от API като локални части (без timezone трикове).
 * Очаква нещо като: 2026-08-06T17:00:00
 */
function splitDateTime(value) {
    if (!value) return null;

    if (value instanceof Date && !Number.isNaN(value.getTime())) {
        return {
            year: value.getFullYear(),
            month: value.getMonth() + 1,
            day: value.getDate(),
            hour: value.getHours(),
            minute: value.getMinutes()
        };
    }

    if (typeof value === 'string') {
        const m = value.match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?/);
        if (m) {
            return {
                year: Number(m[1]),
                month: Number(m[2]),
                day: Number(m[3]),
                hour: Number(m[4]),
                minute: Number(m[5])
            };
        }
    }

    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return null;
    return {
        year: d.getFullYear(),
        month: d.getMonth() + 1,
        day: d.getDate(),
        hour: d.getHours(),
        minute: d.getMinutes()
    };
}

/** Час във формат 00-23 (military time), без AM/PM */
function formatTime24(value) {
    const parts = splitDateTime(value);
    if (!parts) return '';
    return `${pad2(parts.hour)}:${pad2(parts.minute)}`;
}

/** Дата + час в 24-часов формат, напр. 06.08.2026 17:00 */
function formatDateTime(value) {
    const parts = splitDateTime(value);
    if (!parts) return value || '';
    return `${pad2(parts.day)}.${pad2(parts.month)}.${parts.year} ${pad2(parts.hour)}:${pad2(parts.minute)}`;
}

function formatDateOnly(value) {
    const parts = splitDateTime(value);
    if (!parts) return '';
    return `${pad2(parts.day)}.${pad2(parts.month)}.${parts.year}`;
}

function formatLabel(format) {
    if (!format) return '2D';
    if (format === 'THREE_D' || format === '3D') return '3D';
    if (format === 'TWO_D' || format === '2D') return '2D';
    return String(format);
}

function cityKey(city) {
    return String(city || '').trim().replace(/\s+/g, ' ').toLowerCase();
}

function uniqueCities(cinemas) {
    const map = new Map();
    (cinemas || []).forEach(c => {
        const raw = String(c.city || '').trim().replace(/\s+/g, ' ');
        if (!raw) return;
        const key = raw.toLowerCase();
        if (!map.has(key)) map.set(key, raw);
    });
    return [...map.values()].sort((a, b) => a.localeCompare(b, 'bg'));
}

function cinemasForCity(cinemas, city) {
    const key = cityKey(city);
    if (!key) return [];
    return (cinemas || []).filter(c => cityKey(c.city) === key);
}

/** За обратна съвместимост със стар код */
function parseDate(value) {
    const parts = splitDateTime(value);
    if (!parts) return null;
    return new Date(parts.year, parts.month - 1, parts.day, parts.hour, parts.minute, 0);
}
