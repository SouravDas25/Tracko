package com.trako.services;

import com.trako.entities.JsonStore;
import com.trako.repositories.JsonStoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class JsonStoreService {

    @Autowired
    private JsonStoreRepository jsonStoreRepository;

    public List<JsonStore> findAllByUser(String userId) {
        return jsonStoreRepository.findByUserId(userId);
    }

    public Optional<JsonStore> findByNameAndUser(String name, String userId) {
        return jsonStoreRepository.findByUserIdAndName(userId, name);
    }

    public JsonStore save(JsonStore jsonStore) {
        // Upsert within the user's namespace: reuse the existing row's id so a
        // repeated POST/PUT updates in place instead of violating the
        // (user_id, name) unique constraint.
        jsonStoreRepository.findByUserIdAndName(jsonStore.getUserId(), jsonStore.getName())
                .ifPresent(existing -> jsonStore.setId(existing.getId()));
        return jsonStoreRepository.save(jsonStore);
    }

    @Transactional
    public void delete(String name, String userId) {
        jsonStoreRepository.deleteByUserIdAndName(userId, name);
    }
}
