package fi.newdoska.doska.service;

import fi.newdoska.doska.entity.Banner;
import fi.newdoska.doska.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerService {
    
    private final BannerRepository bannerRepository;
    
    public List<Banner> getActiveBannersByPosition(String position) {
        LocalDateTime now = LocalDateTime.now();
        return bannerRepository.findByPositionAndActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                position, now, now);
    }
    
    public void incrementBannerViews(Long bannerId) {
        bannerRepository.findById(bannerId).ifPresent(banner -> {
            banner.setViews(banner.getViews() + 1);
            bannerRepository.save(banner);
        });
    }
    
    public void incrementBannerClicks(Long bannerId) {
        bannerRepository.findById(bannerId).ifPresent(banner -> {
            banner.setClicks(banner.getClicks() + 1);
            bannerRepository.save(banner);
        });
    }
}

