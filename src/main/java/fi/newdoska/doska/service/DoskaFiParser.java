package fi.newdoska.doska.service;

import fi.newdoska.doska.entity.Advertisement;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class DoskaFiParser {

    public static final String SOURCE = "doska.fi";

    private static final Pattern POST_ID = Pattern.compile("/post/(\\d+)");
    private static final Pattern PRICE = Pattern.compile("([\\d\\s.,]+)\\s*(?:€|eur|euro)", Pattern.CASE_INSENSITIVE);

    @Value("${doska.import.base-url:https://doska.fi}")
    private String baseUrl;

    @Value("${doska.import.user-agent:Mozilla/5.0 (compatible; RuomiBot/1.0; +https://ruomi.fi)}")
    private String userAgent;

    public record ParsedPost(
            String externalId,
            String title,
            String description,
            String categoryLabel,
            String city,
            BigDecimal price,
            String sourceUrl
    ) {}

    public List<String> fetchLatestPostIds(int limit) {
        try {
            Document doc = fetch(baseUrl + "/fresh");
            Set<String> ids = new LinkedHashSet<>();
            for (Element link : doc.select("a[href*=/post/]")) {
                String href = link.attr("href");
                Matcher m = POST_ID.matcher(href);
                if (m.find()) {
                    ids.add(m.group(1));
                    if (ids.size() >= limit) {
                        break;
                    }
                }
            }
            return new ArrayList<>(ids);
        } catch (Exception e) {
            log.error("Не удалось загрузить список объявлений с doska.fi/fresh", e);
            return List.of();
        }
    }

    public ParsedPost fetchPost(String postId) {
        String url = baseUrl + "/post/" + postId;
        try {
            Document doc = fetch(url);
            String title = textOrEmpty(doc.selectFirst("div.post_title"));
            if (title.isBlank()) {
                String pageTitle = doc.title();
                if (pageTitle.contains("::")) {
                    String[] parts = pageTitle.split("::");
                    if (parts.length >= 2) {
                        title = parts[1].trim();
                    }
                }
            }

            Element categoryLink = doc.selectFirst("div.cate_post_title a");
            String categoryLabel = categoryLink != null ? categoryLink.text().trim() : "";

            String description = extractDescription(doc);
            String city = extractMetadata(doc, "Город");
            BigDecimal price = parsePrice(extractMetadata(doc, "Цена"));

            if (title.isBlank()) {
                log.warn("Пустой заголовок для post {}", postId);
                return null;
            }
            if (description.isBlank()) {
                description = title;
            }

            return new ParsedPost(postId, title, description, categoryLabel, city, price, url);
        } catch (Exception e) {
            log.warn("Не удалось разобрать объявление {}: {}", postId, e.getMessage());
            return null;
        }
    }

    public Advertisement.Category mapCategory(String categoryLabel) {
        if (categoryLabel == null || categoryLabel.isBlank()) {
            return Advertisement.Category.OTHER;
        }
        String lower = categoryLabel.toLowerCase();
        if (containsAny(lower, "недвижим", "квартир", "дом", "комнат", "участок", "офис", "гараж")) {
            return Advertisement.Category.REAL_ESTATE;
        }
        if (containsAny(lower, "авто", "транспорт", "машин", "мото", "прицеп", "лодк", "судн")) {
            return Advertisement.Category.VEHICLES;
        }
        if (containsAny(lower, "электрон", "компьютер", "телефон", "it-", "it ", "техник", "бытов")) {
            return Advertisement.Category.ELECTRONICS;
        }
        if (containsAny(lower, "мебел", "интерьер", "кухн", "быт")) {
            return Advertisement.Category.FURNITURE;
        }
        if (containsAny(lower, "одежд", "обув", "аксессуар")) {
            return Advertisement.Category.CLOTHING;
        }
        if (containsAny(lower, "книг", "литератур")) {
            return Advertisement.Category.BOOKS;
        }
        if (containsAny(lower, "спорт", "фитнес", "туризм")) {
            return Advertisement.Category.SPORTS;
        }
        if (containsAny(lower, "работ", "ваканс", "резюме", "ищу работ", "требуют")) {
            return Advertisement.Category.JOBS;
        }
        if (containsAny(lower, "услуг", "ремонт", "перевод", "образован", "красот", "медиц")) {
            return Advertisement.Category.SERVICES;
        }
        return Advertisement.Category.OTHER;
    }

    public Advertisement.AdvertisementType mapType(String categoryLabel, String title) {
        String combined = ((categoryLabel != null ? categoryLabel : "") + " " + (title != null ? title : "")).toLowerCase();
        if (containsAny(combined, "ищу работ", "резюме", "ищу подработ")) {
            return Advertisement.AdvertisementType.JOB_SEEKING;
        }
        if (containsAny(combined, "требу", "ваканс", "предлагаю работ", "на работу")) {
            return Advertisement.AdvertisementType.JOB_OFFERING;
        }
        if (containsAny(combined, "куплю", "купл ", "ищу ")) {
            return Advertisement.AdvertisementType.BUY;
        }
        if (containsAny(combined, "сниму", "сним", "аренд", "сдам", "сдаю", "сдаётся")) {
            return Advertisement.AdvertisementType.RENT;
        }
        if (containsAny(combined, "обмен", "меняю")) {
            return Advertisement.AdvertisementType.EXCHANGE;
        }
        if (containsAny(combined, "услуг")) {
            return Advertisement.AdvertisementType.SERVICE;
        }
        return Advertisement.AdvertisementType.SALE;
    }

    private Document fetch(String url) throws java.io.IOException {
        return Jsoup.connect(url)
                .userAgent(userAgent)
                .timeout(20000)
                .followRedirects(true)
                .get();
    }

    private String extractDescription(Document doc) {
        Element bodyCell = doc.selectFirst("table.post_body td");
        if (bodyCell == null) {
            return "";
        }
        return bodyCell.text().trim();
    }

    private String extractMetadata(Document doc, String label) {
        for (Element row : doc.select("table.post_metadata tr")) {
            Elements cells = row.select("td");
            if (cells.size() >= 2 && label.equalsIgnoreCase(cells.get(0).text().trim())) {
                return cells.get(1).text().trim();
            }
        }
        return "";
    }

    private BigDecimal parsePrice(String raw) {
        if (raw == null || raw.isBlank() || raw.toLowerCase().contains("не указ")) {
            return null;
        }
        Matcher m = PRICE.matcher(raw);
        if (m.find()) {
            String num = m.group(1).replace(" ", "").replace(",", ".");
            try {
                return new BigDecimal(num);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        String digits = raw.replaceAll("[^\\d.,]", "").replace(",", ".");
        if (digits.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String textOrEmpty(Element el) {
        return el != null ? el.text().trim() : "";
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String n : needles) {
            if (haystack.contains(n)) {
                return true;
            }
        }
        return false;
    }
}
