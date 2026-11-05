package com.hackathon.seoul.llm;

import com.hackathon.seoul.domain.AppUser;
import com.hackathon.seoul.domain.Stage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 프롬프트는 resources/prompts/*.txt 에 있음 (Java 몰라도 수정 가능).
 * {{이름}} 자리에 값이 들어간다. 수정 후 서버 재시작하면 반영.
 */
@Component
public class PromptFactory {

    private final String chatTemplate = load("prompts/npc-chat.txt");
    private final String phraseCardTemplate = load("prompts/phrase-card.txt");

    public String chatSystemPrompt(AppUser user, Stage stage, Set<Integer> completed) {
        List<String> missions = stage.getMissionList();
        StringBuilder missionText = new StringBuilder();
        for (int i = 0; i < missions.size(); i++) {
            missionText.append(i).append(": ").append(missions.get(i))
                    .append(completed.contains(i) ? " (완료)" : " (미완료)").append("\n");
        }
        return fill(chatTemplate, Map.of(
                "region", stage.getRegion().getName(),
                "place", nz(stage.getPlace()),
                "category", stage.getCategory().getLabel(),
                "npcRole", nz(stage.getNpcRole()),
                "persona", nz(stage.getNpcPersona()),
                "situation", nz(stage.getSituation()),
                "language", language(user),
                "level", user.getLevel() == null ? "BEGINNER" : user.getLevel(),
                "missions", missionText.toString().trim()));
    }

    public String phraseCardSystemPrompt(AppUser user, Stage stage) {
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
