package com.hackathon.seoul.service;

import com.hackathon.seoul.common.GameException;
import com.hackathon.seoul.domain.*;
import com.hackathon.seoul.dto.Dtos.*;
import com.hackathon.seoul.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GachaService {

    private final GachaHistoryRepository historyRepo;
    private final CoinTransactionRepository coinRepo;
    private final Finder finder;
    private final RegionRepository regionRepo;
    private final UserRegionRepository userRegionRepo;
    private final RegionService regionService;
    private final GameProperties props;
    private final Random random = new SecureRandom();

    public GachaService(Finder finder, RegionRepository regionRepo, UserRegionRepository userRegionRepo,
                        RegionService regionService, GameProperties props, GachaHistoryRepository historyRepo, CoinTransactionRepository coinRepo) {
        this.historyRepo = historyRepo;
        this.coinRepo = coinRepo;
        this.finder = finder;
        this.regionRepo = regionRepo;
        this.userRegionRepo = userRegionRepo;
        this.regionService = regionService;
        this.props = props;
    }

    @Transactional
    public GachaResponse draw(Long userId) { return draw(userId, java.util.UUID.randomUUID().toString()); }

    @Transactional
    public GachaResponse draw(Long userId, String requestId) {
        if (requestId == null || requestId.isBlank() || requestId.length() > 100) {
            throw new GameException(HttpStatus.BAD_REQUEST, "requestId는 1~100자여야 해요.");
        }
        AppUser user = finder.lockedUser(userId);
        Optional<GachaHistory> previous = historyRepo.findByUserIdAndRequestId(userId, requestId);
        if (previous.isPresent()) {
            GachaHistory old = previous.get();
            return new GachaResponse(regionService.buildDetail(userId, old.getRegion()), old.getCoinsLeft(), requestId);
        }

        List<UserRegion> mine = userRegionRepo.findByUserId(userId);
        Set<Long> owned = mine.stream().map(ur -> ur.getRegion().getId()).collect(Collectors.toSet());

        List<Region> candidates = regionRepo.findPlayableRegions().stream()
                .filter(r -> !owned.contains(r.getId()))
                .toList();
        if (candidates.isEmpty()) {
            throw new GameException(HttpStatus.BAD_REQUEST, "뽑을 수 있는 지역을 모두 모았어요!");
        }

        user.spendCoins(props.gachaCost);   // 코인 부족하면 여기서 400

        Region picked = pickFirstRegion(mine)
                .filter(r -> !owned.contains(r.getId()))
                .orElseGet(() -> candidates.get(random.nextInt(candidates.size())));

        userRegionRepo.save(new UserRegion(user, picked));
        historyRepo.save(new GachaHistory(user, picked, props.gachaCost, requestId));
        coinRepo.save(new CoinTransaction(user, -props.gachaCost, CoinTransaction.Type.GACHA,
                "GACHA:" + userId + ":" + requestId));
        return new GachaResponse(regionService.buildDetail(userId, picked), user.getCoins(), requestId);
    }

    /** 데모 안정성: 첫 가챠는 설정한 지역(서대문구)으로 고정 */
    private Optional<Region> pickFirstRegion(List<UserRegion> mine) {
        if (!mine.isEmpty() || props.firstRegionCode == null || props.firstRegionCode.isBlank()) {
            return Optional.empty();
        }
        return regionRepo.findByCode(props.firstRegionCode);
    }
}
