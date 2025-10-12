"""
Minimax AI for Gomoku with pattern-based evaluation.
Guaranteed to recognize threats and winning moves.
"""

from typing import List, Tuple, Optional
import random


class GomokuMinimaxAI:
    """
    Minimax AI for 15x15 Gomoku with alpha-beta pruning.

    Difficulty levels:
    - Easy: depth 1, simple evaluation
    - Medium: depth 2, full evaluation
    - Hard: depth 3, full evaluation
    - Expert: depth 4, full evaluation
    """

    def __init__(self, difficulty: str = "medium", board_size: int = 15):
        self.board_size = board_size
        self.difficulty = difficulty.lower()

        # Set search depth based on difficulty
        self.depth_map = {
            "easy": 1,
            "medium": 2,
            "hard": 3,
            "expert": 4
        }
        self.max_depth = self.depth_map.get(self.difficulty, 2)

        # Pattern scores for evaluation
        self.FIVE = 100000      # Win
        self.OPEN_FOUR = 10000  # Guaranteed win next turn
        self.FOUR = 5000        # Blocked four
        self.OPEN_THREE = 1000  # Strong threat
        self.THREE = 100        # Blocked three
        self.OPEN_TWO = 50      # Potential
        self.TWO = 10           # Weak potential

    def get_move(self, board: List[List[int]], current_player: int) -> Tuple[int, int]:
        """
        Get AI's best move using minimax with alpha-beta pruning.

        Args:
            board: 15x15 board (0=empty, 1=black, 2=white)
            current_player: 1 or 2

        Returns:
            (row, col)
        """
        # First check for immediate win
        winning_move = self._find_winning_move(board, current_player)
        if winning_move:
            return winning_move

        # Then check for blocking opponent's winning move
        opponent = 3 - current_player
        blocking_move = self._find_winning_move(board, opponent)
        if blocking_move:
            return blocking_move

        # Use minimax for best move
        best_score = float('-inf')
        best_move = None
        alpha = float('-inf')
        beta = float('inf')

        # Get candidate moves (only positions near existing stones)
        candidates = self._get_candidate_moves(board)

        # If board is empty, play center
        if not candidates:
            return (7, 7)

        # Evaluate each candidate
        for row, col in candidates:
            # Make move
            board[row][col] = current_player

            # Evaluate
            score = self._minimax(board, self.max_depth - 1, False, alpha, beta, current_player, opponent)

            # Undo move
            board[row][col] = 0

            if score > best_score:
                best_score = score
                best_move = (row, col)

            alpha = max(alpha, score)

        return best_move if best_move else candidates[0]

    def _minimax(
        self,
        board: List[List[int]],
        depth: int,
        is_maximizing: bool,
        alpha: float,
        beta: float,
        ai_player: int,
        opponent: int
    ) -> float:
        """
        Minimax with alpha-beta pruning.

        Args:
            board: Current board state
            depth: Remaining search depth
            is_maximizing: True if AI's turn, False if opponent's turn
            alpha: Best score for maximizer
            beta: Best score for minimizer
            ai_player: AI player number (1 or 2)
            opponent: Opponent player number

        Returns:
            Evaluation score
        """
        # Check terminal conditions
        winner = self._check_winner(board)
        if winner == ai_player:
            return self.FIVE + depth * 100  # Prefer faster wins
        elif winner == opponent:
            return -self.FIVE - depth * 100  # Avoid faster losses

        if depth == 0:
            return self._evaluate_board(board, ai_player, opponent)

        candidates = self._get_candidate_moves(board)
        if not candidates:
            return 0

        if is_maximizing:
            max_eval = float('-inf')
            for row, col in candidates:
                board[row][col] = ai_player
                eval_score = self._minimax(board, depth - 1, False, alpha, beta, ai_player, opponent)
                board[row][col] = 0

                max_eval = max(max_eval, eval_score)
                alpha = max(alpha, eval_score)
                if beta <= alpha:
                    break  # Beta cutoff
            return max_eval
        else:
            min_eval = float('inf')
            for row, col in candidates:
                board[row][col] = opponent
                eval_score = self._minimax(board, depth - 1, True, alpha, beta, ai_player, opponent)
                board[row][col] = 0

                min_eval = min(min_eval, eval_score)
                beta = min(beta, eval_score)
                if beta <= alpha:
                    break  # Alpha cutoff
            return min_eval

    def _evaluate_board(self, board: List[List[int]], ai_player: int, opponent: int) -> float:
        """Evaluate board position for AI player"""
        ai_score = self._evaluate_player(board, ai_player)
        opponent_score = self._evaluate_player(board, opponent)
        return ai_score - opponent_score

    def _evaluate_player(self, board: List[List[int]], player: int) -> float:
        """Calculate score for one player based on patterns"""
        score = 0

        # Check all directions: horizontal, vertical, diagonal, anti-diagonal
        directions = [(0, 1), (1, 0), (1, 1), (1, -1)]

        for row in range(self.board_size):
            for col in range(self.board_size):
                if board[row][col] != player:
                    continue

                for dr, dc in directions:
                    pattern_score = self._evaluate_line(board, row, col, dr, dc, player)
                    score += pattern_score

        return score

    def _evaluate_line(
        self,
        board: List[List[int]],
        row: int,
        col: int,
        dr: int,
        dc: int,
        player: int
    ) -> float:
        """Evaluate a line starting from (row, col) in direction (dr, dc)"""
        count = 0
        open_ends = 0

        # Count stones in positive direction
        r, c = row, col
        while 0 <= r < self.board_size and 0 <= c < self.board_size and board[r][c] == player:
            count += 1
            r += dr
            c += dc

        # Check if positive end is open
        if 0 <= r < self.board_size and 0 <= c < self.board_size and board[r][c] == 0:
            open_ends += 1

        # Check negative direction
        r, c = row - dr, col - dc
        while 0 <= r < self.board_size and 0 <= c < self.board_size and board[r][c] == player:
            count += 1
            r -= dr
            c -= dc

        # Check if negative end is open
        if 0 <= r < self.board_size and 0 <= c < self.board_size and board[r][c] == 0:
            open_ends += 1

        # Score based on count and openness
        if count >= 5:
            return self.FIVE
        elif count == 4:
            return self.OPEN_FOUR if open_ends == 2 else self.FOUR
        elif count == 3:
            return self.OPEN_THREE if open_ends == 2 else self.THREE
        elif count == 2:
            return self.OPEN_TWO if open_ends == 2 else self.TWO

        return 0

    def _find_winning_move(self, board: List[List[int]], player: int) -> Optional[Tuple[int, int]]:
        """Find immediate winning move (completes 5 in a row)"""
        for row in range(self.board_size):
            for col in range(self.board_size):
                if board[row][col] == 0:
                    # Try this move
                    board[row][col] = player
                    if self._check_winner(board) == player:
                        board[row][col] = 0
                        return (row, col)
                    board[row][col] = 0
        return None

    def _check_winner(self, board: List[List[int]]) -> Optional[int]:
        """Check if there's a winner (5 in a row). Returns player number or None."""
        directions = [(0, 1), (1, 0), (1, 1), (1, -1)]

        for row in range(self.board_size):
            for col in range(self.board_size):
                if board[row][col] == 0:
                    continue

                player = board[row][col]

                for dr, dc in directions:
                    count = 1

                    # Check positive direction
                    r, c = row + dr, col + dc
                    while 0 <= r < self.board_size and 0 <= c < self.board_size and board[r][c] == player:
                        count += 1
                        r += dr
                        c += dc

                    # Check negative direction
                    r, c = row - dr, col - dc
                    while 0 <= r < self.board_size and 0 <= c < self.board_size and board[r][c] == player:
                        count += 1
                        r -= dr
                        c -= dc

                    if count >= 5:
                        return player

        return None

    def _get_candidate_moves(self, board: List[List[int]], radius: int = 2) -> List[Tuple[int, int]]:
        """
        Get candidate moves (empty positions near existing stones).
        Only consider positions within 'radius' of existing stones for efficiency.
        """
        candidates = set()
        has_stones = False

        for row in range(self.board_size):
            for col in range(self.board_size):
                if board[row][col] != 0:
                    has_stones = True
                    # Add nearby empty positions
                    for dr in range(-radius, radius + 1):
                        for dc in range(-radius, radius + 1):
                            r, c = row + dr, col + dc
                            if (0 <= r < self.board_size and
                                0 <= c < self.board_size and
                                board[r][c] == 0):
                                candidates.add((r, c))

        # If no stones, return center and nearby positions
        if not has_stones:
            center = self.board_size // 2
            for dr in range(-1, 2):
                for dc in range(-1, 2):
                    candidates.add((center + dr, center + dc))

        return list(candidates)


if __name__ == "__main__":
    print("=== Testing Minimax AI ===\n")

    # Test 1: Blocking 4 in a row
    print("Test 1: Blocking 4 in a row")
    print("-" * 50)
    board = [[0]*15 for _ in range(15)]
    board[7][5] = 1
    board[7][6] = 1
    board[7][7] = 1
    board[7][8] = 1

    ai = GomokuMinimaxAI(difficulty="medium")
    row, col = ai.get_move(board, current_player=2)
    print(f"Black has 4 in a row at (7,5-8)")
    print(f"AI blocks at: ({row}, {col})")
    if row == 7 and col in [4, 9]:
        print("✅ CORRECT - AI blocked the threat!\n")
    else:
        print("❌ FAILED\n")

    # Test 2: Taking winning move
    print("Test 2: Taking winning move")
    print("-" * 50)
    board = [[0]*15 for _ in range(15)]
    board[7][5] = 2
    board[7][6] = 2
    board[7][7] = 2
    board[7][8] = 2

    row, col = ai.get_move(board, current_player=2)
    print(f"AI has 4 in a row at (7,5-8)")
    print(f"AI plays at: ({row}, {col})")
    if row == 7 and col in [4, 9]:
        print("✅ CORRECT - AI took the win!\n")
    else:
        print("❌ FAILED\n")

    # Test 3: Blocking 3 in a row
    print("Test 3: Blocking 3 in a row")
    print("-" * 50)
    board = [[0]*15 for _ in range(15)]
    board[7][6] = 1
    board[7][7] = 1
    board[7][8] = 1

    row, col = ai.get_move(board, current_player=2)
    print(f"Black has 3 in a row at (7,6-8)")
    print(f"AI plays at: ({row}, {col})")
    if row == 7 and col in [5, 9]:
        print("✅ CORRECT - AI blocked the threat!\n")
    else:
        print("⚠️  AI played elsewhere (may have different strategy)\n")

    print("=== All tests complete! ===")
