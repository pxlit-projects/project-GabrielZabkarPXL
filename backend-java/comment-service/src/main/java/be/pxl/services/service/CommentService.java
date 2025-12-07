package be.pxl.services.service;

import be.pxl.services.client.PostClient;
import be.pxl.services.dto.PostDTO;
import be.pxl.services.entity.Comment;
import be.pxl.services.repository.CommentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostClient postClient;

    public CommentService(CommentRepository commentRepository, PostClient postClient) {
        this.commentRepository = commentRepository;
        this.postClient = postClient;
    }

    public Comment addComment(Long postId, String author, String content) {
        PostDTO post = postClient.getPostByIdInternal(postId);

        if (post == null || !"PUBLISHED".equalsIgnoreCase(post.getStatus())) {
            throw new IllegalStateException("Post bestaat niet of is niet gepubliceerd.");
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Comment mag niet leeg zijn.");
        }

        Comment comment = Comment.builder()
                .postId(postId)
                .author(author)
                .content(content)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return commentRepository.save(comment);
    }

    public List<Comment> getCommentsForPost(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }

    public Comment updateComment(Long id, String author, String content) {
        Comment existing = commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comment niet gevonden: " + id));

        if (!existing.getAuthor().equals(author)) {
            throw new IllegalStateException("Je mag enkel je eigen comments aanpassen.");
        }

        existing.setContent(content);
        existing.setUpdatedAt(LocalDateTime.now());
        return commentRepository.save(existing);
    }

    public void deleteComment(Long id, String author) {
        Comment existing = commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comment niet gevonden: " + id));

        if (!existing.getAuthor().equals(author)) {
            throw new IllegalStateException("Je mag enkel je eigen comments verwijderen.");
        }

        commentRepository.delete(existing);
    }
}
