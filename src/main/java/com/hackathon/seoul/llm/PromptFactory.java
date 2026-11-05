package com.hackathon.seoul.llm;

import com.hackathon.seoul.domain.AppUser;
import com.hackathon.seoul.domain.Quest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 프롬프트는 resources/prompts/*.txt 에 있음 (Java 몰라도 수정 가능).
 * {{이름}} 자리에 값이 들어간다. 수정 후 서버 재시작하면 반영.
 */
@Component
public class PromptFactory {

    private final String chatTemplate = load("prompts/npc-chat.txt");
    private final String phraseCardTemplate = load("prompts/phrase-card.txt");

    public String fallbackReply() { return load("prompts/fallback.txt").trim(); }

    public String chatSystemPrompt(AppUser user, Quest stage, Map<String, Object> context) {
        StringBuilder missionText = new StringBuilder();
        List<String> missions = stage.getMissionList();
        for (int i = 0; i < missions.size(); i++) missionText.append(i).append(": ").append(missions.get(i)).append("\n");
        Map<String, String> values = new java.util.HashMap<>();
        values.put("region", stage.getRegion().getName());
        values.put("place", nz(stage.getPlace()));
        values.put("category", stage.getCategory().getLabel());
        values.put("npcRole", nz(stage.getNpcRole()));
        values.put("persona", nz(stage.getNpcPersona()));
        values.put("situation", nz(stage.getSituation()));
        values.put("language", language(user));
        values.put("level", user.getLevel() == null ? "BEGINNER" : user.getLevel());
        values.put("missions", missionText.toString());
        values.put("completionRule", stage.getCompletionRule().name());
        try { values.put("context", new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(context)); }
        catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException(e); }
        return fill(chatTemplate, values);
    }

    public String phraseCardSystemPrompt(AppUser user, Quest stage) {
        return fill(phraseCardTemplate, Map.of(
                "region", stage.getRegion().getName(),
                "place", nz(stage.getPlace()),
                "situation", nz(stage.getSituation()),
                "language", language(user)));
    }

    private static String fill(String template, Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> e : values.entrySet()) {
            result = result.replace("{{" + e.getKey() + "}}", e.getValue());
        }
        return result;
    }

    private static String language(AppUser user) {
        return user.getLanguage() == null ? "English" : user.getLanguage();
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static String load(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("프롬프트 파일을 못 읽음: " + path, e);
        }
    }
}
