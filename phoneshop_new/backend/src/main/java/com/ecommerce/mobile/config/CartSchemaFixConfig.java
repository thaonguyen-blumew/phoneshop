package com.ecommerce.mobile.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnProperty(name = "phoneshop.schema-fix.enabled", havingValue = "true", matchIfMissing = true)
public class CartSchemaFixConfig {

    @Bean
    CommandLineRunner normalizeCartSchema(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                Integer userIdColumns = jdbcTemplate.queryForObject("""
                        select count(*)
                        from information_schema.columns
                        where table_schema = database()
                          and table_name = 'carts'
                          and column_name = 'user_id'
                        """, Integer.class);

                Integer customerIdColumns = jdbcTemplate.queryForObject("""
                        select count(*)
                        from information_schema.columns
                        where table_schema = database()
                          and table_name = 'carts'
                          and column_name = 'customer_id'
                        """, Integer.class);

                boolean hasUserId = userIdColumns != null && userIdColumns > 0;
                boolean hasCustomerId = customerIdColumns != null && customerIdColumns > 0;

                if (!hasUserId && hasCustomerId) {
                    List<String> foreignKeys = jdbcTemplate.queryForList("""
                            select constraint_name
                            from information_schema.key_column_usage
                            where table_schema = database()
                              and table_name = 'carts'
                              and column_name = 'customer_id'
                              and referenced_table_name is not null
                            """, String.class);

                    for (String fk : foreignKeys) {
                        try {
                            jdbcTemplate.execute("alter table carts drop foreign key `" + fk + "`");
                        } catch (Exception ignored) {
                        }
                    }

                    try {
                        jdbcTemplate.execute("alter table carts change column customer_id user_id bigint not null");
                    } catch (Exception ignored) {
                    }
                } else if (hasUserId && hasCustomerId) {
                    List<String> foreignKeys = jdbcTemplate.queryForList("""
                            select constraint_name
                            from information_schema.key_column_usage
                            where table_schema = database()
                              and table_name = 'carts'
                              and column_name = 'customer_id'
                              and referenced_table_name is not null
                            """, String.class);

                    for (String fk : foreignKeys) {
                        try {
                            jdbcTemplate.execute("alter table carts drop foreign key `" + fk + "`");
                        } catch (Exception ignored) {
                        }
                    }

                    try {
                        jdbcTemplate.execute("alter table carts drop column customer_id");
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }
        };
    }
}
