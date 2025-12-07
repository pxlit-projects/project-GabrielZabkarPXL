package be.pxl.services.dto;

import be.pxl.services.entity.PostStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostDTO {
    private Long id;
    private String title;
    private String content;
    private String author;
    private LocalDateTime createdAt;
    private PostStatus status;
}

