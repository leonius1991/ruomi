package fi.newdoska.doska.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubUpdateService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final VersionService versionService;
    
    @Value("${app.update.github.owner:}")
    private String githubOwner;
    
    @Value("${app.update.github.repo:}")
    private String githubRepo;
    
    @Value("${app.update.github.token:}")
    private String githubToken;
    
    public static class ReleaseInfo {
        private String version;
        private String tagName;
        private String downloadUrl;
        private String resourcesUrl; // URL для скачивания ресурсов
        private String releaseNotes;
        private String publishedAt;
        private boolean isNewer;
        
        public ReleaseInfo() {}
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public String getTagName() { return tagName; }
        public void setTagName(String tagName) { this.tagName = tagName; }
        
        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
        
        public String getResourcesUrl() { return resourcesUrl; }
        public void setResourcesUrl(String resourcesUrl) { this.resourcesUrl = resourcesUrl; }
        
        public String getReleaseNotes() { return releaseNotes; }
        public void setReleaseNotes(String releaseNotes) { this.releaseNotes = releaseNotes; }
        
        public String getPublishedAt() { return publishedAt; }
        public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }
        
        public boolean isNewer() { return isNewer; }
        public void setNewer(boolean newer) { isNewer = newer; }
    }
    
    public ReleaseInfo getLatestRelease() {
        if (githubOwner == null || githubOwner.isEmpty() || 
            githubRepo == null || githubRepo.isEmpty()) {
            log.warn("GitHub репозиторий не настроен");
            return null;
        }
        
        String url = String.format("https://api.github.com/repos/%s/%s/releases/latest", 
                githubOwner, githubRepo);
        
        try {
            HttpHeaders headers = new HttpHeaders();
            if (githubToken != null && !githubToken.isEmpty()) {
                headers.set("Authorization", "token " + githubToken);
            }
            headers.set("Accept", "application/vnd.github.v3+json");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());
                
                ReleaseInfo release = new ReleaseInfo();
                release.setTagName(json.get("tag_name").asText());
                release.setVersion(extractVersionFromTag(json.get("tag_name").asText()));
                release.setReleaseNotes(json.has("body") ? json.get("body").asText() : "");
                release.setPublishedAt(json.has("published_at") ? json.get("published_at").asText() : "");
                
                // Ищем JAR файл и ресурсы в assets
                if (json.has("assets")) {
                    for (JsonNode asset : json.get("assets")) {
                        String name = asset.get("name").asText();
                        String downloadUrl = asset.get("browser_download_url").asText();
                        
                        if (name.endsWith(".jar") && !name.contains("sources") && !name.contains("javadoc")) {
                            release.setDownloadUrl(downloadUrl);
                        } else if (name.contains("resources") && name.endsWith(".zip")) {
                            release.setResourcesUrl(downloadUrl);
                        }
                    }
                }
                
                // Проверяем, является ли версия новой
                String currentVersion = versionService.getCurrentVersion();
                release.setNewer(isVersionNewer(release.getVersion(), currentVersion));
                
                return release;
            } else {
                log.error("GitHub API вернул статус: {} для URL: {}", response.getStatusCode(), url);
                if (response.getStatusCode().value() == 403) {
                    log.error("403 Forbidden - проверьте настройки GitHub token");
                }
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode().value() == 403) {
                log.error("GitHub API вернул 403 Forbidden при получении последнего релиза");
                log.error("URL: {}", url);
                log.error("Проверьте:");
                log.error("1. Настройку app.update.github.token в application.properties");
                log.error("2. Права доступа токена (нужны права на чтение репозитория)");
                log.error("3. Является ли репозиторий приватным (для приватных нужен токен)");
                if (githubToken == null || githubToken.isEmpty()) {
                    log.error("GitHub token не настроен!");
                }
            } else if (e.getStatusCode().value() == 404) {
                log.error("Репозиторий не найден: {}/{}", githubOwner, githubRepo);
            } else {
                log.error("Ошибка HTTP при получении последнего релиза: {}", e.getStatusCode(), e);
            }
        } catch (Exception e) {
            log.error("Ошибка при получении последнего релиза с GitHub", e);
        }
        
        return null;
    }
    
    public List<ReleaseInfo> getAllReleases() {
        if (githubOwner == null || githubOwner.isEmpty() || 
            githubRepo == null || githubRepo.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            String url = String.format("https://api.github.com/repos/%s/%s/releases", 
                    githubOwner, githubRepo);
            
            HttpHeaders headers = new HttpHeaders();
            if (githubToken != null && !githubToken.isEmpty()) {
                headers.set("Authorization", "token " + githubToken);
            }
            headers.set("Accept", "application/vnd.github.v3+json");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode releases = objectMapper.readTree(response.getBody());
                List<ReleaseInfo> releaseList = new ArrayList<>();
                
                String currentVersion = versionService.getCurrentVersion();
                
                for (JsonNode releaseJson : releases) {
                    ReleaseInfo release = new ReleaseInfo();
                    release.setTagName(releaseJson.get("tag_name").asText());
                    release.setVersion(extractVersionFromTag(releaseJson.get("tag_name").asText()));
                    release.setReleaseNotes(releaseJson.has("body") ? releaseJson.get("body").asText() : "");
                    release.setPublishedAt(releaseJson.has("published_at") ? releaseJson.get("published_at").asText() : "");
                    
                    if (releaseJson.has("assets")) {
                        for (JsonNode asset : releaseJson.get("assets")) {
                            String name = asset.get("name").asText();
                            String downloadUrl = asset.get("browser_download_url").asText();
                            
                            if (name.endsWith(".jar") && !name.contains("sources") && !name.contains("javadoc")) {
                                release.setDownloadUrl(downloadUrl);
                            } else if (name.contains("resources") && name.endsWith(".zip")) {
                                release.setResourcesUrl(downloadUrl);
                            }
                        }
                    }
                    
                    release.setNewer(isVersionNewer(release.getVersion(), currentVersion));
                    releaseList.add(release);
                }
                
                    return releaseList;
                } else {
                    log.error("GitHub API вернул статус: {}", response.getStatusCode());
                    if (response.getStatusCode().value() == 403) {
                        log.error("403 Forbidden - проверьте настройки GitHub token и права доступа");
                    }
                }
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                if (e.getStatusCode().value() == 403) {
                    log.error("GitHub API вернул 403 Forbidden при получении списка релизов");
                    log.error("Проверьте настройки: app.update.github.token в application.properties");
                } else {
                    log.error("Ошибка HTTP при получении списка релизов с GitHub: {}", e.getStatusCode(), e);
                }
            } catch (Exception e) {
                log.error("Ошибка при получении списка релизов с GitHub", e);
            }
            
            return new ArrayList<>();
        }
    
    private String extractVersionFromTag(String tag) {
        // Убираем префикс "v" если есть (например, v1.0.0 -> 1.0.0)
        if (tag.startsWith("v") || tag.startsWith("V")) {
            return tag.substring(1);
        }
        return tag;
    }
    
    private boolean isVersionNewer(String newVersion, String currentVersion) {
        try {
            if (newVersion == null || currentVersion == null) {
                return false;
            }
            
            // Убираем префикс "v" если есть
            String cleanNew = newVersion.replaceFirst("^v", "").replace("-SNAPSHOT", "");
            String cleanCurrent = currentVersion.replaceFirst("^v", "").replace("-SNAPSHOT", "");
            
            // Если версии одинаковые, это не новая версия
            if (cleanNew.equals(cleanCurrent)) {
                return false;
            }
            
            // Разбиваем версии на части
            String[] newParts = cleanNew.split("\\.");
            String[] currentParts = cleanCurrent.split("\\.");
            
            int maxLength = Math.max(newParts.length, currentParts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int newPart = 0;
                int currentPart = 0;
                
                try {
                    if (i < newParts.length) {
                        // Убираем нечисловые символы
                        newPart = Integer.parseInt(newParts[i].replaceAll("[^0-9]", ""));
                    }
                    if (i < currentParts.length) {
                        currentPart = Integer.parseInt(currentParts[i].replaceAll("[^0-9]", ""));
                    }
                } catch (NumberFormatException e) {
                    // Если не число, сравниваем как строки
                    String newStr = i < newParts.length ? newParts[i] : "";
                    String currentStr = i < currentParts.length ? currentParts[i] : "";
                    int compare = newStr.compareTo(currentStr);
                    if (compare != 0) {
                        return compare > 0;
                    }
                    continue;
                }
                
                if (newPart > currentPart) {
                    return true;
                } else if (newPart < currentPart) {
                    return false;
                }
            }
            
            // Если все части равны, но строки разные, считаем новой версией
            return !cleanNew.equals(cleanCurrent);
        } catch (Exception e) {
            log.warn("Ошибка при сравнении версий: {} и {}", newVersion, currentVersion, e);
            // В случае ошибки считаем, что это новая версия, если строки разные
            return newVersion != null && currentVersion != null && 
                   !newVersion.equals(currentVersion);
        }
    }
}

