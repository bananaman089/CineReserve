async function loadProgram() {
    const list = document.getElementById('projectionList');
    const msg = document.getElementById('clientMsg');

    try {
        const projections = await api('/api/projections');
        renderMovieProgram(list, projections, { mode: 'link' });
    } catch (err) {
        showMsg(msg, err.message, false);
    }
}

loadProgram();
