package com.trako.services;

import com.trako.entities.Split;
import com.trako.repositories.SplitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class SplitService {

    @Autowired
    private SplitRepository splitRepository;

    public List<Split> findAll() {
        return splitRepository.findAll();
    }

    public Optional<Split> findById(Long id) {
        return splitRepository.findById(id);
    }

    public List<Split> findByTransactionId(Long transactionId) {
        return splitRepository.findByTransactionId(transactionId);
    }

    public List<Split> findByUserId(String userId) {
        return splitRepository.findByUserId(userId);
    }

    public List<Split> findUnsettledByUserId(String userId) {
        return splitRepository.findByUserIdAndIsSettled(userId, 0);
    }

    public List<Split> findByContactId(Long contactId) {
        return splitRepository.findByContactId(contactId);
    }

    public List<Split> findUnsettledByContactId(Long contactId) {
        return splitRepository.findByContactIdAndIsSettled(contactId, 0);
    }

    public Split save(Split split) {
        return splitRepository.save(split);
    }

    @Transactional
    public void settleSplit(Long splitId) {
        splitRepository.settleSplit(splitId);
    }

    @Transactional
    public void unsettleSplit(Long splitId) {
        splitRepository.unsettleSplit(splitId);
    }

    public void delete(Long id) {
        splitRepository.deleteById(id);
    }
}
