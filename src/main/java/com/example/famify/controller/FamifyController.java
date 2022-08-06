package com.example.famify.controller;

import com.example.famify.data.AuthData;
import com.example.famify.service.PlaylistService;
import com.example.famify.service.SpotifyAuthBuilderService;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.json.JsonParseException;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

@SessionAttributes("authData")
@Controller
@AllArgsConstructor
@Slf4j
public class FamifyController {

    SpotifyAuthBuilderService spotifyAuthBuilderService;
    PlaylistService playlistService;

    @RequestMapping(value = "/auth", method = RequestMethod.GET)
    public ModelAndView authRedirect(@ModelAttribute("authData") AuthData authData) {
        spotifyAuthBuilderService.createCodeVerifier(authData);
        spotifyAuthBuilderService.createCodeChallenge(authData);
        return new ModelAndView("redirect:" + spotifyAuthBuilderService.getAuthUrl(authData));
    }

    @RequestMapping(value = "/redirect", method = RequestMethod.GET)
    public String getRedirect(@ModelAttribute("authData") AuthData authData, @RequestParam(value = "code") final String authCode) {
        ResponseEntity<String> response = spotifyAuthBuilderService.getAccessToken(authData, authCode);

        int status = response.getStatusCodeValue();
        if (status == 200) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response.getBody());
                authData.setAccess_token(rootNode.path("access_token").asText());
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
            return "redirect";
        } else {
            return "error";
        }
    }

    @RequestMapping(value = "/playlist", method = RequestMethod.POST)
    public String postPlaylistUrl(@ModelAttribute("authData") AuthData authData, String playlistUrl) {
        String playlistId = playlistService.parseForPlaylistId(playlistUrl);
        if (playlistId.equals("bad link")) {
            return "error";
        }

        ResponseEntity<String> getResponse = playlistService.getPlaylistItems(playlistId, authData.getAccess_token());

        if (getResponse.getStatusCodeValue() != 200) {
            return "error";
        }

        HttpEntity<String> entity = playlistService.deleteQueryDtoToJsonEntity(
                playlistService.parsePlaylistItemsJsonToDeleteQueryDto(getResponse.getBody()), authData.getAccess_token());

        ResponseEntity<String> deleteResponse = playlistService.deleteExplicitItems(entity, playlistId);

        if (deleteResponse.getStatusCodeValue() == 200) {
            return "success";
        } else {
            return "error";
        }
    }

    @RequestMapping(value = "/error", method = RequestMethod.GET)
    public String getError() {
        return "error";
    }

    @ModelAttribute("authData")
    public AuthData getAuthData() {
        return new AuthData();
    }
}
