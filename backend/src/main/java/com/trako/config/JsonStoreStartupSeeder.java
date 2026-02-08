package com.trako.config;

import com.trako.entities.JsonStore;
import com.trako.services.JsonStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class JsonStoreStartupSeeder implements ApplicationRunner {

    @Autowired
    private JsonStoreService jsonStoreService;

    @Override
    public void run(ApplicationArguments args) {
        seedIfMissing("autoBackUp", "0");
        seedIfMissing("autoBackUpTimeStamp", "0");
        seedIfMissing("lastReadSmsId", "0");
    }

    private void seedIfMissing(String name, String value) {
        if (jsonStoreService.findByName(name).isPresent()) {
            return;
        }
        JsonStore jsonStore = new JsonStore();
        jsonStore.setName(name);
        jsonStore.setValue(value);
        jsonStoreService.save(jsonStore);
    }
}
