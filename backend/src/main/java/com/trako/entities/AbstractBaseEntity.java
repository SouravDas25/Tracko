package com.trako.entities;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;

@MappedSuperclass
public abstract class AbstractBaseEntity implements Serializable {
    protected static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "id")
    protected String id;

//    public AbstractBaseEntity() {
//        this.id = UUID.randomUUID().toString();
//    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        if (this.id != null)
            return id.hashCode();
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this.id == null)
            return super.equals(obj);
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof AbstractBaseEntity other)) {
            return false;
        }
        return getId().equals(other.getId());
    }
}
