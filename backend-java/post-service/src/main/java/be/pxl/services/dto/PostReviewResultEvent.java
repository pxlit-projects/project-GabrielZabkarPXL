package be.pxl.services.dto;

import be.pxl.services.entity.ReviewDecision;

import java.time.Instant;

public record PostReviewResultEvent(
        Long postId,
        String title,
        String editor,
        String reviewer,
        ReviewDecision decision,
        String comment,
        Instant reviewedAt
) {}
