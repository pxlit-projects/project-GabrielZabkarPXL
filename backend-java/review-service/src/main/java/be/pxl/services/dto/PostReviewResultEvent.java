package be.pxl.services.dto;

import be.pxl.services.entity.ReviewDecision;

public record PostReviewResultEvent(
        Long postId,
        ReviewDecision decision,
        String reviewer,
        String comment
) {}
