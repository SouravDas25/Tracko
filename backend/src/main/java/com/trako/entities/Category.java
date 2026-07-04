package com.trako.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.trako.enums.CategoryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "categories")
@Getter
@Setter
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "name", length = 250)
    private String name;

    @NotNull
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "is_roll_over_enabled")
    private Boolean isRollOverEnabled;

    @Column(name = "parent_category_id")
    private Long parentCategoryId;

    @NotNull
    @Column(name = "category_type", length = 16)
    @Enumerated(EnumType.STRING)
    private CategoryType categoryType = CategoryType.EXPENSE;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id", insertable = false, updatable = false)
    private Category parentCategory;
}
