package com.openqualityhub.service.impl;

import com.openqualityhub.entity.DataField;
import com.openqualityhub.entity.TestDataProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DataGenerationEngineTest {

    private DataGenerationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new DataGenerationEngine();
    }

    // ---- POSITIVE profile ----

    @Test
    void generate_positiveProfile_stringField() {
        DataField field = buildField("username", DataField.FieldType.STRING);
        TestDataProfile profile = buildProfile(TestDataProfile.DataProfileType.POSITIVE);

        List<Map<String, Object>> result = engine.generate(List.of(field), profile, 5);

        assertEquals(5, result.size());
        for (Map<String, Object> row : result) {
            assertTrue(row.containsKey("username"));
            assertInstanceOf(String.class, row.get("username"));
        }
    }

    @Test
    void generate_positiveProfile_integerField() {
        DataField field = buildField("age", DataField.FieldType.INTEGER);
        TestDataProfile profile = buildProfile(TestDataProfile.DataProfileType.POSITIVE);

        List<Map<String, Object>> result = engine.generate(List.of(field), profile, 5);

        assertEquals(5, result.size());
        for (Map<String, Object> row : result) {
            assertTrue(row.containsKey("age"));
            assertInstanceOf(Integer.class, row.get("age"));
        }
    }

    @Test
    void generate_positiveProfile_emailField() {
        DataField field = buildField("email", DataField.FieldType.EMAIL);
        TestDataProfile profile = buildProfile(TestDataProfile.DataProfileType.POSITIVE);

        List<Map<String, Object>> result = engine.generate(List.of(field), profile, 5);

        assertEquals(5, result.size());
        for (Map<String, Object> row : result) {
            assertTrue(row.containsKey("email"));
            Object value = row.get("email");
            assertInstanceOf(String.class, value);
            assertTrue(((String) value).contains("@"), "Email should contain @");
        }
    }

    @Test
    void generate_positiveProfile_booleanField() {
        DataField field = buildField("active", DataField.FieldType.BOOLEAN);
        TestDataProfile profile = buildProfile(TestDataProfile.DataProfileType.POSITIVE);

        List<Map<String, Object>> result = engine.generate(List.of(field), profile, 10);

        assertEquals(10, result.size());
        for (Map<String, Object> row : result) {
            assertTrue(row.containsKey("active"));
            assertInstanceOf(Boolean.class, row.get("active"));
        }
    }

    @Test
    void generate_positiveProfile_enumField() {
        DataField field = DataField.builder()
                .name("status")
                .fieldType(DataField.FieldType.ENUM)
                .nullable(false)
                .constraints("{\"enumValues\":[\"A\",\"B\",\"C\"]}")
                .build();
        TestDataProfile profile = buildProfile(TestDataProfile.DataProfileType.POSITIVE);

        List<Map<String, Object>> result = engine.generate(List.of(field), profile, 20);

        assertEquals(20, result.size());
        for (Map<String, Object> row : result) {
            Object value = row.get("status");
            assertNotNull(value);
            assertTrue(List.of("A", "B", "C").contains(value),
                    "Enum value should be one of A, B, C but was: " + value);
        }
    }

    // ---- NEGATIVE profile ----

    @Test
    void generate_negativeProfile_stringField() {
        DataField field = buildField("name", DataField.FieldType.STRING);
        TestDataProfile profile = buildProfile(TestDataProfile.DataProfileType.NEGATIVE);

        List<Map<String, Object>> result = engine.generate(List.of(field), profile, 5);

        assertEquals(5, result.size());
        // Negative profile produces empty strings, overlong strings, XSS, SQL injection, or null
        for (Map<String, Object> row : result) {
            assertTrue(row.containsKey("name"));
            // Values can be null or String -- both are acceptable for negative testing
        }
    }

    @Test
    void generate_negativeProfile_integerField() {
        DataField field = buildField("count", DataField.FieldType.INTEGER);
        TestDataProfile profile = buildProfile(TestDataProfile.DataProfileType.NEGATIVE);

        List<Map<String, Object>> result = engine.generate(List.of(field), profile, 5);

        assertEquals(5, result.size());
        for (Map<String, Object> row : result) {
            assertTrue(row.containsKey("count"));
            // Negative integers can be out-of-range values, MIN/MAX_VALUE, or null
        }
    }

    // ---- EDGE_CASE profile ----

    @Test
    void generate_edgeCaseProfile_stringField() {
        DataField field = buildField("text", DataField.FieldType.STRING);
        TestDataProfile profile = buildProfile(TestDataProfile.DataProfileType.EDGE_CASE);

        List<Map<String, Object>> result = engine.generate(List.of(field), profile, 6);

        assertEquals(6, result.size());
        // Edge cases: empty, single space, whitespace chars, max-length repeats, null bytes, leading/trailing spaces
        Object firstValue = result.get(0).get("text");
        assertEquals("", firstValue, "First row (variant 0) should be empty string");

        Object secondValue = result.get(1).get("text");
        assertEquals(" ", secondValue, "Second row (variant 1) should be single space");
    }

    // ---- BOUNDARY profile ----

    @Test
    void generate_boundaryProfile_integerField() {
        DataField field = DataField.builder()
                .name("score")
                .fieldType(DataField.FieldType.INTEGER)
                .nullable(false)
                .constraints("{\"min\":0,\"max\":100}")
                .build();
        TestDataProfile profile = buildProfile(TestDataProfile.DataProfileType.BOUNDARY);

        List<Map<String, Object>> result = engine.generate(List.of(field), profile, 4);

        assertEquals(4, result.size());

        // variant 0 -> min (0), variant 1 -> max (100), variant 2 -> min-1 (-1), variant 3 -> max+1 (101)
        assertEquals(0, result.get(0).get("score"), "Boundary variant 0 should be min value");
        assertEquals(100, result.get(1).get("score"), "Boundary variant 1 should be max value");
        assertEquals(-1, result.get(2).get("score"), "Boundary variant 2 should be min-1");
        assertEquals(101, result.get(3).get("score"), "Boundary variant 3 should be max+1");
    }

    // ---- LOCALIZATION profile ----

    @Test
    void generate_localizationProfile_stringField() {
        DataField field = buildField("display_name", DataField.FieldType.STRING);
        TestDataProfile profile = buildProfile(TestDataProfile.DataProfileType.LOCALIZATION);

        List<Map<String, Object>> result = engine.generate(List.of(field), profile, 4);

        assertEquals(4, result.size());
        for (Map<String, Object> row : result) {
            assertTrue(row.containsKey("display_name"));
            Object value = row.get("display_name");
            assertNotNull(value, "Localization values should not be null");
            assertInstanceOf(String.class, value);
            assertFalse(((String) value).isEmpty(), "Localization string should not be empty");
        }
    }

    // ---- PERFORMANCE profile ----

    @Test
    void generate_performanceProfile_stringField() {
        DataField field = buildField("payload", DataField.FieldType.STRING);
        TestDataProfile profile = buildProfile(TestDataProfile.DataProfileType.PERFORMANCE);

        List<Map<String, Object>> result = engine.generate(List.of(field), profile, 3);

        assertEquals(3, result.size());
        for (Map<String, Object> row : result) {
            Object value = row.get("payload");
            assertInstanceOf(String.class, value);
            assertTrue(((String) value).length() >= 1000,
                    "Performance string should be at least 1000 chars, was " + ((String) value).length());
        }
    }

    // ---- Faker generator ----

    @Test
    void generate_withFakerGenerator() {
        DataField field = DataField.builder()
                .name("first_name")
                .fieldType(DataField.FieldType.STRING)
                .nullable(false)
                .fakerGenerator("Name.firstName")
                .constraints("{}")
                .build();
        TestDataProfile profile = buildProfile(TestDataProfile.DataProfileType.POSITIVE);

        List<Map<String, Object>> result = engine.generate(List.of(field), profile, 5);

        assertEquals(5, result.size());
        for (Map<String, Object> row : result) {
            Object value = row.get("first_name");
            assertNotNull(value, "Faker-generated value should not be null");
            assertInstanceOf(String.class, value);
            assertFalse(((String) value).isEmpty(), "Faker-generated name should not be empty");
        }
    }

    // ---- Null profile defaults to POSITIVE ----

    @Test
    void generate_nullProfile_defaultsToPositive() {
        DataField field = buildField("value", DataField.FieldType.STRING);

        List<Map<String, Object>> result = engine.generate(List.of(field), null, 3);

        assertEquals(3, result.size());
        for (Map<String, Object> row : result) {
            assertTrue(row.containsKey("value"));
            // With null profile, engine defaults to POSITIVE; values should be valid strings
            assertInstanceOf(String.class, row.get("value"));
        }
    }

    // ---- Multiple rows ----

    @Test
    void generate_multipleRows_correctCount() {
        DataField nameField = buildField("name", DataField.FieldType.STRING);
        DataField ageField = buildField("age", DataField.FieldType.INTEGER);
        DataField emailField = buildField("email", DataField.FieldType.EMAIL);
        TestDataProfile profile = buildProfile(TestDataProfile.DataProfileType.POSITIVE);

        List<Map<String, Object>> result = engine.generate(
                List.of(nameField, ageField, emailField), profile, 50);

        assertEquals(50, result.size());
        for (Map<String, Object> row : result) {
            assertEquals(3, row.size(), "Each row should have 3 fields");
            assertTrue(row.containsKey("name"));
            assertTrue(row.containsKey("age"));
            assertTrue(row.containsKey("email"));
        }
    }

    // ---- Helpers ----

    private DataField buildField(String name, DataField.FieldType fieldType) {
        return DataField.builder()
                .name(name)
                .fieldType(fieldType)
                .nullable(false)
                .constraints("{}")
                .build();
    }

    private TestDataProfile buildProfile(TestDataProfile.DataProfileType profileType) {
        return TestDataProfile.builder()
                .profileType(profileType)
                .build();
    }
}
