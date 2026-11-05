package com.hackathon.seoul.domain;

import com.hackathon.seoul.common.GameException;
import jakarta.persistence.*;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nickname;
    private String language;   // 교정 설명을 쓸 언어 (English, 日本語, 中文 ...)
    private String level;      // BEGINNER / INTERMEDIATE / ADVANCED
    private int coins;

    protected AppUser() {}

    public AppUser(String nickname, String language, String level, int coins) {
        this.nickname = nickname;
        this.language = language;
        this.level = level;
        this.coins = coins;
    }

    public void addCoins(int amount) { this.coins += amount; }

    public void spendCoins(int amount) {
        if (coins < amount) {
            throw new GameException(HttpStatus.BAD_REQUEST, "코인이 부족해요. (보유 " + coins + " / 필요 " + amount + ")");
        }
        this.coins -= amount;
    }

    public Long getId() { return id; }
    public String getNickname() { return nickname; }
    public String getLanguage() { return language; }
    public String getLevel() { return level; }
    public int getCoins() { return coins; }
}
