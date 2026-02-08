package com.trako.config;

import com.trako.entities.User;
import com.trako.repositories.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserStartupSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserStartupSeeder.class);

    @Autowired
    private UsersRepository usersRepository;

    @Override
    public void run(ApplicationArguments args) {
        seedUserIfMissing(
                "Default User",
                "9999999999",
                "user@example.com",
                "password",
                0
        );
        seedUserIfMissing(
                "Friend User",
                "8888888888",
                "friend@example.com",
                "password",
                1
        );
    }

    private void seedUserIfMissing(String name, String phoneNo, String email, String fireBaseId, Integer isShadow) {
        try {
            User existing = usersRepository.findByPhoneNo(phoneNo);
            if (existing != null) {
                return;
            }
            User u = new User();
            u.setName(name);
            u.setPhoneNo(phoneNo);
            u.setEmail(email);
            u.setFireBaseId(fireBaseId);
            u.setIsShadow(isShadow);
            u.setGlobalId(UUID.randomUUID().toString().replace("-", ""));
            usersRepository.save(u);
            log.info("Seeded sample user: {} ({})", name, phoneNo);
        } catch (Exception e) {
            log.warn("Failed to seed sample user {}: {}", phoneNo, e.getMessage());
        }
    }
}
