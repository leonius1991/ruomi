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
import java.util.concurrent.TimeUnit;
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
    
    @Value("${app.update.restart.enabled:true}")
    private boolean restartEnabled;
    
    @Value("${app.update.restart.script:./restart.sh}")
    private String restartScript;
    
    @Value("${app.update.restart.service:ruomi}")
    private String systemdService;
    
    @Value("${app.resources.external.path:./external-resources}")
    private String externalResourcesPath;
    
    @Value("${app.resources.use-external:false}")
    private boolean useExternalResources;
    
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
    
    public CompletableFuture<Boolean> performUpdate(String downloadUrl, String newVersion, String resourcesUrl) {
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
                
                // 5. Скачиваем и обновляем ресурсы (если есть)
                if (resourcesUrl != null && !resourcesUrl.isEmpty()) {
                    addLog("Скачивание и обновление ресурсов...");
                    try {
                        downloadAndExtractResources(resourcesUrl, newVersion);
                        addLog("Ресурсы успешно обновлены");
                    } catch (Exception e) {
                        addLog("ПРЕДУПРЕЖДЕНИЕ: Не удалось обновить ресурсы: " + e.getMessage());
                        // Не прерываем обновление из-за ошибки ресурсов
                    }
                }
                
                // 6. Обновляем информацию о версии в БД
                updateVersionInDatabase(newVersion, downloadUrl);
                
                // 7. Перезапускаем приложение (если включено)
                if (restartEnabled) {
                    addLog("Подготовка к перезапуску приложения...");
                    try {
                        restartApplication();
                        addLog("Команда перезапуска отправлена");
                    } catch (Exception e) {
                        addLog("ПРЕДУПРЕЖДЕНИЕ: Не удалось перезапустить приложение автоматически: " + e.getMessage());
                        addLog("Перезапустите приложение вручную");
                    }
                } else {
                    addLog("Автоматический перезапуск отключен. Перезапустите приложение вручную.");
                }
                
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
    
    private void downloadAndExtractResources(String resourcesUrl, String version) throws IOException {
        addLog("Скачивание ресурсов из: " + resourcesUrl);
        
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<byte[]> response = restTemplate.exchange(
                resourcesUrl, HttpMethod.GET, entity, byte[].class);
        
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IOException("Не удалось скачать ресурсы. HTTP статус: " + response.getStatusCode());
        }
        
        // Сохраняем ZIP файл
        String zipFileName = "resources-" + version + ".zip";
        Path zipPath = Paths.get(this.downloadPath, zipFileName);
        Files.write(zipPath, response.getBody());
        addLog("Ресурсы скачаны: " + zipPath);
        addLog("Размер ZIP файла: " + Files.size(zipPath) + " байт");
        
        // Создаем директорию для внешних ресурсов
        Path externalPath = Paths.get(externalResourcesPath).toAbsolutePath();
        Files.createDirectories(externalPath);
        addLog("Целевая директория: " + externalPath);
        
        // Распаковываем ZIP
        addLog("Распаковка ресурсов в: " + externalPath);
        try (java.util.zip.ZipInputStream zipInputStream = new java.util.zip.ZipInputStream(
                new java.io.FileInputStream(zipPath.toFile()))) {
            
            java.util.zip.ZipEntry entry;
            int filesExtracted = 0;
            int dirsCreated = 0;
            
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String entryName = entry.getName();
                
                // Нормализуем имя записи - убираем лишние слеши и точки
                entryName = entryName.replace("\\", "/"); // Заменяем обратные слеши на прямые
                if (entryName.startsWith("/")) {
                    entryName = entryName.substring(1); // Убираем ведущий слеш
                }
                
                // Пропускаем пустые записи
                if (entryName.isEmpty() || entryName.equals(".") || entryName.equals("..")) {
                    zipInputStream.closeEntry();
                    continue;
                }
                
                // Разрешаем только пути, начинающиеся с templates/ или static/
                if (!entryName.startsWith("templates/") && !entryName.startsWith("static/")) {
                    // Пропускаем записи, которые не в нужных директориях
                    addLog("Пропущена запись вне templates/ или static/: " + entryName);
                    zipInputStream.closeEntry();
                    continue;
                }
                
                // Создаем путь относительно externalPath
                Path entryPath = externalPath.resolve(entryName).normalize();
                
                // Проверка безопасности (защита от Zip Slip) - проверяем что путь внутри externalPath
                Path normalizedExternal = externalPath.normalize();
                if (!entryPath.startsWith(normalizedExternal)) {
                    addLog("ПРЕДУПРЕЖДЕНИЕ: Пропущен небезопасный путь (zip slip): " + entryName);
                    zipInputStream.closeEntry();
                    continue;
                }
                
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                    dirsCreated++;
                    addLog("Создана директория: " + entryPath);
                } else {
                    // Создаем родительские директории если их нет
                    Path parent = entryPath.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    
                    // Копируем файл
                    Files.copy(zipInputStream, entryPath, StandardCopyOption.REPLACE_EXISTING);
                    filesExtracted++;
                    
                    if (filesExtracted <= 10) {
                        addLog("Распакован файл: " + entryPath);
                    }
                }
                zipInputStream.closeEntry();
            }
            
            addLog("Распаковано файлов: " + filesExtracted);
            addLog("Создано директорий: " + dirsCreated);
            
            // Проверяем что файлы действительно распаковались
            if (Files.exists(externalPath.resolve("templates"))) {
                long templateFiles = Files.walk(externalPath.resolve("templates"))
                    .filter(Files::isRegularFile)
                    .count();
                addLog("Файлов в templates: " + templateFiles);
            }
            if (Files.exists(externalPath.resolve("static"))) {
                long staticFiles = Files.walk(externalPath.resolve("static"))
                    .filter(Files::isRegularFile)
                    .count();
                addLog("Файлов в static: " + staticFiles);
            }
        } catch (Exception e) {
            log.error("Ошибка при распаковке ресурсов", e);
            throw new IOException("Ошибка при распаковке ресурсов: " + e.getMessage(), e);
        }
        
        // Удаляем ZIP файл после распаковки
        try {
            Files.deleteIfExists(zipPath);
            addLog("Временный ZIP файл удален");
        } catch (Exception e) {
            addLog("ПРЕДУПРЕЖДЕНИЕ: Не удалось удалить временный ZIP файл: " + e.getMessage());
        }
        
        addLog("Ресурсы успешно распакованы в: " + externalPath);
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
    
    public void restartApplication() throws IOException, InterruptedException {
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("linux")) {
            // Linux: сначала пробуем напрямую через systemctl (если есть права)
            // Затем пробуем через sudo без пароля, затем через скрипт
            boolean restarted = false;
            
            // Попытка 1: Прямой вызов systemctl (если приложение запущено от нужного пользователя)
            if (Files.exists(Paths.get("/etc/systemd/system/" + systemdService + ".service")) ||
                Files.exists(Paths.get("/usr/lib/systemd/system/" + systemdService + ".service"))) {
                try {
                    addLog("Попытка перезапуска через systemctl напрямую: " + systemdService);
                    ProcessBuilder pb = new ProcessBuilder("systemctl", "restart", systemdService);
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    
                    // Читаем вывод
                    StringBuilder output = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            output.append(line).append("\n");
                            addLog("systemctl: " + line);
                        }
                    }
                    
                    boolean finished = process.waitFor(5, TimeUnit.SECONDS);
                    if (finished && process.exitValue() == 0) {
                        addLog("Перезапуск через systemctl выполнен успешно");
                        restarted = true;
                    }
                } catch (Exception e) {
                    addLog("Не удалось перезапустить через systemctl напрямую: " + e.getMessage());
                }
            }
            
            // Попытка 2: Через sudo без пароля (если настроено)
            if (!restarted) {
                try {
                    addLog("Попытка перезапуска через sudo systemctl: " + systemdService);
                    ProcessBuilder pb = new ProcessBuilder("sudo", "-n", "systemctl", "restart", systemdService);
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            addLog("sudo systemctl: " + line);
                        }
                    }
                    
                    boolean finished = process.waitFor(5, TimeUnit.SECONDS);
                    if (finished && process.exitValue() == 0) {
                        addLog("Перезапуск через sudo systemctl выполнен успешно");
                        restarted = true;
                    }
                } catch (Exception e) {
                    addLog("Не удалось перезапустить через sudo systemctl: " + e.getMessage());
                }
            }
            
            // Попытка 3: Через скрипт restart.sh (который сам попробует sudo)
            if (!restarted) {
                File scriptFile = new File(restartScript);
                if (scriptFile.exists() && scriptFile.canExecute()) {
                    addLog("Использование скрипта перезапуска: " + restartScript);
                    ProcessBuilder pb = new ProcessBuilder("bash", restartScript);
                    pb.directory(scriptFile.getParentFile() != null ? scriptFile.getParentFile() : new File("."));
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    
                    // Читаем вывод скрипта
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            addLog("restart: " + line);
                        }
                    }
                    
                    // Ждем завершения скрипта (максимум 10 секунд)
                    boolean finished = process.waitFor(10, TimeUnit.SECONDS);
                    if (!finished) {
                        addLog("ПРЕДУПРЕЖДЕНИЕ: Скрипт перезапуска выполняется дольше ожидаемого");
                        process.destroyForcibly();
                    } else {
                        int exitCode = process.exitValue();
                        if (exitCode == 0) {
                            addLog("Скрипт перезапуска выполнен успешно");
                            restarted = true;
                        } else {
                            addLog("ПРЕДУПРЕЖДЕНИЕ: Скрипт перезапуска завершился с кодом: " + exitCode);
                        }
                    }
                }
            }
            
            if (!restarted) {
                addLog("ПРЕДУПРЕЖДЕНИЕ: Не удалось автоматически перезапустить приложение");
                addLog("Перезапустите вручную: sudo systemctl restart " + systemdService);
            }
        } else if (os.contains("win")) {
            // Windows: используем скрипт или просто выходим
            addLog("Windows: перезапуск через скрипт или вручную");
            if (Files.exists(Paths.get(restartScript.replace(".sh", ".bat")))) {
                String batScript = restartScript.replace(".sh", ".bat");
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", batScript);
                pb.start();
            }
        } else {
            throw new IOException("Автоматический перезапуск не поддерживается для ОС: " + os);
        }
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
    
    public List<String> getUpdateLogs() {
        synchronized (updateLogs) {
            return new ArrayList<>(updateLogs);
        }
    }
}

