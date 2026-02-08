package com.trako.config;

import com.trako.entities.Category;
import com.trako.entities.CategoryType;
import com.trako.entities.User;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Seeds a default set of categories for each existing user in dev profile.
 */
@Component
@Profile("dev")
public class CategoryStartupSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CategoryStartupSeeder.class);

    private static final List<String> DEFAULT_CATEGORIES = Arrays.asList(
            "RENT",
            "FOOD",
            "GROCERIES",
            "UTILITIES",
            "TRAVEL",
            "FUEL",
            "HEALTH",
            "ENTERTAINMENT",
            "SHOPPING",
            "INCOME",
            "TRANSFER"
    );


    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<User> users = usersRepository.findAll();
            for (User user : users) {
                seedForUser(user);
            }
        } catch (Exception e) {
            log.warn("Category seeding failed: {}", e.getMessage());
        }
    }

    private void seedForUser(User user) {
        try {
            List<Category> existing = categoryRepository.findByUserId(user.getId());
            Set<String> existingNames = existing.stream()
                    .map(Category::getName)
                    .filter(Objects::nonNull)
                    .map(s -> s.trim().toUpperCase(Locale.ROOT))
                    .collect(Collectors.toSet());

            int created = 0;
            for (String name : DEFAULT_CATEGORIES) {
                String norm = name.trim();
                if (norm.isEmpty()) continue;
                if (existingNames.contains(norm.toUpperCase(Locale.ROOT))) continue;
                Category c = new Category();
                c.setName(norm);
                c.setUserId(user.getId());
                if ("INCOME".equalsIgnoreCase(norm) || "TRANSFER".equalsIgnoreCase(norm)) {
                    c.setCategoryType(CategoryType.INCOME);
                } else {
                    c.setCategoryType(CategoryType.EXPENSE);
                }
                categoryRepository.save(c);
                created++;
            }
            if (created > 0) {
                log.info("Seeded {} categories for user {}", created, user.getPhoneNo());
            }
        } catch (Exception e) {
            log.warn("Failed seeding categories for user {}: {}", user.getId(), e.getMessage());
        }
    }
}
