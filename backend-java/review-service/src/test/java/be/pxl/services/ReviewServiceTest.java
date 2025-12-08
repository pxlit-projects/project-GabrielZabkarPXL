package be.pxl.services;

import be.pxl.services.client.PostClient;
import be.pxl.services.dto.PostReviewResultEvent;
import be.pxl.services.entity.Review;
import be.pxl.services.entity.ReviewDecision;
import be.pxl.services.messaging.ReviewResultPublisher;
import be.pxl.services.repository.ReviewRepository;
import be.pxl.services.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private PostClient postClient;

    @Mock
    private ReviewResultPublisher reviewResultPublisher;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        // adapt constructor call to your actual ReviewService
        reviewService = new ReviewService(reviewRepository, postClient, reviewResultPublisher);
    }

    @Test
    void reviewPost_savesReviewAndUpdatesPostStatus() {
        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(invocation -> {
                    Review r = invocation.getArgument(0);
                    r.setId(1L);
                    return r;
                });

        Review result = reviewService.reviewPost(
                10L, "reviewer", ReviewDecision.APPROVED, "looks good");

        assertThat(result.getId()).isEqualTo(1L);

        verify(postClient).updatePostStatus(10L, "PUBLISHED");

        ArgumentCaptor<PostReviewResultEvent> eventCaptor =
                ArgumentCaptor.forClass(PostReviewResultEvent.class);
        verify(reviewResultPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().postId()).isEqualTo(10L);
    }

    @Test
    void getReviewsForPost_delegatesToRepository() {
        Review r1 = new Review();
        Review r2 = new Review();
        when(reviewRepository.findByPostIdOrderByCreatedAtDesc(5L))
                .thenReturn(List.of(r1, r2));

        List<Review> result = reviewService.getReviewsForPost(5L);

        assertThat(result).hasSize(2);
        verify(reviewRepository).findByPostIdOrderByCreatedAtDesc(5L);
    }
}
