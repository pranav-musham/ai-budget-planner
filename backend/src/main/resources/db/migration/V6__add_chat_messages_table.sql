-- Migration: Add chat messages table for AI chat feature
-- Version: V6
-- Description: Creates chat_messages table to store conversation history between users and AI assistant

CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    response TEXT NOT NULL,
    context_data JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_chat_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- Indexes for performance optimization
CREATE INDEX idx_chat_messages_user_id ON chat_messages(user_id);
CREATE INDEX idx_chat_messages_created_at ON chat_messages(created_at DESC);
CREATE INDEX idx_chat_messages_user_created ON chat_messages(user_id, created_at DESC);

-- Comment on table
COMMENT ON TABLE chat_messages IS 'Stores conversation history between users and AI budget assistant';
COMMENT ON COLUMN chat_messages.context_data IS 'JSON data containing spending context used to generate response';
