package com.company.mts.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Component
@Profile("!test")
@Slf4j
public class DatabaseSchemaFixer implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    public DatabaseSchemaFixer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Tables we expect to contain audit columns
        String[] tables = new String[]{"accounts", "bank_details", "auth_users"};

        for (String table : tables) {
            try {
                // Ensure created_on exists
                List<java.util.Map<String,Object>> cols = jdbc.queryForList("SHOW COLUMNS FROM " + table + " LIKE 'created_on'");
                if (cols == null || cols.isEmpty()) {
                    log.warn("[DatabaseSchemaFixer] Table '{}' is missing column 'created_on'. Adding it.", table);
                    jdbc.execute("ALTER TABLE " + table + " ADD COLUMN created_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
                } else {
                    log.debug("[DatabaseSchemaFixer] Table '{}' already has 'created_on'", table);
                }

                // Ensure last_updated exists
                List<java.util.Map<String,Object>> lastCols = jdbc.queryForList("SHOW COLUMNS FROM " + table + " LIKE 'last_updated'");
                if (lastCols == null || lastCols.isEmpty()) {
                    log.warn("[DatabaseSchemaFixer] Table '{}' is missing column 'last_updated'. Adding it.", table);
                    jdbc.execute("ALTER TABLE " + table + " ADD COLUMN last_updated TIMESTAMP NULL");
                } else {
                    log.debug("[DatabaseSchemaFixer] Table '{}' already has 'last_updated'", table);
                }

                // If there's an old 'created_at' column that is NOT NULL without default, set a default
                List<java.util.Map<String,Object>> createdAtCols = jdbc.queryForList("SHOW COLUMNS FROM " + table + " LIKE 'created_at'");
                if (createdAtCols != null && !createdAtCols.isEmpty()) {
                    try {
                        log.warn("[DatabaseSchemaFixer] Table '{}' contains legacy 'created_at' column. Ensuring DEFAULT CURRENT_TIMESTAMP.", table);
                        jdbc.execute("ALTER TABLE " + table + " MODIFY created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
                    } catch (Exception inner) {
                        log.error("[DatabaseSchemaFixer] Failed to modify 'created_at' on '{}': {}", table, inner.getMessage());
                    }
                }

            } catch (Exception e) {
                log.error("[DatabaseSchemaFixer] Error checking/updating table '{}': {}", table, e.getMessage());
            }
        }
    }
}
