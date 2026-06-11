package com.ecommerce.mobile.seeder;

import com.ecommerce.mobile.entity.*;
import com.ecommerce.mobile.enums.ProductStatus;
import com.ecommerce.mobile.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@ConditionalOnProperty(name = "phoneshop.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductVariantRepository productVariantRepository;
    @Autowired
    private ProductImageRepository productImageRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (roleRepository.count() == 0) {
            System.out.println("Bắt đầu tự động thêm dữ liệu mẫu (Seeding)...");
            seedRolesAndUsers();
            seedCategoriesAndProducts();
            System.out.println("Đã thêm dữ liệu mẫu thành công!");
        }
    }

    private void seedRolesAndUsers() {
        Role adminRole = new Role();
        adminRole.setNameRole("ADMIN");
        adminRole = roleRepository.save(adminRole);

        Role customerRole = new Role();
        customerRole.setNameRole("CUSTOMER");
        customerRole = roleRepository.save(customerRole);

        Manager admin = new Manager();
        admin.setEmail("admin@phoneshop.com");
        admin.setHashPassword(passwordEncoder.encode("123456"));
        admin.setFullName("Quản Trị Viên");
        admin.setPhone("0987654321");
        admin.setIsActive(true);
        admin.setRole(adminRole);
        userRepository.save(admin);

        Customer customer = new Customer();
        customer.setEmail("customer@gmail.com");
        customer.setHashPassword(passwordEncoder.encode("123456"));
        customer.setFullName("Khách Hàng Mẫu");
        customer.setPhone("0123456789");
        customer.setIsActive(true);
        customer.setRole(customerRole);
        userRepository.save(customer);
    }

    private void seedCategoriesAndProducts() {
        Category appleCat = new Category();
        appleCat.setName("Apple");
        appleCat = categoryRepository.save(appleCat);

        Category samsungCat = new Category();
        samsungCat.setName("Samsung");
        samsungCat = categoryRepository.save(samsungCat);

        // Product 1: iPhone 15 Pro Max
        Product p1 = new Product();
        p1.setName("iPhone 15 Pro Max");
        p1.setBrand("Apple");
        p1.setDescription("Siêu phẩm công nghệ mới nhất từ Apple với khung Titanium.");
        p1.setStatus(ProductStatus.ACTIVE);
        p1.setCategory(appleCat);
        p1 = productRepository.save(p1);

        ProductVariant v1 = new ProductVariant();
        v1.setProduct(p1);
        v1.setStorageGb(256);
        v1.setPrice(new BigDecimal("29990000"));
        v1.setImportPrice(new BigDecimal("25000000"));
        v1.setStockQty(50);
        v1.setSku("IP15PM-256");
        v1 = productVariantRepository.save(v1);

        ProductImage i1 = new ProductImage();
        i1.setVariant(v1);
        i1.setUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/c/c4/IPhone_15_Pro_Max_Natural_Titanium.svg/800px-IPhone_15_Pro_Max_Natural_Titanium.svg.png");
        productImageRepository.save(i1);

        // Product 2: Samsung Galaxy S24 Ultra
        Product p2 = new Product();
        p2.setName("Samsung Galaxy S24 Ultra");
        p2.setBrand("Samsung");
        p2.setDescription("Trải nghiệm AI đỉnh cao trên dòng điện thoại flagship của Samsung.");
        p2.setStatus(ProductStatus.ACTIVE);
        p2.setCategory(samsungCat);
        p2 = productRepository.save(p2);

        ProductVariant v2 = new ProductVariant();
        v2.setProduct(p2);
        v2.setStorageGb(256);
        v2.setPrice(new BigDecimal("27990000"));
        v2.setImportPrice(new BigDecimal("24000000"));
        v2.setStockQty(30);
        v2.setSku("S24U-256");
        v2 = productVariantRepository.save(v2);

        ProductImage i2 = new ProductImage();
        i2.setVariant(v2);
        i2.setUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/8/86/Samsung_Galaxy_S24_Ultra.jpg/800px-Samsung_Galaxy_S24_Ultra.jpg");
        productImageRepository.save(i2);
    }
}
