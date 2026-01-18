package com.ragnarok.ragnarok_customers_training_diary.security;

import com.ragnarok.ragnarok_customers_training_diary.entity.AdminEntity;
import com.ragnarok.ragnarok_customers_training_diary.entity.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("adminUserDetailsService")
public class AdminUserDetailsService implements UserDetailsService {

    @Autowired
    AdminRepository adminRepository;

    public AdminUserDetailsService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String adminEmail) throws UsernameNotFoundException {
        AdminEntity admin = adminRepository.findByAdminEmail(adminEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found with email: " + adminEmail));

        return User.builder()
                .username(admin.getAdminEmail())
                .password(admin.getPassword())
                .roles("ADMIN")
                .build();
    }
}
