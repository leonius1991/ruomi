package fi.newdoska.doska.service;

import fi.newdoska.doska.dto.CategoryMenuItem;
import fi.newdoska.doska.dto.CategorySubItem;
import fi.newdoska.doska.entity.Category;
import fi.newdoska.doska.entity.Subcategory;
import fi.newdoska.doska.entity.AdvertisementTypeEntity;
import fi.newdoska.doska.repository.CategoryRepository;
import fi.newdoska.doska.repository.SubcategoryRepository;
import fi.newdoska.doska.repository.AdvertisementTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryMenuService {

    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final AdvertisementTypeRepository advertisementTypeRepository;

    @Transactional(readOnly = true)
    public List<CategoryMenuItem> getCategoryMenus() {
        List<Category> categories = categoryRepository.findByActiveTrueOrderBySortOrderAsc();
        
        return categories.stream()
                .map(category -> {
                    // Получаем подкатегории
                    List<Subcategory> subcategories = subcategoryRepository
                            .findByParentCategoryIdAndActiveTrueOrderBySortOrderAsc(category.getId());
                    
                    // Получаем типы объявлений для категории (если есть)
                    List<AdvertisementTypeEntity> types = advertisementTypeRepository
                            .findByCategoryIdAndActiveTrueOrderBySortOrderAsc(category.getId());
                    
                    // Формируем список подкатегорий
                    List<CategorySubItem> subItems = subcategories.stream()
                            .map(sub -> new CategorySubItem(
                                    sub.getDisplayName(),
                                    "/advertisements?category=" + category.getName() + "&subcategory=" + sub.getName()
                            ))
                            .collect(Collectors.toList());
                    
                    // Добавляем типы объявлений (для категории Работа и других, где есть типы)
                    if (!types.isEmpty()) {
                        types.forEach(type -> {
                            subItems.add(new CategorySubItem(
                                    type.getDisplayName(),
                                    "/advertisements?category=" + category.getName() + "&type=" + type.getName()
                            ));
                        });
                    }
                    
                    // Если нет подкатегорий и типов, добавляем общую ссылку на категорию
                    if (subItems.isEmpty()) {
                        subItems.add(new CategorySubItem(
                                "Все объявления",
                                "/advertisements?category=" + category.getName()
                        ));
                    }
                    
                    return new CategoryMenuItem(
                            category.getDisplayName(),
                            category.getIcon() != null ? category.getIcon() : "fa-box",
                            category.getName(),
                            subItems
                    );
                })
                .collect(Collectors.toList());
    }
}

