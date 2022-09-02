package com.example.famify.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@NoArgsConstructor
@Data
public class SpotifyAuthConfig {
    private final String clientId = "";
    private final String redirectUrl = "http://localhost:8080/redirect";
    private final String authUrl = "https://accounts.spotify.com/api/token";
    private final String authScopes = "playlist-read-collaborative,playlist-modify-public,playlist-read-private,playlist-modify-private";
}
