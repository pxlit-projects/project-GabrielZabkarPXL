package be.pxl.services.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long postId;
    private String reviewer;

    @Enumerated(EnumType.STRING)
    private ReviewDecision decision;

    @Column(columnDefinition = "TEXT")
    private String comment;

    private LocalDateTime createdAt;
}
