/**
 * Lightweight modal helper for ruomi.fi
 */
window.RuomiModal = {
    show(options) {
        const { title, body, buttons = [], onClose } = options;
        const backdrop = document.createElement('div');
        backdrop.className = 'ruomi-modal-backdrop';
        backdrop.setAttribute('role', 'dialog');

        const buttonsHtml = buttons.map(b =>
            `<button type="button" class="btn ${b.className || 'btn-outline-teal'} btn-sm" data-action="${b.action || 'close'}">${b.label}</button>`
        ).join(' ');

        backdrop.innerHTML = `
            <div class="ruomi-modal">
                <div class="ruomi-modal-header">
                    <h5>${title || ''}</h5>
                    <button type="button" class="ruomi-modal-close" aria-label="Закрыть">&times;</button>
                </div>
                <div class="ruomi-modal-body">${body || ''}</div>
                ${buttons.length ? `<div class="px-3 pb-3 d-flex gap-2 justify-content-end flex-wrap">${buttonsHtml}</div>` : ''}
            </div>`;

        const close = () => {
            backdrop.remove();
            if (onClose) onClose();
        };

        backdrop.querySelector('.ruomi-modal-close').addEventListener('click', close);
        backdrop.addEventListener('click', e => { if (e.target === backdrop) close(); });

        buttons.forEach((btn, i) => {
            const el = backdrop.querySelectorAll('[data-action]')[i];
            if (el) {
                el.addEventListener('click', () => {
                    if (btn.onClick) btn.onClick(close);
                    else close();
                });
            }
        });

        document.body.appendChild(backdrop);
        return { close, el: backdrop };
    },

    alert(title, message, onClose) {
        return this.show({
            title,
            body: `<p class="mb-0">${message}</p>`,
            buttons: [{ label: 'OK', className: 'btn-coral', onClick: close => close() }],
            onClose
        });
    },

    confirm(title, message, onConfirm) {
        return this.show({
            title,
            body: `<p class="mb-0">${message}</p>`,
            buttons: [
                { label: 'Отмена', className: 'btn-outline-teal', onClick: close => close() },
                { label: 'Подтвердить', className: 'btn-coral', onClick: close => { close(); if (onConfirm) onConfirm(); } }
            ]
        });
    }
};
