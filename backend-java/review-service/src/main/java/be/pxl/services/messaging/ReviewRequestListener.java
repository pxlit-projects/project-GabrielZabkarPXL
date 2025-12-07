package be.pxl.services.messaging;

import be.pxl.services.client.PostClient;
import be.pxl.services.dto.PostDTO;
import be.pxl.services.dto.PostReviewRequestedEvent;
import be.pxl.services.dto.PostReviewResultEvent;
import be.pxl.services.entity.Review;
import be.pxl.services.entity.ReviewDecision;
import be.pxl.services.service.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReviewRequestListener {

    private static final Logger log = LoggerFactory.getLogger(ReviewRequestListener.class);

    public ReviewRequestListener() {
    }

    @RabbitListener(queues = "${app.rabbitmq.review-request-queue}")
    public void handleReviewRequest(PostReviewRequestedEvent event) {
        log.info("Received review request event for post {} (async-notificatie)", event.getPostId());
    }
}
