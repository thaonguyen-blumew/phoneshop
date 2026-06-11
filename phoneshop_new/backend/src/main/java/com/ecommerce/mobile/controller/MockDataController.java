package com.ecommerce.mobile.controller;

import com.ecommerce.mobile.entity.*;
import com.ecommerce.mobile.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@RestController
public class MockDataController {

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ProductVariantRepository productVariantRepository;
    
    @Autowired
    private ProductImageRepository productImageRepository;
    
    @Autowired
    private ReviewRepository reviewRepository;
    
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/api/mock-demo-data")
    @Transactional
    public String mockDemoData() {
        List<String> mockImages = Arrays.asList(
            "https://images.unsplash.com/photo-1695048133142-1a20484d2569?q=80&w=800&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1706015502695-93dfdcf0ed54?q=80&w=800&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1605236453806-6ff36851218e?q=80&w=800&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?q=80&w=800&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1610945415295-d9bbf067e59c?q=80&w=800&auto=format&fit=crop"
        );

        List<String> mockComments = Arrays.asList(
            "Sản phẩm rất tuyệt vời, giao hàng nhanh!",
            "Máy dùng mượt, camera chụp đẹp. Rất đáng tiền.",
            "Shop phục vụ nhiệt tình. Đóng gói cẩn thận.",
            "Tạm ổn trong tầm giá. Sẽ ủng hộ shop lần sau.",
            "Máy thiết kế đẹp, pin trâu, chơi game không bị nóng."
        );

        Random random = new Random();

        // 1. Tìm 1 Customer bất kỳ để dùng làm người review
        Customer customer = (Customer) userRepository.findAll().stream()
                .filter(u -> u instanceof Customer)
                .findFirst().orElse(null);

        if (customer == null) {
            return "Lỗi: Không tìm thấy Customer nào trong hệ thống để mock review. Hãy tạo 1 tài khoản trước.";
        }

        // 2. Lặp qua tất cả sản phẩm
        List<Product> products = productRepository.findAll();
        int imageAdded = 0;
        int reviewAdded = 0;

        for (Product p : products) {
            // Mock Images cho các Variant chưa có ảnh
            if (p.getVariants() != null) {
                for (ProductVariant v : p.getVariants()) {
                    if (v.getImages() == null || v.getImages().isEmpty()) {
                        ProductImage img = new ProductImage();
                        img.setVariant(v);
                        // Chọn random 1 ảnh
                        img.setUrl(mockImages.get(random.nextInt(mockImages.size())));
                        img.setIsPrimary(true);
                        productImageRepository.save(img);
                        imageAdded++;
                    }
                }
            }

            // Mock Review ngẫu nhiên (chỉ mock nếu chưa có review nào)
            long reviewCount = reviewRepository.countByProductProductId(p.getProductId());
            if (reviewCount == 0 && random.nextBoolean()) { 
                // 50% cơ hội sản phẩm này sẽ được nhận review
                Review review = new Review();
                review.setProduct(p);
                review.setCustomer(customer);
                review.setRating(random.nextInt(2) + 4); // Random 4 hoặc 5 sao
                review.setComment(mockComments.get(random.nextInt(mockComments.size())));
                reviewRepository.save(review);
                reviewAdded++;
            }
        }

        return String.format("Thành công! Đã mock %d ảnh cho các phân loại rỗng và %d đánh giá cho các sản phẩm.", imageAdded, reviewAdded);
    }
}
