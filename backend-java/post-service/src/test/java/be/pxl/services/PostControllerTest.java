package be.pxl.services;

import be.pxl.services.controller.PostController;
import be.pxl.services.entity.Post;
import be.pxl.services.entity.PostStatus;
import be.pxl.services.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostController.class)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostService postService;

    @Test
    void getAllPosts_asEditor_returnsList() throws Exception {
        Post p1 = new Post();
        Post p2 = new Post();

        when(postService.getAllPosts()).thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/api/posts")
                        .header("X-User-Role", "EDITOR"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getAllPosts_forbiddenForOtherRoles() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .header("X-User-Role", "VIEWER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPublishedPosts_returnsListWithoutRoleCheck() throws Exception {
        Post p1 = new Post();
        when(postService.getAllPublishedPosts()).thenReturn(List.of(p1));

        mockMvc.perform(get("/api/posts/published"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void submitPostForReview_asEditor_ok() throws Exception {
        Post post = new Post();
        post.setId(1L);
        post.setStatus(PostStatus.REQUESTED);

        when(postService.submitForReview(1L)).thenReturn(post);

        mockMvc.perform(put("/api/posts/1/submit")
                        .header("X-User-Role", "EDITOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void submitPostForReview_asNonEditor_forbidden() throws Exception {
        mockMvc.perform(put("/api/posts/1/submit")
                        .header("X-User-Role", "REVIEWER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRequestedPosts_asReviewer_returnsList() throws Exception {
        Post p = new Post();
        when(postService.getPostsByStatus(PostStatus.REQUESTED))
                .thenReturn(List.of(p));

        mockMvc.perform(get("/api/posts/requested")
                        .header("X-User-Role", "REVIEWER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getRequestedPosts_nonReviewer_forbidden() throws Exception {
        mockMvc.perform(get("/api/posts/requested")
                        .header("X-User-Role", "EDITOR"))
                .andExpect(status().isForbidden());
    }
}
