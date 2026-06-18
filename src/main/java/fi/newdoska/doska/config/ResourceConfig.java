package fi.newdoska.doska.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class ResourceConfig implements WebMvcConfigurer {
    
    @Value("${app.resources.external.path:./external-resources}")
    private String externalResourcesPath;
    
    @Value("${app.resources.use-external:false}")
    private boolean useExternalResources;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Внешние ресурсы (если включены) имеют приоритет
        if (useExternalResources) {
            Path externalPath = Paths.get(externalResourcesPath).toAbsolutePath().normalize();
            
            // Статические файлы (CSS, JS, изображения)
            registry.addResourceHandler("/css/**", "/js/**", "/images/**")
                    .addResourceLocations(
                            "file:" + externalPath.resolve("static/css/") + "/",
                            "file:" + externalPath.resolve("static/js/") + "/",
                            "file:" + externalPath.resolve("static/images/") + "/",
                            "classpath:/static/css/",
                            "classpath:/static/js/",
                            "classpath:/static/images/"
                    )
                    .setCachePeriod(0); // Отключаем кеширование
            
            // HTML шаблоны (если нужно редактировать на лету)
            // Thymeleaf будет искать сначала во внешней папке, потом в classpath
        }
        
        // Всегда отключаем кеширование для статических ресурсов
    }
    
}

