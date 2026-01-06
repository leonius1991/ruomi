package fi.newdoska.doska.controller;

import fi.newdoska.doska.entity.PremiumPricing;
import fi.newdoska.doska.repository.PremiumPricingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/premium-pricing")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class PremiumPricingController {
    
    private final PremiumPricingRepository pricingRepository;
    
    @GetMapping
    public String showPricing(Model model) {
        PremiumPricing pricing = pricingRepository.findById(1L).orElse(new PremiumPricing());
        model.addAttribute("pricing", pricing);
        return "admin/premium-pricing";
    }
    
    @PostMapping
    public String savePricing(@ModelAttribute PremiumPricing pricing, RedirectAttributes redirectAttributes) {
        pricing.setId(1L);
        pricingRepository.save(pricing);
        redirectAttributes.addFlashAttribute("success", "Цены обновлены");
        return "redirect:/admin/premium-pricing";
    }
}

