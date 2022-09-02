package com.example.famify.repository;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class SpotifyApiRepository {
    private final RestTemplate restTemplate = new RestTemplate();
    public <T, R> ResponseEntity<T> get(String url, HttpEntity<R> entity, Class<T> clazz) {
        return restTemplate.exchange(url, HttpMethod.GET, entity, clazz);
    }

    public <T, R> ResponseEntity<T> post(String url, HttpEntity<R> entity, Class<T> clazz) {
        return restTemplate.exchange(url, HttpMethod.POST, entity, clazz);
    }

    public <T, R> ResponseEntity<T> put(String url, HttpEntity<R> entity, Class<T> clazz) {
        return restTemplate.exchange(url, HttpMethod.PUT, entity, clazz);
    }

    public <T, R> ResponseEntity<T> delete(String url, HttpEntity<R> entity, Class<T> clazz) {
        return restTemplate.exchange(url, HttpMethod.DELETE, entity, clazz);
    }
}
