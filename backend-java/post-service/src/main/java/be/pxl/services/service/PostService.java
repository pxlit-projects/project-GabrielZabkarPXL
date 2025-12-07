package be.pxl.services.service;

import be.pxl.services.dto.PostReviewRequestedEvent;
import be.pxl.services.entity.Post;
import be.pxl.services.entity.PostStatus;
import be.pxl.services.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.review-request-routing-key}")
    private String reviewRequestRoutingKey;

    public PostService(PostRepository postRepository,
                       RabbitTemplate rabbitTemplate) {
        this.postRepository = postRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public Post createPost(Post post) {
        post.setId(null);
        post.setCreatedAt(LocalDateTime.now());
        post.setStatus(PostStatus.DRAFT);

        Post saved = postRepository.save(post);
        log.info("Created new post with id={} and status={}", saved.getId(), saved.getStatus());
        return saved;
    }

    public Post updatePost(Long id, Post updated) {
        Post existing = getPostById(id);

        if (existing.getStatus() != PostStatus.DRAFT &&
                existing.getStatus() != PostStatus.REJECTED) {

            log.warn("Attempt to edit post id={} in status={}", id, existing.getStatus());
            throw new IllegalStateException("Post can only be edited in DRAFT or REJECTED state");
        }

        if (updated.getTitle() != null) {
            existing.setTitle(updated.getTitle());
        }
        if (updated.getContent() != null) {
            existing.setContent(updated.getContent());
        }
        if (updated.getAuthor() != null) {
            existing.setAuthor(updated.getAuthor());
        }
        if (existing.getStatus() == PostStatus.REJECTED) {
            existing.setStatus(PostStatus.DRAFT);
        }

        Post saved = postRepository.save(existing);
        log.info("Post with id={} updated; new status={}", saved.getId(), saved.getStatus());
        return saved;
    }

    public List<Post> getAllPosts() {
        log.debug("Fetching all posts (including drafts)");
        return postRepository.findAll();
    }

    public List<Post> getPostsByStatus(PostStatus status) {
        return postRepository.findByStatus(status);
    }

    public List<Post> getAllPublishedPosts() {
        log.debug("Fetching all PUBLISHED posts");
        return postRepository.findByStatus(PostStatus.PUBLISHED);
    }

    public void deletePost(Long id) {
        log.info("Deleting post with id={}", id);
        postRepository.deleteById(id);
    }

    public List<Post> searchPosts(String text, String author,
                                  LocalDate from, LocalDate to) {

        List<Post> posts = postRepository.findByStatus(PostStatus.PUBLISHED);

        return posts.stream()
                .filter(p -> text == null || text.isBlank()
                        || p.getTitle().toLowerCase().contains(text.toLowerCase())
                        || p.getContent().toLowerCase().contains(text.toLowerCase()))
                .filter(p -> author == null || author.isBlank()
                        || p.getAuthor().toLowerCase().contains(author.toLowerCase()))
                .filter(p -> {
                    if (from == null && to == null) {
                        return true;
                    }
                    LocalDate date = p.getCreatedAt().toLocalDate();
                    boolean afterFrom = from == null || !date.isBefore(from);
                    boolean beforeTo = to == null || !date.isAfter(to);
                    return afterFrom && beforeTo;
                })
                .toList();
    }

    public Post getPostById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
    }

    public Post updateStatus(Long id, PostStatus newStatus) {
        Post existing = getPostById(id);
        existing.setStatus(newStatus);
        Post saved = postRepository.save(existing);
        log.info("Post {} status changed to {}", saved.getId(), saved.getStatus());
        return saved;
    }

    public Post submitForReview(Long id) {
        Post post = getPostById(id);

        if (post.getStatus() != PostStatus.DRAFT) {
            throw new IllegalStateException("Post kan enkel vanuit DRAFT ingediend worden.");
        }

        post.setStatus(PostStatus.REQUESTED);
        Post saved = postRepository.save(post);

        PostReviewRequestedEvent event = new PostReviewRequestedEvent(
                saved.getId(),
                saved.getTitle(),
                saved.getContent(),
                saved.getAuthor()
        );

        log.info("Sending review request event for post {}", saved.getId());
        rabbitTemplate.convertAndSend(exchangeName, reviewRequestRoutingKey, event);

        return saved;
    }
}
