package fi.newdoska.doska.service;

import fi.newdoska.doska.dto.CategoryMenuItem;
import fi.newdoska.doska.dto.CategorySubItem;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryMenuService {

    private static final List<CategoryMenuItem> CATEGORY_MENUS = List.of(
            new CategoryMenuItem(
                    "Недвижимость",
                    "fa-house",
                    "REAL_ESTATE",
                    List.of(
                            new CategorySubItem("Квартиры", "/advertisements?category=REAL_ESTATE&search=квартира"),
                            new CategorySubItem("Дома", "/advertisements?category=REAL_ESTATE&search=дом"),
                            new CategorySubItem("Коммерческая", "/advertisements?category=REAL_ESTATE&search=коммерческая"),
                            new CategorySubItem("Аренда", "/advertisements?category=REAL_ESTATE&search=аренда")
                    )
            ),
            new CategoryMenuItem(
                    "Транспорт",
                    "fa-car",
                    "VEHICLES",
                    List.of(
                            new CategorySubItem("Легковые", "/advertisements?category=VEHICLES&search=авто"),
                            new CategorySubItem("Коммерческие", "/advertisements?category=VEHICLES&search=грузовик"),
                            new CategorySubItem("Мото/скутеры", "/advertisements?category=VEHICLES&search=мото"),
                            new CategorySubItem("Водный транспорт", "/advertisements?category=VEHICLES&search=лодка")
                    )
            ),
            new CategoryMenuItem(
                    "Электроника",
                    "fa-plug",
                    "ELECTRONICS",
                    List.of(
                            new CategorySubItem("Смартфоны", "/advertisements?category=ELECTRONICS&search=смартфон"),
                            new CategorySubItem("Ноутбуки", "/advertisements?category=ELECTRONICS&search=ноутбук"),
                            new CategorySubItem("Аудио/Видео", "/advertisements?category=ELECTRONICS&search=аудио"),
                            new CategorySubItem("Умный дом", "/advertisements?category=ELECTRONICS&search=smart")
                    )
            ),
            new CategoryMenuItem(
                    "Услуги",
                    "fa-briefcase",
                    "SERVICES",
                    List.of(
                            new CategorySubItem("Ремонт и стройка", "/advertisements?category=SERVICES&search=ремонт"),
                            new CategorySubItem("Красота и здоровье", "/advertisements?category=SERVICES&search=косметолог"),
                            new CategorySubItem("Образование", "/advertisements?category=SERVICES&search=курсы"),
                            new CategorySubItem("IT и маркетинг", "/advertisements?category=SERVICES&search=маркетинг")
                    )
            ),
            new CategoryMenuItem(
                    "Работа",
                    "fa-briefcase",
                    "JOBS",
                    List.of(
                            new CategorySubItem("Ищу работу", "/advertisements?category=JOBS&type=JOB_SEEKING"),
                            new CategorySubItem("Предлагаю работу", "/advertisements?category=JOBS&type=JOB_OFFERING"),
                            new CategorySubItem("Полная занятость", "/advertisements?category=JOBS&type=FULL_TIME"),
                            new CategorySubItem("Частичная занятость", "/advertisements?category=JOBS&type=PART_TIME"),
                            new CategorySubItem("Удалённо", "/advertisements?category=JOBS&type=REMOTE"),
                            new CategorySubItem("Стажировки", "/advertisements?category=JOBS&type=INTERNSHIP"),
                            new CategorySubItem("Контракт", "/advertisements?category=JOBS&type=CONTRACT"),
                            new CategorySubItem("Временная работа", "/advertisements?category=JOBS&type=TEMPORARY")
                    )
            ),
            new CategoryMenuItem(
                    "Дом и быт",
                    "fa-couch",
                    "FURNITURE",
                    List.of(
                            new CategorySubItem("Мебель", "/advertisements?category=FURNITURE"),
                            new CategorySubItem("Бытовая техника", "/advertisements?category=ELECTRONICS&search=бытовая"),
                            new CategorySubItem("Декор", "/advertisements?category=FURNITURE&search=декор"),
                            new CategorySubItem("Сад и дача", "/advertisements?category=FURNITURE&search=сад")
                    )
            )
    );

    public List<CategoryMenuItem> getCategoryMenus() {
        return CATEGORY_MENUS;
    }
}

