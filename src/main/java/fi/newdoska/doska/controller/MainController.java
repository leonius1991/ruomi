package fi.newdoska.doska.controller;

import fi.newdoska.doska.entity.Advertisement;
import fi.newdoska.doska.dto.SeoMetadata;
import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {
    
    private final AdvertisementService advertisementService;
    private final UserService userService;
    private final CategoryMenuService categoryMenuService;
    private final SeoMetadataService seoMetadataService;
    private final SearchAnalyticsService searchAnalyticsService;
    private final BannerService bannerService;
    
    @GetMapping("/")
    public String home(Model model) {
        // Получаем премиум и срочные объявления для главной страницы
        List<Advertisement> premiumAds = advertisementService.getPremiumAdvertisements();
        List<Advertisement> urgentAds = advertisementService.getUrgentAdvertisements();
        
        // Получаем последние объявления
        Page<Advertisement> latestAds = advertisementService.getApprovedAdvertisements(0, 12);
        
        model.addAttribute("premiumAdvertisements", premiumAds);
        model.addAttribute("urgentAdvertisements", urgentAds);
        model.addAttribute("latestAdvertisements", latestAds.getContent());
        model.addAttribute("totalAdvertisements", advertisementService.getTotalApprovedAdvertisementsCount());
        model.addAttribute("totalUsers", userService.getTotalUsersCount());
        model.addAttribute("categoryMenus", categoryMenuService.getCategoryMenus());
        model.addAttribute("topBanners", bannerService.getActiveBannersByPosition("TOP"));
        model.addAttribute("sidebarBanners", bannerService.getActiveBannersByPosition("SIDEBAR"));
        applySeoMeta(model, seoMetadataService.getHomePageMeta());
        
        return "index";
    }
    
    @GetMapping("/advertisements")
    public String advertisements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Model model) {
        
        Page<Advertisement> advertisements;
        
        if (search != null && !search.trim().isEmpty()) {
            advertisements = advertisementService.searchAdvertisements(search.trim(), page, size);
        } else if (category != null && city != null) {
            advertisements = advertisementService.getAdvertisementsByCityAndCategory(city, category, page, size);
        } else if (category != null && type != null) {
            advertisements = advertisementService.getAdvertisementsByCategoryAndType(category, type, page, size);
        } else if (category != null) {
            advertisements = advertisementService.getAdvertisementsByCategory(category, page, size);
        } else if (city != null) {
            advertisements = advertisementService.getAdvertisementsByCity(city, page, size);
        } else if (minPrice != null && maxPrice != null) {
            advertisements = advertisementService.getAdvertisementsByPriceRange(minPrice, maxPrice, page, size);
        } else {
            advertisements = advertisementService.getApprovedAdvertisements(page, size);
        }
        
        model.addAttribute("advertisements", advertisements);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", advertisements.getTotalPages());
        model.addAttribute("totalElements", advertisements.getTotalElements());
        model.addAttribute("category", category);
        model.addAttribute("city", city);
        model.addAttribute("search", search);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        
        // Добавляем все категории для фильтра
        model.addAttribute("categories", Advertisement.Category.values());
        model.addAttribute("categoryMenus", categoryMenuService.getCategoryMenus());
        model.addAttribute("sidebarBanners", bannerService.getActiveBannersByPosition("SIDEBAR"));

        if (category != null) {
            applySeoMeta(model, seoMetadataService.getCategoryMeta(category));
        } else if (search != null && !search.isBlank()) {
            applySeoMeta(model, seoMetadataService.getSearchMeta(search));
        } else {
            applySeoMeta(model, seoMetadataService.getHomePageMeta());
        }

        if (search != null && !search.isBlank()) {
            searchAnalyticsService.logSearch(
                    search,
                    category,
                    city,
                    advertisements.getTotalElements(),
                    getCurrentUsername()
            );
        }
        
        return "advertisements";
    }
    
    @GetMapping("/advertisement/{id}")
    public String advertisementDetail(@PathVariable Long id, Model model) {
        advertisementService.getAdvertisementById(id).ifPresent(advertisement -> {
            // Увеличиваем счетчик просмотров
            advertisementService.incrementViews(id);
            
            User user = advertisement.getUser();
            // Загружаем счетчик объявлений пользователя
            if (user.getAdvertisementsCount() == null) {
                long count = advertisementService.getUserAdvertisements(user.getUsername(), 0, 1000).getTotalElements();
                user.setAdvertisementsCount((int) count);
            }
            
            model.addAttribute("advertisement", advertisement);
            model.addAttribute("user", user);
            model.addAttribute("sidebarBanners", bannerService.getActiveBannersByPosition("SIDEBAR"));
        });
        
        return "advertisement-detail";
    }
    
    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("totalAdvertisements", advertisementService.getTotalApprovedAdvertisementsCount());
        model.addAttribute("totalUsers", userService.getTotalUsersCount());
        return "about";
    }
    
    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }
    
    @GetMapping("/terms")
    public String terms() {
        return "terms";
    }
    
    @GetMapping("/privacy")
    public String privacy() {
        return "privacy";
    }
    
    @GetMapping("/help")
    public String help() {
        return "help";
    }
    
    @GetMapping("/my-advertisements")
    public String myAdvertisements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        String username = getCurrentUsername();
        if (username == null) {
            return "redirect:/login";
        }
        
        Page<Advertisement> advertisements = advertisementService.getUserAdvertisements(username, page, size);
        model.addAttribute("advertisements", advertisements);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", advertisements.getTotalPages());
        model.addAttribute("totalElements", advertisements.getTotalElements());
        model.addAttribute("categoryMenus", categoryMenuService.getCategoryMenus());
        
        return "my-advertisements";
    }
    private void applySeoMeta(Model model, SeoMetadata meta) {
        model.addAttribute("pageTitle", meta.title());
        model.addAttribute("metaDescription", meta.description());
        model.addAttribute("metaKeywords", meta.keywords());
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
}