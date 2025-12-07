package be.pxl.services.client;

import be.pxl.services.dto.PostDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "post-service",
        url = "http://localhost:8081"
)
public interface PostClient {

    @PutMapping("/api/posts/{id}/status")
    void updatePostStatus(@PathVariable("id") Long id,
                          @RequestParam("status") String status);

    @GetMapping("/api/posts/requested")
    List<PostDTO> getRequestedPosts(@RequestHeader("X-User-Role") String role);
}

