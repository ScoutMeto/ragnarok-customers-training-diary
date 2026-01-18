package com.ragnarok.ragnarok_customers_training_diary.service;

import com.ragnarok.ragnarok_customers_training_diary.dto.AdminDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;

public interface AdminService {

    AdminDTO createAdmin(AdminDTO adminDTO);

//    void loginAdmin(AdminDTO adminDTO, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;

    void logoutAdmin(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;

    ResponseEntity<AdminDTO> getCurrentAdminInfo(HttpServletRequest request);
}
