package be.pxl.services.messaging;

import be.pxl.services.dto.PostReviewResultEvent;
import be.pxl.services.entity.ReviewDecision;
import be.pxl.services.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReviewResultListener {

    private static final Logger log = LoggerFactory.getLogger(ReviewResultListener.class);

    private final PostService postService;

    public ReviewResultListener(PostService postService) {
        this.postService = postService;
    }

    @RabbitListener(queues = "${app.rabbitmq.review-result-queue}")
    public void handleReviewResult(PostReviewResultEvent event) {
        log.info("Received review result event for post {}: decision={}, reviewer={}, comment='{}'",
                event.postId(), event.decision(), event.reviewer(), event.comment());
    }
}
