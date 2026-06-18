package fi.newdoska.doska.service;

import fi.newdoska.doska.entity.Advertisement;
import fi.newdoska.doska.entity.Category;
import fi.newdoska.doska.entity.Subcategory;
import fi.newdoska.doska.repository.AdvertisementRepository;
import fi.newdoska.doska.repository.CategoryRepository;
import fi.newdoska.doska.repository.CategorySubscriptionRepository;
import fi.newdoska.doska.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryManagementService {

    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final CategorySubscriptionRepository subscriptionRepository;
    private final AdvertisementRepository advertisementRepository;

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));

        long adsInCategory = countAdsByCategoryName(category.getName());
        if (adsInCategory > 0) {
            throw new IllegalStateException(
                    "Нельзя удалить категорию: " + adsInCategory + " объявлений используют её. Деактивируйте категорию вместо удаления.");
        }

        List<Long> subcategoryIds = subcategoryRepository.findByParentCategoryId(id).stream()
                .map(Subcategory::getId)
                .collect(Collectors.toList());
        if (!subcategoryIds.isEmpty()) {
            advertisementRepository.clearSubcategoryReferences(subcategoryIds);
        }

        subscriptionRepository.deleteByCategory(category);
        categoryRepository.delete(category);
        log.info("Категория {} ({}) удалена", id, category.getDisplayName());
    }

    private long countAdsByCategoryName(String categoryName) {
        try {
            Advertisement.Category cat = Advertisement.Category.valueOf(categoryName);
            return advertisementRepository.countByCategory(cat);
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }
}
