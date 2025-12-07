package be.pxl.services.messaging;

import be.pxl.services.dto.PostReviewResultEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReviewResultPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReviewResultPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.review-result-routing-key}")
    private String reviewResultRoutingKey;

    public ReviewResultPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(PostReviewResultEvent event) {
        rabbitTemplate.convertAndSend(exchangeName, reviewResultRoutingKey, event);
        log.info("Sent review result event for post {}: decision={}, reviewer={}",
                event.postId(), event.decision(), event.reviewer());
    }
}
