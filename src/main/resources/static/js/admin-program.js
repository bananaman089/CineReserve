async function initAdminProgram() {
    const msg = document.getElementById('msg');
    try {
        const projections = await api('/api/projections');
        renderMovieProgram(document.getElementById('adminProgram'), projections, { mode: 'link' });
        if (!projections.length) {
            showMsg(msg, 'Все още няма прожекции.', true);
        }
    } catch (err) {
        showMsg(msg, err.message, false);
    }
}

window.initAdminProgram = initAdminProgram;

if (!window.__adminSoftNavBoot) {
    initAdminProgram().finally(() => revealAdminPage());
}
