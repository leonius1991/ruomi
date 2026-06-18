package fi.newdoska.doska.controller;

import fi.newdoska.doska.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryApiController {
    
    private final CategoryRepository categoryRepository;
    
    @GetMapping
    public java.util.List<fi.newdoska.doska.entity.Category> getAllCategories() {
        return categoryRepository.findByActiveTrueOrderBySortOrderAsc();
    }
}


