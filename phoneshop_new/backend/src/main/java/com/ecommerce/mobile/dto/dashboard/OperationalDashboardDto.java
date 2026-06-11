package com.ecommerce.mobile.dto.dashboard;

import java.util.List;
import lombok.Data;

@Data
public class OperationalDashboardDto {
    private List<ChartPoint> orderStatusesToday;
    private List<ChartPoint> revenueLast7Days;
    private List<ChartPoint> topProductsThisWeek;
}
