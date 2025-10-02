-- db/init/init.sql
-- This script runs automatically when PostgreSQL container starts for the first time

-- Create uuid-ossp extension for uuid_generate_v4()
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create the main database schema
CREATE SCHEMA IF NOT EXISTS gomoku;

-- Set the search path
SET search_path TO gomoku, public;

-- PLAYER table (user authentication and profile)
CREATE TABLE player (
    player_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT true,
    account_status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (account_status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

-- AI_OPPONENT table
CREATE TABLE ai_opponent (
    ai_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    difficulty_level VARCHAR(20) NOT NULL CHECK (difficulty_level IN ('EASY', 'MEDIUM', 'HARD', 'EXPERT')),
    model_version VARCHAR(50) NOT NULL,
    model_file_path VARCHAR(500) NOT NULL,
    win_rate_target DECIMAL(5,4) NOT NULL CHECK (win_rate_target >= 0 AND win_rate_target <= 1),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- PLAYER_STATS table
CREATE TABLE player_stats (
    stats_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    player_id UUID NOT NULL REFERENCES player(player_id) ON DELETE CASCADE,
    total_games INTEGER DEFAULT 0 CHECK (total_games >= 0),
    wins INTEGER DEFAULT 0 CHECK (wins >= 0),
    losses INTEGER DEFAULT 0 CHECK (losses >= 0),
    draws INTEGER DEFAULT 0 CHECK (draws >= 0),
    current_mmr INTEGER DEFAULT 1000 CHECK (current_mmr >= 0),
    peak_mmr INTEGER DEFAULT 1000 CHECK (peak_mmr >= 0),
    current_streak INTEGER DEFAULT 0,
    longest_win_streak INTEGER DEFAULT 0 CHECK (longest_win_streak >= 0),
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(player_id)
);

-- GAME table
CREATE TABLE game (
    game_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    game_type VARCHAR(20) NOT NULL CHECK (game_type IN ('HUMAN_VS_HUMAN', 'HUMAN_VS_AI')),
    game_status VARCHAR(20) DEFAULT 'WAITING' CHECK (game_status IN ('WAITING', 'IN_PROGRESS', 'COMPLETED', 'ABANDONED')),
    player1_id UUID NOT NULL REFERENCES player(player_id) ON DELETE CASCADE,
    player2_id UUID REFERENCES player(player_id) ON DELETE CASCADE,
    ai_opponent_id UUID REFERENCES ai_opponent(ai_id) ON DELETE SET NULL,
    winner_type VARCHAR(20) DEFAULT 'NONE' CHECK (winner_type IN ('PLAYER1', 'PLAYER2', 'AI', 'DRAW', 'NONE')),
    winner_id UUID REFERENCES player(player_id) ON DELETE SET NULL,
    total_moves INTEGER DEFAULT 0 CHECK (total_moves >= 0),
    started_at TIMESTAMP WITH TIME ZONE,
    ended_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    final_board_state JSONB,
    game_duration_seconds INTEGER CHECK (game_duration_seconds >= 0),
    CONSTRAINT game_type_consistency CHECK (
        (game_type = 'HUMAN_VS_HUMAN' AND player2_id IS NOT NULL AND ai_opponent_id IS NULL) OR
        (game_type = 'HUMAN_VS_AI' AND player2_id IS NULL AND ai_opponent_id IS NOT NULL)
    ),
    CONSTRAINT winner_consistency CHECK (
        (winner_type = 'PLAYER1' AND winner_id = player1_id) OR
        (winner_type = 'PLAYER2' AND winner_id = player2_id) OR
        (winner_type = 'AI' AND winner_id IS NULL) OR
        (winner_type = 'DRAW' AND winner_id IS NULL) OR
        (winner_type = 'NONE' AND winner_id IS NULL)
    )
);

-- GAME_MOVE table (populated by Kafka consumers for game replay)
CREATE TABLE game_move (
    move_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    game_id UUID NOT NULL REFERENCES game(game_id) ON DELETE CASCADE,
    move_number INTEGER NOT NULL CHECK (move_number > 0),
    player_type VARCHAR(20) NOT NULL CHECK (player_type IN ('HUMAN', 'AI')),
    player_id UUID REFERENCES player(player_id) ON DELETE CASCADE,
    ai_opponent_id UUID REFERENCES ai_opponent(ai_id) ON DELETE CASCADE,
    board_x INTEGER NOT NULL CHECK (board_x >= 0 AND board_x < 15),
    board_y INTEGER NOT NULL CHECK (board_y >= 0 AND board_y < 15),
    stone_color VARCHAR(20) NOT NULL CHECK (stone_color IN ('BLACK', 'WHITE')),
    move_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    time_taken_ms INTEGER CHECK (time_taken_ms >= 0),
    board_state_after_move JSONB,
    UNIQUE(game_id, move_number),
    UNIQUE(game_id, board_x, board_y),
    CONSTRAINT move_player_consistency CHECK (
        (player_type = 'HUMAN' AND player_id IS NOT NULL AND ai_opponent_id IS NULL) OR
        (player_type = 'AI' AND player_id IS NULL AND ai_opponent_id IS NOT NULL)
    )
);

-- Create indexes for performance (5 core tables only)
-- Player indexes
CREATE INDEX idx_player_username ON player(username);
CREATE INDEX idx_player_email ON player(email);
CREATE INDEX idx_player_active ON player(is_active);
CREATE INDEX idx_player_account_status ON player(account_status);

-- AI opponent indexes
CREATE INDEX idx_ai_opponent_difficulty ON ai_opponent(difficulty_level);
CREATE INDEX idx_ai_opponent_active ON ai_opponent(is_active);

-- Player stats indexes
CREATE INDEX idx_player_stats_player_id ON player_stats(player_id);
CREATE INDEX idx_player_stats_mmr ON player_stats(current_mmr);

-- Game indexes
CREATE INDEX idx_game_status ON game(game_status);
CREATE INDEX idx_game_type ON game(game_type);
CREATE INDEX idx_game_player1 ON game(player1_id);
CREATE INDEX idx_game_player2 ON game(player2_id);
CREATE INDEX idx_game_ai_opponent ON game(ai_opponent_id);
CREATE INDEX idx_game_created_at ON game(created_at);
CREATE INDEX idx_game_winner ON game(winner_id);

-- Game move indexes (for replay queries)
CREATE INDEX idx_move_game_id ON game_move(game_id);
CREATE INDEX idx_move_game_move_number ON game_move(game_id, move_number);
CREATE INDEX idx_move_board_position ON game_move(board_x, board_y);
CREATE INDEX idx_move_timestamp ON game_move(move_timestamp);

-- Create functions for automatic timestamp updates
CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for automatic timestamp updates
CREATE TRIGGER update_player_stats_modified BEFORE UPDATE ON player_stats
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();

CREATE TRIGGER update_ai_opponent_modified BEFORE UPDATE ON ai_opponent
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();

-- Insert default AI opponents
INSERT INTO ai_opponent (name, difficulty_level, model_version, model_file_path, win_rate_target) VALUES
    ('Rookie Bot', 'EASY', 'v1.0', '/models/rookie_v1.0.pth', 0.3),
    ('Challenger Bot', 'MEDIUM', 'v1.0', '/models/challenger_v1.0.pth', 0.5),
    ('Expert Bot', 'HARD', 'v1.0', '/models/expert_v1.0.pth', 0.7),
    ('Master Bot', 'EXPERT', 'v1.0', '/models/master_v1.0.pth', 0.85);

-- Grant permissions to application user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA gomoku TO gomoku_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA gomoku TO gomoku_user;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA gomoku TO gomoku_user;
