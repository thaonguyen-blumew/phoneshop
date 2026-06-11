package com.ecommerce.mobile.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*; //phân trang thư viện
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ecommerce.mobile.entity.Category;
import com.ecommerce.mobile.entity.Product;
import com.ecommerce.mobile.enums.ProductStatus;
import com.ecommerce.mobile.repository.CategoryRepository;
import com.ecommerce.mobile.repository.ProductRepository;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    /// claude dạy?

    // searchProducts(),
    public Page<Product> searchProducts(String keyword, int page, int size, Long categoryId) {
        // PageRequest.of(page, size): tạo Pageable từ số trang và kích thước
        Pageable pageable = PageRequest.of(page, size); // ko cần code phân trang
        return productRepository.searchByStatusAndKeyword(ProductStatus.ACTIVE, keyword, pageable); // k hề phưucs tạp,
                                                                                                    // tìm rồi kết quả
                                                                                                    // là hiển thị, ok?
                                                                                                    // repo truy vấn hộ
                                                                                                    // rồi.
        // service chỉ cần phân tragn để hiện sản phẩm hộ repo?
    }

    // toi vẫn thấy nó giống ma thuật.
    // dù tôi hiểu pageable nó làm hết tất cả cv phân trang, còn repo tập trung tìm
    // sản phẩm.
    // Tóm lại mình k code thuật toán mà dùng thuật toán của CSDL? và các từ ngữ để
    // két nối với csdl ?
    // findById(),
    public Optional<Product> findById(Long productId) {
        return productRepository.findById(productId);
    }

    // getAllCategories()
    public List<Category> getAllCategories() {
        return categoryRepository.findByIsActiveTrue();
    }

    // lấy sản phẩm status ACTIVE, phân trang đàng hoàng
    public Page<Product> getActiveProducts(String keyword, int page, int size) {

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")); // tạo lệnh phân trang, tham số trang CHẮC CHẮN KHÔNG ÂM ,
                                                            // tham số size: số phần tử trong 1 trang, tham số lọc trang
                                                            // theo createdAt.
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        // DÒNG NÀY CÓ NGHĨA LÀ IF (KEYWORD == NULL){ cái này có nghĩa là chuẩn hóa từ
        // khóa, nếu nó null thì return "" còn k null thì trim nó.
        /// RETURN " "
        /// ELSE{
        /// KEYWORD.TRIM() (CẮT TỈA CÁI GÌ ĐÓ)}}
        // chuẩn hóa từ khóa. condition?A:B
        /// if (...) {
        // return A;}
        // return B;
        return normalizedKeyword.isEmpty()
                ? productRepository.findByStatus(ProductStatus.ACTIVE, pageable)
                : productRepository.searchByStatusAndKeyword(
                        ProductStatus.ACTIVE,
                        normalizedKeyword,
                        pageable);
        /// DÒNG NÀY LÀ RETURN IF NORMALIZEDKEYWORD.ISEMPTY() RETURN
        /// PRODUCTREPOSITORY.findByStatus(..., PAGEABLE) else
        /// productRepository.searchByStatusandkeyword()
        /// có nghĩa là khi k có từ khóa thì hiển thị toàn bộ ản phẩm active, nếu có thì
        /// tìm kiếm nó.
    }

    public Page<Product> getActiveProducts(String keyword,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sort,
            int page,
            int size) {
        Sort sortBy;
        if ("price_asc".equals(sort)) {
            sortBy = Sort.by(Sort.Direction.ASC, "minPrice");
        } else if ("price_desc".equals(sort)) {
            sortBy = Sort.by(Sort.Direction.DESC, "minPrice");
        } else {
            sortBy = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                sortBy);
        return productRepository.searchActiveProducts(
                ProductStatus.ACTIVE,
                normalize(keyword),
                normalize(brand),
                minPrice,
                maxPrice,
                pageable);
    }

    // HÀM LẤY SẢN PHẨM BẰNG ID , THÊM CẢ FILTER STATUS ACTIVE.
    public Product getActiveProductById(Long id) {
        return productRepository.findDetailedByProductId(id)
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Product getActiveProductDetailById(Long id) {
        return productRepository.findDetailedByProductId(id)
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .orElse(null);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
