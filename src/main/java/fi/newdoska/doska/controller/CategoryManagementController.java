package fi.newdoska.doska.controller;

import fi.newdoska.doska.entity.AdvertisementTypeEntity;
import fi.newdoska.doska.entity.Category;
import fi.newdoska.doska.entity.Subcategory;
import fi.newdoska.doska.repository.AdvertisementTypeRepository;
import fi.newdoska.doska.repository.CategoryRepository;
import fi.newdoska.doska.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class CategoryManagementController {
    
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final AdvertisementTypeRepository typeRepository;
    
    @GetMapping
    public String manageCategories(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/category-management";
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
    
    @PostMapping("/category/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        categoryRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Категория удалена");
        return "redirect:/admin/categories";
    }
    
    @PostMapping("/subcategory/create")
    public String createSubcategory(@RequestParam Long categoryId, @ModelAttribute Subcategory subcategory, RedirectAttributes redirectAttributes) {
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        subcategory.setParentCategory(category);
        subcategoryRepository.save(subcategory);
        redirectAttributes.addFlashAttribute("success", "Подкатегория создана");
        return "redirect:/admin/categories";
    }
    
    @PostMapping("/subcategory/{id}/update")
    public String updateSubcategory(@PathVariable Long id, @ModelAttribute Subcategory subcategory, RedirectAttributes redirectAttributes) {
        Subcategory existing = subcategoryRepository.findById(id).orElseThrow();
        existing.setName(subcategory.getName());
        existing.setDisplayName(subcategory.getDisplayName());
        existing.setSortOrder(subcategory.getSortOrder());
        existing.setActive(subcategory.getActive());
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

