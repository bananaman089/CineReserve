function createParallaxBackground(target = document.body) {
    if (document.querySelector('.parallax-bg')) return;

    const wrap = document.createElement('div');
    wrap.className = 'parallax-bg';
    wrap.setAttribute('aria-hidden', 'true');

    const count = 14;
    for (let i = 0; i < count; i++) {
        const size = 70 + Math.round(Math.random() * 140);
        const circle = document.createElement('div');
        circle.className = 'parallax-circle';
        circle.style.width = size + 'px';
        circle.style.height = size + 'px';
        circle.style.top = Math.round(Math.random() * 100) + '%';
        circle.style.left = Math.round(Math.random() * 100) + '%';
        circle.style.animationDuration = (14 + Math.random() * 18) + 's';
        circle.style.animationDelay = (-Math.random() * 12) + 's';
        circle.style.opacity = (0.25 + Math.random() * 0.45).toFixed(2);
        wrap.appendChild(circle);
    }

    target.prepend(wrap);

    window.addEventListener('mousemove', (e) => {
        const x = (e.clientX / window.innerWidth - 0.5) * 18;
        const y = (e.clientY / window.innerHeight - 0.5) * 18;
        wrap.style.setProperty('--px', x.toFixed(1) + 'px');
        wrap.style.setProperty('--py', y.toFixed(1) + 'px');
    });
}

document.addEventListener('DOMContentLoaded', () => createParallaxBackground());
