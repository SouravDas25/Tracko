package com.trako.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "json_store")
public class JsonStore {

    @NotBlank
    @Size(max = 191)
    @Id
    @Column(name = "name", nullable = false, length = 191)
    private String name;

    @Size(max = 10000)
    @Column(name = "json_value", length = 10000)
    private String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
