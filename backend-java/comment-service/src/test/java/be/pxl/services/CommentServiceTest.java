package be.pxl.services;

import be.pxl.services.client.PostClient;
import be.pxl.services.dto.PostDTO;
import be.pxl.services.entity.Comment;
import be.pxl.services.repository.CommentRepository;
import be.pxl.services.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostClient postClient;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentRepository, postClient);
    }

    private PostDTO publishedPostDto() {
        // record from review-service: (Long id, String title, String content, String author, LocalDateTime createdAt, String status)
        return new PostDTO(1L, "t", "c", "a", LocalDateTime.now(), "PUBLISHED");
    }

    @Test
    void addComment_postNotPublished_throwsIllegalState() {
        Long postId = 1L;

        PostDTO notPublished = new PostDTO(1L, "t", "c", "a", LocalDateTime.now(), "DRAFT");
        when(postClient.getPostByIdInternal(postId)).thenReturn(notPublished);

        assertThatThrownBy(() -> commentService.addComment(postId, "gabriel", "test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Post bestaat niet of is niet gepubliceerd");

        verify(commentRepository, never()).save(any());
    }

    @Test
    void addComment_postNull_throwsIllegalState() {
        Long postId = 1L;
        when(postClient.getPostByIdInternal(postId)).thenReturn(null);

        assertThatThrownBy(() -> commentService.addComment(postId, "gabriel", "test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Post bestaat niet of is niet gepubliceerd");

        verify(commentRepository, never()).save(any());
    }

    @Test
    void getCommentsForPost_returnsCommentsOrdered() {
        Long postId = 1L;
        Comment c1 = new Comment();
        Comment c2 = new Comment();

        when(commentRepository.findByPostIdOrderByCreatedAtAsc(postId))
                .thenReturn(List.of(c1, c2));

        List<Comment> result = commentService.getCommentsForPost(postId);

        assertThat(result).containsExactly(c1, c2);
        verify(commentRepository).findByPostIdOrderByCreatedAtAsc(postId);
    }

    @Test
    void updateComment_correctAuthor_updatesAndSaves() {
        Long id = 10L;
        String author = "gabriel";
        String newText = "Bijgewerkte comment";

        Comment existing = new Comment();
        existing.setId(id);
        existing.setAuthor(author);

        when(commentRepository.findById(id)).thenReturn(Optional.of(existing));
        when(commentRepository.save(existing)).thenReturn(existing);

        Comment result = commentService.updateComment(id, author, newText);

        assertThat(result).isSameAs(existing);
        assertThat(existing.getContent()).isEqualTo(newText);
        assertThat(existing.getUpdatedAt()).isNotNull();
        verify(commentRepository).findById(id);
        verify(commentRepository).save(existing);
    }

    @Test
    void updateComment_wrongAuthor_throwsException() {
        Long id = 10L;
        Comment existing = new Comment();
        existing.setId(id);
        existing.setAuthor("iemandAnders");

        when(commentRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> commentService.updateComment(id, "gabriel", "tekst"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Je mag enkel je eigen comments aanpassen");

        verify(commentRepository).findById(id);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void updateComment_unknownComment_throwsException() {
        Long id = 99L;
        when(commentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.updateComment(id, "gabriel", "tekst"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Comment niet gevonden");

        verify(commentRepository).findById(id);
    }

    @Test
    void deleteComment_correctAuthor_deletes() {
        Long id = 5L;
        Comment existing = new Comment();
        existing.setId(id);
        existing.setAuthor("gabriel");

        when(commentRepository.findById(id)).thenReturn(Optional.of(existing));

        commentService.deleteComment(id, "gabriel");

        verify(commentRepository).findById(id);
        verify(commentRepository).delete(existing);
    }

    @Test
    void deleteComment_wrongAuthor_throwsException() {
        Long id = 5L;
        Comment existing = new Comment();
        existing.setId(id);
        existing.setAuthor("ander");

        when(commentRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> commentService.deleteComment(id, "gabriel"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Je mag enkel je eigen comments verwijderen");

        verify(commentRepository).findById(id);
        verify(commentRepository, never()).delete(any());
    }
}
