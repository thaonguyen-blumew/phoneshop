package com.ecommerce.mobile.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(new Server().url("/").description("Default Server")))
                .info(new Info()
                        .title("Phone Shop API Documentation (Hợp Đồng API)")
                        .version("1.0")
                        .description("Bảng hợp đồng API dành cho Frontend và Backend tích hợp. Tất cả các endpoint đều được liệt kê ở đây.")
                        .contact(new Contact()
                                .name("Tuấn Anh Vũ")
                                .email("contact@ecommerce.com")));
    }
}
