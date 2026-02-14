package com.example.auth.DAO;

import com.example.auth.bean.User;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Repository
public class FileUserRepository implements UserRepository{

    private final Path file = Paths.get("data/users.json");
    private final ObjectMapper mapper = new ObjectMapper();

    private Map<String, User> load() {
        try {
            if (!Files.exists(file)) {
                return new HashMap<>();
            }

            String content = Files.readString(file);

            if (content == null || content.isBlank()) {
                return new HashMap<>();
            }

            return mapper.readValue(
                    content,
                    new TypeReference<Map<String, User>>() {}
            );

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void persist(Map<String, User> users) {
        try {
            Files.createDirectories(file.getParent());
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(file.toFile(), users);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(load().get(username));
    }

    public void save(User user) {
        Map<String, User> users = load();
        users.put(user.getUsername(), user);
        persist(users);
    }
}
