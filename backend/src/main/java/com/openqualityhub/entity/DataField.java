package com.openqualityhub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "data_fields")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataField extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private TestDataTemplate template;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 30)
    private FieldType fieldType;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean nullable = false;

    @Column(name = "faker_generator", length = 200)
    private String fakerGenerator;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private String constraints = "{}";

    public enum FieldType {
        STRING, INTEGER, DECIMAL, BOOLEAN, DATE, DATETIME,
        EMAIL, PHONE, UUID, ENUM, REGEX_PATTERN
    }
}
