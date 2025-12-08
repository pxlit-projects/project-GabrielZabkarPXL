package be.pxl.services.repository;

import be.pxl.services.entity.Post;
import be.pxl.services.entity.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByStatus(PostStatus status);

    List<Post> findByAuthorContainingIgnoreCaseAndStatus(String author, PostStatus status);

    List<Post> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(String title, String content);

    List<Post> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<Post> findByAuthorAndLastReviewDecisionIsNotNullOrderByLastReviewedAtDesc(String author);

}
