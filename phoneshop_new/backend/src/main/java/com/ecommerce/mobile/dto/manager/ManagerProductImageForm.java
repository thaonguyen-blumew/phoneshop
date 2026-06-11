package com.ecommerce.mobile.dto.manager;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ManagerProductImageForm {
    private Long imageId;
    private String url;
    private Boolean isPrimary;
}
