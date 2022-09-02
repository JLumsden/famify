package com.example.famify.controller;

import com.example.famify.data.AuthData;
import com.example.famify.service.PlaylistService;
import com.example.famify.service.SpotifyAuthBuilderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@SessionAttributes("authData")
@Controller
@AllArgsConstructor
@Slf4j
public class FamifyController {

    private SpotifyAuthBuilderService spotifyAuthBuilderService;
    private PlaylistService playlistService;

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
            return spotifyAuthBuilderService.parseJsonForAccessToken(response.getBody(), authData);
        } else {
            return "error";
        }
    }

    /*
    postPlaylistUrl
    Calls delegator to parse for playlistId, get explicit playlist items and delete said items
     */
    @RequestMapping(value = "/playlist", method = RequestMethod.POST)
    public String postPlaylistUrl(@ModelAttribute("authData") AuthData authData, String playlistUrl) {
        return playlistService.postPlaylistUrlDelegator(authData.getAccess_token(), playlistUrl);
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
