package com.example.auth.DAO;

import com.example.auth.bean.PasswordResetToken;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class FilePasswordResetRepository {

    private final Path file = Paths.get("data/password_reset_tokens.json");
    private final ObjectMapper mapper = new ObjectMapper();

    private Map<String, PasswordResetToken> load() {
        try {
            if (!Files.exists(file) || Files.size(file) == 0) {
                return new HashMap<>();
            }

            return mapper.readValue(
                    Files.readString(file),
                    new TypeReference<Map<String, PasswordResetToken>>() {}
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void persist(Map<String, PasswordResetToken> tokens) {
        try {
            Files.createDirectories(file.getParent());
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(file.toFile(), tokens);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void save(PasswordResetToken token) {
        Map<String, PasswordResetToken> tokens = load();
        tokens.put(token.getToken(), token);
        persist(tokens);
    }

    public Optional<PasswordResetToken> find(String token) {
        return Optional.ofNullable(load().get(token));
    }

    public void delete(String token) {
        Map<String, PasswordResetToken> tokens = load();
        tokens.remove(token);
        persist(tokens);
    }
}