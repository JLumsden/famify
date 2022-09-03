package com.example.famify.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@NoArgsConstructor
@Data
public class SpotifyApiConfig {
    private final String apiUrl = "https://api.spotify.com";
}
