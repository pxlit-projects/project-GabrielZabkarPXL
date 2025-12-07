package be.pxl.services.service;

import be.pxl.services.client.PostClient;
import be.pxl.services.dto.PostDTO;
import be.pxl.services.entity.Review;
import be.pxl.services.entity.ReviewDecision;
import be.pxl.services.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final PostClient postClient;

    public ReviewService(ReviewRepository reviewRepository, PostClient postClient) {
        this.reviewRepository = reviewRepository;
        this.postClient = postClient;
    }

    public Review reviewPost(Long postId, String reviewer,
                             ReviewDecision decision, String comment) {

        Review review = Review.builder()
                .postId(postId)
                .reviewer(reviewer)
                .decision(decision)
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .build();

        Review saved = reviewRepository.save(review);

        String newStatus = (decision == ReviewDecision.APPROVED)
                ? "PUBLISHED"
                : "REJECTED";

        postClient.updatePostStatus(postId, newStatus);
        log.info("Post {} reviewed as {}", postId, newStatus);

        return saved;
    }

    public List<Review> getReviewsForPost(Long postId) {
        return reviewRepository.findByPostIdOrderByCreatedAtDesc(postId);
    }

    public List<PostDTO> getRequestedPosts() {
        return postClient.getRequestedPosts("REVIEWER");
    }
}
