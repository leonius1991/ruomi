/**
 * Finnish cities, regions (maakunta) and districts — autocomplete + modal picker
 */
window.FinnishLocations = (function () {
    let items = [];
    let loaded = false;
    let loadPromise = null;

    const TYPE_LABELS = { city: 'Город', region: 'Область', district: 'Район' };

    const REGIONS = [
        'Уусимаа', 'Юго-Западная Финляндия', 'Сatakunta', 'Kanta-Häme', 'Pirkanmaa',
        'Päijät-Häme', 'Kymenlaakso', 'Южная Карелия', 'Южное Сavo', 'Северное Сavo',
        'Северная Карелия', 'Центральная Финляндия', 'Южная Остроботния', 'Остроботния',
        'Центральная Остроботния', 'Северная Остроботния', 'Kainuu', 'Лапландия', 'Аlandские острова'
    ];

    function load() {
        if (loaded) return Promise.resolve(items);
        if (loadPromise) return loadPromise;
        loadPromise = fetch('/data/finnish-locations.json')
            .then(r => r.ok ? r.json() : fetch('/data/finnish-cities.json').then(x => x.json()))
            .then(data => {
                if (Array.isArray(data)) {
                    items = [...new Set(data)].map(name => ({ name, type: 'city', group: 'Города' }));
                } else {
                    items = data.items || [];
                }
                REGIONS.forEach(name => {
                    if (!items.some(i => i.name === name)) {
                        items.push({ name, type: 'region', group: 'Области (маакунта)' });
                    }
                });
                loaded = true;
                return items;
            })
            .catch(() => {
                items = REGIONS.map(name => ({ name, type: 'region', group: 'Области' }));
                loaded = true;
                return items;
            });
        return loadPromise;
    }

    function filter(query, limit) {
        const q = (query || '').trim().toLowerCase();
        if (q.length < 1) return [];
        return items.filter(i => i.name.toLowerCase().includes(q)).slice(0, limit || 12);
    }

    function initAutocomplete(inputId) {
        const input = document.getElementById(inputId);
        if (!input) return;
        const parent = input.parentElement;
        if (getComputedStyle(parent).position === 'static') parent.style.position = 'relative';

        let box = parent.querySelector('.location-suggestions');
        if (!box) {
            box = document.createElement('div');
            box.className = 'location-suggestions list-group';
            box.style.cssText = 'position:absolute;z-index:1050;max-height:220px;overflow-y:auto;display:none;width:100%;margin-top:2px;border-radius:8px;box-shadow:0 8px 24px rgba(0,0,0,.2);';
            parent.appendChild(box);
        }

        load().then(() => {
            input.addEventListener('input', () => {
                const hits = filter(input.value, 10);
                box.innerHTML = '';
                if (!hits.length) { box.style.display = 'none'; return; }
                hits.forEach(item => {
                    const a = document.createElement('button');
                    a.type = 'button';
                    a.className = 'list-group-item list-group-item-action py-2 px-3';
                    a.innerHTML = `<span>${item.name}</span> <small class="text-muted ms-1">${TYPE_LABELS[item.type] || ''}</small>`;
                    a.addEventListener('click', () => {
                        input.value = item.name;
                        box.style.display = 'none';
                        input.dispatchEvent(new Event('change', { bubbles: true }));
                    });
                    box.appendChild(a);
                });
                box.style.display = 'block';
            });
            document.addEventListener('click', e => {
                if (!input.contains(e.target) && !box.contains(e.target)) box.style.display = 'none';
            });
        });
    }

    function openPickerModal(onSelect) {
        load().then(() => {
            const backdrop = document.createElement('div');
            backdrop.className = 'ruomi-modal-backdrop';
            backdrop.innerHTML = `
                <div class="ruomi-modal" style="max-width:480px;">
                    <div class="ruomi-modal-header">
                        <h5>Выбор города или области</h5>
                        <button type="button" class="ruomi-modal-close">&times;</button>
                    </div>
                    <div class="ruomi-modal-body">
                        <input type="search" class="form-control mb-3" id="locPickerSearch" placeholder="Начните вводить название..." autofocus>
                        <div id="locPickerResults" style="max-height:320px;overflow-y:auto;"></div>
                    </div>
                </div>`;
            document.body.appendChild(backdrop);
            const close = () => backdrop.remove();
            backdrop.querySelector('.ruomi-modal-close').addEventListener('click', close);
            backdrop.addEventListener('click', e => { if (e.target === backdrop) close(); });

            const search = backdrop.querySelector('#locPickerSearch');
            const results = backdrop.querySelector('#locPickerResults');

            function render(list) {
                results.innerHTML = list.length ? '' : '<p class="text-muted small mb-0">Ничего не найдено</p>';
                list.forEach(item => {
                    const btn = document.createElement('button');
                    btn.type = 'button';
                    btn.className = 'list-group-item list-group-item-action d-flex justify-content-between align-items-center';
                    btn.innerHTML = `<span>${item.name}</span><small class="text-muted">${item.group || TYPE_LABELS[item.type]}</small>`;
                    btn.addEventListener('click', () => {
                        if (onSelect) onSelect(item.name);
                        close();
                    });
                    results.appendChild(btn);
                });
            }

            render(items.slice(0, 20));
            search.addEventListener('input', () => render(filter(search.value, 30)));
        });
    }

    function navigateToCity(city) {
        window.location.href = '/advertisements?city=' + encodeURIComponent(city);
    }

    function initNavbarPicker() {
        document.querySelectorAll('[data-location-pick]').forEach(el => {
            el.addEventListener('click', e => {
                e.preventDefault();
                openPickerModal(name => navigateToCity(name));
            });
        });
        const label = document.getElementById('navbarCityLabel');
        if (label) {
            const params = new URLSearchParams(window.location.search);
            const c = params.get('city');
            if (c) label.textContent = c;
        }
    }

    return { load, filter, initAutocomplete, openPickerModal, navigateToCity, initNavbarPicker };
})();

document.addEventListener('DOMContentLoaded', () => {
    FinnishLocations.initNavbarPicker();
    if (document.getElementById('cityInput')) {
        FinnishLocations.initAutocomplete('cityInput');
    }
});
