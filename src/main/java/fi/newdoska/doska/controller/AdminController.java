package fi.newdoska.doska.controller;

import fi.newdoska.doska.entity.BroadcastMessage;
import fi.newdoska.doska.entity.Category;
import fi.newdoska.doska.entity.SeoMetadataEntity;
import fi.newdoska.doska.entity.SiteTheme;
import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.repository.CategoryRepository;
import fi.newdoska.doska.repository.SeoMetadataRepository;
import fi.newdoska.doska.repository.SiteThemeRepository;
import fi.newdoska.doska.service.AdvertisementService;
import fi.newdoska.doska.service.BroadcastMessageService;
import fi.newdoska.doska.service.CategorySubscriptionService;
import fi.newdoska.doska.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminController {

    private final SeoMetadataRepository seoMetadataRepository;
    private final SiteThemeRepository siteThemeRepository;
    private final CategorySubscriptionService categorySubscriptionService;
    private final CategoryRepository categoryRepository;
    private final BroadcastMessageService broadcastMessageService;
    private final UserService userService;
    private final AdvertisementService advertisementService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", userService.getTotalUsersCount());
        model.addAttribute("activeAdvertisements", advertisementService.getTotalApprovedAdvertisementsCount());
        model.addAttribute("pendingAdvertisements", advertisementService.getPendingAdvertisementsCount());
        // Жалобы пока не реализованы, ставим 0
        model.addAttribute("totalReports", 0L);
        return "admin/dashboard";
    }

    @GetMapping("/seo")
    public String seoEditor(Model model) {
        model.addAttribute("metadataList", seoMetadataRepository.findAll());
        return "admin/seo-editor";
    }

    @PostMapping("/seo")
    public String saveSeo(@ModelAttribute SeoMetadataEntity metadata, RedirectAttributes redirectAttributes) {
        seoMetadataRepository.save(metadata);
        redirectAttributes.addFlashAttribute("success", "SEO-метаданные сохранены");
        return "redirect:/admin/seo";
    }

    @GetMapping("/theme")
    public String themeEditor(Model model) {
        SiteTheme theme = siteThemeRepository.findById(1L).orElse(new SiteTheme());
        if (theme.getId() == null) {
            theme.setId(1L);
        }
        model.addAttribute("theme", theme);
        return "admin/theme-editor";
    }

    @PostMapping("/theme")
    public String saveTheme(
            @RequestParam(required = false) String primaryColor,
            @RequestParam(required = false) String secondaryColor,
            @RequestParam(required = false) String successColor,
            @RequestParam(required = false) String dangerColor,
            @RequestParam(required = false) String warningColor,
            @RequestParam(required = false) String infoColor,
            @RequestParam(required = false) String baseFontSize,
            @RequestParam(required = false) String headingFontSize,
            @RequestParam(required = false) String smallFontSize,
            @RequestParam(required = false) String largeFontSize,
            @RequestParam(required = false) String borderRadius,
            @RequestParam(required = false) String boxShadow,
            @RequestParam(required = false) String navbarHeight,
            @RequestParam(required = false) String containerMaxWidth,
            @RequestParam(required = false) String heroGradientStart,
            @RequestParam(required = false) String heroGradientEnd,
            RedirectAttributes redirectAttributes) {
        
        SiteTheme theme = siteThemeRepository.findById(1L).orElse(new SiteTheme());
        theme.setId(1L);
        
        if (primaryColor != null && !primaryColor.isEmpty()) theme.setPrimaryColor(primaryColor);
        if (secondaryColor != null && !secondaryColor.isEmpty()) theme.setSecondaryColor(secondaryColor);
        if (successColor != null && !successColor.isEmpty()) theme.setSuccessColor(successColor);
        if (dangerColor != null && !dangerColor.isEmpty()) theme.setDangerColor(dangerColor);
        if (warningColor != null && !warningColor.isEmpty()) theme.setWarningColor(warningColor);
        if (infoColor != null && !infoColor.isEmpty()) theme.setInfoColor(infoColor);
        if (baseFontSize != null && !baseFontSize.isEmpty()) theme.setBaseFontSize(baseFontSize);
        if (headingFontSize != null && !headingFontSize.isEmpty()) theme.setHeadingFontSize(headingFontSize);
        if (smallFontSize != null && !smallFontSize.isEmpty()) theme.setSmallFontSize(smallFontSize);
        if (largeFontSize != null && !largeFontSize.isEmpty()) theme.setLargeFontSize(largeFontSize);
        if (borderRadius != null && !borderRadius.isEmpty()) theme.setBorderRadius(borderRadius);
        if (boxShadow != null && !boxShadow.isEmpty()) theme.setBoxShadow(boxShadow);
        if (navbarHeight != null && !navbarHeight.isEmpty()) theme.setNavbarHeight(navbarHeight);
        if (containerMaxWidth != null && !containerMaxWidth.isEmpty()) theme.setContainerMaxWidth(containerMaxWidth);
        if (heroGradientStart != null && !heroGradientStart.isEmpty()) theme.setHeroGradientStart(heroGradientStart);
        if (heroGradientEnd != null && !heroGradientEnd.isEmpty()) theme.setHeroGradientEnd(heroGradientEnd);
        
        siteThemeRepository.save(theme);
        redirectAttributes.addFlashAttribute("success", "Тема сохранена");
        return "redirect:/admin/theme";
    }
    
    @GetMapping("/subscriptions")
    public String subscriptionsStats(Model model) {
        long totalSubscriptions = categorySubscriptionService.getTotalSubscriberCount();
        java.util.List<Category> categories = categoryRepository.findByActiveTrueOrderBySortOrderAsc();
        
        java.util.Map<Category, Long> categoryStats = new java.util.HashMap<>();
        for (Category category : categories) {
            categoryStats.put(category, categorySubscriptionService.getCategorySubscriberCount(category));
        }
        
        model.addAttribute("totalSubscriptions", totalSubscriptions);
        model.addAttribute("categoryStats", categoryStats);
        return "admin/subscriptions-stats";
    }
    
    @GetMapping("/broadcasts")
    public String broadcasts(Model model) {
        model.addAttribute("broadcasts", broadcastMessageService.getAllBroadcasts());
        return "admin/broadcasts";
    }
    
    @PostMapping("/broadcasts/create")
    public String createBroadcast(@AuthenticationPrincipal User user,
                                  @RequestParam String content,
                                  RedirectAttributes redirectAttributes) {
        try {
            BroadcastMessage broadcast = broadcastMessageService.createBroadcast(user, content);
            broadcastMessageService.sendBroadcast(broadcast.getId());
            redirectAttributes.addFlashAttribute("success", "Рассылка успешно отправлена!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при отправке рассылки: " + e.getMessage());
        }
        return "redirect:/admin/broadcasts";
    }
}

