package com.example.famify.service;

import com.example.famify.data.DeleteQueryDto;
import com.example.famify.data.TrackDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.json.JsonParseException;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class PlaylistService {

    RestTemplate restTemplate = new RestTemplate();

    public String parseForPlaylistId(String playlistUrl) {
        String[] tokens = playlistUrl.split("/");
        int i;
        for (i = 0; i < tokens.length; i++) {
            if (tokens[i].equals("playlist")) {
                String tag = tokens[i + 1];
                int queryIndex = tag.indexOf("?");

                //Returns substring until first "?"
                //Ex: 6GMCSqe6k9qr6izzDoseQ1?si=351ba6ccb6374d12
                //Returns: 6GMCSqe6k9qr6izzDoseQ1
                //If queryIndex is -1, there is no "?"
                //Can still be valid in this case
                if (queryIndex > 0) {
                    return tag.substring(0, queryIndex);
                } else if (queryIndex == -1){
                    return tag;
                }
            }
        }
        return "bad link";
    }

    public ResponseEntity getPlaylistItems(String playlistId, String accessToken) {
        //fields=items(track(id,explicit))
        String url = "https://api.spotify.com/v1/playlists/" + playlistId + "/tracks";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        return response;
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
            Boolean isExplicit;
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

    public HttpEntity deleteQueryDtoToJsonEntity(DeleteQueryDto deleteQueryDto, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        ObjectMapper mapper = new ObjectMapper();

        String jsonString = null;
        try {
            jsonString = mapper.writeValueAsString(deleteQueryDto);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        HttpEntity<String> entity = new HttpEntity<String>(jsonString, headers);

        return entity;
    }

    public ResponseEntity deleteExplicitItems(HttpEntity entity, String playlistId) {
        String url = "https://api.spotify.com/v1/playlists/" + playlistId + "/tracks";

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);

        return response;
    }
}