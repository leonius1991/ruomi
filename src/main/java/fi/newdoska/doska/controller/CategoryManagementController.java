package fi.newdoska.doska.controller;

import fi.newdoska.doska.entity.AdvertisementTypeEntity;
import fi.newdoska.doska.entity.Category;
import fi.newdoska.doska.entity.Subcategory;
import fi.newdoska.doska.repository.AdvertisementTypeRepository;
import fi.newdoska.doska.repository.CategoryRepository;
import fi.newdoska.doska.repository.SubcategoryRepository;
import fi.newdoska.doska.service.CategoryManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Slf4j
public class CategoryManagementController {
    
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final AdvertisementTypeRepository typeRepository;
    private final CategoryManagementService categoryManagementService;
    
    @GetMapping
    public String manageCategories(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/category-management";
    }
    
    @GetMapping("/category/{id}")
    @ResponseBody
    public Category getCategory(@PathVariable Long id) {
        return categoryRepository.findById(id).orElseThrow();
    }
    
    @GetMapping("/subcategories/{categoryId}")
    @ResponseBody
    public java.util.List<Subcategory> getSubcategories(@PathVariable Long categoryId) {
        return subcategoryRepository.findByParentCategoryId(categoryId);
    }
    
    @GetMapping("/types/{categoryId}")
    @ResponseBody
    public java.util.List<AdvertisementTypeEntity> getTypes(@PathVariable Long categoryId) {
        return typeRepository.findByCategoryIdAndActiveTrueOrderBySortOrderAsc(categoryId);
    }
    
    @GetMapping("/types/global")
    @ResponseBody
    public java.util.List<AdvertisementTypeEntity> getGlobalTypes() {
        return typeRepository.findByCategoryIsNullAndActiveTrueOrderBySortOrderAsc();
    }
    
    @GetMapping("/subcategory/{id}")
    @ResponseBody
    public Subcategory getSubcategory(@PathVariable Long id) {
        return subcategoryRepository.findById(id).orElseThrow();
    }
    
    @GetMapping("/type/{id}")
    @ResponseBody
    public AdvertisementTypeEntity getType(@PathVariable Long id) {
        return typeRepository.findById(id).orElseThrow();
    }
    
    @PostMapping("/category/create")
    public String createCategory(@ModelAttribute Category category, RedirectAttributes redirectAttributes) {
        categoryRepository.save(category);
        redirectAttributes.addFlashAttribute("success", "Категория создана");
        return "redirect:/admin/categories";
    }
    
    @PostMapping("/category/{id}/update")
    public String updateCategory(@PathVariable Long id, @ModelAttribute Category category, RedirectAttributes redirectAttributes) {
        Category existing = categoryRepository.findById(id).orElseThrow();
        existing.setName(category.getName());
        existing.setDisplayName(category.getDisplayName());
        existing.setIcon(category.getIcon());
        existing.setSortOrder(category.getSortOrder());
        existing.setActive(category.getActive());
        categoryRepository.save(existing);
        redirectAttributes.addFlashAttribute("success", "Категория обновлена");
        return "redirect:/admin/categories";
    }
    
    @GetMapping("/category/{id}/delete")
    public String deleteCategoryGet(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error",
                "Удаление выполняется через кнопку на странице категорий (POST-запрос).");
        return "redirect:/admin/categories";
    }

    @PostMapping("/category/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryManagementService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("success", "Категория удалена");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка удаления категории {}", id, e);
            redirectAttributes.addFlashAttribute("error",
                    "Не удалось удалить категорию. Возможно, на неё есть подписки или связанные данные.");
        }
        return "redirect:/admin/categories";
    }
    
    @PostMapping("/subcategory/create")
    public String createSubcategory(@RequestParam Long categoryId, 
                                   @RequestParam(required = false) Long advertisementTypeId,
                                   @ModelAttribute Subcategory subcategory, 
                                   RedirectAttributes redirectAttributes) {
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        subcategory.setParentCategory(category);
        
        if (advertisementTypeId != null) {
            AdvertisementTypeEntity type = typeRepository.findById(advertisementTypeId).orElse(null);
            subcategory.setAdvertisementType(type);
        } else {
            subcategory.setAdvertisementType(null);
        }
        
        subcategoryRepository.save(subcategory);
        redirectAttributes.addFlashAttribute("success", "Подкатегория создана");
        return "redirect:/admin/categories";
    }
    
    @PostMapping("/subcategory/{id}/update")
    public String updateSubcategory(@PathVariable Long id, 
                                   @RequestParam(required = false) Long advertisementTypeId,
                                   @ModelAttribute Subcategory subcategory, 
                                   RedirectAttributes redirectAttributes) {
        Subcategory existing = subcategoryRepository.findById(id).orElseThrow();
        existing.setName(subcategory.getName());
        existing.setDisplayName(subcategory.getDisplayName());
        existing.setIcon(subcategory.getIcon());
        existing.setSortOrder(subcategory.getSortOrder());
        existing.setActive(subcategory.getActive());
        
        if (advertisementTypeId != null) {
            AdvertisementTypeEntity type = typeRepository.findById(advertisementTypeId).orElse(null);
            existing.setAdvertisementType(type);
        } else {
            existing.setAdvertisementType(null);
        }
        
        subcategoryRepository.save(existing);
        redirectAttributes.addFlashAttribute("success", "Подкатегория обновлена");
        return "redirect:/admin/categories";
    }
    
    @PostMapping("/subcategory/{id}/delete")
    public String deleteSubcategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        subcategoryRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Подкатегория удалена");
        return "redirect:/admin/categories";
    }
    
    @PostMapping("/type/create")
    public String createType(@RequestParam(required = false) Long categoryId, @ModelAttribute AdvertisementTypeEntity type, RedirectAttributes redirectAttributes) {
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId).orElse(null);
            type.setCategory(category);
        }
        typeRepository.save(type);
        redirectAttributes.addFlashAttribute("success", "Тип объявления создан");
        return "redirect:/admin/categories";
    }
    
    @PostMapping("/type/{id}/update")
    public String updateType(@PathVariable Long id, @RequestParam(required = false) Long categoryId, @ModelAttribute AdvertisementTypeEntity type, RedirectAttributes redirectAttributes) {
        AdvertisementTypeEntity existing = typeRepository.findById(id).orElseThrow();
        existing.setName(type.getName());
        existing.setDisplayName(type.getDisplayName());
        existing.setSortOrder(type.getSortOrder());
        existing.setActive(type.getActive());
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId).orElse(null);
            existing.setCategory(category);
        } else {
            existing.setCategory(null);
        }
        typeRepository.save(existing);
        redirectAttributes.addFlashAttribute("success", "Тип объявления обновлен");
        return "redirect:/admin/categories";
    }
    
    @PostMapping("/type/{id}/delete")
    public String deleteType(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        typeRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Тип объявления удален");
        return "redirect:/admin/categories";
    }
}

