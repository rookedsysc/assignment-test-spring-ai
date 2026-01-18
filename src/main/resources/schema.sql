-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- Thread table
-- 대화 목록을 그룹화하는 스레드 테이블
CREATE TABLE IF NOT EXISTS thread
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Thread table indexes
CREATE INDEX IF NOT EXISTS idx_thread_user_id ON thread (user_id);
CREATE INDEX IF NOT EXISTS idx_thread_updated_at ON thread (updated_at);

-- Chat History table
-- 사용자와 AI 어시스턴트 간의 대화 내용을 저장하는 테이블
CREATE TABLE IF NOT EXISTS chat_history
(
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    thread_id          UUID NOT NULL REFERENCES thread (id) ON DELETE CASCADE,
    user_id            VARCHAR(255) NOT NULL,
    user_message       TEXT NOT NULL,
    assistant_message  TEXT NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Chat History table indexes
CREATE INDEX IF NOT EXISTS idx_chat_history_thread_id ON chat_history (thread_id);
CREATE INDEX IF NOT EXISTS idx_chat_history_user_id ON chat_history (user_id);
CREATE INDEX IF NOT EXISTS idx_chat_history_created_at ON chat_history (created_at);
