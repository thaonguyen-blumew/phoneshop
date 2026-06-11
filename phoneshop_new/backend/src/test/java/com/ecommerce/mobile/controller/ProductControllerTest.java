package com.ecommerce.mobile.controller;

import com.ecommerce.mobile.dto.response.ProductDto;
import com.ecommerce.mobile.entity.Product;
import com.ecommerce.mobile.service.ProductService;
import com.ecommerce.mobile.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ProductController productController;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController).build();
    }

    @Test
    public void testListProducts_Success() throws Exception {
        // Mock data
        Product p1 = new Product();
        p1.setProductId(1L);
        p1.setName("iPhone 15");

        Page<Product> page = new PageImpl<>(Arrays.asList(p1));
        
        // When service is called, return mock data
        when(productService.getActiveProducts(null, 0, 8)).thenReturn(page);

        // Perform GET request
        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Lấy danh sách sản phẩm thành công"))
                .andExpect(jsonPath("$.data.content[0].name").value("iPhone 15"));
    }

    @Test
    public void testProductDetail_NotFound() throws Exception {
        // When service is called with ID 999, return null
        when(productService.getActiveProductDetailById(999L)).thenReturn(null);

        // Perform GET request
        mockMvc.perform(get("/api/products/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // Our ApiResponse still returns 200 HTTP status, but custom status inside JSON is 404
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Không tìm thấy sản phẩm"))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
