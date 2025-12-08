package be.pxl.services.messaging;

import be.pxl.services.dto.PostReviewResultEvent;
import be.pxl.services.entity.Post;
import be.pxl.services.entity.PostStatus;
import be.pxl.services.entity.ReviewDecision;
import be.pxl.services.repository.PostRepository;
import be.pxl.services.service.PostService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class ReviewResultListener {

    private final PostRepository postRepository;

    public ReviewResultListener(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @RabbitListener(queues = "${app.rabbitmq.review-result-queue}")
    public void handleReviewResult(PostReviewResultEvent event) {
        log.info("Received review result event for post {}: decision={}, reviewer={}, comment='{}'",
                event.postId(), event.decision(), event.reviewer(), event.comment());

        Post post = postRepository.findById(event.postId())
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + event.postId()));

        if (event.decision() == ReviewDecision.APPROVED) {
            post.setStatus(PostStatus.PUBLISHED);
        } else if (event.decision() == ReviewDecision.REJECTED) {
            post.setStatus(PostStatus.REJECTED);
        }

        post.setLastReviewDecision(event.decision());
        post.setLastReviewComment(event.comment());
        post.setLastReviewReviewer(event.reviewer());
        post.setLastReviewedAt(LocalDateTime.now());

        postRepository.save(post);

        log.info("Post {} status changed to {}, lastReviewDecision={}, lastReviewComment='{}'",
                post.getId(), post.getStatus(), post.getLastReviewDecision(), post.getLastReviewComment());
    }
}

