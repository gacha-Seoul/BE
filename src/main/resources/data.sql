-- =========================================================
-- 서울 25개 구 (지도 표시용)
-- =========================================================
INSERT INTO region (id, code, name, name_en, description) VALUES
(1,  'JONGNO',       '종로구',   'Jongno-gu',       '경복궁과 광화문, 인사동, 광장시장이 있는 서울 역사 문화의 중심.'),
(2,  'JUNG',         '중구',     'Jung-gu',         NULL),
(3,  'YONGSAN',      '용산구',   'Yongsan-gu',      NULL),
(4,  'SEONGDONG',    '성동구',   'Seongdong-gu',    NULL),
(5,  'GWANGJIN',     '광진구',   'Gwangjin-gu',     NULL),
(6,  'DONGDAEMUN',   '동대문구', 'Dongdaemun-gu',   NULL),
(7,  'JUNGNANG',     '중랑구',   'Jungnang-gu',     NULL),
(8,  'SEONGBUK',     '성북구',   'Seongbuk-gu',     NULL),
(9,  'GANGBUK',      '강북구',   'Gangbuk-gu',      NULL),
(10, 'DOBONG',       '도봉구',   'Dobong-gu',       NULL),
(11, 'NOWON',        '노원구',   'Nowon-gu',        NULL),
(12, 'EUNPYEONG',    '은평구',   'Eunpyeong-gu',    NULL),
(13, 'SEODAEMUN',    '서대문구', 'Seodaemun-gu',    '독립문과 서대문형무소역사관, 신촌·이대 거리, 연희동, 홍제천, 전통시장이 함께 있는 동네.'),
(14, 'MAPO',         '마포구',   'Mapo-gu',         NULL),
(15, 'YANGCHEON',    '양천구',   'Yangcheon-gu',    NULL),
(16, 'GANGSEO',      '강서구',   'Gangseo-gu',      NULL),
(17, 'GURO',         '구로구',   'Guro-gu',         NULL),
(18, 'GEUMCHEON',    '금천구',   'Geumcheon-gu',    NULL),
(19, 'YEONGDEUNGPO', '영등포구', 'Yeongdeungpo-gu', NULL),
(20, 'DONGJAK',      '동작구',   'Dongjak-gu',      NULL),
(21, 'GWANAK',       '관악구',   'Gwanak-gu',       NULL),
(22, 'SEOCHO',       '서초구',   'Seocho-gu',       NULL),
(23, 'GANGNAM',      '강남구',   'Gangnam-gu',      NULL),
(24, 'SONGPA',       '송파구',   'Songpa-gu',       NULL),
(25, 'GANGDONG',     '강동구',   'Gangdong-gu',     NULL);

-- =========================================================
-- 가챠 후보 카드 5개 = 지역 · 카테고리 (코스)
-- category: HISTORY / CULTURE / ART / TOURISM / LIVING
-- =========================================================
INSERT INTO course (id, region_id, category, order_no, open_copy, image_key) VALUES
(1, 13, 'CULTURE', 1, '서대문구의 문화 퀘스트가 열렸어. 첫 장소를 확인해 봐', 'course_seodaemun_culture'),
(2, 6, 'LIVING', 2, '동대문구의 생활자원 퀘스트가 열렸어. 첫 장소를 확인해 봐', 'course_dongdaemun_living'),
(3, 14, 'ART', 3, '마포구의 예술 퀘스트가 열렸어. 첫 장소를 확인해 봐', 'course_mapo_art'),
(4, 1, 'TOURISM', 4, '종로구의 관광 퀘스트가 열렸어. 첫 장소를 확인해 봐', 'course_jongno_tourism'),
(5, 2, 'HISTORY', 5, '중구의 역사 퀘스트가 열렸어. 첫 장소를 확인해 봐', 'course_jung_history');

-- =========================================================
-- 장소: 코스당 4개, 순차 해금. 퀘스트가 없는 장소는 COMING_SOON
-- =========================================================
INSERT INTO place (id, course_id, order_no, name, representative, candidates, pin_key, image_key, title, story, ending_text) VALUES
(1, 1, 1, '전통시장', '영천시장', '영천시장, 포방터시장, 인왕시장, 백련시장', 'pin_market', 'place_market', '시장 간식, 두 사람 몫', '친구랑 먹을 간식을 사러 왔어요. 사진 속 전집을 찾아 전을 맛보고, 두 팩을 9천 원에 살 수 있는지 물어봐요. 돌아오는 길엔 간을 뺀 순대 한 팩도 포장해요. 친구는 시장 입구에서 기다리고 있어요.', '입구의 친구에게 간식을 건넨다. “전 아직 따뜻하다. 하나는 네 거야. 같이 먹자.”'),
(2, 1, 2, '대학가', '신촌 연세로', '신촌 연세로, 명물거리, 이대 앞 이화여대길', 'pin_campus', 'place_campus', NULL, NULL, NULL),
(3, 1, 3, '뷰티스토어', '이대 앞 쇼핑거리', '이대 앞 쇼핑거리, 신촌 명물거리, 홍제역 상권', 'pin_beauty', 'place_beauty', NULL, NULL, NULL),
(4, 1, 4, '느좋카페', '연희동 카페거리', '연희동 카페거리, 홍제폭포 일대, 이대 앞 카페골목', 'pin_cafe', 'place_cafe', NULL, NULL, NULL);

-- =========================================================
-- 퀘스트: 장소당 Q1~Q4, 순차 해금, 최초 완료 시 코인 1
-- complete_event: 마지막 대화 후 필요한 화면 이벤트 (arrived 등)
-- complete_effects: 완료 시 화면 처리, ';' 구분
-- =========================================================
INSERT INTO quest (id, place_id, order_no, code, name, title, story, location, destination_key, npc_persona, opening_speaker, opening_ko, opening_roman, opening_en, rules, complete_event, complete_effects, reward_coin) VALUES
(1, 1, 1, 'Q1', '길 찾기', '사진 속 전집 찾기', '사진 속 전집을 찾아야 해요. 시장 입구 채소 좌판 상인에게 사진을 보여주며 어디인지 물어봐요.', '시장 입구 채소 좌판 → 방앗간 옆 전집', 'jeonjip', '바쁘지만 친절한 시장 입구 채소 좌판 상인. 짧은 해요체.', '채소 가게 상인', '뭐 찾으세요?', 'Mwo chajeuseyo?', 'What are you looking for?', '학습자는 전집 사진을 보여주며 묻는다. ''이거 어디예요?'', ''전집이 어디예요?''는 통과. ''어디예요?''만 말하면(무엇을 찾는지 없음) ''어떤 가게 찾으세요?''라고 묻는다. 그다음 ''전집이요''라고 답하면 통과.', 'arrived', NULL, 1),
(2, 1, 2, 'Q2', '시식하기', '갓 부친 전, 한 입 먼저', '전집에 도착했어요. 어떤 전이 맛있는지 물어보고, 사기 전에 한 입 맛봐요.', '전집 진열대', 'jeonjip_counter', '정 많고 자부심 있는 전집 사장님. 해요체.', NULL, NULL, NULL, NULL, '다른 전을 맛보고 싶다고 하면 ''오늘은 호박전만 시식할 수 있어요''라고 안내한다.', NULL, 'SAMPLE_EAT:hobakjeon;SHOW_SELECT:호박전 2팩', 1),
(3, 1, 3, 'Q3', '흥정하기', '만 원인데, 가진 돈은 구천 원', '호박전 두 팩을 사고 싶은데 가진 돈은 9천 원이에요. 가격을 묻고 조금 깎아 달라고 해 봐요.', '같은 전집 계산대 · 호박전 2팩 선택', 'jeonjip_counter', '정 많은 전집 사장님. 흥정을 받아 주지만 9,000원 아래로는 안 깎아 준다. 해요체.', NULL, NULL, NULL, NULL, '9,000원 미만을 제안하면 ''두 팩이면 구천 원까지 해 드릴게요''처럼 9,000원으로 다시 제안한다. ''네''는 깎은 가격을 들은 뒤에만 수락으로 인정한다.', NULL, 'CART_ADD:호박전 2팩|9000;HIGHLIGHT:bunsik', 1),
(4, 1, 4, 'Q4', '음식 구매하기', '친구가 부탁한 순대', '친구가 순대도 부탁했어요. 간은 빼고 순대만, 포장으로 사 가요.', '시장 입구 분식집 → 시장 입구', 'bunsik', '시원시원한 분식집 사장님. 짧은 해요체.', NULL, NULL, NULL, NULL, '한 문장에 들어온 조건은 다시 묻지 않는다 (예: ''간 빼고 순대 포장해 주세요''는 세 단계 모두 통과).', NULL, 'CART_ADD:순대 1팩 (간 제외, 포장)', 1);

-- =========================================================
-- 대화 단계: 1단계 = 플레이어 대사(하단) + NPC 응답(상단 말풍선)
-- accepts: 같이 인정하는 표현 '|' 구분 (AI는 이 외에도 같은 의미면 인정)
-- required_event: 이 단계 전에 필요한 화면 이벤트 / retry_ko: 이벤트 없을 때 NPC 대사
-- =========================================================
INSERT INTO dialogue_step (id, quest_id, step_no, target, user_ko, user_roman, user_en, accepts, speaker, npc_ko, npc_roman, npc_en, effects, required_event, retry_ko) VALUES
(1, 1, 1, '채소 가게 상인', '이거 어디예요?', 'Igeo eodiyeyo?', 'Where is this place?', '전집이 어디예요?|전집이요|전 파는 가게 어디예요?|이 가게 어디예요?', '채소 가게 상인', '빨간 천막 지나서 방앗간 옆이에요.', 'Ppalgan cheonmak jinaseo banggatgan yeopieyo.', 'Go past the red tent. It''s next to the rice mill.', 'HIGHLIGHT:jeonjip', NULL, NULL),
(2, 2, 1, '전집 사장님', '뭐가 맛있어요?', 'Mwoga masisseoyo?', 'What''s good here?', '추천해 주세요|뭐가 제일 맛있어요?', '전집 사장님', '호박전이요. 방금 부쳤어요.', 'Hobakjeonieyo. Banggeum buchyeosseoyo.', 'The zucchini jeon. I just made it.', NULL, NULL, NULL),
(3, 2, 2, '전집 사장님', '먹어봐도 돼요?', 'Meogeobwado dwaeyo?', 'Can I try some?', '맛봐도 돼요?|한 입 먹어봐도 돼요?', '전집 사장님', '네, 여기 잘라 놓은 거 드셔 보세요.', 'Ne, yeogi jalla noeun geo deusyeo boseyo.', 'Sure, try a piece I cut here.', NULL, NULL, NULL),
(4, 3, 1, '전집 사장님', '이거 얼마예요?', 'Igeo eolmayeyo?', 'How much is this?', '두 팩에 얼마예요?|호박전 얼마예요?', '전집 사장님', '두 팩에 만 원이에요.', 'Du paege man wonieyo.', 'It''s 10,000 won for two packs.', 'PRICE:10000', 'selected', '어떤 거요?'),
(5, 3, 2, '전집 사장님', '조금 깎아 주세요.', 'Jogeum kkakka juseyo.', 'Could you give me a little discount?', '좀 깎아 주세요|구천 원에 주세요|싸게 해 주세요', '전집 사장님', '두 팩이면 구천 원에 드릴게요.', 'Du paegimyeon gucheon wone deurilgeyo.', 'For two packs, I''ll give you 9,000 won.', 'PRICE:9000', NULL, NULL),
(6, 3, 3, '전집 사장님', '네, 주세요.', 'Ne, juseyo.', 'Yes, I''ll take them.', '그걸로 주세요|좋아요 주세요', '전집 사장님', '하나씩 따로 담았어요. 순대는 입구 쪽 분식집에서 사면 돼요.', 'Hanassik ttaro damasseoyo. Sundaeneun ipgu jjok bunsikjibeseo samyeon dwaeyo.', 'I packed them separately. You can get sundae at the snack shop by the entrance.', NULL, NULL, NULL),
(7, 4, 1, '분식집 사장님', '순대 주세요.', 'Sundae juseyo.', 'Sundae, please.', '순대 하나 주세요|순대 1인분 주세요', '분식집 사장님', '간도 넣어드려요?', 'Gando neoeodeuryeoyo?', 'Do you want liver too?', 'SHOW_CHOICE:순대만|순대와 간', NULL, NULL),
(8, 4, 2, '분식집 사장님', '순대만 주세요.', 'Sundaeman juseyo.', 'Just sundae, please.', '간은 빼 주세요|간 빼고 주세요', '분식집 사장님', '여기서 드세요?', 'Yeogiseo deuseyo?', 'For here?', NULL, NULL, NULL),
(9, 4, 3, '분식집 사장님', '포장해 주세요.', 'Pojanghae juseyo.', 'To go, please.', '가져갈게요|포장이요', '분식집 사장님', '네, 순대만 담아드릴게요.', 'Ne, sundaeman damadeurilgeyo.', 'Okay, I''ll pack just the sundae.', NULL, NULL, NULL);
