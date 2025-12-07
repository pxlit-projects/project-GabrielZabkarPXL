package be.pxl.services.dto;

import lombok.Data;

@Data
public class PostDTO {
    private Long id;
    private String title;
    private String author;
    private String status;
}
