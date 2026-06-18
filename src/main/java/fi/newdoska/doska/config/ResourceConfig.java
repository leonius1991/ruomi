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
        if (useExternalResources) {
            Path externalPath = Paths.get(externalResourcesPath).toAbsolutePath().normalize();
            String external = "file:" + externalPath.resolve("static").toString().replace("\\", "/") + "/";
            String classpath = "classpath:/static/";

            registry.addResourceHandler("/css/**")
                    .addResourceLocations(external + "css/", classpath + "css/")
                    .setCachePeriod(0);
            registry.addResourceHandler("/js/**")
                    .addResourceLocations(external + "js/", classpath + "js/")
                    .setCachePeriod(0);
            registry.addResourceHandler("/images/**")
                    .addResourceLocations(external + "images/", classpath + "images/")
                    .setCachePeriod(0);
            registry.addResourceHandler("/data/**")
                    .addResourceLocations(external + "data/", classpath + "data/")
                    .setCachePeriod(0);
        }
    }
    
}

