package be.pxl.services.dto;

import java.time.LocalDateTime;

public record PostDTO(
        Long id,
        String title,
        String content,
        String author,
        LocalDateTime createdAt,
        String status
) {}
