package com.trako.config;

import com.trako.entities.*;
import com.trako.enums.CategoryType;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.UsersRepository;
import com.trako.services.JsonStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class GlobalStartupSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GlobalStartupSeeder.class);

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
    private Environment environment;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JsonStoreService jsonStoreService;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        log.info("GlobalStartupSeeder: starting");
        try {
            log.info("GlobalStartupSeeder: activeProfiles={}", Arrays.toString(environment.getActiveProfiles()));
            log.info("GlobalStartupSeeder: defaultProfiles={}", Arrays.toString(environment.getDefaultProfiles()));
        } catch (Exception e) {
            log.warn("GlobalStartupSeeder: unable to read profiles: {}", e.getMessage());
        }

        seedUsers();
        seedJsonStore();

        if (isDevProfileActive()) {
            log.info("GlobalStartupSeeder: dev profile detected, seeding accounts/categories");
            seedAccounts();
            seedCategories();
        } else {
            log.info("GlobalStartupSeeder: dev profile NOT detected, skipping accounts/categories");
        }

        log.info("GlobalStartupSeeder: finished");
    }

    private boolean isDevProfileActive() {
        try {
            // Important: spring.profiles.default=dev does not populate getActiveProfiles().
            // acceptsProfiles handles both active and default profiles.
            return environment.acceptsProfiles(Profiles.of("dev"));
        } catch (Exception e) {
            return false;
        }
    }

    private void seedUsers() {
        seedUserIfMissing(
                "Default User",
                "9999999999",
                "user@example.com",
                "password",
                0,
                0
        );
        seedUserIfMissing(
                "Admin User",
                "0000000000",
                "admin@mail.com",
                "password",
                0,
                1
        );
    }

    private void seedUserIfMissing(String name, String phoneNo, String email, String password, Integer isShadow, Integer isAdmin) {
        try {
            User existing = usersRepository.findByPhoneNo(phoneNo);
            if (existing != null) {
                boolean changed = false;
                if (existing.getIsAdmin() == null || !existing.getIsAdmin().equals(isAdmin)) {
                    existing.setIsAdmin(isAdmin);
                    changed = true;
                }
                if (existing.getIsShadow() == null || !existing.getIsShadow().equals(isShadow)) {
                    existing.setIsShadow(isShadow);
                    changed = true;
                }

                if (existing.getPassword() == null || !passwordEncoder.matches(password, existing.getPassword())) {
                    existing.setPassword(passwordEncoder.encode(password));
                    changed = true;
                }

                if (existing.getEmail() == null || !existing.getEmail().equals(email)) {
                    existing.setEmail(email);
                    changed = true;
                }
                if (existing.getName() == null || !existing.getName().equals(name)) {
                    existing.setName(name);
                    changed = true;
                }
                if (changed) {
                    usersRepository.save(existing);
                    log.info("Updated seeded user: {} ({})", name, phoneNo);
                }
                return;
            }
            User u = new User();
            u.setName(name);
            u.setPhoneNo(phoneNo);
            u.setEmail(email);
            u.setPassword(passwordEncoder.encode(password));
            u.setIsShadow(isShadow);
            u.setIsAdmin(isAdmin);
            u.setGlobalId(UUID.randomUUID().toString().replace("-", ""));
            usersRepository.save(u);
            log.info("Seeded sample user: {} ({})", name, phoneNo);
        } catch (Exception e) {
            log.warn("Failed to seed sample user {}: {}", phoneNo, e.getMessage());
        }
    }

    private void seedAccounts() {
        try {
            List<User> users = usersRepository.findAll();
            for (User user : users) {
                if (accountRepository.findByUserId(user.getId()).isEmpty()) {
                    Account account = new Account();
                    account.setName("Cash");
                    account.setUserId(user.getId());
                    account.setCurrency(user.getBaseCurrency());
                    accountRepository.save(account);
                    log.info("Seeded default 'Cash' account for user: {} (ID: {})", user.getPhoneNo(), account.getId());
                }
            }
        } catch (Exception e) {
            log.warn("Account seeding failed: {}", e.getMessage());
        }
    }

    private void seedCategories() {
        try {
            List<User> users = usersRepository.findAll();
            for (User user : users) {
                seedCategoriesForUser(user);
            }
        } catch (Exception e) {
            log.warn("Category seeding failed: {}", e.getMessage());
        }
    }

    private void seedCategoriesForUser(User user) {
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

    private void seedJsonStore() {
        seedJsonStoreIfMissing("autoBackUp", "0");
        seedJsonStoreIfMissing("autoBackUpTimeStamp", "0");
        seedJsonStoreIfMissing("lastReadSmsId", "0");
    }

    private void seedJsonStoreIfMissing(String name, String value) {
        try {
            if (jsonStoreService.findByName(name).isPresent()) {
                return;
            }
            JsonStore jsonStore = new JsonStore();
            jsonStore.setName(name);
            jsonStore.setValue(value);
            jsonStoreService.save(jsonStore);
        } catch (Exception e) {
            log.warn("Failed seeding JsonStore {}: {}", name, e.getMessage());
        }
    }
}
