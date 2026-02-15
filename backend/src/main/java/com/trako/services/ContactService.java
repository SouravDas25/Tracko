package com.trako.services;

import com.trako.entities.Contact;
import com.trako.repositories.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ContactService {

    @Autowired
    private ContactRepository contactRepository;

    public List<Contact> findByUserId(String userId) {
        return contactRepository.findByUserId(userId);
    }

    public Optional<Contact> findByIdAndUserId(Long id, String userId) {
        return contactRepository.findByIdAndUserId(id, userId);
    }

    public Contact save(Contact contact) {
        return contactRepository.save(contact);
    }

    public void delete(Long id) {
        contactRepository.deleteById(id);
    }
}
