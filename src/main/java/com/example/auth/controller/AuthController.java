package com.example.auth.controller;

import com.example.auth.DAO.FilePasswordResetRepository;
import com.example.auth.DAO.FileVerificationTokenRepository;
import com.example.auth.DAO.UserRepository;
import com.example.auth.DAO.RefreshTokenRepository;
import com.example.auth.bean.PasswordResetToken;
import com.example.auth.bean.VerificationToken;
import com.example.auth.bean.User;
import com.example.auth.dto.ForgotPasswordRequest;
import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.ResetPasswordRequest;
import com.example.auth.dto.SignupRequest;
import com.example.auth.security.JwtUtil;
import com.example.auth.security.TokenUtil;
import com.example.auth.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseCookie;

import com.example.auth.bean.RefreshToken;
import com.example.auth.security.RefreshTokenUtil;

import jakarta.servlet.http.Cookie;

@RestController
public class AuthController {

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final RefreshTokenRepository refreshRepo;
    private final EmailService emailService;
    private final FileVerificationTokenRepository verifyTokenRepo;
    private final FilePasswordResetRepository resetPwRepo;
    private static final long ACCESS_EXPIRY_SECONDS = 15 * 60;       // 15 min
    private static final long REFRESH_EXPIRY_SECONDS = 7 * 24 * 60 * 60; // 7 days



    public AuthController(
            UserRepository repo,
            PasswordEncoder encoder,
            RefreshTokenRepository refreshRepo,
            EmailService emailService,
            FileVerificationTokenRepository verifyTokenRepo,
            FilePasswordResetRepository resetPwRepo
    ) {
        this.repo = repo;
        this.encoder = encoder;
        this.refreshRepo = refreshRepo;
        this.emailService = emailService;
        this.verifyTokenRepo = verifyTokenRepo;
        this.resetPwRepo = resetPwRepo;
    }

    @GetMapping("/home")
    public ResponseEntity<?> gethome(){
        return ResponseEntity.ok("welcome to homepage");
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest req) {

        if (repo.findByUsername(req.username()).isPresent()) {
            return ResponseEntity.badRequest().body("User exists");
        }

        User user = new User();
        user.setUsername(req.username());
        user.setPasswordHash(encoder.encode(req.password()));
        user.setEmailVerified(false);

        repo.save(user);

        // Generate verification token
        String token = TokenUtil.generateToken();
        long expiry = System.currentTimeMillis() + (10 * 60 * 1000); // 10 min

        VerificationToken vt =
                new VerificationToken(token, user.getUsername(), expiry);

        verifyTokenRepo.save(vt);

        // Send email
        String link = "http://localhost:8080/verify?token=" + token;
        emailService.sendSimpleMail(
                user.getUsername(),
                "Verify your email",
                "Click to verify: " + link
        );

        return ResponseEntity.ok("Signup successful. Check email.");
    }





    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String token) {

        VerificationToken vt = verifyTokenRepo.find(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (vt.getExpiryEpoch() < System.currentTimeMillis()) {
            verifyTokenRepo.delete(token);
            return ResponseEntity.badRequest().body("Token expired");
        }

        User user = repo.findByUsername(vt.getUsername())
                .orElseThrow();

        user.setEmailVerified(true);
        repo.save(user);

        verifyTokenRepo.delete(token);

        return ResponseEntity.ok("Email verified. You can login now.");
    }





    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest req,
            HttpServletResponse response
            ) {

        User user = repo.findByUsername(req.username())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!user.isEmailVerified()) {
            return ResponseEntity.status(403)
                    .body("Please verify your email first");
        }

        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        String username = user.getUsername();

        // 1️⃣ Generate access token (JWT)
        String accessToken = JwtUtil.generateToken(username);

        // 2️⃣ Generate refresh token (random)
        String refreshTokenValue = RefreshTokenUtil.generateToken();

        long expiryEpoch = System.currentTimeMillis() + (REFRESH_EXPIRY_SECONDS * 1000);

        RefreshToken refreshToken = new RefreshToken(
                refreshTokenValue,
                username,
                expiryEpoch
        );

        // 3️⃣ Save refresh token (server-side)
        refreshRepo.save(refreshToken);

        // 4️⃣ Set Access Token cookie
        ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(false) // true in production
                .path("/")
                .sameSite("Strict")
                .maxAge(ACCESS_EXPIRY_SECONDS)
                .build();

        // 5️⃣ Set Refresh Token cookie
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshTokenValue)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Strict")
                .maxAge(REFRESH_EXPIRY_SECONDS)
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());

        return ResponseEntity.ok("Login successful");


    }





    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request,
                                     HttpServletResponse response) {

        // 1️⃣ Extract refresh token from cookies
        String refreshTokenValue = null;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshTokenValue = cookie.getValue();
                }
            }
        }

        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return ResponseEntity.status(401).body("No refresh token");
        }

        // 2️⃣ Lookup
        RefreshToken existing = refreshRepo.findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        // 3️⃣ Expiry check
        if (existing.getExpiryEpoch() < System.currentTimeMillis()) {
            refreshRepo.delete(refreshTokenValue);
            return ResponseEntity.status(401).body("Refresh token expired");
        }

        String username = existing.getUsername();

        // 4️⃣ ROTATION: delete old token
        refreshRepo.delete(refreshTokenValue);

        // 5️⃣ Create new refresh token
        String newRefreshValue = RefreshTokenUtil.generateToken();

        long newExpiry = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000);

        RefreshToken newRefresh = new RefreshToken(
                newRefreshValue,
                username,
                newExpiry
        );

        refreshRepo.save(newRefresh);

        // 6️⃣ Create new access token
        String newAccessToken = JwtUtil.generateToken(username);

        // 7️⃣ Set cookies

        ResponseCookie accessCookie = ResponseCookie.from("access_token", newAccessToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Strict")
                .maxAge(15 * 60)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", newRefreshValue)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Strict")
                .maxAge(7 * 24 * 60 * 60)
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());

        return ResponseEntity.ok("Tokens refreshed");
    }








    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request,
                                    HttpServletResponse response) {


        // 1️⃣ Extract refresh token
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshRepo.delete(cookie.getValue());
                }
            }
        }

        ResponseCookie accessCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());

        return ResponseEntity.ok("Logged out");
    }




    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest req) {

        var userOpt = repo.findByUsername(req.email());

        // Do NOT reveal whether user exists (security)
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok("If account exists, email sent");
        }

        String token = TokenUtil.generateToken();
        long expiry = System.currentTimeMillis() + (15 * 60 * 1000); // 15 min

        PasswordResetToken prt =
                new PasswordResetToken(token, req.email(), expiry);

        resetPwRepo.save(prt);

        String link = "http://localhost:8080/reset-password?token=" + token;

        emailService.sendSimpleMail(
                req.email(),
                "Password Reset",
                "Click to reset password: " + link
        );

        return ResponseEntity.ok("If account exists, email sent");
    }



    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req) {

        PasswordResetToken prt = resetPwRepo.find(req.token())
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (prt.getExpiryEpoch() < System.currentTimeMillis()) {
            resetPwRepo.delete(req.token());
            return ResponseEntity.badRequest().body("Token expired");
        }

        User user = repo.findByUsername(prt.getUsername())
                .orElseThrow();

        user.setPasswordHash(encoder.encode(req.newPassword()));
        repo.save(user);

        resetPwRepo.delete(req.token());

        // 3️⃣ SECURITY: invalidate all sessions
        refreshRepo.deleteAllForUser(user.getUsername());

        return ResponseEntity.ok("Password reset successful");
    }

















}


// short lived access token
// long lived refresh token



// problems : new refresh token being created on every login, also even
// with refresh token rotation the new token would still go to attacker!


// solution to problem 2 :
// no real user is going to manually play with cookies
// hence absence of cookie -> go to login
// trying to login when cookie already there -> no need to generate refresh token again


// “Logout from this device”
//“Logout from all devices”
//Device management screen (like Google)