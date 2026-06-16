-- 트랜잭션
BEGIN;

-- User (1,000명)
INSERT INTO "user" (id, email, nickname, password, created_at, updated_at)
SELECT
    gen_random_uuid(),
    'seed_user_' || g || '@deokhugam.com',
    '시드유저' || g,
    '$2a$10$dummydummydummydummydummydummydummydummydummy',  -- 더미 해시
    NOW() - (random() * interval '365 days'),
    NOW()
FROM generate_series(1, 1000) g;

-- Book (5,000권)
INSERT INTO book (id, title, author, description, publisher, published_date,
                  isbn, thumbnail_url, review_count, rating, created_at, updated_at)
SELECT
    gen_random_uuid(),
    '테스트도서_' || g,
    '저자_' || (g % 200),
    '이 책은 시드 데이터로 생성된 테스트 도서 ' || g || ' 입니다.',
    '출판사_' || (g % 50),
    DATE '2000-01-01' + (g % 9000),
    '978' || LPAD(g::text, 10, '0'),
    'https://example.com/thumb/' || g || '.jpg',
    0, -- 추후 갱신
    0.00, -- 추후 갱신
    NOW() - (random() * interval '365 days'),
    NOW()
FROM generate_series(1, 5000) g;

-- Review (25,000개)
-- 한 유저가 한 책에 1개만
INSERT INTO review (id, user_id, book_id, rating, content,
                    like_count, comment_count, created_at, updated_at)
SELECT
    gen_random_uuid(),
    u.id,
    b.id,
    (floor(random() * 5) + 1)::int,                -- 평점 1~5
    '시드 리뷰 내용입니다. 도서에 대한 감상 ' || b.rn || '-' || u.rn,
    0,                                             -- 추후 갱신
    0,                                        -- 추후 갱신
    NOW() - (random() * interval '180 days'),
    NOW()
FROM
    (SELECT id, row_number() OVER (ORDER BY id) AS rn FROM book) b
        CROSS JOIN LATERAL (
        SELECT id, rn FROM (
                               SELECT id, row_number() OVER (ORDER BY id) AS rn FROM "user"
                           ) usr
        WHERE usr.rn BETWEEN ((b.rn * 5) % 1000) + 1 AND ((b.rn * 5) % 1000) + 5
    ) u
WHERE b.rn <= 5000;


-- Comment (50,000개)
INSERT INTO comment (id, review_id, user_id, content, created_at, updated_at)
SELECT
    gen_random_uuid(),
    r.id,
    (SELECT id FROM "user" ORDER BY random() LIMIT 1),  -- 랜덤 유저
    '시드 댓글 내용 ' || gs,
    NOW() - (random() * interval '90 days'),
    NOW()
FROM review r
    CROSS JOIN generate_series(1, 2) gs;                    -- 리뷰당 2개

-- Review_like (50,000개)
-- (review_id, user_id) 유니크
INSERT INTO review_like (id, review_id, user_id, created_at)
SELECT
    gen_random_uuid(),
    r.id,
    u.id,
    NOW() - (random() * interval '90 days')
FROM
    (SELECT id, row_number() OVER (ORDER BY id) AS rn FROM review) r
        CROSS JOIN LATERAL (
        SELECT id FROM (
                           SELECT id, row_number() OVER (ORDER BY id) AS rn FROM "user"
                       ) usr
        WHERE usr.rn BETWEEN ((r.rn * 2) % 1000) + 1 AND ((r.rn * 2) % 1000) + 2
    ) u;


-- 집계 컬럼 갱신
-- 리뷰의 like_count 갱신
UPDATE review r
SET like_count = sub.cnt
    FROM (SELECT review_id, COUNT(*) AS cnt FROM review_like GROUP BY review_id) sub
WHERE r.id = sub.review_id;

-- 리뷰의 comment_count 갱신
UPDATE review r
SET comment_count = sub.cnt
    FROM (SELECT review_id, COUNT(*) AS cnt FROM comment GROUP BY review_id) sub
WHERE r.id = sub.review_id;

-- 도서의 review_count, rating 갱신
UPDATE book b
SET review_count = sub.cnt,
    rating = sub.avg_rating
    FROM (
    SELECT book_id, COUNT(*) AS cnt, ROUND(AVG(rating), 2) AS avg_rating
    FROM review GROUP BY book_id
) sub
WHERE b.id = sub.book_id;


-- Notification (약 50,000건)
-- 내 리뷰에 좋아요/댓글이 달려서 생긴 알림
-- confirmed  = 70% 확인됨 / 30% 미확인
INSERT INTO notification (id, user_id, review_id, review_content, message,
                          confirmed, created_at, updated_at)
SELECT
    gen_random_uuid(),
    r.user_id,                                       -- 리뷰 작성자가 알림 수신
    r.id,
    LEFT(r.content, 100),                            -- 리뷰 내용 일부
    CASE (gs % 2)
    WHEN 0 THEN '회원님의 리뷰에 좋아요가 달렸습니다.'
    ELSE '회원님의 리뷰에 댓글이 달렸습니다.'
END,
    (random() < 0.7),                                -- 70% 확인됨
    NOW() - (random() * interval '14 days'),         -- 최근 2주 내 분산
    NOW()
FROM review r
CROSS JOIN generate_series(1, 2) gs;                 -- 리뷰당 2건

COMMIT;

-- 적재 결과 확인
SELECT 'user' AS table_name, COUNT(*) FROM "user"
UNION ALL SELECT 'book', COUNT(*) FROM book
UNION ALL SELECT 'review', COUNT(*) FROM review
UNION ALL SELECT 'comment', COUNT(*) FROM comment
UNION ALL SELECT 'review_like', COUNT(*) FROM review_like
UNION ALL SELECT 'notification', COUNT(*) FROM notification;