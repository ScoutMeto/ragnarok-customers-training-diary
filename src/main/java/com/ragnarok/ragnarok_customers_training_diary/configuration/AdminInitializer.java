package com.ragnarok.ragnarok_customers_training_diary.configuration;

import com.ragnarok.ragnarok_customers_training_diary.entity.AdminEntity;
import com.ragnarok.ragnarok_customers_training_diary.entity.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    @Autowired
    AdminRepository adminRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    public AdminInitializer(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (adminRepository.findByAdminEmail("admin@admin.cz").isEmpty()) {
            AdminEntity adminEntity = new AdminEntity();
            adminEntity.setAdminEmail("admin@admin.cz");
            adminEntity.setNickname("admin");
            adminEntity.setPassword(passwordEncoder.encode("heslo123"));
            adminEntity.setAdmin(true);
            adminRepository.save(adminEntity);
            System.out.println("Základní účet admina vytvořen.");
        }
    }
}