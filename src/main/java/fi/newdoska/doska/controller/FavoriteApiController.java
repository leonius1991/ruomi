package fi.newdoska.doska.controller;

import fi.newdoska.doska.entity.Advertisement;
import fi.newdoska.doska.entity.Favorite;
import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.repository.AdvertisementRepository;
import fi.newdoska.doska.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteApiController {
    
    private final FavoriteRepository favoriteRepository;
    private final AdvertisementRepository advertisementRepository;
    
    @PostMapping("/toggle/{advertisementId}")
    public ResponseEntity<Map<String, Object>> toggleFavorite(
            @PathVariable Long advertisementId,
            @AuthenticationPrincipal User user) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (user == null) {
            response.put("success", false);
            response.put("error", "Требуется авторизация");
            return ResponseEntity.status(401).body(response);
        }
        
        Advertisement advertisement = advertisementRepository.findById(advertisementId)
                .orElse(null);
        
        if (advertisement == null) {
            response.put("success", false);
            response.put("error", "Объявление не найдено");
            return ResponseEntity.badRequest().body(response);
        }
        
        Optional<Favorite> favoriteOpt = favoriteRepository.findByUserAndAdvertisement(user, advertisement);
        
        if (favoriteOpt.isPresent()) {
            favoriteRepository.delete(favoriteOpt.get());
            response.put("success", true);
            response.put("isFavorite", false);
        } else {
            Favorite favorite = new Favorite();
            favorite.setUser(user);
            favorite.setAdvertisement(advertisement);
            favoriteRepository.save(favorite);
            response.put("success", true);
            response.put("isFavorite", true);
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/check/{advertisementId}")
    public ResponseEntity<Map<String, Object>> checkFavorite(
            @PathVariable Long advertisementId,
            @AuthenticationPrincipal User user) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (user == null) {
            response.put("isFavorite", false);
            return ResponseEntity.ok(response);
        }
        
        Advertisement advertisement = advertisementRepository.findById(advertisementId)
                .orElse(null);
        
        if (advertisement == null) {
            response.put("isFavorite", false);
            return ResponseEntity.ok(response);
        }
        
        boolean isFavorite = favoriteRepository.existsByUserAndAdvertisement(user, advertisement);
        response.put("isFavorite", isFavorite);
        
        return ResponseEntity.ok(response);
    }
}
