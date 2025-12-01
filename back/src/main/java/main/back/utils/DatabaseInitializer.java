package main.back.utils;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.logging.Level;
import java.util.logging.Logger;

//@Singleton
@Startup
public class DatabaseInitializer {

    private static final Logger LOGGER = Logger.getLogger(DatabaseInitializer.class.getName());

    @PersistenceContext
    private EntityManager entityManager;

    @PostConstruct
    @Transactional
    public void initialize() {
        try {
            LOGGER.info("Initializing database functions for Person...");

            // Удаляем существующие функции если они есть
            dropFunctionIfExists("get_average_height");
            dropFunctionIfExists("get_count_by_nationality");
            dropFunctionIfExists("get_count_height_greater");
            dropFunctionIfExists("get_count_by_hair_color");
            dropFunctionIfExists("get_hair_color_percentage");

            // Функция для расчета среднего значения height
            entityManager.createNativeQuery(
                    "CREATE OR REPLACE FUNCTION get_average_height() RETURNS NUMERIC AS $$ " +
                            "BEGIN " +
                            "    RETURN (SELECT AVG(height) FROM person WHERE height > 0); " +
                            "END; $$ LANGUAGE plpgsql;"
            ).executeUpdate();

            // Функция для группировки по nationality
            entityManager.createNativeQuery(
                    "CREATE OR REPLACE FUNCTION get_count_by_nationality() RETURNS TABLE(nationality TEXT, count BIGINT) AS $$ " +
                            "BEGIN " +
                            "    RETURN QUERY SELECT p.nationality\\:\\:TEXT, COUNT(*) as count FROM person p GROUP BY p.nationality; " +
                            "END; $$ LANGUAGE plpgsql;"
            ).executeUpdate();

            // Функция для подсчета объектов с height больше заданного
            entityManager.createNativeQuery(
                    "CREATE OR REPLACE FUNCTION get_count_height_greater(threshold BIGINT) RETURNS BIGINT AS $$ " +
                            "BEGIN " +
                            "    RETURN (SELECT COUNT(*) FROM person WHERE height > threshold); " +
                            "END; $$ LANGUAGE plpgsql;"
            ).executeUpdate();

            // Функция для подсчета людей с заданным цветом волос
            entityManager.createNativeQuery(
                    "CREATE OR REPLACE FUNCTION get_count_by_hair_color(hair_color TEXT) RETURNS BIGINT AS $$ " +
                            "BEGIN " +
                            "    RETURN (SELECT COUNT(*) FROM person WHERE hairColor\\:\\:TEXT = hair_color); " +
                            "END; $$ LANGUAGE plpgsql;"
            ).executeUpdate();

            // Функция для расчета доли людей с заданным цветом волос
            entityManager.createNativeQuery(
                    "CREATE OR REPLACE FUNCTION get_hair_color_percentage(hair_color TEXT) RETURNS NUMERIC AS $$ " +
                            "DECLARE " +
                            "    total_count BIGINT; " +
                            "    color_count BIGINT; " +
                            "BEGIN " +
                            "    SELECT COUNT(*) INTO total_count FROM person; " +
                            "    SELECT COUNT(*) INTO color_count FROM person WHERE hairColor\\:\\:TEXT = hair_color; " +
                            "    " +
                            "    IF total_count = 0 THEN " +
                            "        RETURN 0; " +
                            "    ELSE " +
                            "        RETURN (color_count * 100.0 / total_count); " +
                            "    END IF; " +
                            "END; $$ LANGUAGE plpgsql;"
            ).executeUpdate();

            LOGGER.info("Person database functions initialized successfully.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize Person database functions", e);
            throw new RuntimeException(e);
        }
    }

    private void dropFunctionIfExists(String functionName) {
        try {
            entityManager.createNativeQuery("DROP FUNCTION IF EXISTS " + functionName).executeUpdate();
        } catch (Exception e) {
            LOGGER.warning("Failed to drop function " + functionName + ": " + e.getMessage());
        }
    }
}