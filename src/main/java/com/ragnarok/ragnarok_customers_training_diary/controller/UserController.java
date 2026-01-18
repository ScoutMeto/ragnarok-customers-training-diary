package com.ragnarok.ragnarok_customers_training_diary.controller;

import com.ragnarok.ragnarok_customers_training_diary.dto.AdminDTO;
import com.ragnarok.ragnarok_customers_training_diary.dto.UserDTO;
import com.ragnarok.ragnarok_customers_training_diary.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Setter
@Getter
@RestController
public class UserController {

    @Autowired
    UserService userService;


    @Autowired
    AuthenticationManager authenticationManager;

    ////////////////////////////////////////////////////////////////////////////////////////

    @PostMapping({"api/registrationNewUser"})
    public ResponseEntity<?> registrationNewUser(@RequestBody @Valid UserDTO userDTO) {
        userService.createUser(userDTO);
        return ResponseEntity.ok("Uživatel vytvořen.");
    }

    ////////////////////////////////////////////////////////////////////////////////////////

    @PostMapping("/api/loginUser")
    public ResponseEntity<?> loginUser(@RequestBody UserDTO userDTO, HttpServletRequest request) {
        try {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDTO.getUserEmail(), userDTO.getPassword());
            authenticationManager.authenticate(authToken);

            request.getSession(true);

            return ResponseEntity.ok("/index-userPart.html");

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Přihlášení selhalo.");
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////

    // Aktuálně přihlášený uživatel?
    @GetMapping("/api/whoami")
    public ResponseEntity<UserDTO> whoAmI(HttpServletRequest request){
        return userService.getCurrentUserInfo(request);
    }

    ////////////////////////////////////////////////////////////////////////////////////////

    @PostMapping({"api/logoutUser/", "api/logoutUser"})
    public void logout(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        userService.logoutUser(request, response);
    }

    ////////////////////////////////////////////////////////////////////////////////////////

}


