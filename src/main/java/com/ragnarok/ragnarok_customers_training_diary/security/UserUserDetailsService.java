package com.ragnarok.ragnarok_customers_training_diary.security;

import com.ragnarok.ragnarok_customers_training_diary.entity.UserEntity;
import com.ragnarok.ragnarok_customers_training_diary.entity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("userUserDetailsService")
public class UserUserDetailsService implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

    public UserUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String userEmail) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));

        return User.builder()
                .username(user.getUserEmail())
                .password(user.getPassword())
                .roles("USER")
                .build();
    }
}