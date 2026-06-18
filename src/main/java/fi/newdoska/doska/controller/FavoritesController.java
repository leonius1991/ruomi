package fi.newdoska.doska.controller;

import fi.newdoska.doska.entity.Favorite;
import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoritesController {
    
    private final FavoriteRepository favoriteRepository;
    
    @GetMapping
    public String showFavorites(@AuthenticationPrincipal User user,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               Model model) {
        if (user == null) {
            return "redirect:/login";
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Favorite> favoritesPage = favoriteRepository.findByUserOrderByAddedAtDesc(user, pageable);
        
        List<fi.newdoska.doska.entity.Advertisement> advertisements = favoritesPage.getContent().stream()
                .map(Favorite::getAdvertisement)
                .collect(Collectors.toList());
        
        model.addAttribute("advertisements", advertisements);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", favoritesPage.getTotalPages());
        model.addAttribute("totalElements", favoritesPage.getTotalElements());
        
        return "favorites";
    }
}
