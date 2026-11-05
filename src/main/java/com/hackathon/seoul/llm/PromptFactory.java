package com.hackathon.seoul.llm;

import com.hackathon.seoul.domain.AppUser;
import com.hackathon.seoul.domain.DialogueStep;
import com.hackathon.seoul.domain.Quest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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

    /** 대화 단계 판정 프롬프트. currentIdx = 현재 단계 index (0부터) */
    public String stepJudgePrompt(AppUser user, Quest quest, List<DialogueStep> steps, int currentIdx) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            DialogueStep s = steps.get(i);
            sb.append(s.getStepNo()).append(" | ")
                    .append(i < currentIdx ? "완료" : (i == currentIdx ? "현재" : "대기")).append(" | ")
                    .append(nz(s.getTarget())).append(" | ")
                    .append(nz(s.getUserKo())).append(" | ")
                    .append(s.getAcceptList().isEmpty() ? "-" : String.join(" / ", s.getAcceptList())).append(" | ")
                    .append(nz(s.getSpeaker())).append(": ").append(nz(s.getNpcKo())).append("\n");
        }
        Map<String, String> v = new HashMap<>();
        v.put("region", quest.getPlace().getCourse().getRegion().getName());
        v.put("category", quest.getPlace().getCourse().getCategory().getLabel());
        v.put("place", nz(quest.getPlace().getName()));
        v.put("questCode", nz(quest.getCode()));
        v.put("questTitle", nz(quest.getTitle()));
        v.put("story", nz(quest.getStory()));
        v.put("persona", nz(quest.getNpcPersona()));
        v.put("language", language(user));
        v.put("level", user.getLevel() == null ? "BEGINNER" : user.getLevel());
        v.put("steps", sb.toString().trim());
        v.put("rules", nz(quest.getRules()));
        v.put("currentStep", String.valueOf(steps.get(currentIdx).getStepNo()));
        return fill(chatTemplate, v);
    }

    public String phraseCardSystemPrompt(AppUser user, Quest quest) {
        Map<String, String> v = new HashMap<>();
        v.put("region", quest.getPlace().getCourse().getRegion().getName());
        v.put("place", nz(quest.getPlace().getName()));
        v.put("questTitle", nz(quest.getTitle()));
        v.put("language", language(user));
        return fill(phraseCardTemplate, v);
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
