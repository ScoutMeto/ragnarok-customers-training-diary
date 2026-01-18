package com.ragnarok.ragnarok_customers_training_diary.service;

import com.ragnarok.ragnarok_customers_training_diary.dto.AdminDTO;
import com.ragnarok.ragnarok_customers_training_diary.dto.UserDTO;
import com.ragnarok.ragnarok_customers_training_diary.dto.mapper.UserMapper;
import com.ragnarok.ragnarok_customers_training_diary.entity.AdminEntity;
import com.ragnarok.ragnarok_customers_training_diary.entity.repository.UserRepository;
import com.ragnarok.ragnarok_customers_training_diary.security.UserUserDetailsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.Principal;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserMapper userMapper;
    @Autowired
    UserRepository userRepository;

    @Override
    public void createUser(UserDTO userDTO) {
        try {
        userRepository.save(userMapper.toEntity(userDTO));

    } catch (
    DataIntegrityViolationException dataIntegrityViolationException) {
        throw new com.ragnarok.ragnarok_customers_training_diary.exceptionHandler.DuplicateAdminEmailRegistrationException();
    }

    }

    @Override
    public void logoutUser(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.logout();
        response.sendRedirect("/index.html");
    }

    @Override
    public ResponseEntity<UserDTO> getCurrentUserInfo(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();

        if (principal instanceof AdminEntity user) {
            UserDTO dto = new UserDTO();
            dto.setUserEmail(user.getAdminEmail());
            dto.setUserId(user.getAdminId());
            dto.setAdmin(user.isAdmin());

            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

}
