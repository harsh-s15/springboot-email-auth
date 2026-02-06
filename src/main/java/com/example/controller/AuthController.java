package com.example.controller;

import com.example.DAO.UserRepository;
import com.example.bean.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public AuthController(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest req) {

        if (repo.findByUsername(req.username()).isPresent()) {
            return ResponseEntity.badRequest().body("User exists");
        }

        User user = new User();
        user.setUsername(req.username());
        user.setPasswordHash(encoder.encode(req.password()));

        repo.save(user);
        return ResponseEntity.ok("Signup successful");
    }
}
