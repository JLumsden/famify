package com.example.famify.service;

import com.example.famify.config.SpotifyApiConfig;
import com.example.famify.data.DeleteQueryDto;
import com.example.famify.data.TrackDto;
import com.example.famify.repository.SpotifyApiRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.json.JsonParseException;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class PlaylistService {
    private SpotifyApiRepository spotifyApiRepository;
    private SpotifyApiConfig spotifyApiConfig;

    public String postPlaylistUrlDelegator(String accessToken, String playlistUrl) {
        String playlistId = parseForPlaylistId(playlistUrl);
        if (playlistId.equals("error")) {
            return playlistId;
        }

        ResponseEntity<String> response = getPlaylistItems(playlistId, accessToken);
        if (response.getStatusCodeValue() != 200) {
            return "error";
        }

        HttpEntity<String> entity = deleteQueryDtoToJsonEntity(
                parsePlaylistItemsJsonToDeleteQueryDto(response.getBody()), accessToken);

        response = deleteExplicitItems(entity, playlistId);
        if (response.getStatusCodeValue() != 200) {
            return "error";
        }

        return "redirect";
    }

    public String parseForPlaylistId(String playlistUrl) {
        String[] tokens = playlistUrl.split("/");

        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equals("playlist")) {
                String tag = tokens[i + 1];

                //Returns substring until first "?"
                //Ex: 6GMCSqe6k9qr6izzDoseQ1?si=351ba6ccb6374d12
                //Returns: 6GMCSqe6k9qr6izzDoseQ1
                //If queryIndex is -1, there is no "?"
                //Can still be valid in this case
                int queryIndex = tag.indexOf("?");

                if (queryIndex > 0) {
                    return tag.substring(0, queryIndex);
                } else if (queryIndex == -1){
                    return tag;
                }
            }
        }
        return "error";
    }

    public ResponseEntity<String> getPlaylistItems(String playlistId, String accessToken) {
        //fields=items(track(id,explicit))
        String url = apiUrlBuilder(playlistId);

        HttpEntity<Void> entity = new HttpEntity<>(createHeaders(accessToken));

        return spotifyApiRepository.get(url, entity, String.class);
    }

    public DeleteQueryDto parsePlaylistItemsJsonToDeleteQueryDto(String playlistItems) {
        ObjectMapper mapper = new ObjectMapper();
        DeleteQueryDto deleteQueryDto = new DeleteQueryDto();
        List<TrackDto> explicitList = new ArrayList<>();
        try {
            JsonNode rootNode = mapper.readTree(playlistItems);
            JsonNode itemsNode = rootNode.path("items");
            JsonNode trackNode;

            String trackUri;
            boolean isExplicit;
            for (JsonNode itemNode : itemsNode) {
                trackNode = itemNode.path("track");

                trackUri = trackNode.path("uri").asText();
                isExplicit = trackNode.path("explicit").asBoolean();

                if(isExplicit) {
                    explicitList.add(new TrackDto(trackUri));
                }
            }
        }
        catch (JsonParseException e) {
            e.printStackTrace();
        }
        catch (JsonMappingException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        deleteQueryDto.setTracks(explicitList);

        return deleteQueryDto;
    }

    public HttpEntity<String> deleteQueryDtoToJsonEntity(DeleteQueryDto deleteQueryDto, String accessToken) {

        ObjectMapper mapper = new ObjectMapper();

        String jsonString = null;
        try {
            jsonString = mapper.writeValueAsString(deleteQueryDto);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return new HttpEntity<String>(jsonString, createHeaders(accessToken));
    }

    public ResponseEntity<String> deleteExplicitItems(HttpEntity entity, String playlistId) {
        String url = apiUrlBuilder(playlistId);

        return spotifyApiRepository.delete(url, entity, String.class);
    }

    public HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        return headers;
    }

    public String apiUrlBuilder(String playlistId) {
        return spotifyApiConfig.getApiUrl() + "/v1/playlists/" + playlistId + "/tracks";
    }
}