CREATE
    EXTENSION IF NOT EXISTS pg_trgm;

-- Book
CREATE TABLE book
(
    id                UUID PRIMARY KEY,
    title             VARCHAR(255)  NOT NULL,
    author            VARCHAR(100)  NOT NULL,
    description       TEXT          NOT NULL,
    publisher         VARCHAR(100)  NOT NULL,
    published_date    DATE          NOT NULL,
    isbn              TEXT,
    thumbnail_url     VARCHAR(512),
    original_filename VARCHAR(255),
    review_count      INTEGER       NOT NULL,
    rating            DECIMAL(3, 2) NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL,

    -- 제약
    CONSTRAINT uk_book_isbn UNIQUE (isbn)
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
    id                UUID PRIMARY KEY,
    title             VARCHAR(255)  NOT NULL,
    author            VARCHAR(100)  NOT NULL,
    description       TEXT          NOT NULL,
    publisher         VARCHAR(100)  NOT NULL,
    published_date    DATE          NOT NULL,
    isbn              VARCHAR(20),
    thumbnail_url     VARCHAR(512),
    original_filename VARCHAR(255),
    review_count      INTEGER       NOT NULL,
    rating            DECIMAL(3, 2) NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL,
    deleted_at        TIMESTAMPTZ   NOT NULL
);

-- 썸네일 고아파일
CREATE TABLE orphan_thumbnail
(
    id         UUID PRIMARY KEY,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ,
    file_url   VARCHAR(512) NOT NULL
);

-- 배치 조회
CREATE INDEX idx_orphan_thumbnail_created_at ON orphan_thumbnail (created_at);

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

-- 정렬
CREATE INDEX idx_review_cursor_created_at ON review (book_id, created_at DESC, id DESC);
CREATE INDEX idx_review_cursor_rating ON review (book_id, rating DESC, created_at DESC, id DESC);

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

-- 정렬
CREATE INDEX idx_comment_review_created ON comment (review_id, created_at DESC);


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
CREATE INDEX idx_notification_batch_clean ON notification (confirmed, updated_at);

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

-- Book Ranking
CREATE TABLE book_ranking
(
    id                  UUID           NOT NULL,
    book_id             UUID           NOT NULL,
    period              VARCHAR(20)    NOT NULL,
    score               DECIMAL(10, 4) NOT NULL,
    ranking             INTEGER        NOT NULL,
    period_review_count BIGINT         NOT NULL,
    period_rating       DECIMAL(3, 2)  NOT NULL,
    created_at          TIMESTAMPTZ    NOT NULL,
    updated_at          TIMESTAMPTZ    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_book_ranking_book_period UNIQUE (book_id, period)
);

--Book Ranking Index
CREATE INDEX idx_book_ranking_period_ranking ON book_ranking (period, ranking);


-- 배치 메타 테이블
CREATE TABLE BATCH_JOB_INSTANCE
(
    JOB_INSTANCE_ID BIGINT       NOT NULL PRIMARY KEY,
    VERSION         BIGINT,
    JOB_NAME        VARCHAR(100) NOT NULL,
    JOB_KEY         VARCHAR(32)  NOT NULL,
    constraint JOB_INST_UN unique (JOB_NAME, JOB_KEY)
);

CREATE TABLE BATCH_JOB_EXECUTION
(
    JOB_EXECUTION_ID BIGINT    NOT NULL PRIMARY KEY,
    VERSION          BIGINT,
    JOB_INSTANCE_ID  BIGINT    NOT NULL,
    CREATE_TIME      TIMESTAMP NOT NULL,
    START_TIME       TIMESTAMP DEFAULT NULL,
    END_TIME         TIMESTAMP DEFAULT NULL,
    STATUS           VARCHAR(10),
    EXIT_CODE        VARCHAR(2500),
    EXIT_MESSAGE     VARCHAR(2500),
    LAST_UPDATED     TIMESTAMP,
    constraint JOB_INST_EXEC_FK foreign key (JOB_INSTANCE_ID)
        references BATCH_JOB_INSTANCE (JOB_INSTANCE_ID)
);

CREATE TABLE BATCH_JOB_EXECUTION_PARAMS
(
    JOB_EXECUTION_ID BIGINT       NOT NULL,
    PARAMETER_NAME   VARCHAR(100) NOT NULL,
    PARAMETER_TYPE   VARCHAR(100) NOT NULL,
    PARAMETER_VALUE  VARCHAR(2500),
    IDENTIFYING      CHAR(1)      NOT NULL,
    constraint JOB_EXEC_PARAMS_FK foreign key (JOB_EXECUTION_ID)
        references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

CREATE TABLE BATCH_STEP_EXECUTION
(
    STEP_EXECUTION_ID  BIGINT       NOT NULL PRIMARY KEY,
    VERSION            BIGINT       NOT NULL,
    STEP_NAME          VARCHAR(100) NOT NULL,
    JOB_EXECUTION_ID   BIGINT       NOT NULL,
    CREATE_TIME        TIMESTAMP    NOT NULL,
    START_TIME         TIMESTAMP DEFAULT NULL,
    END_TIME           TIMESTAMP DEFAULT NULL,
    STATUS             VARCHAR(10),
    COMMIT_COUNT       BIGINT,
    READ_COUNT         BIGINT,
    FILTER_COUNT       BIGINT,
    WRITE_COUNT        BIGINT,
    READ_SKIP_COUNT    BIGINT,
    WRITE_SKIP_COUNT   BIGINT,
    PROCESS_SKIP_COUNT BIGINT,
    ROLLBACK_COUNT     BIGINT,
    EXIT_CODE          VARCHAR(2500),
    EXIT_MESSAGE       VARCHAR(2500),
    LAST_UPDATED       TIMESTAMP,
    constraint JOB_EXEC_STEP_FK foreign key (JOB_EXECUTION_ID)
        references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

CREATE TABLE BATCH_STEP_EXECUTION_CONTEXT
(
    STEP_EXECUTION_ID  BIGINT        NOT NULL PRIMARY KEY,
    SHORT_CONTEXT      VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    constraint STEP_EXEC_CTX_FK foreign key (STEP_EXECUTION_ID)
        references BATCH_STEP_EXECUTION (STEP_EXECUTION_ID)
);

CREATE TABLE BATCH_JOB_EXECUTION_CONTEXT
(
    JOB_EXECUTION_ID   BIGINT        NOT NULL PRIMARY KEY,
    SHORT_CONTEXT      VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    constraint JOB_EXEC_CTX_FK foreign key (JOB_EXECUTION_ID)
        references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

CREATE SEQUENCE BATCH_STEP_EXECUTION_SEQ MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE BATCH_JOB_EXECUTION_SEQ MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE BATCH_JOB_SEQ MAXVALUE 9223372036854775807 NO CYCLE;

-- Power User Ranking
CREATE TABLE power_user_ranking
(
    id         UUID           NOT NULL,
    user_id    UUID           NOT NULL,
    period     VARCHAR(20)    NOT NULL,
    score      DECIMAL(10, 4) NOT NULL,
    ranking    INTEGER        NOT NULL,
    created_at TIMESTAMPTZ    NOT NULL,
    updated_at TIMESTAMPTZ    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_power_user_ranking_user_period UNIQUE (user_id, period),
    CONSTRAINT fk_power_user_ranking_user FOREIGN KEY (user_id) REFERENCES "user" (id)
);

-- Power User Ranking Index (기간별 랭킹 조회를 위한 인덱스)
CREATE INDEX idx_power_user_ranking_period_ranking ON power_user_ranking (period, ranking);