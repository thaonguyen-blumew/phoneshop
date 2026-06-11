package com.ecommerce.mobile.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.mobile.entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug); // hàm tìm bởi seo?

    List<Category> findByIsActiveTrue();// hàm tìm category đang hoạt đông?

    Category findByName(String name);


    List<Category> findByParentIsNull();
} 