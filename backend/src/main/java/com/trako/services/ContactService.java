package com.trako.services;

import com.trako.entities.Contact;
import com.trako.repositories.ContactRepository;
import com.trako.repositories.SplitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ContactService {

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private SplitRepository splitRepository;

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
        // Prevent deletion if any Split references this contact
        if (splitRepository.existsByContactId(id)) {
            throw new IllegalArgumentException("Cannot delete contact: Splits reference this contact. Remove or update splits first.");
        }
        contactRepository.deleteById(id);
    }
}
