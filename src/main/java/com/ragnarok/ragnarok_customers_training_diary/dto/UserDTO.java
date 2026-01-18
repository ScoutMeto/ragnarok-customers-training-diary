package com.ragnarok.ragnarok_customers_training_diary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserDTO {

    @JsonProperty("user_id")
    private Long userId;

    @Email
    private String userEmail;

    private String nickname;

    @JsonProperty("isAdmin")
    private boolean admin = false;

    @Size(min = 6, message = "Použij minimálně 6 znaků.")
    private String password;

}


