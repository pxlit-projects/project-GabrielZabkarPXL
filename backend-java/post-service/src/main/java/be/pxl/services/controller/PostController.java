package be.pxl.services.controller;

import be.pxl.services.dto.PostDTO;
import be.pxl.services.entity.Post;
import be.pxl.services.entity.PostStatus;
import be.pxl.services.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private static final Logger log = LoggerFactory.getLogger(PostController.class);

    private static final String ROLE_EDITOR = "EDITOR";
    private static final String ROLE_REVIEWER = "REVIEWER";

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    private boolean isEditor(String role) {
        return ROLE_EDITOR.equalsIgnoreCase(role);
    }

    private boolean isReviewer(String role) {
        return ROLE_REVIEWER.equalsIgnoreCase(role);
    }

    private boolean isEditorOrReviewer(String role) {
        return ROLE_EDITOR.equalsIgnoreCase(role) || ROLE_REVIEWER.equalsIgnoreCase(role);
    }

    @PostMapping
    public ResponseEntity<Post> createPost(
            @RequestBody Post post,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Name") String username) {

        if (!isEditor(role)) {
            log.warn("Forbidden createPost attempt by role={}", role);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        post.setAuthor(username);

        Post created = postService.createPost(post);
        log.info("Post with id={} created by editor={}", created.getId(), username);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePost(
            @PathVariable Long id,
            @RequestBody Post post,
            @RequestHeader("X-User-Role") String role) {

        if (!isEditor(role)) {
            log.warn("Forbidden updatePost attempt by role={} for id={}", role, id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Post updated = postService.updatePost(id, post);
            log.info("Post with id={} updated", id);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            log.warn("Conflict updating post id={}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Post>> getAllPosts(
            @RequestHeader("X-User-Role") String role) {

        if (!isEditorOrReviewer(role)) {
            log.warn("Forbidden getAllPosts attempt by role={}", role);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(postService.getAllPosts());
    }

    @GetMapping("/published")
    public ResponseEntity<List<Post>> getPublishedPosts() {
        return ResponseEntity.ok(postService.getAllPublishedPosts());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Post>> searchPosts(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) String author,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<Post> result = postService.searchPosts(text, author, from, to);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String role) {

        if (!isEditor(role)) {
            log.warn("Forbidden deletePost attempt by role={} for id={}", role, id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        postService.deletePost(id);
        log.info("Post with id={} deleted", id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/submit")
    public ResponseEntity<Post> submitPostForReview(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String role) {

        if (!isEditor(role)) {
            log.warn("Forbidden submitPost attempt by role={} for id={}", role, id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Post updated = postService.submitForReview(id);
            log.info("Post id={} submitted for review (status REQUESTED + event)", id);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping("/requested")
    public ResponseEntity<List<Post>> getRequestedPosts(
            @RequestHeader("X-User-Role") String role) {

        if (!isReviewer(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(postService.getPostsByStatus(PostStatus.REQUESTED));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Post> updateStatusInternal(
            @PathVariable Long id,
            @RequestParam PostStatus status) {

        Post updated = postService.updateStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/internal/{id}")
    public ResponseEntity<Post> getPostInternal(@PathVariable Long id) {
        Post post = postService.getPostById(id);
        return ResponseEntity.ok(post);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Post> getPostById(@PathVariable Long id) {
        Post post = postService.getPostById(id);
        return ResponseEntity.ok(post);
    }

    @GetMapping("/notifications")
    public List<PostDTO> getReviewNotifications(@RequestParam String editor) {
        return postService.getReviewNotifications(editor);
    }

}
