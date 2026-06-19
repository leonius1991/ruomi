package fi.newdoska.doska.service;

import fi.newdoska.doska.dto.SeoMetadata;
import fi.newdoska.doska.repository.SeoMetadataRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SeoMetadataService {

    private final Environment environment;
    private final SeoMetadataRepository seoMetadataRepository;
    private final Map<String, SeoMetadata> categoryMeta = new HashMap<>();

    @Value("${seo.home.title:ruomi.fi — объявления для русскоязычных в Финляндии}")
    private String homeTitle;
    @Value("${seo.home.description:ruomi.fi — доска объявлений для русскоязычных жителей Финляндии.}")
    private String homeDescription;
    @Value("${seo.home.keywords:ruomi, объявления, финляндия, купить, продать}")
    private String homeKeywords;

    @PostConstruct
    public void initDefaults() {
        putCategoryMeta("REAL_ESTATE",
                "${seo.category.REAL_ESTATE.title:Недвижимость в Финляндии — ruomi.fi}",
                "${seo.category.REAL_ESTATE.description:Аренда и продажа жилья в Финляндии на русском языке.}",
                "${seo.category.REAL_ESTATE.keywords:квартира, аренда, финляндия, недвижимость}");
        putCategoryMeta("VEHICLES",
                "${seo.category.VEHICLES.title:Транспорт и авто — ruomi.fi}",
                "${seo.category.VEHICLES.description:Автомобили, мото и спецтехника для русскоязычных в Финляндии.}",
                "${seo.category.VEHICLES.keywords:авто, машина, мото, финляндия}");
        putCategoryMeta("ELECTRONICS",
                "${seo.category.ELECTRONICS.title:Электроника — ruomi.fi}",
                "${seo.category.ELECTRONICS.description:Телефоны, ноутбуки и техника по лучшим ценам.}",
                "${seo.category.ELECTRONICS.keywords:смартфон, ноутбук, электроника}");
        putCategoryMeta("SERVICES",
                "${seo.category.SERVICES.title:Услуги — ruomi.fi}",
                "${seo.category.SERVICES.description:Русскоязычные специалисты и сервисы в Финляндии.}",
                "${seo.category.SERVICES.keywords:услуги, ремонт, финляндия}");
        putCategoryMeta("JOBS",
                "${seo.category.JOBS.title:Работа в Финляндии — ruomi.fi}",
                "${seo.category.JOBS.description:Вакансии и резюме для русскоязычных в Финляндии.}",
                "${seo.category.JOBS.keywords:работа, вакансии, финляндия}");
        putCategoryMeta("FURNITURE",
                "${seo.category.FURNITURE.title:Дом и мебель — ruomi.fi}",
                "${seo.category.FURNITURE.description:Мебель и товары для дома в Финляндии.}",
                "${seo.category.FURNITURE.keywords:мебель, дом, финляндия}");
        putCategoryMeta("CLOTHING",
                "Одежда и обувь — ruomi.fi",
                "Одежда, обувь и аксессуары в Финляндии на ruomi.fi.",
                "одежда, обувь, финляндия");
        putCategoryMeta("BOOKS",
                "Книги — ruomi.fi",
                "Книги и литература на ruomi.fi.",
                "книги, литература, финляндия");
        putCategoryMeta("SPORTS",
                "Спорт и отдых — ruomi.fi",
                "Спортивные товары и туризм на ruomi.fi.",
                "спорт, туризм, финляндия");
        putCategoryMeta("OTHER",
                "Прочие объявления — ruomi.fi",
                "Разные объявления на ruomi.fi.",
                "объявления, финляндия");
    }

    private void putCategoryMeta(String category, String titleSpel, String descriptionSpel, String keywordsSpel) {
        String title = titleSpel.startsWith("${") ? environment.resolveRequiredPlaceholders(titleSpel) : titleSpel;
        String desc = descriptionSpel.startsWith("${") ? environment.resolveRequiredPlaceholders(descriptionSpel) : descriptionSpel;
        String kw = keywordsSpel.startsWith("${") ? environment.resolveRequiredPlaceholders(keywordsSpel) : keywordsSpel;
        categoryMeta.put(category.toUpperCase(), new SeoMetadata(title, desc, kw));
    }

    public SeoMetadata getPageMeta(String pageKey) {
        return seoMetadataRepository.findByPageKey(pageKey)
                .map(e -> new SeoMetadata(e.getTitle(), e.getDescription(), e.getKeywords()))
                .orElseGet(() -> switch (pageKey) {
                    case "home" -> getHomePageMeta();
                    case "advertisements" -> new SeoMetadata(
                            "Все объявления — ruomi.fi",
                            "Каталог объявлений ruomi.fi по всей Финляндии.",
                            "объявления, ruomi, финляндия");
                    default -> getHomePageMeta();
                });
    }

    public SeoMetadata getHomePageMeta() {
        return seoMetadataRepository.findByPageKey("home")
                .map(e -> new SeoMetadata(e.getTitle(), e.getDescription(), e.getKeywords()))
                .orElse(new SeoMetadata(homeTitle, homeDescription, homeKeywords));
    }

    public SeoMetadata getCategoryMeta(String category) {
        String key = "category:" + category.toUpperCase();
        return seoMetadataRepository.findByPageKey(key)
                .map(e -> new SeoMetadata(e.getTitle(), e.getDescription(), e.getKeywords()))
                .orElse(categoryMeta.getOrDefault(category.toUpperCase(), getHomePageMeta()));
    }

    public SeoMetadata getSearchMeta(String query) {
        String title = "Поиск «" + query + "» — ruomi.fi";
        String description = "Объявления по запросу «" + query + "» на ruomi.fi.";
        String keywords = "поиск, объявления, " + query + ", финляндия";
        return new SeoMetadata(title, description, keywords);
    }

    public SeoMetadata getAdvertisementMeta(String title, String category, String city) {
        String pageTitle = title + " — ruomi.fi";
        String desc = title + ". Категория: " + category
                + (city != null && !city.isBlank() ? ", город: " + city : "")
                + ". Объявления для русскоязычных в Финляндии.";
        return new SeoMetadata(pageTitle, desc, title + ", " + category + ", объявления финляндия");
    }
}
