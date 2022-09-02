package com.example.famify.service;

import com.example.famify.config.SpotifyAuthConfig;
import com.example.famify.data.AuthData;
import com.example.famify.repository.SpotifyApiRepository;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.json.JsonParseException;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Service
@AllArgsConstructor
public class SpotifyAuthBuilderService {
    private final SpotifyAuthConfig spotifyAuthConfig;
    private final SpotifyApiRepository spotifyApiRepository;

    public String getAccessTokenDelegator(AuthData authData, String authCode) {
        ResponseEntity<String> response = getAccessToken(authData, authCode);
        if(response.getStatusCodeValue() != 200) {
            return "error";
        }

        return parseJsonForAccessToken(response.getBody(), authData);
    }

    public String getAuthUrl(AuthData authData) {
        return "https://accounts.spotify.com/en/authorize?client_id=" + spotifyAuthConfig.getClientId()
                + "&response_type=code&redirect_uri=" + spotifyAuthConfig.getRedirectUrl()
                + "&code_challenge_method=S256&code_challenge=" + authData.getCode_challenge()
                + "&scope=" + spotifyAuthConfig.getAuthScopes()
                + "&show_dialog=true";
    }

    public ResponseEntity<String> getAccessToken(AuthData authData, String code) {
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(createAuthPayload(authData, code), createHeaders());

        return spotifyApiRepository.post(spotifyAuthConfig.getAuthUrl(), request, String.class);
    }

    public void createCodeVerifier(AuthData authData) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] codeVerifier = new byte[32];
        secureRandom.nextBytes(codeVerifier);
        authData.setCode_verifier(Base64.getUrlEncoder()
                .withoutPadding().encodeToString(codeVerifier));
    }

    public void createCodeChallenge(AuthData authData) {
        byte[] digest = null;
        try {
            byte[] bytes = authData.getCode_verifier().getBytes("US-ASCII");
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(bytes, 0, bytes.length);
            digest = messageDigest.digest();
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException exception) {
            log.error("Unable to generate code challenge {}", exception);
        }
        authData.setCode_challenge(Base64.getUrlEncoder().withoutPadding().encodeToString(digest));
    }

    //TODO Change to utilize jackson and probably data object
    public MultiValueMap<String, String> createAuthPayload(AuthData authData, String code) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", spotifyAuthConfig.getClientId());
        map.add("grant_type", "authorization_code");
        map.add("code", code);
        map.add("redirect_uri", spotifyAuthConfig.getRedirectUrl());
        map.add("code_verifier", authData.getCode_verifier());

        return map;
    }

    public HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        return headers;
    }

    public String parseJsonForAccessToken(String response, AuthData authData) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);
            authData.setAccess_token(rootNode.path("access_token").asText());
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "redirect";
    }
}
