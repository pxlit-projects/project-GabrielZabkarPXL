package be.pxl.services.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostDTO {
    private Long id;
    private String title;
    private String author;
    private String status;

    public PostDTO(long l, String t, String c, String a, LocalDateTime now, String published) {
    }
}
