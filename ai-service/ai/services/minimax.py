"""
Minimax algorithm with alpha-beta pruning for Gomoku AI.

Combines traditional game tree search with neural network evaluation.
"""
from typing import Tuple, Optional
import math
from ai.game import Board, BLACK, WHITE


class MinimaxAI:
    """
    Minimax AI with alpha-beta pruning.

    Uses depth-limited search with neural network position evaluation.
    Different difficulty levels use different search depths.
    """

    # Difficulty configurations: (depth, use_nn)
    DIFFICULTY_CONFIGS = {
        'easy': (1, False),      # Depth 1, heuristic only
        'medium': (2, False),    # Depth 2, heuristic only
        'hard': (3, True),       # Depth 3, with neural network
        'expert': (4, True),     # Depth 4, with neural network
    }

    def __init__(self, difficulty: str = 'medium', model=None):
        """
        Initialize Minimax AI.

        Args:
            difficulty: 'easy', 'medium', 'hard', or 'expert'
            model: Optional GomokuNet model for neural network evaluation
        """
        if difficulty not in self.DIFFICULTY_CONFIGS:
            raise ValueError(f"Invalid difficulty: {difficulty}")

        self.difficulty = difficulty
        self.max_depth, self.use_nn = self.DIFFICULTY_CONFIGS[difficulty]
        self.model = model
        self.nodes_evaluated = 0

    def get_best_move(self, board: Board, player: int) -> Tuple[int, int]:
        """
        Get the best move for the current player.

        Args:
            board: Current board state
            player: Current player (BLACK or WHITE)

        Returns:
            Tuple (row, col) of best move
        """
        self.nodes_evaluated = 0

        # Get candidate moves (only consider moves near existing stones)
        moves = board.get_smart_moves(player, limit=30)

        if not moves:
            # Fallback to center if no smart moves
            return (7, 7)

        best_move = moves[0]
        best_score = -math.inf

        # Evaluate each move
        for move in moves:
            row, col = move

            # Make move on copy
            board_copy = board.copy()
            board_copy.make_move(row, col, player)

            # Check if this move wins immediately
            if board_copy.check_win(row, col):
                return move

            # Evaluate position after this move
            score = self._minimax(
                board_copy,
                depth=self.max_depth - 1,
                alpha=-math.inf,
                beta=math.inf,
                is_maximizing=False,
                player=player
            )

            if score > best_score:
                best_score = score
                best_move = move

        return best_move

    def _minimax(
        self,
        board: Board,
        depth: int,
        alpha: float,
        beta: float,
        is_maximizing: bool,
        player: int
    ) -> float:
        """
        Minimax algorithm with alpha-beta pruning.

        Args:
            board: Current board state
            depth: Remaining search depth
            alpha: Alpha value for pruning
            beta: Beta value for pruning
            is_maximizing: True if maximizing player's turn
            player: Original player (for evaluation)

        Returns:
            Evaluation score for this position
        """
        self.nodes_evaluated += 1

        # Terminal conditions
        if depth == 0:
            return self._evaluate_position(board, player)

        opponent = WHITE if player == BLACK else BLACK
        current_player = player if is_maximizing else opponent

        # Get candidate moves
        moves = board.get_smart_moves(current_player, limit=20)

        if not moves or board.is_full():
            return self._evaluate_position(board, player)

        if is_maximizing:
            max_eval = -math.inf
            for row, col in moves:
                board_copy = board.copy()
                board_copy.make_move(row, col, current_player)

                # Check for immediate win
                if board_copy.check_win(row, col):
                    return 10000 - (self.max_depth - depth)  # Prefer quicker wins

                eval_score = self._minimax(
                    board_copy, depth - 1, alpha, beta, False, player
                )
                max_eval = max(max_eval, eval_score)
                alpha = max(alpha, eval_score)

                if beta <= alpha:
                    break  # Beta cutoff

            return max_eval
        else:
            min_eval = math.inf
            for row, col in moves:
                board_copy = board.copy()
                board_copy.make_move(row, col, current_player)

                # Check for immediate loss
                if board_copy.check_win(row, col):
                    return -10000 + (self.max_depth - depth)  # Avoid quicker losses

                eval_score = self._minimax(
                    board_copy, depth - 1, alpha, beta, True, player
                )
                min_eval = min(min_eval, eval_score)
                beta = min(beta, eval_score)

                if beta <= alpha:
                    break  # Alpha cutoff

            return min_eval

    def _evaluate_position(self, board: Board, player: int) -> float:
        """
        Evaluate a board position.

        Uses neural network if available and enabled, otherwise uses heuristic.

        Args:
            board: Board to evaluate
            player: Player to evaluate for

        Returns:
            Evaluation score (higher is better for player)
        """
        if self.use_nn and self.model is not None:
            return self._evaluate_with_nn(board, player)
        else:
            return self._evaluate_heuristic(board, player)

    def _evaluate_with_nn(self, board: Board, player: int) -> float:
        """
        Evaluate position using neural network.

        Args:
            board: Board to evaluate
            player: Player to evaluate for

        Returns:
            Evaluation score from neural network
        """
        from ai.ml import board_to_tensor

        board_tensor = board_to_tensor(board.to_list(), player)
        _, value = self.model.predict(board_tensor)

        # value is in [-1, 1], scale to match heuristic range
        return value * 1000

    def _evaluate_heuristic(self, board: Board, player: int) -> float:
        """
        Evaluate position using hand-crafted heuristic.

        Counts patterns of consecutive stones:
        - 5 in a row: win (10000)
        - 4 in a row (open): 1000
        - 3 in a row (open): 100
        - 2 in a row (open): 10

        Args:
            board: Board to evaluate
            player: Player to evaluate for

        Returns:
            Heuristic evaluation score
        """
        opponent = WHITE if player == BLACK else BLACK

        player_score = self._count_patterns(board, player)
        opponent_score = self._count_patterns(board, opponent)

        return player_score - opponent_score

    def _count_patterns(self, board: Board, player: int) -> float:
        """
        Count valuable patterns for a player.

        Args:
            board: Board state
            player: Player to count for

        Returns:
            Total pattern score
        """
        score = 0
        board_array = board.board

        # Pattern values
        patterns = {
            5: 10000,  # Five in a row (win)
            4: 1000,   # Four in a row
            3: 100,    # Three in a row
            2: 10,     # Two in a row
        }

        # Check all directions from each cell
        directions = [(0, 1), (1, 0), (1, 1), (1, -1)]

        for row in range(15):
            for col in range(15):
                if board_array[row, col] != player:
                    continue

                for dr, dc in directions:
                    length = self._count_line_length(board, row, col, dr, dc, player)
                    if length >= 2:
                        score += patterns.get(min(length, 5), 0)

        return score

    def _count_line_length(
        self,
        board: Board,
        row: int,
        col: int,
        dr: int,
        dc: int,
        player: int
    ) -> int:
        """
        Count consecutive stones in one direction.

        Args:
            board: Board state
            row: Starting row
            col: Starting column
            dr: Row direction
            dc: Column direction
            player: Player to count

        Returns:
            Number of consecutive stones
        """
        count = 1
        r, c = row + dr, col + dc

        while (0 <= r < 15 and 0 <= c < 15 and
               board.board[r, c] == player):
            count += 1
            r += dr
            c += dc

        return count
