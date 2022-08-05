package com.example.famify.data;

import lombok.*;

@NoArgsConstructor
@Getter
@Setter
public class AuthData {
    private String code_verifier;
    private String code_challenge;
    private String access_token;
}
