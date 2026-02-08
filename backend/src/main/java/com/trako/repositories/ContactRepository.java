package com.trako.repositories;

import com.trako.entities.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByUserId(String userId);
    Optional<Contact> findByIdAndUserId(Long id, String userId);
}
