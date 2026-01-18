package com.ragnarok.ragnarok_customers_training_diary.controller;

import com.ragnarok.ragnarok_customers_training_diary.dto.AdminDTO;
import com.ragnarok.ragnarok_customers_training_diary.service.AdminService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import lombok.Setter;
import lombok.Getter;

import java.io.IOException;

@Setter
@Getter
@RestController
public class AdminController {
    @Autowired
    AdminService adminService;

    @Autowired
    AuthenticationManager authenticationManager;


    ////////////////////////////////////////////////////////////////////////////////////////

    @PostMapping({"api/registrationNewAdmin/", "api/registrationNewAdmin"})
    public ResponseEntity<?> addAdmin(@RequestBody @Valid AdminDTO adminDTO) {
        adminService.createAdmin(adminDTO);
        return ResponseEntity.ok("Nový admin vytvořen");
    }

    ////////////////////////////////////////////////////////////////////////////////////////

    @PostMapping("/api/loginAdmin")
    public ResponseEntity<?> loginAdmin(@RequestBody AdminDTO adminDTO, HttpServletRequest request) {
        try {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(adminDTO.getAdminEmail(), adminDTO.getPassword());
            authenticationManager.authenticate(authToken);

            request.getSession(true);

            return ResponseEntity.ok("/index-adminPart.html");

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Přihlášení selhalo.");
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////

    // Aktuálně přihlášený uživatel?
    @GetMapping("/api/whoami")
    public ResponseEntity<AdminDTO> whoAmI(HttpServletRequest request){
        return adminService.getCurrentAdminInfo(request);
    }



    ////////////////////////////////////////////////////////////////////////////////////////

    @PostMapping({"api/logoutAdmin/", "api/logoutAdmin"})
    public void logout(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        adminService.logoutAdmin(request, response);
    }

}
    ////////////////////////////////////////////////////////////////////////////////////////