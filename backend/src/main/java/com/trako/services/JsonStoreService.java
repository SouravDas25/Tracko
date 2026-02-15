package com.trako.services;

import com.trako.entities.JsonStore;
import com.trako.repositories.JsonStoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class JsonStoreService {

    @Autowired
    private JsonStoreRepository jsonStoreRepository;

    public List<JsonStore> findAll() {
        return jsonStoreRepository.findAll();
    }

    public Optional<JsonStore> findByName(String name) {
        return jsonStoreRepository.findById(name);
    }

    public JsonStore save(JsonStore jsonStore) {
        return jsonStoreRepository.save(jsonStore);
    }

    public void delete(String name) {
        jsonStoreRepository.deleteById(name);
    }
}
