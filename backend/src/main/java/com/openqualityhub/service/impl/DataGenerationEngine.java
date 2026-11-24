package com.openqualityhub.service.impl;

import com.openqualityhub.entity.DataField;
import com.openqualityhub.entity.TestDataProfile;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class DataGenerationEngine {

    private static final Faker FAKER = new Faker();
    private static final Faker FAKER_AR = new Faker(new Locale("ar"));
    private static final Faker FAKER_JA = new Faker(new Locale("ja"));
    private static final Faker FAKER_FR = new Faker(new Locale("fr"));

    public List<Map<String, Object>> generate(List<DataField> fields, TestDataProfile profile, int rowCount) {
        var profileType = profile != null ? profile.getProfileType() : TestDataProfile.DataProfileType.POSITIVE;
        log.info("Generating {} rows with profile type: {}", rowCount, profileType);

        List<Map<String, Object>> rows = new ArrayList<>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (DataField field : fields) {
                row.put(field.getName(), generateValue(field, profileType, i));
            }
            rows.add(row);
        }
        return rows;
    }

    private Object generateValue(DataField field, TestDataProfile.DataProfileType profileType, int rowIndex) {
        return switch (profileType) {
            case POSITIVE -> generatePositive(field);
            case NEGATIVE -> generateNegative(field, rowIndex);
            case EDGE_CASE -> generateEdgeCase(field, rowIndex);
            case BOUNDARY -> generateBoundary(field, rowIndex);
            case LOCALIZATION -> generateLocalization(field, rowIndex);
            case PERFORMANCE -> generatePerformance(field);
        };
    }

    // ---- POSITIVE: valid, realistic data ----

    private Object generatePositive(DataField field) {
        if (field.isNullable() && ThreadLocalRandom.current().nextInt(10) == 0) {
            return null;
        }

        String faker = field.getFakerGenerator();
        if (faker != null && !faker.isBlank()) {
            return invokeFaker(faker);
        }

        return generateByType(field);
    }

    private Object generateByType(DataField field) {
        Map<String, Object> c = parseConstraints(field.getConstraints());

        return switch (field.getFieldType()) {
            case STRING -> {
                int maxLen = getInt(c, "maxLength", 50);
                int minLen = getInt(c, "minLength", 3);
                String val = FAKER.lorem().characters(minLen, maxLen);
                yield val;
            }
            case INTEGER -> {
                int min = getInt(c, "min", 0);
                int max = getInt(c, "max", 10000);
                yield ThreadLocalRandom.current().nextInt(min, max + 1);
            }
            case DECIMAL -> {
                double min = getDouble(c, "min", 0.0);
                double max = getDouble(c, "max", 10000.0);
                double val = ThreadLocalRandom.current().nextDouble(min, max);
                yield BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP);
            }
            case BOOLEAN -> ThreadLocalRandom.current().nextBoolean();
            case DATE -> {
                LocalDate start = LocalDate.now().minusYears(1);
                long days = ChronoUnit.DAYS.between(start, LocalDate.now().plusYears(1));
                yield start.plusDays(ThreadLocalRandom.current().nextLong(days));
            }
            case DATETIME -> {
                Instant now = Instant.now();
                long range = 365L * 24 * 3600;
                yield now.minusSeconds(ThreadLocalRandom.current().nextLong(range));
            }
            case EMAIL -> FAKER.internet().emailAddress();
            case PHONE -> FAKER.phoneNumber().cellPhone();
            case UUID -> java.util.UUID.randomUUID().toString();
            case ENUM -> {
                List<String> values = getStringList(c, "enumValues");
                yield values.isEmpty() ? "UNKNOWN" : values.get(ThreadLocalRandom.current().nextInt(values.size()));
            }
            case REGEX_PATTERN -> {
                String pattern = getString(c, "pattern", "[a-zA-Z0-9]{10}");
                yield FAKER.regexify(pattern);
            }
        };
    }

    // ---- NEGATIVE: intentionally invalid data ----

    private Object generateNegative(DataField field, int rowIndex) {
        int variant = rowIndex % 5;
        Map<String, Object> c = parseConstraints(field.getConstraints());

        return switch (field.getFieldType()) {
            case STRING -> switch (variant) {
                case 0 -> "";
                case 1 -> FAKER.lorem().characters(getInt(c, "maxLength", 50) + 20);
                case 2 -> "<script>alert('xss')</script>";
                case 3 -> "'; DROP TABLE users; --";
                default -> null;
            };
            case INTEGER -> switch (variant) {
                case 0 -> getInt(c, "min", 0) - 1;
                case 1 -> getInt(c, "max", 10000) + 1;
                case 2 -> Integer.MIN_VALUE;
                case 3 -> Integer.MAX_VALUE;
                default -> null;
            };
            case DECIMAL -> switch (variant) {
                case 0 -> BigDecimal.valueOf(getDouble(c, "min", 0.0) - 0.01);
                case 1 -> BigDecimal.valueOf(getDouble(c, "max", 10000.0) + 0.01);
                case 2 -> BigDecimal.valueOf(-999999.99);
                default -> null;
            };
            case BOOLEAN -> switch (variant) {
                case 0 -> null;
                default -> ThreadLocalRandom.current().nextBoolean();
            };
            case DATE -> switch (variant) {
                case 0 -> "not-a-date";
                case 1 -> "9999-99-99";
                case 2 -> LocalDate.of(1, 1, 1);
                default -> null;
            };
            case DATETIME -> switch (variant) {
                case 0 -> "invalid-timestamp";
                case 1 -> Instant.EPOCH;
                default -> null;
            };
            case EMAIL -> switch (variant) {
                case 0 -> "not-an-email";
                case 1 -> "@missing-local.com";
                case 2 -> "user@";
                case 3 -> "user @spaces.com";
                default -> "";
            };
            case PHONE -> switch (variant) {
                case 0 -> "abc-not-phone";
                case 1 -> "+999999999999999999";
                case 2 -> "";
                default -> null;
            };
            case UUID -> switch (variant) {
                case 0 -> "not-a-uuid";
                case 1 -> "12345";
                case 2 -> "";
                default -> null;
            };
            case ENUM -> switch (variant) {
                case 0 -> "INVALID_VALUE";
                case 1 -> "";
                case 2 -> null;
                default -> "NONEXISTENT_" + rowIndex;
            };
            case REGEX_PATTERN -> switch (variant) {
                case 0 -> "!!!invalid!!!";
                case 1 -> "";
                default -> null;
            };
        };
    }

    // ---- EDGE_CASE: boundary and special values ----

    private Object generateEdgeCase(DataField field, int rowIndex) {
        int variant = rowIndex % 6;
        Map<String, Object> c = parseConstraints(field.getConstraints());

        return switch (field.getFieldType()) {
            case STRING -> switch (variant) {
                case 0 -> "";
                case 1 -> " ";
                case 2 -> "\t\n\r";
                case 3 -> "a".repeat(getInt(c, "maxLength", 255));
                case 4 -> "\u0000null-byte";
                default -> "   leading-trailing-spaces   ";
            };
            case INTEGER -> switch (variant) {
                case 0 -> 0;
                case 1 -> -1;
                case 2 -> getInt(c, "min", Integer.MIN_VALUE);
                case 3 -> getInt(c, "max", Integer.MAX_VALUE);
                default -> 1;
            };
            case DECIMAL -> switch (variant) {
                case 0 -> BigDecimal.ZERO;
                case 1 -> BigDecimal.valueOf(-0.01);
                case 2 -> BigDecimal.valueOf(0.001);
                default -> BigDecimal.valueOf(99999.99);
            };
            case BOOLEAN -> variant % 2 == 0;
            case DATE -> switch (variant) {
                case 0 -> LocalDate.of(2000, 1, 1);
                case 1 -> LocalDate.of(2000, 2, 29);
                case 2 -> LocalDate.of(2099, 12, 31);
                default -> LocalDate.now();
            };
            case DATETIME -> switch (variant) {
                case 0 -> Instant.EPOCH;
                case 1 -> Instant.parse("2000-01-01T00:00:00Z");
                default -> Instant.now();
            };
            case EMAIL -> switch (variant) {
                case 0 -> "a@b.c";
                case 1 -> "very.long.email.address.test+" + "x".repeat(50) + "@example.com";
                case 2 -> "user+tag@example.com";
                default -> "UPPERCASE@DOMAIN.COM";
            };
            case PHONE -> switch (variant) {
                case 0 -> "+1";
                case 1 -> "+971501234567";
                case 2 -> "0000000000";
                default -> FAKER.phoneNumber().cellPhone();
            };
            case UUID -> java.util.UUID.randomUUID().toString();
            case ENUM -> {
                List<String> values = getStringList(c, "enumValues");
                yield values.isEmpty() ? null : values.get(variant % values.size());
            }
            case REGEX_PATTERN -> {
                String pattern = getString(c, "pattern", "[a-z]{5}");
                yield FAKER.regexify(pattern);
            }
        };
    }

    // ---- BOUNDARY: exact boundary values ----

    private Object generateBoundary(DataField field, int rowIndex) {
        int variant = rowIndex % 4;
        Map<String, Object> c = parseConstraints(field.getConstraints());

        return switch (field.getFieldType()) {
            case STRING -> {
                int minLen = getInt(c, "minLength", 1);
                int maxLen = getInt(c, "maxLength", 255);
                yield switch (variant) {
                    case 0 -> "a".repeat(minLen);
                    case 1 -> "z".repeat(maxLen);
                    case 2 -> minLen > 1 ? "b".repeat(minLen - 1) : "";
                    default -> "x".repeat(maxLen + 1);
                };
            }
            case INTEGER -> {
                int min = getInt(c, "min", 0);
                int max = getInt(c, "max", 10000);
                yield switch (variant) {
                    case 0 -> min;
                    case 1 -> max;
                    case 2 -> min - 1;
                    default -> max + 1;
                };
            }
            case DECIMAL -> {
                double min = getDouble(c, "min", 0.0);
                double max = getDouble(c, "max", 10000.0);
                yield switch (variant) {
                    case 0 -> BigDecimal.valueOf(min);
                    case 1 -> BigDecimal.valueOf(max);
                    case 2 -> BigDecimal.valueOf(min - 0.01);
                    default -> BigDecimal.valueOf(max + 0.01);
                };
            }
            default -> generateEdgeCase(field, rowIndex);
        };
    }

    // ---- LOCALIZATION: i18n data ----

    private Object generateLocalization(DataField field, int rowIndex) {
        int variant = rowIndex % 4;

        return switch (field.getFieldType()) {
            case STRING -> switch (variant) {
                case 0 -> FAKER_AR.name().fullName();
                case 1 -> FAKER_JA.name().fullName();
                case 2 -> FAKER_FR.address().fullAddress();
                default -> "Ñoño García — 日本語テスト — العربية";
            };
            case EMAIL -> switch (variant) {
                case 0 -> FAKER_FR.internet().emailAddress();
                case 1 -> "用户@例え.jp";
                default -> FAKER.internet().emailAddress();
            };
            case PHONE -> switch (variant) {
                case 0 -> "+33 " + FAKER_FR.phoneNumber().cellPhone();
                case 1 -> "+81 " + FAKER_JA.phoneNumber().cellPhone();
                case 2 -> "+971 " + FAKER.phoneNumber().subscriberNumber(9);
                default -> FAKER.phoneNumber().cellPhone();
            };
            default -> generatePositive(field);
        };
    }

    // ---- PERFORMANCE: stress-test data ----

    private Object generatePerformance(DataField field) {
        return switch (field.getFieldType()) {
            case STRING -> FAKER.lorem().characters(1000, 5000);
            case INTEGER -> ThreadLocalRandom.current().nextInt();
            case DECIMAL -> BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble() * 999999999);
            default -> generatePositive(field);
        };
    }

    // ---- Faker invocation ----

    private Object invokeFaker(String expression) {
        try {
            return FAKER.expression("#{" + expression + "}");
        } catch (Exception e) {
            log.warn("Failed to invoke faker expression '{}': {}", expression, e.getMessage());
            return FAKER.lorem().word();
        }
    }

    // ---- Constraint helpers ----

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseConstraints(String json) {
        if (json == null || json.isBlank() || "{}".equals(json)) {
            return Collections.emptyMap();
        }
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private int getInt(Map<String, Object> c, String key, int defaultVal) {
        Object val = c.get(key);
        if (val instanceof Number n) return n.intValue();
        return defaultVal;
    }

    private double getDouble(Map<String, Object> c, String key, double defaultVal) {
        Object val = c.get(key);
        if (val instanceof Number n) return n.doubleValue();
        return defaultVal;
    }

    private String getString(Map<String, Object> c, String key, String defaultVal) {
        Object val = c.get(key);
        return val instanceof String s ? s : defaultVal;
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> c, String key) {
        Object val = c.get(key);
        if (val instanceof List<?> list) {
            return (List<String>) list;
        }
        return Collections.emptyList();
    }
}
