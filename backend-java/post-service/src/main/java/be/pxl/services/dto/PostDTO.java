package be.pxl.services.dto;

import be.pxl.services.entity.Post;
import be.pxl.services.entity.PostStatus;
import be.pxl.services.entity.ReviewDecision;
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
    private ReviewDecision lastReviewDecision;
    private String lastReviewComment;
    private String lastReviewReviewer;
    private LocalDateTime lastReviewedAt;

    public PostDTO(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.author = post.getAuthor();
        this.createdAt = post.getCreatedAt();
        this.status = post.getStatus();

        this.lastReviewDecision = post.getLastReviewDecision();
        this.lastReviewComment = post.getLastReviewComment();
        this.lastReviewReviewer = post.getLastReviewReviewer();
        this.lastReviewedAt = post.getLastReviewedAt();
    }
}



