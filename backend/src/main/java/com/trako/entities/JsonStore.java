package com.trako.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "json_store")
@Getter
@Setter
public class JsonStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @JsonIgnore
    private Long id;

    // Set server-side from the authenticated user; never bound from the request
    // body. DB nullable=false is the integrity safety net.
    @JsonIgnore
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @NotBlank
    @Size(max = 191)
    @Column(name = "name", nullable = false, length = 191)
    private String name;

    @Size(max = 10000)
    @Column(name = "json_value", length = 10000)
    private String value;
}
