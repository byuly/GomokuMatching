-- db/init/01_create_tables.sql
-- This script runs automatically when PostgreSQL container starts for the first time

-- Create the main database schema
CREATE SCHEMA IF NOT EXISTS gomoku;

-- Set the search path
SET search_path TO gomoku, public;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create enums
CREATE TYPE account_status_enum AS ENUM ('ACTIVE', 'SUSPENDED', 'DELETED');
CREATE TYPE difficulty_level_enum AS ENUM ('EASY', 'MEDIUM', 'HARD', 'EXPERT');
CREATE TYPE game_type_enum AS ENUM ('HUMAN_VS_HUMAN', 'HUMAN_VS_AI');
CREATE TYPE game_status_enum AS ENUM ('WAITING', 'IN_PROGRESS', 'COMPLETED', 'ABANDONED');
CREATE TYPE winner_type_enum AS ENUM ('PLAYER1', 'PLAYER2', 'AI', 'DRAW', 'NONE');
CREATE TYPE player_type_enum AS ENUM ('HUMAN', 'AI');
CREATE TYPE stone_color_enum AS ENUM ('BLACK', 'WHITE');
CREATE TYPE preferred_opponent_enum AS ENUM ('HUMAN_ONLY', 'AI_ONLY', 'ANY');
CREATE TYPE queue_status_enum AS ENUM ('WAITING', 'MATCHED', 'EXPIRED', 'CANCELLED');
CREATE TYPE processing_status_enum AS ENUM ('PENDING', 'PROCESSED', 'FAILED');
CREATE TYPE game_outcome_enum AS ENUM ('PLAYER1_WIN', 'PLAYER2_WIN', 'AI_WIN', 'DRAW', 'ABANDONED');

-- PLAYER table
CREATE TABLE player (
                        player_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                        username VARCHAR(50) NOT NULL UNIQUE,
                        email VARCHAR(255) NOT NULL UNIQUE,
                        password_hash VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                        last_login TIMESTAMP WITH TIME ZONE,
                        is_active BOOLEAN DEFAULT true,
                        account_status account_status_enum DEFAULT 'ACTIVE'
);

-- AI_OPPONENT table
CREATE TABLE ai_opponent (
                             ai_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                             name VARCHAR(100) NOT NULL,
                             difficulty_level difficulty_level_enum NOT NULL,
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
                      game_type game_type_enum NOT NULL,
                      game_status game_status_enum DEFAULT 'WAITING',
                      player1_id UUID NOT NULL REFERENCES player(player_id) ON DELETE CASCADE,
                      player2_id UUID REFERENCES player(player_id) ON DELETE CASCADE,
                      ai_opponent_id UUID REFERENCES ai_opponent(ai_id) ON DELETE SET NULL,
                      winner_type winner_type_enum DEFAULT 'NONE',
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

-- GAME_MOVE table
CREATE TABLE game_move (
                           move_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                           game_id UUID NOT NULL REFERENCES game(game_id) ON DELETE CASCADE,
                           move_number INTEGER NOT NULL CHECK (move_number > 0),
                           player_type player_type_enum NOT NULL,
                           player_id UUID REFERENCES player(player_id) ON DELETE CASCADE,
                           ai_opponent_id UUID REFERENCES ai_opponent(ai_id) ON DELETE CASCADE,
                           board_x INTEGER NOT NULL CHECK (board_x >= 0 AND board_x < 15),
                           board_y INTEGER NOT NULL CHECK (board_y >= 0 AND board_y < 15),
                           stone_color stone_color_enum NOT NULL,
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

-- MATCHMAKING_QUEUE table
CREATE TABLE matchmaking_queue (
                                   queue_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                   player_id UUID NOT NULL REFERENCES player(player_id) ON DELETE CASCADE,
                                   preferred_opponent preferred_opponent_enum DEFAULT 'ANY',
                                   ai_difficulty difficulty_level_enum,
                                   mmr_min INTEGER CHECK (mmr_min >= 0),
                                   mmr_max INTEGER CHECK (mmr_max >= 0 AND mmr_max >= mmr_min),
                                   queued_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                   queue_status queue_status_enum DEFAULT 'WAITING',
                                   expires_at TIMESTAMP WITH TIME ZONE DEFAULT (CURRENT_TIMESTAMP + INTERVAL '5 minutes'),
                                   CONSTRAINT ai_difficulty_when_ai_preferred CHECK (
                                       preferred_opponent != 'AI_ONLY' OR ai_difficulty IS NOT NULL
)
    );

-- GAME_SESSION table
CREATE TABLE game_session (
                              session_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                              game_id UUID NOT NULL REFERENCES game(game_id) ON DELETE CASCADE,
                              player_id UUID NOT NULL REFERENCES player(player_id) ON DELETE CASCADE,
                              websocket_session_id VARCHAR(255) NOT NULL,
                              connected_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                              disconnected_at TIMESTAMP WITH TIME ZONE,
                              is_active BOOLEAN DEFAULT true,
                              reconnect_count INTEGER DEFAULT 0 CHECK (reconnect_count >= 0)
);

-- KAFKA_EVENT_LOG table
CREATE TABLE kafka_event_log (
                                 event_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                 topic_name VARCHAR(100) NOT NULL,
                                 event_type VARCHAR(100) NOT NULL,
                                 related_game_id UUID REFERENCES game(game_id) ON DELETE SET NULL,
                                 related_player_id UUID REFERENCES player(player_id) ON DELETE SET NULL,
                                 event_payload JSONB NOT NULL,
                                 event_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                 kafka_partition VARCHAR(10),
                                 kafka_offset BIGINT,
                                 processing_status processing_status_enum DEFAULT 'PENDING'
);

-- GAME_ANALYTICS table
CREATE TABLE game_analytics (
                                analytics_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                game_id UUID NOT NULL REFERENCES game(game_id) ON DELETE CASCADE,
                                game_outcome game_outcome_enum NOT NULL,
                                game_length_moves INTEGER CHECK (game_length_moves >= 0),
                                average_move_time_ms INTEGER CHECK (average_move_time_ms >= 0),
                                player1_total_time_ms INTEGER CHECK (player1_total_time_ms >= 0),
                                player2_total_time_ms INTEGER CHECK (player2_total_time_ms >= 0),
                                opening_moves_pattern JSONB,
                                winning_pattern JSONB,
                                had_disconnections BOOLEAN DEFAULT false,
                                analyzed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                UNIQUE(game_id)
);

-- LEADERBOARD table
CREATE TABLE leaderboard (
                             leaderboard_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                             player_id UUID NOT NULL REFERENCES player(player_id) ON DELETE CASCADE,
                             current_rank INTEGER NOT NULL CHECK (current_rank > 0),
                             previous_rank INTEGER CHECK (previous_rank > 0),
                             mmr_score INTEGER NOT NULL CHECK (mmr_score >= 0),
                             games_played_season INTEGER DEFAULT 0 CHECK (games_played_season >= 0),
                             wins_season INTEGER DEFAULT 0 CHECK (wins_season >= 0 AND wins_season <= games_played_season),
                             win_rate_season DECIMAL(5,4) DEFAULT 0 CHECK (win_rate_season >= 0 AND win_rate_season <= 1),
                             last_rank_update TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             season_identifier VARCHAR(50) NOT NULL DEFAULT 'season_2025_1',
                             UNIQUE(player_id, season_identifier),
                             UNIQUE(current_rank, season_identifier)
);

-- AI_MODEL_PERFORMANCE table
CREATE TABLE ai_model_performance (
                                      performance_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                      ai_opponent_id UUID NOT NULL REFERENCES ai_opponent(ai_id) ON DELETE CASCADE,
                                      model_version VARCHAR(50) NOT NULL,
                                      games_played INTEGER DEFAULT 0 CHECK (games_played >= 0),
                                      games_won INTEGER DEFAULT 0 CHECK (games_won >= 0 AND games_won <= games_played),
                                      actual_win_rate DECIMAL(5,4) DEFAULT 0 CHECK (actual_win_rate >= 0 AND actual_win_rate <= 1),
                                      target_win_rate DECIMAL(5,4) NOT NULL CHECK (target_win_rate >= 0 AND target_win_rate <= 1),
                                      total_moves_made INTEGER DEFAULT 0 CHECK (total_moves_made >= 0),
                                      average_thinking_time_ms DECIMAL(10,2) CHECK (average_thinking_time_ms >= 0),
                                      performance_period_start TIMESTAMP WITH TIME ZONE NOT NULL,
                                      performance_period_end TIMESTAMP WITH TIME ZONE NOT NULL,
                                      performance_metrics JSONB,
                                      CONSTRAINT valid_performance_period CHECK (performance_period_end > performance_period_start)
);

-- PLAYER_AI_MATCHUP table
CREATE TABLE player_ai_matchup (
                                   matchup_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                   player_id UUID NOT NULL REFERENCES player(player_id) ON DELETE CASCADE,
                                   ai_opponent_id UUID NOT NULL REFERENCES ai_opponent(ai_id) ON DELETE CASCADE,
                                   games_played INTEGER DEFAULT 0 CHECK (games_played >= 0),
                                   player_wins INTEGER DEFAULT 0 CHECK (player_wins >= 0 AND player_wins <= games_played),
                                   ai_wins INTEGER DEFAULT 0 CHECK (ai_wins >= 0 AND ai_wins <= games_played),
                                   draws INTEGER DEFAULT 0 CHECK (draws >= 0 AND draws <= games_played),
                                   player_win_rate DECIMAL(5,4) DEFAULT 0 CHECK (player_win_rate >= 0 AND player_win_rate <= 1),
                                   last_game_date TIMESTAMP WITH TIME ZONE,
                                   first_game_date TIMESTAMP WITH TIME ZONE,
                                   UNIQUE(player_id, ai_opponent_id),
                                   CONSTRAINT matchup_games_consistency CHECK (player_wins + ai_wins + draws = games_played)
);

-- Create indexes for performance
CREATE INDEX idx_player_username ON player(username);
CREATE INDEX idx_player_email ON player(email);
CREATE INDEX idx_player_active ON player(is_active);
CREATE INDEX idx_player_account_status ON player(account_status);

CREATE INDEX idx_ai_opponent_difficulty ON ai_opponent(difficulty_level);
CREATE INDEX idx_ai_opponent_active ON ai_opponent(is_active);

CREATE INDEX idx_player_stats_player_id ON player_stats(player_id);
CREATE INDEX idx_player_stats_mmr ON player_stats(current_mmr);

CREATE INDEX idx_game_status ON game(game_status);
CREATE INDEX idx_game_type ON game(game_type);
CREATE INDEX idx_game_player1 ON game(player1_id);
CREATE INDEX idx_game_player2 ON game(player2_id);
CREATE INDEX idx_game_ai_opponent ON game(ai_opponent_id);
CREATE INDEX idx_game_created_at ON game(created_at);
CREATE INDEX idx_game_winner ON game(winner_id);

CREATE INDEX idx_move_game_id ON game_move(game_id);
CREATE INDEX idx_move_game_move_number ON game_move(game_id, move_number);
CREATE INDEX idx_move_board_position ON game_move(board_x, board_y);
CREATE INDEX idx_move_timestamp ON game_move(move_timestamp);

CREATE INDEX idx_queue_status ON matchmaking_queue(queue_status);
CREATE INDEX idx_queue_player ON matchmaking_queue(player_id);
CREATE INDEX idx_queue_mmr ON matchmaking_queue(mmr_min, mmr_max);
CREATE INDEX idx_queue_expires_at ON matchmaking_queue(expires_at);

CREATE INDEX idx_session_game_id ON game_session(game_id);
CREATE INDEX idx_session_player_id ON game_session(player_id);
CREATE INDEX idx_session_websocket_id ON game_session(websocket_session_id);
CREATE INDEX idx_session_active ON game_session(is_active);

CREATE INDEX idx_kafka_topic ON kafka_event_log(topic_name);
CREATE INDEX idx_kafka_event_type ON kafka_event_log(event_type);
CREATE INDEX idx_kafka_status ON kafka_event_log(processing_status);
CREATE INDEX idx_kafka_timestamp ON kafka_event_log(event_timestamp);
CREATE INDEX idx_kafka_game_id ON kafka_event_log(related_game_id);
CREATE INDEX idx_kafka_player_id ON kafka_event_log(related_player_id);

CREATE INDEX idx_analytics_game ON game_analytics(game_id);
CREATE INDEX idx_analytics_outcome ON game_analytics(game_outcome);
CREATE INDEX idx_analytics_analyzed_at ON game_analytics(analyzed_at);

CREATE INDEX idx_leaderboard_season ON leaderboard(season_identifier);
CREATE INDEX idx_leaderboard_rank ON leaderboard(current_rank, season_identifier);
CREATE INDEX idx_leaderboard_mmr ON leaderboard(mmr_score, season_identifier);
CREATE INDEX idx_leaderboard_player ON leaderboard(player_id);

CREATE INDEX idx_ai_performance_ai_id ON ai_model_performance(ai_opponent_id);
CREATE INDEX idx_ai_performance_model ON ai_model_performance(model_version);
CREATE INDEX idx_ai_performance_period ON ai_model_performance(performance_period_start, performance_period_end);

CREATE INDEX idx_matchup_player ON player_ai_matchup(player_id);
CREATE INDEX idx_matchup_ai ON player_ai_matchup(ai_opponent_id);
CREATE INDEX idx_matchup_last_game ON player_ai_matchup(last_game_date);

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

-- Grant permissions on types
GRANT USAGE ON TYPE account_status_enum TO gomoku_user;
GRANT USAGE ON TYPE difficulty_level_enum TO gomoku_user;
GRANT USAGE ON TYPE game_type_enum TO gomoku_user;
GRANT USAGE ON TYPE game_status_enum TO gomoku_user;
GRANT USAGE ON TYPE winner_type_enum TO gomoku_user;
GRANT USAGE ON TYPE player_type_enum TO gomoku_user;
GRANT USAGE ON TYPE stone_color_enum TO gomoku_user;
GRANT USAGE ON TYPE preferred_opponent_enum TO gomoku_user;
GRANT USAGE ON TYPE queue_status_enum TO gomoku_user;
GRANT USAGE ON TYPE processing_status_enum TO gomoku_user;
GRANT USAGE ON TYPE game_outcome_enum TO gomoku_user;