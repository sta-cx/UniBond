-- V1__init_schema.sql

-- Enums
CREATE TYPE auth_provider AS ENUM ('APPLE', 'EMAIL');
CREATE TYPE couple_status AS ENUM ('ACTIVE', 'DISSOLVED');
CREATE TYPE quiz_type AS ENUM ('BLIND', 'GUESS', 'THEME');
CREATE TYPE generation_source AS ENUM ('AI', 'FALLBACK_POOL');

-- Users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255),
    nickname VARCHAR(20),
    avatar_url VARCHAR(500),
    auth_provider auth_provider NOT NULL,
    apple_sub VARCHAR(255),
    partner_id BIGINT,
    invite_code VARCHAR(6) NOT NULL,
    device_token VARCHAR(255),
    timezone VARCHAR(50) DEFAULT 'Asia/Shanghai',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_apple_sub UNIQUE (apple_sub),
    CONSTRAINT uk_users_invite_code UNIQUE (invite_code),
    CONSTRAINT fk_users_partner FOREIGN KEY (partner_id) REFERENCES users(id)
);

-- Couples
CREATE TABLE couples (
    id BIGSERIAL PRIMARY KEY,
    user_a_id BIGINT NOT NULL,
    user_b_id BIGINT NOT NULL,
    anniversary_date DATE,
    bind_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    status couple_status NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT fk_couples_user_a FOREIGN KEY (user_a_id) REFERENCES users(id),
    CONSTRAINT fk_couples_user_b FOREIGN KEY (user_b_id) REFERENCES users(id)
);

-- Daily quizzes
CREATE TABLE daily_quizzes (
    id BIGSERIAL PRIMARY KEY,
    quiz_date DATE NOT NULL,
    couple_id BIGINT NOT NULL,
    quiz_type quiz_type NOT NULL,
    theme VARCHAR(50),
    questions JSONB NOT NULL,
    generation_source generation_source NOT NULL DEFAULT 'AI',
    prompt_context JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_daily_quizzes_couple FOREIGN KEY (couple_id) REFERENCES couples(id),
    CONSTRAINT uk_daily_quizzes_couple_date UNIQUE (couple_id, quiz_date)
);

-- Quiz answers
CREATE TABLE quiz_answers (
    id BIGSERIAL PRIMARY KEY,
    daily_quiz_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    couple_id BIGINT NOT NULL,
    answers JSONB NOT NULL,
    partner_guess JSONB,
    score INT,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    revealed BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_quiz_answers_quiz FOREIGN KEY (daily_quiz_id) REFERENCES daily_quizzes(id),
    CONSTRAINT fk_quiz_answers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_quiz_answers_couple FOREIGN KEY (couple_id) REFERENCES couples(id),
    CONSTRAINT uk_quiz_answers_quiz_user UNIQUE (daily_quiz_id, user_id)
);

-- Question pool (fallback)
CREATE TABLE question_pool (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    quiz_type quiz_type NOT NULL,
    question TEXT NOT NULL,
    options JSONB NOT NULL,
    difficulty INT NOT NULL DEFAULT 1,
    used_count INT NOT NULL DEFAULT 0
);

-- Mood status
CREATE TABLE mood_status (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    couple_id BIGINT NOT NULL,
    mood_emoji VARCHAR(10) NOT NULL,
    mood_text VARCHAR(50),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_mood_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_mood_couple FOREIGN KEY (couple_id) REFERENCES couples(id)
);

-- Achievements
CREATE TABLE achievements (
    id BIGSERIAL PRIMARY KEY,
    couple_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    unlocked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_achievements_couple FOREIGN KEY (couple_id) REFERENCES couples(id),
    CONSTRAINT uk_achievements_couple_type UNIQUE (couple_id, type)
);

-- Daily stats
CREATE TABLE daily_stats (
    couple_id BIGINT NOT NULL,
    stat_date DATE NOT NULL,
    match_score INT NOT NULL DEFAULT 0,
    streak_days INT NOT NULL DEFAULT 0,
    quiz_type_played quiz_type,

    PRIMARY KEY (couple_id, stat_date),
    CONSTRAINT fk_daily_stats_couple FOREIGN KEY (couple_id) REFERENCES couples(id)
);

-- Indexes
CREATE INDEX idx_daily_quizzes_date ON daily_quizzes(quiz_date);
CREATE INDEX idx_quiz_answers_couple ON quiz_answers(couple_id);
CREATE INDEX idx_mood_status_user ON mood_status(user_id);
CREATE INDEX idx_achievements_couple ON achievements(couple_id);
