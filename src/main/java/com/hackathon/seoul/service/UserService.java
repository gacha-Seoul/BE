package com.hackathon.seoul.service;

import com.hackathon.seoul.domain.AppUser;
import com.hackathon.seoul.dto.Dtos.*;
import com.hackathon.seoul.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final AppUserRepository userRepo;
    private final Finder finder;
    private final GameProperties props;

    public UserService(AppUserRepository userRepo, Finder finder, GameProperties props) {
        this.userRepo = userRepo;
        this.finder = finder;
        this.props = props;
    }

    @Transactional
    public UserResponse create(CreateUserRequest req) {
        String nickname = (req == null || req.nickname() == null || req.nickname().isBlank()) ? "Traveler" : req.nickname();
        String language = (req == null || req.language() == null || req.language().isBlank()) ? "English" : req.language();
        String level = (req == null || req.level() == null || req.level().isBlank()) ? "BEGINNER" : req.level();
        AppUser user = userRepo.save(new AppUser(nickname, language, level, props.initialCoins, props.freeDraws));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return toResponse(finder.user(id));
    }

    static UserResponse toResponse(AppUser u) {
        return new UserResponse(u.getId(), u.getNickname(), u.getLanguage(), u.getLevel(),
                u.getCoins(), u.getFreeDraws(), u.getDrawsLeft());
    }
}
