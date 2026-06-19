package fi.newdoska.doska.service;

import fi.newdoska.doska.entity.Advertisement;
import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.repository.AdvertisementRepository;
import fi.newdoska.doska.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoskaFiImportService {

    private static final String IMPORT_USERNAME = "doska_import";

    private final DoskaFiParser parser;
    private final AdvertisementRepository advertisementRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppSettingsService appSettingsService;

    @Value("${doska.import.daily-limit:15}")
    private int dailyLimit;

    @Transactional
    public ImportResult importLatest() {
        if (!appSettingsService.isDoskaImportEnabled()) {
            log.debug("Импорт doska.fi отключён");
            return new ImportResult(0, 0, 0);
        }

        User importUser = getOrCreateImportUser();
        List<String> postIds = parser.fetchLatestPostIds(Math.max(dailyLimit, 20));
        int imported = 0;
        int skipped = 0;
        int failed = 0;

        for (String postId : postIds) {
            if (imported >= dailyLimit) {
                break;
            }
            if (advertisementRepository.existsByExternalSourceAndExternalId(DoskaFiParser.SOURCE, postId)) {
                skipped++;
                continue;
            }

            DoskaFiParser.ParsedPost post = parser.fetchPost(postId);
            if (post == null) {
                failed++;
                continue;
            }

            Advertisement ad = new Advertisement();
            ad.setTitle(truncate(post.title(), 200));
            ad.setDescription(buildDescription(post));
            ad.setCategory(parser.mapCategory(post.categoryLabel()));
            ad.setType(parser.mapType(post.categoryLabel(), post.title()));
            ad.setPrice(post.price());
            ad.setCity(truncate(post.city(), 100));
            ad.setLocation(ad.getCity());
            ad.setStatus(Advertisement.Status.APPROVED);
            ad.setPremium(false);
            ad.setUrgent(false);
            ad.setShowPhone(false);
            ad.setUser(importUser);
            ad.setCreatedAt(LocalDateTime.now());
            ad.setPublishedAt(LocalDateTime.now());
            ad.setExpiresAt(LocalDateTime.now().plusDays(30));
            ad.setExternalSource(DoskaFiParser.SOURCE);
            ad.setExternalId(post.externalId());

            advertisementRepository.save(ad);
            imported++;
            log.info("Импортировано объявление doska.fi #{}: {}", postId, post.title());
        }

        log.info("Импорт doska.fi завершён: добавлено {}, пропущено {}, ошибок {}", imported, skipped, failed);
        return new ImportResult(imported, skipped, failed);
    }

    public record ImportResult(int imported, int skipped, int failed) {}

    private User getOrCreateImportUser() {
        return userRepository.findByUsername(IMPORT_USERNAME).orElseGet(() -> {
            User user = new User();
            user.setUsername(IMPORT_USERNAME);
            user.setEmail("import@ruomi.fi");
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setFirstName("Doska.fi");
            user.setLastName("Import");
            user.setRole(User.UserRole.USER);
            user.setEnabled(true);
            return userRepository.save(user);
        });
    }

    private String buildDescription(DoskaFiParser.ParsedPost post) {
        String body = truncate(post.description(), 4800);
        String footer = "\n\n—\nОбъявление импортировано с doska.fi для наполнения каталога. "
                + "Оригинал: " + post.sourceUrl();
        return body + footer;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
