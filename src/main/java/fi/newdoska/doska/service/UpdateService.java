package fi.newdoska.doska.service;

import fi.newdoska.doska.entity.AppVersion;
import fi.newdoska.doska.repository.AppVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateService {
    
    private final AppVersionRepository appVersionRepository;
    private final VersionService versionService;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${app.update.download.path:./updates}")
    private String downloadPath;
    
    @Value("${app.update.backup.path:./backups}")
    private String backupPath;
    
    @Value("${app.jar.path:./target/doska-0.0.1-SNAPSHOT.jar}")
    private String currentJarPath;
    
    private final AtomicReference<UpdateStatus> currentUpdateStatus = new AtomicReference<>(UpdateStatus.IDLE);
    private final List<String> updateLogs = new ArrayList<>();
    
    public enum UpdateStatus {
        IDLE, CHECKING, DOWNLOADING, BACKING_UP, INSTALLING, SUCCESS, FAILED, ROLLING_BACK
    }
    
    public static class UpdateProgress {
        private UpdateStatus status;
        private int progress; // 0-100
        private String message;
        private List<String> logs;
        
        public UpdateProgress() {
            this.logs = new ArrayList<>();
        }
        
        public UpdateStatus getStatus() { return status; }
        public void setStatus(UpdateStatus status) { this.status = status; }
        
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public List<String> getLogs() { return logs; }
        public void setLogs(List<String> logs) { this.logs = logs; }
    }
    
    public CompletableFuture<Boolean> performUpdate(String downloadUrl, String newVersion) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (updateLogs) {
                updateLogs.clear();
            }
            currentUpdateStatus.set(UpdateStatus.CHECKING);
            addLog("Начало процесса обновления до версии: " + newVersion);
            
            String backupJarPath = null;
            
            try {
                // 1. Создаем директории
                addLog("Создание директорий для обновления...");
                Files.createDirectories(Paths.get(downloadPath));
                Files.createDirectories(Paths.get(backupPath));
                
                // 2. Создаем backup текущего JAR
                addLog("Создание резервной копии текущей версии...");
                currentUpdateStatus.set(UpdateStatus.BACKING_UP);
                backupJarPath = createBackup();
                addLog("Резервная копия создана: " + backupJarPath);
                
                // 3. Скачиваем новую версию
                addLog("Скачивание новой версии...");
                currentUpdateStatus.set(UpdateStatus.DOWNLOADING);
                String downloadedJarPath = downloadJar(downloadUrl, newVersion);
                addLog("Новая версия скачана: " + downloadedJarPath);
                
                // 4. Устанавливаем новую версию
                addLog("Установка новой версии...");
                currentUpdateStatus.set(UpdateStatus.INSTALLING);
                installJar(downloadedJarPath);
                addLog("Новая версия установлена");
                
                // 5. Обновляем информацию о версии в БД
                updateVersionInDatabase(newVersion, downloadUrl);
                
                currentUpdateStatus.set(UpdateStatus.SUCCESS);
                addLog("Обновление успешно завершено!");
                return true;
                
            } catch (Exception e) {
                log.error("Ошибка при обновлении", e);
                currentUpdateStatus.set(UpdateStatus.FAILED);
                addLog("ОШИБКА: " + e.getMessage());
                
                // Откат изменений
                try {
                    if (backupJarPath != null && Files.exists(Paths.get(backupJarPath))) {
                        addLog("Выполняется откат к предыдущей версии...");
                        currentUpdateStatus.set(UpdateStatus.ROLLING_BACK);
                        rollback(backupJarPath);
                        addLog("Откат выполнен успешно");
                    }
                } catch (Exception rollbackException) {
                    log.error("Ошибка при откате", rollbackException);
                    addLog("КРИТИЧЕСКАЯ ОШИБКА при откате: " + rollbackException.getMessage());
                }
                
                return false;
            } finally {
                if (currentUpdateStatus.get() != UpdateStatus.SUCCESS) {
                    currentUpdateStatus.set(UpdateStatus.IDLE);
                }
            }
        });
    }
    
    private String createBackup() throws IOException {
        Path currentJar = Paths.get(currentJarPath);
        if (!Files.exists(currentJar)) {
            throw new IOException("Текущий JAR файл не найден: " + currentJarPath);
        }
        
        String backupFileName = "doska-backup-" + versionService.getCurrentVersion() + 
                "-" + System.currentTimeMillis() + ".jar";
        Path backupPath = Paths.get(this.backupPath, backupFileName);
        
        Files.copy(currentJar, backupPath, StandardCopyOption.REPLACE_EXISTING);
        return backupPath.toString();
    }
    
    private String downloadJar(String downloadUrl, String version) throws IOException {
        addLog("Скачивание из: " + downloadUrl);
        
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<byte[]> response = restTemplate.exchange(
                downloadUrl, HttpMethod.GET, entity, byte[].class);
        
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IOException("Не удалось скачать файл. HTTP статус: " + response.getStatusCode());
        }
        
        String fileName = "doska-" + version + ".jar";
        Path downloadPath = Paths.get(this.downloadPath, fileName);
        
        Files.write(downloadPath, response.getBody());
        addLog("Файл сохранен: " + downloadPath);
        
        return downloadPath.toString();
    }
    
    private void installJar(String newJarPath) throws IOException {
        Path newJar = Paths.get(newJarPath);
        Path currentJar = Paths.get(currentJarPath);
        
        if (!Files.exists(newJar)) {
            throw new IOException("Скачанный JAR файл не найден: " + newJarPath);
        }
        
        // Копируем новый JAR на место текущего
        Files.copy(newJar, currentJar, StandardCopyOption.REPLACE_EXISTING);
        addLog("JAR файл заменен: " + currentJarPath);
    }
    
    private void rollback(String backupJarPath) throws IOException {
        Path backupJar = Paths.get(backupJarPath);
        Path currentJar = Paths.get(currentJarPath);
        
        if (!Files.exists(backupJar)) {
            throw new IOException("Резервная копия не найдена: " + backupJarPath);
        }
        
        Files.copy(backupJar, currentJar, StandardCopyOption.REPLACE_EXISTING);
        addLog("Откат выполнен: восстановлен JAR из " + backupJarPath);
    }
    
    @Transactional
    private void updateVersionInDatabase(String newVersion, String downloadUrl) {
        // Снимаем флаг текущей версии со всех
        appVersionRepository.findAll().forEach(v -> v.setIsCurrent(false));
        appVersionRepository.saveAll(appVersionRepository.findAll());
        
        // Создаем новую запись
        AppVersion version = new AppVersion();
        version.setVersion(newVersion);
        version.setIsCurrent(true);
        version.setInstalledAt(LocalDateTime.now());
        version.setDownloadUrl(downloadUrl);
        version.setUpdateStatus("SUCCESS");
        appVersionRepository.save(version);
    }
    
    public UpdateProgress getUpdateProgress() {
        UpdateProgress progress = new UpdateProgress();
        progress.setStatus(currentUpdateStatus.get());
        
        switch (currentUpdateStatus.get()) {
            case IDLE:
                progress.setProgress(0);
                progress.setMessage("Готов к обновлению");
                break;
            case CHECKING:
                progress.setProgress(10);
                progress.setMessage("Проверка обновления...");
                break;
            case BACKING_UP:
                progress.setProgress(30);
                progress.setMessage("Создание резервной копии...");
                break;
            case DOWNLOADING:
                progress.setProgress(50);
                progress.setMessage("Скачивание новой версии...");
                break;
            case INSTALLING:
                progress.setProgress(80);
                progress.setMessage("Установка новой версии...");
                break;
            case SUCCESS:
                progress.setProgress(100);
                progress.setMessage("Обновление завершено успешно!");
                break;
            case FAILED:
                progress.setProgress(0);
                progress.setMessage("Ошибка при обновлении");
                break;
            case ROLLING_BACK:
                progress.setProgress(90);
                progress.setMessage("Откат изменений...");
                break;
        }
        
        synchronized (updateLogs) {
            progress.setLogs(new ArrayList<>(updateLogs));
        }
        
        return progress;
    }
    
    private void addLog(String message) {
        String logEntry = LocalDateTime.now() + " - " + message;
        synchronized (updateLogs) {
            updateLogs.add(logEntry);
            // Ограничиваем размер логов
            if (updateLogs.size() > 100) {
                updateLogs.remove(0);
            }
        }
        log.info(message);
    }
    
    public UpdateStatus getCurrentStatus() {
        return currentUpdateStatus.get();
    }
}

