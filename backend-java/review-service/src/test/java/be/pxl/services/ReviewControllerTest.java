package be.pxl.services;

import be.pxl.services.client.PostClient;
import be.pxl.services.controller.ReviewController;
import be.pxl.services.dto.PostDTO;
import be.pxl.services.entity.Review;
import be.pxl.services.entity.ReviewDecision;
import be.pxl.services.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private PostClient postClient;

    @Test
    void getReviewsForPost_asReviewer_returnsList() throws Exception {
        Review r1 = new Review();
        Review r2 = new Review();

        when(reviewService.getReviewsForPost(7L))
                .thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/reviews/post/7")
                        .header("X-User-Role", "REVIEWER"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getReviewsForPost_asEditor_returnsList() throws Exception {
        when(reviewService.getReviewsForPost(7L))
                .thenReturn(List.of(new Review()));

        mockMvc.perform(get("/api/reviews/post/7")
                        .header("X-User-Role", "EDITOR"))
                .andExpect(status().isOk());
    }

    @Test
    void getReviewsForPost_forbiddenForOtherRoles() throws Exception {
        mockMvc.perform(get("/api/reviews/post/7")
                        .header("X-User-Role", "VIEWER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void reviewPost_asReviewer_createsReview() throws Exception {
        Review saved = new Review();
        saved.setId(1L);
        when(reviewService.reviewPost(anyLong(), anyString(), any(), any()))
                .thenReturn(saved);

        mockMvc.perform(post("/api/reviews/10")
                        .param("decision", "APPROVED")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("looks good")
                        .header("X-User-Role", "REVIEWER")
                        .header("X-User-Name", "reviewer1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void reviewPost_rejectWithoutComment_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/reviews/10")
                        .param("decision", "REJECTED")
                        .header("X-User-Role", "REVIEWER")
                        .header("X-User-Name", "reviewer1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reviewPost_nonReviewer_forbidden() throws Exception {
        mockMvc.perform(post("/api/reviews/10")
                        .param("decision", "APPROVED")
                        .header("X-User-Role", "EDITOR")
                        .header("X-User-Name", "reviewer1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRequestedPosts_asReviewer_returnsListOfPostDto() throws Exception {
        PostDTO dto = new PostDTO(1L, "t", "c", "a", LocalDateTime.now(), "REQUESTED");
        when(reviewService.getRequestedPosts()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/reviews/requests")
                        .header("X-User-Role", "REVIEWER")
                        .header("X-User-Name", "reviewer1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getRequestedPosts_nonReviewer_forbidden() throws Exception {
        mockMvc.perform(get("/api/reviews/requests")
                        .header("X-User-Role", "EDITOR")
                        .header("X-User-Name", "editor1"))
                .andExpect(status().isForbidden());
    }
}
