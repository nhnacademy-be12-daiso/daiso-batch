show tables;
select * from category_coupon;

-- =====================================================
-- 한국십진분류법 (KDC) 카테고리 INSERT
-- 분류 코드: 000, 010, 020, ..., 990
-- =====================================================

-- ====================
-- 0. 총류 (000-090)
-- ====================
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 000); -- 000 총류
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 010); -- 010 도서학, 서지학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 020); -- 020 문헌정보학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 030); -- 030 백과사전
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 040); -- 040 강연집, 수필집, 연설문집
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 050); -- 050 일반 연속간행물
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 060); -- 060 일반 학회, 단체, 협회, 기관
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 070); -- 070 신문, 저널리즘
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 080); -- 080 일반 전집, 총서
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 090); -- 090 향토자료

-- ====================
-- 1. 철학 (100-190)
-- ====================
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 100); -- 100 철학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 110); -- 110 형이상학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 120); -- 120 인식론, 인과론, 인간학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 130); -- 130 철학의 체계
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 140); -- 140 경학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 150); -- 150 심리학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 160); -- 160 논리학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 170); -- 170 윤리학, 도덕철학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 180); -- 180 고대, 중세 철학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 190); -- 190 근대, 현대 철학

-- ====================
-- 2. 종교 (200-290)
-- ====================
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 200); -- 200 종교
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 210); -- 210 비교종교학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 220); -- 220 불교
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 230); -- 230 기독교
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 240); -- 240 도교
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 250); -- 250 천도교
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 260); -- 260 신도
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 270); -- 270 힌두교, 브라만교
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 280); -- 280 이슬람교
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 290); -- 290 기타 제종교

-- ====================
-- 3. 사회과학 (300-390)
-- ====================
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 300); -- 300 사회과학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 310); -- 310 통계학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 320); -- 320 경제학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 330); -- 330 사회학, 사회문제
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 340); -- 340 정치학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 350); -- 350 행정학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 360); -- 360 법학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 370); -- 370 교육학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 380); -- 380 풍속, 예절, 민속학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 390); -- 390 국방, 군사학

-- ====================
-- 4. 자연과학 (400-490)
-- ====================
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 400); -- 400 자연과학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 410); -- 410 수학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 420); -- 420 물리학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 430); -- 430 화학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 440); -- 440 천문학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 450); -- 450 지학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 460); -- 460 광물학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 470); -- 470 생명과학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 480); -- 480 식물학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 490); -- 490 동물학

-- ====================
-- 5. 기술과학 (500-590)
-- ====================
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 500); -- 500 기술과학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 510); -- 510 의학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 520); -- 520 농업, 농학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 530); -- 530 공학, 공업일반
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 540); -- 540 건축공학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 550); -- 550 기계공학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 560); -- 560 전기공학, 전자공학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 570); -- 570 화학공학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 580); -- 580 제조업
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 590); -- 590 생활과학

-- ====================
-- 6. 예술 (600-690)
-- ====================
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 600); -- 600 예술
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 610); -- 610 건축술
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 620); -- 620 조각, 조형미술
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 630); -- 630 공예
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 640); -- 640 서예
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 650); -- 650 회화, 도화
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 660); -- 660 사진술
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 670); -- 670 음악
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 680); -- 680 연극
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 690); -- 690 오락, 운동

-- ====================
-- 7. 언어 (700-790)
-- ====================
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 700); -- 700 언어
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 710); -- 710 한국어
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 720); -- 720 중국어
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 730); -- 730 일본어
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 740); -- 740 영어
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 750); -- 750 독일어
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 760); -- 760 프랑스어
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 770); -- 770 스페인어
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 780); -- 780 이탈리아어
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 790); -- 790 기타 제어

-- ====================
-- 8. 문학 (800-890)
-- ====================
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 800); -- 800 문학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 810); -- 810 한국문학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 820); -- 820 중국문학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 830); -- 830 일본문학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 840); -- 840 영미문학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 850); -- 850 독일문학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 860); -- 860 프랑스문학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 870); -- 870 스페인문학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 880); -- 880 이탈리아문학
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 890); -- 890 기타 제문학

-- ====================
-- 9. 역사 (900-990)
-- ====================
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 900); -- 900 역사
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 910); -- 910 아시아
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 920); -- 920 유럽
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 930); -- 930 아프리카
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 940); -- 940 북아메리카
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 950); -- 950 남아메리카
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 960); -- 960 오세아니아
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 970); -- 970 양극지방
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 980); -- 980 지리
INSERT INTO category_coupon (coupon_policy_id, book_category_id) VALUES (7, 990); -- 990 전기

-- =====================================================
-- 총 100개 (000, 010, 020, ..., 980, 990)
-- =====================================================