// Список городов Финляндии для автокомплита
const finnishCities = [
    "Хельсинки", "Эспоо", "Тампере", "Вантаа", "Оулу", "Турку", "Ювяскюля",
    "Лахти", "Куопио", "Пори", "Йоэнсуу", "Лаппеенранта", "Вааса", "Сейняйоки",
    "Рованиеми", "Миккели", "Котка", "Сало", "Раума", "Коккола", "Хямеенлинна",
    "Ярвенпяя", "Нурмиярви", "Кеми", "Иматра", "Аньяланкоски", "Рийхимяки"
];

function initCityAutocomplete(inputId) {
    const input = document.getElementById(inputId);
    if (!input) return;
    
    let currentFocus = -1;
    
    input.addEventListener("input", function(e) {
        const val = this.value;
        closeAllLists();
        if (!val) return;
        
        currentFocus = -1;
        const list = document.createElement("DIV");
        list.setAttribute("id", this.id + "autocomplete-list");
        list.setAttribute("class", "autocomplete-items");
        this.parentNode.appendChild(list);
        
        let count = 0;
        for (let i = 0; i < finnishCities.length && count < 10; i++) {
            if (finnishCities[i].toLowerCase().includes(val.toLowerCase())) {
                const item = document.createElement("DIV");
                item.innerHTML = "<strong>" + finnishCities[i].substr(0, val.length) + "</strong>";
                item.innerHTML += finnishCities[i].substr(val.length);
                item.innerHTML += "<input type='hidden' value='" + finnishCities[i] + "'>";
                item.addEventListener("click", function(e) {
                    input.value = this.getElementsByTagName("input")[0].value;
                    closeAllLists();
                });
                list.appendChild(item);
                count++;
            }
        }
    });
    
    input.addEventListener("keydown", function(e) {
        let x = document.getElementById(this.id + "autocomplete-list");
        if (x) x = x.getElementsByTagName("div");
        if (e.keyCode == 40) {
            currentFocus++;
            addActive(x);
        } else if (e.keyCode == 38) {
            currentFocus--;
            addActive(x);
        } else if (e.keyCode == 13) {
            e.preventDefault();
            if (currentFocus > -1 && x) {
                x[currentFocus].click();
            }
        }
    });
    
    function addActive(x) {
        if (!x) return false;
        removeActive(x);
        if (currentFocus >= x.length) currentFocus = 0;
        if (currentFocus < 0) currentFocus = (x.length - 1);
        x[currentFocus].classList.add("autocomplete-active");
    }
    
    function removeActive(x) {
        for (let i = 0; i < x.length; i++) {
            x[i].classList.remove("autocomplete-active");
        }
    }
    
    function closeAllLists(elmnt) {
        const x = document.getElementsByClassName("autocomplete-items");
        for (let i = 0; i < x.length; i++) {
            if (elmnt != x[i] && elmnt != input) {
                x[i].parentNode.removeChild(x[i]);
            }
        }
    }
    
    document.addEventListener("click", function(e) {
        closeAllLists(e.target);
    });
}


