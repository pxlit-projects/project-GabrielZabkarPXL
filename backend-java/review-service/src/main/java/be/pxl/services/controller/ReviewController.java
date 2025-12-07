package be.pxl.services.controller;

import be.pxl.services.dto.PostDTO;
import be.pxl.services.entity.Review;
import be.pxl.services.entity.ReviewDecision;
import be.pxl.services.service.ReviewService;
import be.pxl.services.client.PostClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    private static final String ROLE_REVIEWER = "REVIEWER";
    private static final String ROLE_EDITOR = "EDITOR";

    private final ReviewService reviewService;
    private final PostClient postClient;

    public ReviewController(ReviewService reviewService, PostClient postClient) {
        this.reviewService = reviewService;
        this.postClient = postClient;
    }

    private boolean isReviewer(String role) {
        return ROLE_REVIEWER.equalsIgnoreCase(role);
    }

    private boolean isEditor(String role) {
        return ROLE_EDITOR.equalsIgnoreCase(role);
    }

    @PostMapping("/{postId}")
    public ResponseEntity<?> reviewPost(
            @PathVariable Long postId,
            @RequestParam ReviewDecision decision,
            @RequestBody(required = false) String comment,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Name") String username) {

        if (!isReviewer(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (decision == ReviewDecision.REJECTED &&
                (comment == null || comment.isBlank())) {
            return ResponseEntity.badRequest().body("Comment is required when rejecting a post.");
        }

        Review review = reviewService.reviewPost(postId, username, decision, comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Review>> getReviewsForPost(
            @PathVariable Long postId,
            @RequestHeader("X-User-Role") String role) {

        if (!isEditor(role) && !isReviewer(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(reviewService.getReviewsForPost(postId));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<PostDTO>> getRequestedPosts(
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Name") String reviewerName) {

        if (!isReviewer(role)) {
            log.warn("Forbidden getRequestedPosts attempt by role={}", role);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<PostDTO> requested = reviewService.getRequestedPosts();
        return ResponseEntity.ok(requested);
    }
}
