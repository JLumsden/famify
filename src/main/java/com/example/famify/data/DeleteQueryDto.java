package com.example.famify.data;

import lombok.Data;

import java.util.List;

@Data
public class DeleteQueryDto {
    List<TrackDto> tracks;
}
