```sql
-- ===================================================================================
-- Campung 테스트 데이터 INSERT 스크립트 (v4 - 최종 수정)
-- ===================================================================================
-- 주의: 이 스크립트는 전체를 복사하여 한 번에 실행해야 정상적으로 동작합니다.

-- -----------------------------------------------------
-- 1. 테스트용 사용자 추가 (없을 경우에만)
-- -----------------------------------------------------
INSERT INTO user (user_id, nickname, password_hash, created_at, updated_at)
SELECT 'test', '테스트유저', 'temp_password_hash', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM user WHERE user_id = 'test');


-- -----------------------------------------------------
-- 2. 우울한 분위기의 글 (10개)
-- -----------------------------------------------------
INSERT INTO content (title, content, latitude, longitude, post_type, emotion, author_id, created_at, updated_at) VALUES
('오늘 너무 우울하네요', '날씨도 흐리고 기분도 가라앉고... 아무것도 하기 싫은 날이에요. 다들 어떻게 극복하시나요?', 35.8, 127.61, 'FREE', '우울함', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('시험 망친 것 같아요', '공부 열심히 했는데 결과가 안 좋을 것 같아서 너무 속상해요. F만 안 나왔으면 좋겠다.', 35.8, 127.61, 'FREE', '슬픔', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('알바 가기 싫다', '오늘따라 유난히 알바 가기가 싫네요. 진상 손님만 없기를...', 35.8, 127.61, 'FREE', '우울함', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('미래가 막막하게 느껴질 때', '다들 잘 나아가는 것 같은데 나만 뒤처지는 기분. 내가 잘하고 있는 게 맞을까요?', 35.8, 127.61, 'FREE', '우울함', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('자취방에 혼자 있으니 외롭다', '본가 내려가고 싶다. 혼자 밥 챙겨 먹는 것도 지겹고... 그냥 다 지치네요.', 35.8, 127.61, 'FREE', '슬픔', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('팀플하는데 나만 일하는 기분', '이럴 거면 그냥 혼자 하는 게 낫지. 너무 화나고 지쳐요.', 35.8, 127.61, 'FREE', '화남', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('등록금 너무 비싸다', '다음 학기 등록금 낼 생각 하니 벌써부터 한숨만 나오네요. 언제쯤 이런 걱정 안 하고 살 수 있을까.', 35.8, 127.61, 'FREE', '우울함', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('인간관계가 제일 어려운 것 같아요', '좋은 사람인 줄 알았는데 아니었을 때의 실망감이 너무 크다. 현타 제대로 오네요.', 35.8, 127.61, 'FREE', '슬픔', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('오늘 교수님한테 제대로 깨졌습니다', '수업 시간에 졸다가 걸려서... 너무 창피하고 우울합니다. 집에 가고 싶다.', 35.8, 127.61, 'FREE', '우울함', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('왜 나만 연애 못할까', '주변에 다들 커플인데... 봄이라 더 외로운 것 같아요. 저한테 무슨 문제라도 있는 걸까요?', 35.8, 127.61, 'FREE', '슬픔', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW());


-- -----------------------------------------------------
-- 3. 밝은 분위기의 글 (10개)
-- -----------------------------------------------------
INSERT INTO content (title, content, latitude, longitude, post_type, emotion, author_id, created_at, updated_at) VALUES
('날씨 진짜 좋다!', '구름 한 점 없이 맑네요! 이런 날은 수업째고 놀러 가야 하는데... 다들 맛점하세요!', 35.8, 127.61, 'FREE', '밝음', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('시험 드디어 끝났다!', '결과가 어떻든 일단 해방입니다! 오늘 저녁에 술 마실 사람 구해요!', 35.8, 127.61, 'FREE', '신남', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('오늘 저녁은 치킨이다', '알바비 들어왔습니다. 수고한 나를 위한 선물! 치킨보다 맛있는 게 또 있을까요?', 35.8, 127.61, 'FREE', '신남', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('A+ 받았습니다! 자랑 좀 할게요', '지난 학기 밤새면서 공부한 보람이 있네요. 너무 기분 좋습니다!', 35.8, 127.61, 'FREE', '흥분된', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('학교 고양이 너무 귀여워요', '중앙도서관 앞에서 자고 있는 고양이 봤는데 심장 녹는 줄... 힐링 그 자체', 35.8, 127.61, 'FREE', '밝음', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('드디어 종강!', '한 학기 동안 다들 고생 많으셨습니다. 방학 알차게 보내요!', 35.8, 127.61, 'FREE', '신남', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('축제 라인업 대박!', '이번 축제 라인업 미쳤다! 다들 티켓팅 성공하세요!', 35.8, 127.61, 'FREE', '흥분된', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('새내기들 너무 귀엽고 풋풋하다', '과잠 입고 돌아다니는 거 보니까 옛날 생각나고 좋네요. 파이팅!', 35.8, 127.61, 'FREE', '밝음', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('오늘 학식 역대급 메뉴 나옴', '오늘 점심은 무조건 학식입니다. 다들 빨리 식당으로 달려가세요!', 35.8, 127.61, 'FREE', '신남', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('동아리 합격했습니다!', '들어가고 싶었던 동아리 붙었어요! 선배님들 잘 부탁드립니다!', 35.8, 127.61, 'FREE', '흥분된', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW());


-- -----------------------------------------------------
-- 4. 무감정적이고 정보 전달식의 글 (10개)
-- -----------------------------------------------------
INSERT INTO content (title, content, latitude, longitude, post_type, emotion, author_id, created_at, updated_at) VALUES
('중앙도서관 휴관일 안내', '10월 3일 개천절은 중앙도서관 휴관일입니다. 이용에 참고하시기 바랍니다.', 35.8, 127.61, 'FREE', NULL, (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('오늘 학생회관 식당 메뉴', '점심: 제육볶음, 된장찌개. 저녁: 돈까스, 크림수프. 많은 이용 바랍니다.', 35.8, 127.61, 'FREE', NULL, (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('분실물 찾아가세요 (학생증)', '공학관 302호에서 OOO(20231234) 학생의 학생증을 주웠습니다. 학생회실에 맡겨두겠습니다.', 35.8, 127.61, 'FREE', NULL, (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('수강신청 변경 기간 안내', '수강신청 변경 기간은 9월 5일부터 9월 7일까지입니다. 기간 내에 완료해주세요.', 35.8, 127.61, 'FREE', NULL, (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('에어팟 프로 케이스 주우신 분', '오늘 오후 3시경 중앙도서관 열람실에서 에어팟 프로 케이스를 잃어버렸습니다. 습득하신 분은 연락주세요.', 35.8, 127.61, 'FREE', NULL, (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('IT융합대학 프로그래밍 경진대회 안내', '참가 신청은 10월 10일까지. 자세한 내용은 학과 홈페이지 공지사항을 확인하세요.', 35.8, 127.61, 'FREE', NULL, (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('제1학생회관 엘리베이터 점검 안내', '10월 5일 10:00~12:00 엘리베이터 정기 점검이 실시됩니다. 양해 부탁드립니다.', 35.8, 127.61, 'FREE', NULL, (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('국가장학금 2차 신청 기간', '국가장학금 2차 신청은 9월 15일까지입니다. 아직 신청하지 않은 학생들은 서둘러주세요.', 35.8, 127.61, 'FREE', NULL, (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('스터디 그룹 팀원 모집', '자료구조 C++ 스터디 하실 분 2명 모집합니다. 매주 화, 목 저녁에 진행 예정입니다.', 35.8, 127.61, 'FREE', NULL, (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('셔틀버스 노선 변경 안내', '동문 정류장이 폐쇄되고, 후문 정류장이 신설되었습니다. 10월 1일부터 적용됩니다.', 35.8, 127.61, 'FREE', NULL, (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW());


-- -----------------------------------------------------
-- 5. 모든 감정이 존재하는 복합적인 글 (20개)
-- -----------------------------------------------------
INSERT INTO content (title, content, latitude, longitude, post_type, emotion, author_id, created_at, updated_at) VALUES
('졸업하는데 시원섭섭하네', '드디어 졸업이라 너무 좋은데, 막상 학교 떠날 생각하니 아쉽고 슬프기도 하네요. 다들 잘 지내!', 35.8, 127.61, 'FREE', '밝음', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('팀플하는데 화나면서도 웃기다', '자료조사 시켰더니 나무위키 긁어온 팀원 때문에 화나는데, 너무 당당해서 웃음만 나온다.', 35.8, 127.61, 'FREE', '화남', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('다이어트 중인데 떡볶이 먹고 싶다', '열심히 운동하고 왔는데 룸메가 떡볶이를 시켰다. 냄새 때문에 미치겠고 못 먹는 내가 슬프다.', 35.8, 127.61, 'FREE', '슬픔', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('발표 준비 너무 떨려', '내일 전공 발표인데 너무 떨리고 긴장돼요. 잘할 수 있겠죠? 응원 좀 해주세요!', 35.8, 127.61, 'FREE', '흥분된', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('CC였던 전 애인을 마주쳤다', '헤어진 지 1년 됐는데, 오늘 도서관에서 마주쳤어요. 심장이 쿵 내려앉는 기분... 아직 미련이 남았나봐요.', 35.8, 127.61, 'FREE', '우울함', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('복학하니까 다 모르는 사람들 뿐', '아는 사람도 없고, 아는 척 하기도 뭐하고... 적응하기 힘드네요. 그래도 다시 학교 오니 설레기도 합니다.', 35.8, 127.61, 'FREE', '밝음', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('MT 장기자랑 벌써부터 걱정', '춤도 못 추고 노래도 못하는데 어떡하죠? 벌써부터 스트레스 받는데 막상 가면 재밌을 것 같기도 하고...', 35.8, 127.61, 'FREE', '흥분된', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('길 가다 돈 주웠는데 경찰서 가기 귀찮다', '만원 주웠는데 기분은 좋네요. 근데 이거 경찰서 갖다줘야겠죠? 그냥 내가 쓸까... 양심의 가책이 느껴진다.', 35.8, 127.61, 'FREE', '밝음', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('군대 가는 동기 배웅해주고 오는 길', '잘 다녀오라고 웃으면서 보내줬는데, 막상 혼자 돌아오니 마음이 헛헛하고 슬프네요. 몸 조심해라 친구야.', 35.8, 127.61, 'FREE', '슬픔', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('소개팅 앞두고 옷 고르는 중', '내일 소개팅인데 뭐 입고 갈까요? 너무 설레고 기대돼요! 잘 보이고 싶은데... 옷이 없어서 슬프다.', 35.8, 127.61, 'FREE', '흥분된', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('알바 월급 들어왔는데 통장을 스쳐 지나가네', '월급 들어와서 너무 신났는데, 카드값 내고 월세 내니 순식간에 사라졌어요. 허무하고 슬프다.', 35.8, 127.61, 'FREE', '신남', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('수강신청 망했다가 주운 후기', '올클 실패해서 절망하고 있었는데, 방금 누가 버린 전공필수 주웠습니다! 천국과 지옥을 오갔네요.', 35.8, 127.61, 'FREE', '흥분된', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('오늘따라 부모님 보고 싶다', '자취방에 혼자 아프니까 너무 서럽네요. 엄마가 끓여주던 죽이 먹고 싶다. 전화 드려야지.', 35.8, 127.61, 'FREE', '슬픔', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('조별과제 발표하는데 교수님이 칭찬해주심', '밤새 준비한 보람이 있네요. 너무 뿌듯하고 기분 좋습니다! 근데 팀원 한 명이 자기 혼자 다 한 척 해서 좀 화나요.', 35.8, 127.61, 'FREE', '밝음', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('축제 주점에서 번호 따였다!', '상상도 못한 일이라 너무 놀랐고 설렜어요. 근데 제가 너무 당황해서 제대로 대답을 못 한 것 같아 아쉬워요.', 35.8, 127.61, 'FREE', '흥분된', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('친구가 갑자기 휴학한다고 한다', '같이 졸업하자고 약속했는데... 아쉽고 서운하지만 친구의 결정을 응원해주려고요. 보고 싶을 거야.', 35.8, 127.61, 'FREE', '슬픔', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('장학금 받게 됐어요!', '성적 장학금 대상자라고 문자 왔어요! 부모님 부담 덜어드릴 수 있어서 너무 기쁘고 뿌듯합니다.', 35.8, 127.61, 'FREE', '신남', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('룸메이트랑 생활 패턴이 너무 안 맞아서 스트레스', '저는 아침형 인간, 룸메는 저녁형 인간... 사소한 소음 때문에 잠을 못 자서 너무 피곤하고 화가 나요. 말 꺼내기가 어려워서 답답하네요.', 35.8, 127.61, 'FREE', '화남', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('교양 수업에서 이상형을 발견했다', '말 한 번 못 걸어봤지만... 수업 가는 길이 기다려지고 설레네요. 힐링 그 자체.', 35.8, 127.61, 'FREE', '밝음', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW()),
('드디어 사고 싶었던 노트북 샀다', '알바비 모아서 드디어 샀습니다! 너무 예쁘고 성능도 좋아서 행복해요. 이제 남은 건 할부의 노예... 그래도 후회는 없습니다!', 35.8, 127.61, 'FREE', '신남', (SELECT id FROM user WHERE user_id = 'test'), NOW(), NOW());
```