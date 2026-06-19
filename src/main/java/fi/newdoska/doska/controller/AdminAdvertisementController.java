package fi.newdoska.doska.controller;

import fi.newdoska.doska.dto.AdvertisementDto;
import fi.newdoska.doska.entity.Advertisement;
import fi.newdoska.doska.repository.SubcategoryRepository;
import fi.newdoska.doska.service.AdvertisementService;
import fi.newdoska.doska.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/advertisements")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminAdvertisementController {

    private final AdvertisementService advertisementService;
    private final UserService userService;
    private final SubcategoryRepository subcategoryRepository;

    @GetMapping
    public String list(@RequestParam(required = false) String search,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Page<Advertisement> ads = advertisementService.getAdvertisementsForAdmin(search, page, 25);
        model.addAttribute("advertisements", ads);
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        return "admin/advertisements";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Advertisement ad = advertisementService.getAdvertisementById(id)
                .orElseThrow(() -> new RuntimeException("Объявление не найдено"));

        AdvertisementDto dto = new AdvertisementDto();
        dto.setId(ad.getId());
        dto.setTitle(ad.getTitle());
        dto.setDescription(ad.getDescription());
        dto.setCategory(ad.getCategory().name());
        dto.setSubcategoryId(ad.getSubcategory() != null ? ad.getSubcategory().getId() : null);
        dto.setType(ad.getType().name());
        dto.setPrice(ad.getPrice());
        dto.setLocation(ad.getLocation());
        dto.setCity(ad.getCity());
        dto.setStatus(ad.getStatus().name());
        dto.setPremium(ad.isPremium());
        dto.setUrgent(ad.isUrgent());
        dto.setShowPhone(ad.getShowPhone());
        dto.setUserId(ad.getUser().getId());

        model.addAttribute("adDto", dto);
        model.addAttribute("advertisement", ad);
        model.addAttribute("categories", Advertisement.Category.values());
        model.addAttribute("types", Advertisement.AdvertisementType.values());
        model.addAttribute("statuses", Advertisement.Status.values());
        model.addAttribute("users", userService.getAllUsers(org.springframework.data.domain.PageRequest.of(0, 500)).getContent());
        model.addAttribute("subcategories", subcategoryRepository.findAll());
        return "admin/advertisement-form";
    }

    @PostMapping("/{id}")
    public String save(@PathVariable Long id,
                       @ModelAttribute AdvertisementDto adDto,
                       RedirectAttributes redirectAttributes) {
        try {
            advertisementService.adminUpdateAdvertisement(id, adDto);
            redirectAttributes.addFlashAttribute("success", "Объявление сохранено");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/advertisements/" + id + "/edit";
        }
        return "redirect:/admin/advertisements";
    }

    @PostMapping("/{id}/delete")
    public String deleteOne(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            advertisementService.adminDeleteAdvertisement(id);
            redirectAttributes.addFlashAttribute("success", "Объявление удалено");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/advertisements";
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(@RequestParam(required = false) List<Long> ids,
                             RedirectAttributes redirectAttributes) {
        if (ids == null || ids.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Не выбрано ни одного объявления");
            return "redirect:/admin/advertisements";
        }
        int count = advertisementService.adminBulkDeleteAdvertisements(ids);
        redirectAttributes.addFlashAttribute("success", "Удалено объявлений: " + count);
        return "redirect:/admin/advertisements";
    }
}
