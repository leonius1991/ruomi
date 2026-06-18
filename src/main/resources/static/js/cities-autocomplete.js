// Автокомплит городов Финляндии
function initCityAutocomplete(inputId) {
    let cities = [];
    let currentFocus = -1;
    
    const cityInput = document.getElementById(inputId);
    if (!cityInput) {
        console.warn('City input not found:', inputId);
        return;
    }
    
    // Убеждаемся, что родительский элемент имеет position: relative
    const parentElement = cityInput.parentElement;
    if (window.getComputedStyle(parentElement).position === 'static') {
        parentElement.style.position = 'relative';
    }
    
    // Создаем контейнер для подсказок, если его нет
    let suggestionsDiv = document.getElementById('citySuggestions');
    if (!suggestionsDiv) {
        suggestionsDiv = document.createElement('div');
        suggestionsDiv.id = 'citySuggestions';
        suggestionsDiv.className = 'list-group';
        suggestionsDiv.style.cssText = 'position: absolute; z-index: 1000; max-height: 200px; overflow-y: auto; display: none; width: 100%; margin-top: 2px; background: white; border: 1px solid #dee2e6; border-radius: 0.375rem; box-shadow: 0 0.5rem 1rem rgba(0,0,0,0.15);';
        parentElement.appendChild(suggestionsDiv);
    }
    
    // Загружаем список городов
    fetch('/data/finnish-cities.json')
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to load cities');
            }
            return response.json();
        })
        .then(data => {
            cities = data;
            console.log('Loaded', cities.length, 'cities');
        })
        .catch(error => {
            console.error('Error loading cities:', error);
        });
    
    // Обработка ввода
    cityInput.addEventListener('input', function(e) {
        const value = this.value.trim();
        
        // Закрываем предыдущие подсказки
        closeSuggestions();
        
        if (value.length < 2) {
            return;
        }
        
        // Фильтруем города
        const filtered = cities.filter(city => 
            city.toLowerCase().startsWith(value.toLowerCase())
        ).slice(0, 10); // Максимум 10 подсказок
        
        if (filtered.length > 0) {
            showSuggestions(filtered, value);
        }
    });
    
    // Обработка клавиатуры
    cityInput.addEventListener('keydown', function(e) {
        const items = suggestionsDiv.querySelectorAll('.list-group-item');
        
        if (e.key === 'ArrowDown') {
            e.preventDefault();
            currentFocus++;
            setActive(items);
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            currentFocus--;
            setActive(items);
        } else if (e.key === 'Enter') {
            e.preventDefault();
            if (currentFocus > -1 && items[currentFocus]) {
                items[currentFocus].click();
            }
        } else if (e.key === 'Escape') {
            closeSuggestions();
        }
    });
    
    // Закрытие при клике вне поля
    document.addEventListener('click', function(e) {
        if (!cityInput.contains(e.target) && !suggestionsDiv.contains(e.target)) {
            closeSuggestions();
        }
    });
    
    function showSuggestions(filtered, value) {
        suggestionsDiv.innerHTML = '';
        currentFocus = -1;
        
        if (filtered.length === 0) {
            return;
        }
        
        filtered.forEach(city => {
            const item = document.createElement('a');
            item.className = 'list-group-item list-group-item-action';
            item.href = '#';
            item.style.cssText = 'cursor: pointer; padding: 0.5rem 1rem;';
            
            // Выделяем совпадающую часть
            const matchIndex = city.toLowerCase().indexOf(value.toLowerCase());
            if (matchIndex !== -1) {
                const before = city.substring(0, matchIndex);
                const match = city.substring(matchIndex, matchIndex + value.length);
                const after = city.substring(matchIndex + value.length);
                item.innerHTML = before + '<strong>' + match + '</strong>' + after;
            } else {
                item.textContent = city;
            }
            
            item.addEventListener('click', function(e) {
                e.preventDefault();
                cityInput.value = city;
                closeSuggestions();
                // Триггерим событие change для валидации формы
                cityInput.dispatchEvent(new Event('change', { bubbles: true }));
            });
            
            suggestionsDiv.appendChild(item);
        });
        
        suggestionsDiv.style.display = 'block';
        
        // Позиционируем подсказки относительно родительского элемента
        const parentRect = parentElement.getBoundingClientRect();
        const inputRect = cityInput.getBoundingClientRect();
        suggestionsDiv.style.top = (inputRect.bottom - parentRect.top + 2) + 'px';
        suggestionsDiv.style.left = '0px';
        suggestionsDiv.style.width = inputRect.width + 'px';
    }
    
    function setActive(items) {
        if (!items || items.length === 0) return;
        
        // Убираем активный класс со всех элементов
        items.forEach(item => item.classList.remove('active'));
        
        if (currentFocus >= items.length) {
            currentFocus = 0;
        }
        if (currentFocus < 0) {
            currentFocus = items.length - 1;
        }
        
        if (items[currentFocus]) {
            items[currentFocus].classList.add('active');
            items[currentFocus].scrollIntoView({ block: 'nearest' });
        }
    }
    
    function closeSuggestions() {
        suggestionsDiv.style.display = 'none';
        currentFocus = -1;
    }
}

// Автоматическая инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    const cityInput = document.getElementById('cityInput');
    if (cityInput) {
        initCityAutocomplete('cityInput');
    }
});
