package com.ecommerce.mobile.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeedbackDto {
    private Long feedbackId;
    private String content;
    private String resolution;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String customerName;
    private String employeeName;
}
