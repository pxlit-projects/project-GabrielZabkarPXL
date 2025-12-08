package be.pxl.services;

import be.pxl.services.dto.PostDTO;
import be.pxl.services.dto.PostReviewRequestedEvent;
import be.pxl.services.entity.Post;
import be.pxl.services.entity.PostStatus;
import be.pxl.services.repository.PostRepository;
import be.pxl.services.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, rabbitTemplate);

        // fields injected with \@Value in production
        ReflectionTestUtils.setField(postService,
                "exchangeName", "post.review.exchange");
        ReflectionTestUtils.setField(postService,
                "reviewRequestRoutingKey", "post.review.request");
    }

    @Test
    void createPost_setsDraftStatusAndSaves() {
        Post post = new Post();
        post.setTitle("Title");
        post.setContent("Content");
        post.setAuthor("gabriel");

        when(postRepository.save(any(Post.class)))
                .thenAnswer(invocation -> {
                    Post p = invocation.getArgument(0);
                    p.setId(1L);
                    return p;
                });

        Post result = postService.createPost(post);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(PostStatus.DRAFT);
        assertThat(result.getCreatedAt()).isNotNull();

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PostStatus.DRAFT);
    }

    @Test
    void getPostById_existing_returnsPost() {
        Post post = new Post();
        post.setId(42L);
        when(postRepository.findById(42L)).thenReturn(Optional.of(post));

        Post result = postService.getPostById(42L);

        assertThat(result).isSameAs(post);
    }

    @Test
    void getPostById_notFound_throwsRuntimeException() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Post not found");
    }

    @Test
    void updateStatus_changesStatusAndSaves() {
        Post post = new Post();
        post.setId(1L);
        post.setStatus(PostStatus.DRAFT);

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Post result = postService.updateStatus(1L, PostStatus.REQUESTED);

        assertThat(result.getStatus()).isEqualTo(PostStatus.REQUESTED);
        verify(postRepository).save(post);
    }

    @Test
    void submitForReview_fromDraft_setsRequestedAndSendsEvent() {
        Post post = new Post();
        post.setId(7L);
        post.setAuthor("gabriel");
        post.setTitle("Title");
        post.setContent("Content");
        post.setStatus(PostStatus.DRAFT);

        when(postRepository.findById(7L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Post result = postService.submitForReview(7L);

        assertThat(result.getStatus()).isEqualTo(PostStatus.REQUESTED);

        ArgumentCaptor<PostReviewRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(PostReviewRequestedEvent.class);

        verify(rabbitTemplate).convertAndSend(
                eq("post.review.exchange"),
                eq("post.review.request"),
                eventCaptor.capture()
        );

        PostReviewRequestedEvent event = eventCaptor.getValue();
        assertThat(event.getPostId()).isEqualTo(7L);
        assertThat(event.getAuthor()).isEqualTo("gabriel");
    }

    @Test
    void submitForReview_notFromDraft_throws() {
        Post post = new Post();
        post.setId(7L);
        post.setStatus(PostStatus.PUBLISHED);

        when(postRepository.findById(7L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.submitForReview(7L))
                .isInstanceOf(IllegalStateException.class);
        verify(postRepository, never()).save(any());
    }

    @Test
    void getAllPosts_returnsAll() {
        when(postRepository.findAll()).thenReturn(List.of(new Post(), new Post()));

        List<Post> result = postService.getAllPosts();

        assertThat(result).hasSize(2);
    }

    @Test
    void getAllPublishedPosts_usesRepository() {
        Post p1 = new Post();
        p1.setStatus(PostStatus.PUBLISHED);
        when(postRepository.findByStatus(PostStatus.PUBLISHED))
                .thenReturn(List.of(p1));

        List<Post> result = postService.getAllPublishedPosts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(PostStatus.PUBLISHED);
    }

    @Test
    void searchPosts_filtersByTextAuthorAndDate() {
        LocalDateTime now = LocalDateTime.now();

        Post p1 = new Post();
        p1.setTitle("Spring Boot Tips");
        p1.setContent("Content 1");
        p1.setAuthor("gabriel");
        p1.setCreatedAt(now.minusDays(1));
        p1.setStatus(PostStatus.PUBLISHED);

        Post p2 = new Post();
        p2.setTitle("Other");
        p2.setContent("Different");
        p2.setAuthor("john");
        p2.setCreatedAt(now.minusDays(10));
        p2.setStatus(PostStatus.PUBLISHED);

        when(postRepository.findByStatus(PostStatus.PUBLISHED))
                .thenReturn(List.of(p1, p2));

        List<Post> result = postService.searchPosts(
                "Spring", "gab", LocalDate.now().minusDays(2), LocalDate.now());

        assertThat(result).containsExactly(p1);
    }

    @Test
    void getReviewNotifications_mapsToDto() {
        Post p1 = new Post();
        p1.setId(1L);
        p1.setAuthor("gabriel");
        p1.setStatus(PostStatus.PUBLISHED);
        p1.setCreatedAt(LocalDateTime.now());

        when(postRepository
                .findByAuthorAndLastReviewDecisionIsNotNullOrderByLastReviewedAtDesc("gabriel"))
                .thenReturn(List.of(p1));

        List<PostDTO> result = postService.getReviewNotifications("gabriel");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }
}
