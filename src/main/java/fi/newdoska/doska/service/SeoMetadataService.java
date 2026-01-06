package fi.newdoska.doska.service;

import fi.newdoska.doska.dto.SeoMetadata;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SeoMetadataService {

    private final Environment environment;
    private final Map<String, SeoMetadata> categoryMeta = new HashMap<>();

    @Value("${seo.home.title:Умная доска объявлений для ruomi.fi}")
    private String homeTitle;
    @Value("${seo.home.description:ruomi.fi — самая удобная площадка для русскоязычных жителей Финляндии.}")
    private String homeDescription;
    @Value("${seo.home.keywords:vfinke,доска объявлений,финляндия,купить,продать,аренда}")
    private String homeKeywords;

    public SeoMetadataService(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void initDefaults() {
        // Defaults can be overridden via application.properties:
        putCategoryMeta("REAL_ESTATE",
                "${seo.category.REAL_ESTATE.title:Недвижимость в Финляндии — ruomi.fi}",
                "${seo.category.REAL_ESTATE.description:Самые свежие объявления по аренде и продаже недвижимости в Финляндии.}",
                "${seo.category.REAL_ESTATE.keywords:квартира,аренда,финляндия,недвижимость}");

        putCategoryMeta("VEHICLES",
                "${seo.category.VEHICLES.title:Транспорт и авто — ruomi.fi}",
                "${seo.category.VEHICLES.description:Автомобили, мотоциклы и спецтехника для русскоязычных жителей Финляндии.}",
                "${seo.category.VEHICLES.keywords:авто,машина,мото,финляндия}");

        putCategoryMeta("ELECTRONICS",
                "${seo.category.ELECTRONICS.title:Электроника и гаджеты — ruomi.fi}",
                "${seo.category.ELECTRONICS.description:Телефоны, ноутбуки и техника по лучшим ценам.}",
                "${seo.category.ELECTRONICS.keywords:смартфон,ноутбук,электроника}");

        putCategoryMeta("SERVICES",
                "${seo.category.SERVICES.title:Услуги и фриланс — ruomi.fi}",
                "${seo.category.SERVICES.description:Русскоязычные специалисты и сервисы в Финляндии.}",
                "${seo.category.SERVICES.keywords:услуги,ремонт,фриланс}");

        putCategoryMeta("JOBS",
                "${seo.category.JOBS.title:Работа в Финляндии — ruomi.fi}",
                "${seo.category.JOBS.description:Найдите работу мечты или сотрудника в русскоязычном сообществе.}",
                "${seo.category.JOBS.keywords:работа,вакансии,финляндия}");

        putCategoryMeta("FURNITURE",
                "${seo.category.FURNITURE.title:Дом, мебель и декор — ruomi.fi}",
                "${seo.category.FURNITURE.description:Все для дома, мебели и уюта в Финляндии.}",
                "${seo.category.FURNITURE.keywords:мебель,дом,уют}");
    }

    private void putCategoryMeta(String category, String titleSpel, String descriptionSpel, String keywordsSpel) {
        String title = environment.resolveRequiredPlaceholders(titleSpel);
        String desc = environment.resolveRequiredPlaceholders(descriptionSpel);
        String kw = environment.resolveRequiredPlaceholders(keywordsSpel);
        categoryMeta.put(category.toUpperCase(), new SeoMetadata(title, desc, kw));
    }

    public SeoMetadata getHomePageMeta() {
        return new SeoMetadata(homeTitle, homeDescription, homeKeywords);
    }

    public SeoMetadata getCategoryMeta(String category) {
        return categoryMeta.getOrDefault(category.toUpperCase(), getHomePageMeta());
    }

    public SeoMetadata getSearchMeta(String query) {
        String title = "Результаты по запросу \"" + query + "\" — ruomi.fi";
        String description = "Объявления по запросу \"" + query + "\" на ruomi.fi. Настройте фильтры и найдите нужное быстрее.";
        String keywords = "vfinke, поиск, объявления, " + query;
        return new SeoMetadata(title, description, keywords);
    }
}


