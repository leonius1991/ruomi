package fi.newdoska.doska.controller;

import fi.newdoska.doska.entity.Banner;
import fi.newdoska.doska.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin/banners")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class BannerController {
    
    private final BannerRepository bannerRepository;
    
    @GetMapping
    public String listBanners(Model model) {
        model.addAttribute("banners", bannerRepository.findAll());
        return "admin/banners";
    }
    
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("banner", new Banner());
        return "admin/banner-form";
    }
    
    @PostMapping("/create")
    public String createBanner(
            @ModelAttribute Banner banner,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            RedirectAttributes redirectAttributes) {
        banner.setCreatedAt(LocalDateTime.now());
        if (banner.getStartDate() == null) {
            banner.setStartDate(LocalDateTime.now());
        }
        if (startDate != null && !startDate.isEmpty()) {
            try {
                banner.setStartDate(LocalDateTime.parse(startDate.replace("T", "T")));
            } catch (Exception e) {
                banner.setStartDate(LocalDateTime.now());
            }
        }
        if (endDate != null && !endDate.isEmpty()) {
            try {
                banner.setEndDate(LocalDateTime.parse(endDate.replace("T", "T")));
            } catch (Exception e) {
                // Ignore
            }
        }
        bannerRepository.save(banner);
        redirectAttributes.addFlashAttribute("success", "Баннер создан");
        return "redirect:/admin/banners";
    }
    
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Баннер не найден"));
        model.addAttribute("banner", banner);
        return "admin/banner-form";
    }
    
    @PostMapping("/{id}/edit")
    public String updateBanner(@PathVariable Long id, @ModelAttribute Banner banner, RedirectAttributes redirectAttributes) {
        Banner existingBanner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Баннер не найден"));
        existingBanner.setTitle(banner.getTitle());
        existingBanner.setDescription(banner.getDescription());
        existingBanner.setImageUrl(banner.getImageUrl());
        existingBanner.setLinkUrl(banner.getLinkUrl());
        existingBanner.setPosition(banner.getPosition());
        existingBanner.setActive(banner.isActive());
        existingBanner.setStartDate(banner.getStartDate());
        existingBanner.setEndDate(banner.getEndDate());
        bannerRepository.save(existingBanner);
        redirectAttributes.addFlashAttribute("success", "Баннер обновлен");
        return "redirect:/admin/banners";
    }
    
    @PostMapping("/{id}/delete")
    public String deleteBanner(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        bannerRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Баннер удален");
        return "redirect:/admin/banners";
    }
}

