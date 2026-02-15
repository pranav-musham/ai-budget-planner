package com.receiptscan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "predefined_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredefinedCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    @Builder.Default
    private String icon = "category";

    @Column(length = 20)
    @Builder.Default
    private String color = "#3b82f6";

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
