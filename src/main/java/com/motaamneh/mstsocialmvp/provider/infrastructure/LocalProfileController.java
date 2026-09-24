package com.motaamneh.mstsocialmvp.provider.infrastructure;

import com.motaamneh.mstsocialmvp.verification.domain.InstagramHandle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local")
@ConditionalOnProperty(name = "mst.instagram-provider.mode", havingValue = "fake")
@RequestMapping("/api/v1/local/instagram-profiles")
public class LocalProfileController {
    private final LocalBioProvider provider;

    public LocalProfileController(LocalBioProvider provider) {
        this.provider = provider;
    }

    @PutMapping("/{username}")
    public ResponseEntity<Void> set(@PathVariable String username, @Valid @RequestBody SetBioRequest request) {
        provider.setBiography(InstagramHandle.parse(username), request.biography);
        return ResponseEntity.noContent().build();
    }

    public static class SetBioRequest {
        @NotNull
        @Size(max = 150)
        private String biography;

        public String getBiography() { return biography; }
        public void setBiography(String biography) { this.biography = biography; }
    }
}
