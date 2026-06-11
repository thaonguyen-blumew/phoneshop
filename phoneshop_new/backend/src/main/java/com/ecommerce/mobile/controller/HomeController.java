package com.ecommerce.mobile.controller;

import com.ecommerce.mobile.entity.Product;
import com.ecommerce.mobile.dto.response.ProductDto;
import com.ecommerce.mobile.service.ProductService;
import com.ecommerce.mobile.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private ProductService productService;

    @GetMapping("/")
    public String home(Model model) {
        List<ProductDto> products = productService.getActiveProducts(null, 0, 8)
                .map(ProductMapper::toDto)
                .getContent();
        
        List<ProductDto> featuredProducts = products.size() > 4 ? products.subList(0, 4) : products;
        List<ProductDto> moreProducts = products.size() > 4 ? products.subList(4, products.size()) : List.of();
        
        model.addAttribute("featuredProducts", featuredProducts);
        model.addAttribute("moreProducts", moreProducts);
        
        return "index";
    }
}
