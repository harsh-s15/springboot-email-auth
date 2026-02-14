package com.example.auth.DAO;

import com.example.auth.bean.RefreshToken;
import java.util.Optional;

public interface RefreshTokenRepository {

    void save(RefreshToken token);

    Optional<RefreshToken> findByToken(String token);

    void delete(String token);

    void deleteAllForUser(String username);
}
