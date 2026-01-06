package fi.newdoska.doska.repository;

import fi.newdoska.doska.entity.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {
    List<Subcategory> findByParentCategoryIdAndActiveTrueOrderBySortOrderAsc(Long categoryId);
    List<Subcategory> findByParentCategoryId(Long categoryId);
}


