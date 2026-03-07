package com.trako.repositories;

import com.trako.entities.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByUserId(String userId);

    Optional<Contact> findByIdAndUserId(Long id, String userId);

    @Modifying
    @Query("DELETE FROM Contact c WHERE c.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);
}
