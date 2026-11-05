package com.hackathon.seoul.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GameProperties {
    @Value("${game.initial-coins}") public int initialCoins;
    @Value("${game.gacha-cost}") public int gachaCost;
    @Value("${game.region-conquer-bonus}") public int regionConquerBonus;
    @Value("${game.first-region-code:}") public String firstRegionCode;
}
