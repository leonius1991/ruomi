package fi.newdoska.doska.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class ThymeleafConfig {
    
    @Value("${app.resources.external.path:./external-resources}")
    private String externalResourcesPath;
    
    @Value("${app.resources.use-external:false}")
    private boolean useExternalResources;
    
    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        
        if (useExternalResources) {
            // Используем внешние шаблоны
            Path externalPath = Paths.get(externalResourcesPath).toAbsolutePath().normalize();
            resolver.setPrefix("file:" + externalPath.resolve("templates/") + "/");
        } else {
            // Используем шаблоны из classpath
            resolver.setPrefix("classpath:/templates/");
        }
        
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false); // Отключаем кеширование для возможности редактирования на лету
        resolver.setOrder(1);
        
        return resolver;
    }
    
    @Bean
    public SpringResourceTemplateResolver fallbackTemplateResolver() {
        // Fallback на classpath для фрагментов и шаблонов, которые не найдены во внешних ресурсах
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        resolver.setOrder(2); // Более низкий приоритет, чем основной resolver
        resolver.setCheckExistence(true); // Проверять существование файла
        
        return resolver;
    }
    
}

