package fi.newdoska.doska.controller;

import fi.newdoska.doska.dto.AdvertisementDto;
import fi.newdoska.doska.entity.Advertisement;
import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.service.AdvertisementService;
import fi.newdoska.doska.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class AdvertisementController {
    
    private final AdvertisementService advertisementService;
    private final UserService userService;
    private final fi.newdoska.doska.repository.CategoryRepository categoryRepository;
    private final fi.newdoska.doska.repository.SubcategoryRepository subcategoryRepository;
    
    @GetMapping("/create-advertisement")
    public String showCreateForm(Model model) {
        model.addAttribute("advertisementDto", new AdvertisementDto());
        // Загружаем категории из БД
        model.addAttribute("categories", categoryRepository.findByActiveTrueOrderBySortOrderAsc());
        model.addAttribute("allTypes", Advertisement.AdvertisementType.values());
        return "create-advertisement";
    }
    
    @PostMapping("/create-advertisement")
    public String createAdvertisement(
            @ModelAttribute AdvertisementDto dto,
            @RequestParam(required = false) Integer premiumDays,
            @RequestParam(required = false) List<org.springframework.web.multipart.MultipartFile> images,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        // Валидация
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            result.rejectValue("title", "error.title", "Заголовок обязателен");
        }
        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
            result.rejectValue("description", "error.description", "Описание обязательно");
        }
        if (dto.getCategory() == null || dto.getCategory().trim().isEmpty()) {
            result.rejectValue("category", "error.category", "Категория обязательна");
        }
        
        if (result.hasErrors()) {
            model.addAttribute("categories", Advertisement.Category.values());
            model.addAttribute("types", Advertisement.AdvertisementType.values());
            return "create-advertisement";
        }
        
        try {
            String username = getCurrentUsername();
            if (premiumDays != null && premiumDays > 0) {
                dto.setPremium(true);
            }
            if (images != null && !images.isEmpty()) {
                dto.setImages(images);
            }
            advertisementService.createAdvertisement(dto, username, premiumDays);
            redirectAttributes.addFlashAttribute("success", "Объявление успешно создано и отправлено на модерацию!");
            return "redirect:/my-advertisements";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка при создании объявления: " + e.getMessage());
            model.addAttribute("categories", Advertisement.Category.values());
            model.addAttribute("types", Advertisement.AdvertisementType.values());
            return "create-advertisement";
        }
    }
    
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user.getUsername();
        }
        return authentication.getName();
    }
    
    @GetMapping("/edit-advertisement/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        // Проверяем авторизацию
        String username = getCurrentUsername();
        if (username == null) {
            return "redirect:/login";
        }
        
        Advertisement advertisement = advertisementService.getAdvertisementById(id)
                .orElseThrow(() -> new RuntimeException("Объявление не найдено"));
        
        User user;
        try {
            user = (User) userService.loadUserByUsername(username);
        } catch (Exception e) {
            return "redirect:/login";
        }
        
        // Проверяем права на редактирование
        if (!advertisement.getUser().getId().equals(user.getId()) && 
            !hasModeratorPrivileges(user)) {
            return "redirect:/advertisements";
        }
        
        AdvertisementDto dto = new AdvertisementDto();
        dto.setId(advertisement.getId());
        dto.setTitle(advertisement.getTitle());
        dto.setDescription(advertisement.getDescription());
        dto.setCategory(advertisement.getCategory().name());
        dto.setType(advertisement.getType().name());
        dto.setPrice(advertisement.getPrice());
        dto.setLocation(advertisement.getLocation());
        dto.setCity(advertisement.getCity());
        dto.setPremium(advertisement.isPremium());
        dto.setUrgent(advertisement.isUrgent());
        dto.setShowPhone(advertisement.getShowPhone());
        if (advertisement.getSubcategory() != null) {
            dto.setSubcategoryId(advertisement.getSubcategory().getId());
        }
        
        model.addAttribute("advertisementDto", dto);
        // Загружаем категории из БД
        try {
            if (categoryRepository != null) {
                model.addAttribute("categories", categoryRepository.findByActiveTrueOrderBySortOrderAsc());
            } else {
                // Fallback на enum категории
                model.addAttribute("categories", java.util.Arrays.asList(Advertisement.Category.values()));
            }
        } catch (Exception e) {
            // Fallback на enum категории при ошибке
            model.addAttribute("categories", java.util.Arrays.asList(Advertisement.Category.values()));
        }
        model.addAttribute("types", Advertisement.AdvertisementType.values());
        if (advertisement.getImages() != null) {
            model.addAttribute("images", advertisement.getImages());
        } else {
            model.addAttribute("images", java.util.Collections.emptyList());
        }
        return "edit-advertisement";
    }
    
    @PostMapping("/edit-advertisement/{id}")
    public String updateAdvertisement(
            @PathVariable Long id,
            @Valid @ModelAttribute AdvertisementDto dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            model.addAttribute("categories", Advertisement.Category.values());
            model.addAttribute("types", Advertisement.AdvertisementType.values());
            return "edit-advertisement";
        }
        
        try {
            String username = getCurrentUsername();
            advertisementService.updateAdvertisement(id, dto, username);
            redirectAttributes.addFlashAttribute("success", "Объявление успешно обновлено!");
            return "redirect:/my-advertisements";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка при обновлении объявления: " + e.getMessage());
            model.addAttribute("categories", Advertisement.Category.values());
            model.addAttribute("types", Advertisement.AdvertisementType.values());
            return "edit-advertisement";
        }
    }
    
    private boolean hasModeratorPrivileges(User user) {
        return user.getRole() == User.UserRole.MODERATOR || 
               user.getRole() == User.UserRole.ADMIN || 
               user.getRole() == User.UserRole.SUPER_ADMIN;
    }
}

