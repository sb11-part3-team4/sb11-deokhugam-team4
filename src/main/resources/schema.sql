CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Book
CREATE TABLE book
(
    id             UUID PRIMARY KEY,
    title          VARCHAR(255)  NOT NULL,
    author         VARCHAR(100)  NOT NULL,
    description    TEXT          NOT NULL,
    publisher      VARCHAR(100)  NOT NULL,
    published_date DATE          NOT NULL,
    isbn           VARCHAR(20) UNIQUE,
    thumbnail_url  VARCHAR(512),
    review_count   INTEGER       NOT NULL,
    rating         DECIMAL(3, 2) NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL
);

-- 검색
CREATE INDEX idx_book_search_combined ON book USING GIN ((COALESCE(title, '') || ' ' ||
                                                          COALESCE(author, '') || ' ' ||
                                                          COALESCE(isbn, '')) gin_trgm_ops);
-- 정렬
CREATE INDEX idx_book_cursor_title ON book (title DESC, created_at DESC, id DESC);
-- 정렬
CREATE INDEX idx_book_cursor_published_date ON book (published_date DESC, created_at DESC, id DESC);
-- 정렬
CREATE INDEX idx_book_cursor_rating ON book (rating DESC, created_at DESC, id DESC);
-- 정렬
CREATE INDEX idx_book_cursor_review_count ON book (review_count DESC, created_at DESC, id DESC);

-- DeletedBook
CREATE TABLE deleted_book
(
    id             UUID PRIMARY KEY,
    title          VARCHAR(255)  NOT NULL,
    author         VARCHAR(100)  NOT NULL,
    description    TEXT          NOT NULL,
    publisher      VARCHAR(100)  NOT NULL,
    published_date DATE          NOT NULL,
    isbn           VARCHAR(20),
    thumbnail_url  VARCHAR(512),
    review_count   INTEGER       NOT NULL,
    rating         DECIMAL(3, 2) NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL,
    deleted_at     TIMESTAMPTZ   NOT NULL
);

-- User
CREATE TABLE "user"
(
    id         UUID PRIMARY KEY,
    email      VARCHAR(255) UNIQUE NOT NULL,
    nickname   VARCHAR(50)         NOT NULL,
    password   VARCHAR(255)        NOT NULL,
    created_at TIMESTAMPTZ         NOT NULL,
    updated_at TIMESTAMPTZ         NOT NULL
);

-- DeletedUser
CREATE TABLE deleted_user
(
    id         UUID PRIMARY KEY,
    email      VARCHAR(255) NOT NULL,
    nickname   VARCHAR(50)  NOT NULL,
    password   VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    deleted_at TIMESTAMPTZ  NOT NULL
);

-- Review
CREATE TABLE review
(
    id            UUID PRIMARY KEY,
    user_id       UUID        NOT NULL,
    book_id       UUID        NOT NULL,
    rating        INTEGER     NOT NULL,
    content       TEXT        NOT NULL,
    like_count    INTEGER     NOT NULL,
    comment_count INTEGER     NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES "user" (id),
    CONSTRAINT fk_review_book FOREIGN KEY (book_id) REFERENCES book (id)
);

-- 제약
CREATE UNIQUE INDEX idx_review_user_book_unique ON review (user_id, book_id);

-- DeletedReview
CREATE TABLE deleted_review
(
    id            UUID PRIMARY KEY,
    user_id       UUID        NOT NULL,
    book_id       UUID        NOT NULL,
    rating        INTEGER     NOT NULL,
    content       TEXT        NOT NULL,
    like_count    INTEGER     NOT NULL,
    comment_count INTEGER     NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    deleted_at    TIMESTAMPTZ NOT NULL
);

-- ReviewLike
CREATE TABLE review_like
(
    id         UUID PRIMARY KEY,
    review_id  UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_review_like_review FOREIGN KEY (review_id) REFERENCES review (id),
    CONSTRAINT fk_review_like_user FOREIGN KEY (user_id) REFERENCES "user" (id)
);

-- 제약
CREATE UNIQUE INDEX idx_review_like_user_review_unique ON review_like (review_id, user_id);

-- PopularReview
CREATE TABLE popular_review
(
    id         UUID PRIMARY KEY,
    review_id  UUID           NOT NULL,
    period     VARCHAR(50)    NOT NULL,
    score      DECIMAL(10, 2) NOT NULL,
    rank       INTEGER        NOT NULL,
    base_date  DATE           NOT NULL,
    created_at TIMESTAMPTZ    NOT NULL,
    CONSTRAINT fk_popular_review_review FOREIGN KEY (review_id) REFERENCES review (id)
);

-- 제약
CREATE UNIQUE INDEX idx_popular_review_period_date_rank_unique ON popular_review (period, base_date, rank);
-- 제약
CREATE UNIQUE INDEX idx_popular_review_period_date_review_unique ON popular_review (period, base_date, review_id);

-- Comment
CREATE TABLE comment
(
    id         UUID PRIMARY KEY,
    review_id  UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    content    TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_comment_review FOREIGN KEY (review_id) REFERENCES review (id),
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES "user" (id)
);

-- DeletedComment
CREATE TABLE deleted_comment
(
    id         UUID PRIMARY KEY,
    review_id  UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    content    TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ NOT NULL
);

-- Notification
CREATE TABLE notification
(
    id             UUID PRIMARY KEY,
    user_id        UUID         NOT NULL,
    review_id      UUID         NOT NULL,
    review_content TEXT         NOT NULL,
    message        VARCHAR(512) NOT NULL,
    confirmed      BOOLEAN      NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES "user" (id),
    CONSTRAINT fk_notification_review FOREIGN KEY (review_id) REFERENCES review (id)
);

-- 정렬
CREATE INDEX idx_notification_user_pagination ON notification (user_id, created_at DESC, id DESC);
-- 배치
CREATE INDEX idx_notification_batch_clean ON notification (confirmed, created_at);

-- DeletedNotification
CREATE TABLE deleted_notification
(
    id             UUID PRIMARY KEY,
    user_id        UUID         NOT NULL,
    review_id      UUID         NOT NULL,
    review_content TEXT         NOT NULL,
    message        VARCHAR(512) NOT NULL,
    confirmed      BOOLEAN      NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    deleted_at     TIMESTAMPTZ  NOT NULL
);