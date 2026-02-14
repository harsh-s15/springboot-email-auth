package com.example.auth.DAO;

import com.example.auth.bean.VerificationToken;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class FileVerificationTokenRepository {

    private final Path file = Paths.get("data/verification_tokens.json");
    private final ObjectMapper mapper = new ObjectMapper();

    private Map<String, VerificationToken> load() {
        try {
            if (!Files.exists(file) || Files.size(file) == 0) {
                return new HashMap<>();
            }

            return mapper.readValue(
                    Files.readString(file),
                    new TypeReference<Map<String, VerificationToken>>() {}
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void persist(Map<String, VerificationToken> tokens) {
        try {
            Files.createDirectories(file.getParent());
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(file.toFile(), tokens);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void save(VerificationToken token) {
        Map<String, VerificationToken> tokens = load();
        tokens.put(token.getToken(), token);
        persist(tokens);
    }

    public Optional<VerificationToken> find(String token) {
        return Optional.ofNullable(load().get(token));
    }

    public void delete(String token) {
        Map<String, VerificationToken> tokens = load();
        tokens.remove(token);
        persist(tokens);
    }
}
