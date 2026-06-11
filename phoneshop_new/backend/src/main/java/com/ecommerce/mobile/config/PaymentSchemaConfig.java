package com.ecommerce.mobile.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "phoneshop.schema-fix.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentSchemaConfig implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public PaymentSchemaConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureMultiplePaymentsPerOrder();
    }

    private void ensureMultiplePaymentsPerOrder() {
        try {
            jdbcTemplate.execute("create index idx_payments_order_id on payments(order_id)");
        } catch (Exception ignored) {
            // Index có thể đã tồn tại hoặc DB không cho tạo lại, vẫn tiếp tục xử lý unique index bên dưới.
        }

        List<Map<String, Object>> indexes = jdbcTemplate.queryForList("""
                select index_name
                from information_schema.statistics
                where table_schema = database()
                  and table_name = 'payments'
                  and column_name = 'order_id'
                  and non_unique = 0
                  and index_name <> 'PRIMARY'
                """);

        for (Map<String, Object> row : indexes) {
            String indexName = String.valueOf(row.get("index_name"));
            try {
                jdbcTemplate.execute("alter table payments drop index `" + indexName + "`");
            } catch (Exception ignored) {
                // Nếu index đó đang được FK giữ lại ở DB cũ, bỏ qua để app vẫn chạy.
            }
        }
    }
}
