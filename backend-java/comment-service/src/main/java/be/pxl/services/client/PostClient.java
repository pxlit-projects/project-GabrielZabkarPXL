package be.pxl.services.client;

import be.pxl.services.dto.PostDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "post-service", url = "http://localhost:8081")
public interface PostClient {
    @GetMapping("/api/posts/internal/{id}")
    PostDTO getPostByIdInternal(@PathVariable("id") Long id);
}
