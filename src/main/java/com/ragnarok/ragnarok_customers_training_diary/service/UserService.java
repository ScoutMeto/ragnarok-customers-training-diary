package com.ragnarok.ragnarok_customers_training_diary.service;

import com.ragnarok.ragnarok_customers_training_diary.dto.AdminDTO;
import com.ragnarok.ragnarok_customers_training_diary.dto.UserDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public interface UserService {

    void createUser(UserDTO userDTO);

    ResponseEntity<UserDTO> getCurrentUserInfo(HttpServletRequest request);

    void logoutUser(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;

}

