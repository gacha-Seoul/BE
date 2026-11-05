package com.hackathon.seoul;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.seoul.common.GameException;
import com.hackathon.seoul.domain.*;
import com.hackathon.seoul.dto.Dtos.*;
import com.hackathon.seoul.llm.*;
import com.hackathon.seoul.repository.*;
import com.hackathon.seoul.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {"llm.mock=true", "spring.jpa.hibernate.ddl-auto=create"})
@Testcontainers
@AutoConfigureMockMvc
class GameDatabaseTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");
    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", MYSQL::getJdbcUrl);
        properties.add("spring.datasource.username", MYSQL::getUsername);
        properties.add("spring.datasource.password", MYSQL::getPassword);
    }
    @Autowired MockMvc mvc;
    @Autowired UserService users;
    @Autowired GachaService gacha;
    @Autowired ChatService chat;
    @Autowired RegionService regions;
    @Autowired DemoService demo;
    @Autowired QuestProgressRepository progress;
    @Autowired CoinTransactionRepository coins;
    @Autowired GachaHistoryRepository history;
    @Autowired QuestRepository quests;
    @Autowired ChatMessageRepository messages;
    @Autowired RegionRepository regionRepo;
    @Autowired StageRepository stages;
    @Autowired ObjectMapper json;
    @Autowired DataSource dataSource;
    @MockBean LlmClient llm;

    @BeforeEach void reset() {
        demo.resetAll();
        when(llm.completeJson(any(), anyString(), anyList())).thenReturn("{}");
    }
    private Long player() {
        Long id = users.create(null).id();
        gacha.draw(id, UUID.randomUUID().toString());
        return id;
    }
    private ChatResponse turn(Long user, long quest, Map<String, Object> updates) throws Exception {
        when(llm.completeJson(eq(LlmPurpose.CHAT), anyString(), anyList()))
                .thenReturn(json.writeValueAsString(new LlmTurn("확인했어요.", null, null, List.of(), List.of(), updates)));
        return chat.chatQuest(quest, new ChatRequest(user, "네, 확인했어요."));
    }
    private void firstTwo(Long user) throws Exception {
        chat.startQuest(501L, user);
        assertThat(turn(user, 501, Map.of("arrivedAt", "JEON_STORE")).questCompleted()).isTrue();
        chat.startQuest(502L, user);
        assertThat(turn(user, 502, Map.of("sampleAccepted", true)).questCompleted()).isTrue();
    }
    private void market(Long user) throws Exception {
        firstTwo(user);
        chat.startQuest(503L, user);
        assertThat(turn(user, 503, Map.of("selectedItem", "PUMPKIN_JEON", "quantity", 2, "offeredPrice", 8000)).questCompleted()).isFalse();
        assertThat(turn(user, 503, Map.of("purchaseConfirmed", true)).questCompleted()).isTrue();
        chat.startQuest(504L, user);
        assertThat(turn(user, 504, Map.of("item", "SUNDAE", "quantity", 1, "excludeLiver", true, "takeout", true, "orderConfirmed", true)).questCompleted()).isTrue();
    }
    @Test void initialFreeCoinFundsExactlyOneDraw() {
        UserResponse created = users.create(null);
        Long user = created.id();
        assertThat(created.coins()).isEqualTo(1);
        var initial = coins.findByUserIdOrderByIdAsc(user);
        assertThat(initial).hasSize(1);
        assertThat(initial.get(0).getType()).isEqualTo(CoinTransaction.Type.INITIAL);
        assertThat(initial.get(0).getAmount()).isEqualTo(1);
        assertThat(gacha.draw(user, "first-draw").coinsLeft()).isZero();
        assertThatThrownBy(() -> gacha.draw(user, "second-draw"))
                .isInstanceOf(GameException.class).hasMessageContaining("코인이 부족");
        assertThat(users.get(user).coins()).isZero();
        assertThat(history.count()).isEqualTo(1);
        var ledger = coins.findByUserIdOrderByIdAsc(user);
        assertThat(ledger).hasSize(2);
        assertThat(ledger.get(1).getAmount()).isEqualTo(-1);
        assertThat(ledger.stream().mapToInt(CoinTransaction::getAmount).sum()).isZero();
    }
    @Test void httpFlowExposesQuestStructureAndRequiresGachaRequestId() throws Exception {
        String body = mvc.perform(post("/api/demo/start")).andExpect(status().isOk())
                .andExpect(jsonPath("$.gacha.region.code").value("SEODAEMUN"))
                .andReturn().getResponse().getContentAsString();
        long user = json.readTree(body).path("user").path("id").asLong();
        mvc.perform(get("/api/stages/5").param("userId", String.valueOf(user)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalQuests").value(4))
                .andExpect(jsonPath("$.quests[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.quests[1].status").value("LOCKED"));
        mvc.perform(post("/api/users/" + user + "/gacha").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        mvc.perform(post("/api/users/" + user + "/gacha"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        mvc.perform(post("/api/quests/502/start").contentType("application/json").content("{\"userId\":" + user + "}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/quests/501/start").contentType("application/json").content("{\"userId\":" + user + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.questId").value(501));
        when(llm.completeJson(eq(LlmPurpose.CHAT), anyString(), anyList()))
                .thenReturn(json.writeValueAsString(new LlmTurn("도착했어요.", null, null, List.of(), List.of(), Map.of("arrivedAt", "JEON_STORE"))));
        mvc.perform(post("/api/quests/501/chat").contentType("application/json").content("{\"userId\":" + user + ",\"message\":\"도착했어요\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.questCompleted").value(true))
                .andExpect(jsonPath("$.coinsEarned").value(1)).andExpect(jsonPath("$.coins").value(1));
    }
    @Test void mockScenarioCompletesMarketThroughHttp() throws Exception {
        var fixture = new MockLlmClient(json);
        when(llm.completeJson(any(), anyString(), anyList())).thenAnswer(invocation -> fixture.completeJson(
                invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));
        Long user = player();
        for (long quest : List.of(501L, 502L, 503L, 504L)) {
            mvc.perform(post("/api/quests/" + quest + "/start").contentType("application/json").content("{\"userId\":" + user + "}"))
                    .andExpect(status().isOk());
            int turns = quest == 501 ? 2 : quest == 502 ? 1 : 3;
            for (int i = 0; i < turns; i++) {
                var result = mvc.perform(post("/api/quests/" + quest + "/chat").contentType("application/json")
                        .content("{\"userId\":" + user + ",\"message\":\"확인했어요\"}"))
                        .andExpect(status().isOk());
                if (i == turns - 1) result.andExpect(jsonPath("$.questCompleted").value(true));
            }
        }
        assertThat(users.get(user).coins()).isEqualTo(4);
    }
    @Test void seedIsRepeatableAndUsesNineTables() {
        new ResourceDatabasePopulator(new ClassPathResource("data.sql")).execute(dataSource);
        assertThat(regionRepo.count()).isEqualTo(25);
        assertThat(stages.count()).isEqualTo(10);
        assertThat(quests.count()).isEqualTo(13);
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        assertThat(jdbc.queryForObject("select count(*) from information_schema.tables where table_schema = database()", Integer.class)).isEqualTo(9);
    }
    @Test void questAccessAndStartAreRequired() {
        Long user = users.create(null).id();
        assertThatThrownBy(() -> chat.startQuest(501L, user)).isInstanceOf(GameException.class).hasMessageContaining("가챠");
        gacha.draw(user, "first");
        assertThatThrownBy(() -> chat.startQuest(502L, user)).isInstanceOf(GameException.class).hasMessageContaining("이전");
        assertThatThrownBy(() -> chat.chatQuest(501L, new ChatRequest(user, "도착했어요"))).isInstanceOf(GameException.class).hasMessageContaining("시작");
    }
    @Test void orderStatePersistsAndMissingConditionsDoNotClear() throws Exception {
        Long user = player(); firstTwo(user);
        chat.startQuest(503L, user);
        var negotiation = turn(user, 503, Map.of("selectedItem", "PUMPKIN_JEON", "quantity", 2, "offeredPrice", 8000, "purchaseConfirmed", true));
        assertThat(negotiation.questCompleted()).isFalse(); // 제안한 가격에 대한 수락은 다음 턴에 확인
        assertThat(negotiation.context().get("acceptedPrice")).isEqualTo(9000);
        assertThat(turn(user, 503, Map.of("purchaseConfirmed", true)).questCompleted()).isTrue();
        chat.startQuest(504L, user);
        assertThat(turn(user, 504, Map.of("item", "SUNDAE", "quantity", 1)).questCompleted()).isFalse();
        assertThat(turn(user, 504, Map.of("excludeLiver", true, "orderConfirmed", true, "coins", 99999, "status", "COMPLETED")).questCompleted()).isFalse();
        var done = turn(user, 504, Map.of("takeout", true, "orderConfirmed", true));
        assertThat(done.questCompleted()).isTrue();
        assertThat(done.stageCleared()).isTrue();
        assertThat(done.context()).doesNotContainKeys("coins", "status");
        assertThat(users.get(user).coins()).isEqualTo(4);
        String saved = progress.findByUserIdAndQuestId(user, 504L).orElseThrow().getContextJson();
        long messageCount = messages.count();
        chat.startQuest(504L, user);
        assertThat(progress.findByUserIdAndQuestId(user, 504L).orElseThrow().getContextJson()).isEqualTo(saved);
        assertThat(messages.count()).isEqualTo(messageCount);
        assertThatThrownBy(() -> chat.chatQuest(504L, new ChatRequest(user, "네"))).isInstanceOf(GameException.class);
        assertThat(users.get(user).coins()).isEqualTo(4);
    }
    @Test void discountWithoutNumericOfferCompletesOnLaterAcceptance() throws Exception {
        Long user = player(); firstTwo(user); chat.startQuest(503L, user);
        turn(user, 503, Map.of("selectedItem", "PUMPKIN_JEON", "quantity", 2));
        assertThat(turn(user, 503, Map.of("purchaseConfirmed", true)).questCompleted()).isFalse();
        when(llm.completeJson(eq(LlmPurpose.CHAT), anyString(), anyList())).thenReturn(json.writeValueAsString(
                new LlmTurn("두 팩이면 구천 원에 드릴게요.", null, null, List.of(), List.of(),
                        Map.of("negotiationRequested", true, "purchaseConfirmed", true, "acceptedPrice", 100, "budget", 100))));
        var offer = chat.chatQuest(503L, new ChatRequest(user, "조금 깎아 주세요"));
        assertThat(offer.questCompleted()).isFalse();
        assertThat(offer.coinsEarned()).isZero();
        assertThat(offer.context()).containsEntry("acceptedPrice", 9000).containsEntry("budget", 9000)
                .containsEntry("purchaseConfirmed", false).doesNotContainKey("offeredPrice");
        var saved = json.readTree(progress.findByUserIdAndQuestId(user, 503L).orElseThrow().getContextJson());
        assertThat(saved.path("acceptedPrice").asInt()).isEqualTo(9000);
        assertThat(saved.path("negotiationRequested").asBoolean()).isTrue();
        assertThat(turn(user, 503, Map.of()).questCompleted()).isFalse();
        // 이미 가격이 있어도 새 흥정 요청과 구매 수락을 같은 턴으로 처리하지 않는다.
        assertThat(turn(user, 503, Map.of("negotiationRequested", true, "purchaseConfirmed", true)).questCompleted()).isFalse();
        when(llm.completeJson(eq(LlmPurpose.CHAT), anyString(), anyList())).thenReturn(json.writeValueAsString(
                new LlmTurn("하나씩 따로 담았어요.", null, null, List.of(), List.of(), Map.of("purchaseConfirmed", true))));
        mvc.perform(post("/api/quests/503/chat").contentType("application/json")
                        .content(json.writeValueAsString(new ChatRequest(user, "네, 주세요"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.questCompleted").value(true))
                .andExpect(jsonPath("$.coinsEarned").value(1)).andExpect(jsonPath("$.coins").value(3));
        mvc.perform(post("/api/quests/503/chat").contentType("application/json")
                        .content(json.writeValueAsString(new ChatRequest(user, "네, 주세요"))))
                .andExpect(status().isConflict());
        assertThat(users.get(user).coins()).isEqualTo(3);
    }
    @Test void discountFlagMustBeBooleanAndDoesNotReplaceOrderRequirements() throws Exception {
        Long user = player(); firstTwo(user); chat.startQuest(503L, user);
        var invalid = turn(user, 503, Map.of("negotiationRequested", "true", "purchaseConfirmed", true));
        assertThat(invalid.questCompleted()).isFalse();
        assertThat(invalid.context()).doesNotContainKeys("negotiationRequested", "acceptedPrice");
        assertThat(turn(user, 503, Map.of("negotiationRequested", true)).questCompleted()).isFalse();
        assertThat(turn(user, 503, Map.of("purchaseConfirmed", true)).questCompleted()).isFalse();
        assertThat(turn(user, 503, Map.of("selectedItem", "PUMPKIN_JEON", "quantity", 1)).questCompleted()).isFalse();
        assertThat(turn(user, 503, Map.of("purchaseConfirmed", true)).questCompleted()).isFalse();
        assertThat(users.get(user).coins()).isEqualTo(2);
    }
    @Test void failedModelCallPreservesStateAndRestartOnlyResetsIncompleteQuest() throws Exception {
        Long user = player(); firstTwo(user); chat.startQuest(503L, user);
        turn(user, 503, Map.of("selectedItem", "PUMPKIN_JEON", "quantity", 2));
        String before = progress.findByUserIdAndQuestId(user, 503L).orElseThrow().getContextJson();
        when(llm.completeJson(eq(LlmPurpose.CHAT), anyString(), anyList())).thenThrow(new RuntimeException("offline"));
        assertThat(chat.chatQuest(503L, new ChatRequest(user, "얼마예요")).questCompleted()).isFalse();
        assertThat(progress.findByUserIdAndQuestId(user, 503L).orElseThrow().getContextJson()).isEqualTo(before);
        chat.startQuest(503L, user);
        assertThat(progress.findByUserIdAndQuestId(user, 503L).orElseThrow().getContextJson()).isEqualTo("{}");
        assertThat(progress.findByUserIdAndQuestId(user, 502L).orElseThrow().isCompleted()).isTrue();
    }
    @Test void gachaRetryReturnsOriginalResultWithoutChargingAgain() throws Exception {
        Long user = player(); market(user);
        var first = gacha.draw(user, "second-draw");
        var replay = gacha.draw(user, "second-draw");
        assertThat(replay).isEqualTo(first);
        assertThat(users.get(user).coins()).isEqualTo(3);
        assertThat(first.coinsLeft()).isEqualTo(3);
        assertThat(history.count()).isEqualTo(2);
        assertThat(coins.findByUserIdOrderByIdAsc(user).stream().mapToInt(CoinTransaction::getAmount).sum()).isEqualTo(3);
    }
    @Test void concurrentRetriesChargeOnce() throws Exception {
        Long user = users.create(null).id();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2), go = new CountDownLatch(1);
            Callable<GachaResponse> draw = () -> { ready.countDown(); go.await(); return gacha.draw(user, "same-request"); };
            Future<GachaResponse> a = pool.submit(draw), b = pool.submit(draw);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue(); go.countDown();
            assertThat(a.get(20, TimeUnit.SECONDS)).isEqualTo(b.get(20, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
        assertThat(users.get(user).coins()).isZero();
        assertThat(history.count()).isEqualTo(1);
    }
    @Test void concurrentCompletionRewardsOnce() throws Exception {
        Long user = player(); chat.startQuest(501L, user);
        when(llm.completeJson(eq(LlmPurpose.CHAT), anyString(), anyList()))
                .thenReturn(json.writeValueAsString(new LlmTurn("도착했어요.", null, null, List.of(), List.of(), Map.of("arrivedAt", "JEON_STORE"))));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> complete = () -> {
                try { return chat.chatQuest(501L, new ChatRequest(user, "도착했어요")).questCompleted(); }
                catch (GameException e) { return false; }
            };
            Future<Boolean> a = pool.submit(complete), b = pool.submit(complete);
            assertThat(List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        } finally { pool.shutdownNow(); }
        assertThat(users.get(user).coins()).isEqualTo(1);
        assertThat(coins.findByUserIdOrderByIdAsc(user).stream().filter(c -> c.getType() == CoinTransaction.Type.QUEST_CLEAR).count()).isEqualTo(1);
    }
    @Test void conqueringAllQuestsPaysBonusOnceAndResetKeepsContent() throws Exception {
        Long user = player(); market(user);
        for (long id : List.of(1L, 2L, 3L, 4L)) {
            chat.startQuest(id, user);
            when(llm.completeJson(eq(LlmPurpose.CHAT), anyString(), anyList()))
                    .thenReturn(json.writeValueAsString(new LlmTurn("완료했어요.", null, null, List.of(), List.of(0, 1, 2, 3, -1, 999), Map.of())));
            chat.chatQuest(id, new ChatRequest(user, "감사합니다"));
        }
        assertThat(users.get(user).coins()).isEqualTo(10);
        assertThat(regions.detail(user, 13L).status()).isEqualTo("CONQUERED");
        assertThat(regions.map(user).regions().stream().filter(r -> r.id() == 13L).findFirst().orElseThrow().clearedStages()).isEqualTo(5);
        chat.startQuest(4L, user);
        assertThat(users.get(user).coins()).isEqualTo(10);
        demo.resetAll();
        assertThat(history.count()).isZero();
        assertThat(coins.count()).isZero();
        assertThat(progress.count()).isZero();
        assertThat(quests.count()).isEqualTo(13);
    }
}
