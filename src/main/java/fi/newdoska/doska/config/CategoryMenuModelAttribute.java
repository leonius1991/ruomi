package fi.newdoska.doska.config;

import fi.newdoska.doska.service.CategoryMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
public class CategoryMenuModelAttribute {
    
    private final CategoryMenuService categoryMenuService;
    
    @ModelAttribute("categoryMenus")
    public List<fi.newdoska.doska.dto.CategoryMenuItem> getCategoryMenus() {
        return categoryMenuService.getCategoryMenus();
    }
}


