"""
Board representation and game logic for Gomoku
"""
import numpy as np
from typing import List, Tuple, Optional
from .constants import BOARD_SIZE, EMPTY, BLACK, WHITE, WIN_LENGTH, DIRECTIONS


class Board:
    """
    Represents a Gomoku game board with move validation and win detection.

    Uses a 15x15 NumPy array for efficient operations.
    Implements O(1) win detection by only checking from the last move.
    """

    def __init__(self, board_state: Optional[List[List[int]]] = None):
        """
        Initialize board. Can restore from existing state or create empty board.

        Args:
            board_state: Optional 2D list representing existing board state
        """
        if board_state is not None:
            self.board = np.array(board_state, dtype=np.int8)
            if self.board.shape != (BOARD_SIZE, BOARD_SIZE):
                raise ValueError(f"Board must be {BOARD_SIZE}x{BOARD_SIZE}")
        else:
            self.board = np.zeros((BOARD_SIZE, BOARD_SIZE), dtype=np.int8)

        self.move_count = np.count_nonzero(self.board)

    def is_valid_move(self, row: int, col: int) -> bool:
        """
        Check if a move is valid (in bounds and cell is empty).

        Args:
            row: Row index (0-14)
            col: Column index (0-14)

        Returns:
            True if move is valid, False otherwise
        """
        if not (0 <= row < BOARD_SIZE and 0 <= col < BOARD_SIZE):
            return False
        return self.board[row, col] == EMPTY

    def make_move(self, row: int, col: int, player: int) -> bool:
        """
        Place a stone on the board.

        Args:
            row: Row index
            col: Column index
            player: BLACK or WHITE

        Returns:
            True if move was successful, False if invalid
        """
        if not self.is_valid_move(row, col):
            return False

        if player not in [BLACK, WHITE]:
            return False

        self.board[row, col] = player
        self.move_count += 1
        return True

    def check_win(self, row: int, col: int) -> bool:
        """
        Check if the last move at (row, col) resulted in a win.

        Only checks the 4 directions from the last placed stone for efficiency.

        Args:
            row: Row of last move
            col: Column of last move

        Returns:
            True if this move wins the game
        """
        player = self.board[row, col]
        if player == EMPTY:
            return False

        # Check all 4 directions
        for dr, dc in DIRECTIONS:
            if self._check_direction(row, col, dr, dc, player):
                return True

        return False

    def _check_direction(self, row: int, col: int, dr: int, dc: int, player: int) -> bool:
        """
        Check if there are WIN_LENGTH stones in a line in the given direction.

        Counts stones in both directions from the starting position.

        Args:
            row: Starting row
            col: Starting column
            dr: Row direction (-1, 0, or 1)
            dc: Column direction (-1, 0, or 1)
            player: Player to check for (BLACK or WHITE)

        Returns:
            True if WIN_LENGTH consecutive stones found
        """
        count = 1  # Count the stone at (row, col)

        # Count in positive direction
        count += self._count_consecutive(row, col, dr, dc, player)

        # Count in negative direction
        count += self._count_consecutive(row, col, -dr, -dc, player)

        return count >= WIN_LENGTH

    def _count_consecutive(self, row: int, col: int, dr: int, dc: int, player: int) -> int:
        """
        Count consecutive stones of the same player in one direction.

        Args:
            row: Starting row
            col: Starting column
            dr: Row direction
            dc: Column direction
            player: Player to count

        Returns:
            Number of consecutive stones (not including starting position)
        """
        count = 0
        r, c = row + dr, col + dc

        while (0 <= r < BOARD_SIZE and 0 <= c < BOARD_SIZE and
               self.board[r, c] == player):
            count += 1
            r += dr
            c += dc

        return count

    def get_valid_moves(self) -> List[Tuple[int, int]]:
        """
        Get list of all valid moves (empty cells).

        Returns:
            List of (row, col) tuples for all empty cells
        """
        moves = []
        for row in range(BOARD_SIZE):
            for col in range(BOARD_SIZE):
                if self.board[row, col] == EMPTY:
                    moves.append((row, col))
        return moves

    def get_smart_moves(self, player: int, limit: int = 50) -> List[Tuple[int, int]]:
        """
        Get moves near existing stones (for AI efficiency).

        Only considers cells within 2 spaces of existing stones.
        This dramatically reduces the search space for AI.

        Args:
            player: Current player (for ordering)
            limit: Maximum number of moves to return

        Returns:
            List of (row, col) tuples for promising moves
        """
        if self.move_count == 0:
            # First move: center of board
            return [(BOARD_SIZE // 2, BOARD_SIZE // 2)]

        moves_set = set()

        # Find all stones on the board
        stones = np.argwhere(self.board != EMPTY)

        # For each stone, add nearby empty cells
        for stone_row, stone_col in stones:
            for dr in range(-2, 3):
                for dc in range(-2, 3):
                    r, c = stone_row + dr, stone_col + dc
                    if (0 <= r < BOARD_SIZE and 0 <= c < BOARD_SIZE and
                        self.board[r, c] == EMPTY):
                        moves_set.add((r, c))

        moves = list(moves_set)

        # Prioritize moves: prefer center and near opponent stones
        def move_priority(move):
            r, c = move
            # Distance from center (prefer central moves)
            center = BOARD_SIZE // 2
            center_dist = abs(r - center) + abs(c - center)

            # Check if near opponent's stones (more urgent)
            opponent = WHITE if player == BLACK else BLACK
            near_opponent = 0
            for dr in range(-1, 2):
                for dc in range(-1, 2):
                    nr, nc = r + dr, c + dc
                    if (0 <= nr < BOARD_SIZE and 0 <= nc < BOARD_SIZE and
                        self.board[nr, nc] == opponent):
                        near_opponent = 1
                        break

            return (near_opponent, -center_dist)

        moves.sort(key=move_priority, reverse=True)
        return moves[:limit]

    def is_full(self) -> bool:
        """Check if board is full (draw condition)."""
        return self.move_count == BOARD_SIZE * BOARD_SIZE

    def copy(self) -> 'Board':
        """Create a deep copy of the board."""
        new_board = Board()
        new_board.board = self.board.copy()
        new_board.move_count = self.move_count
        return new_board

    def to_list(self) -> List[List[int]]:
        """Convert board to 2D list for serialization."""
        return self.board.tolist()

    def __str__(self) -> str:
        """String representation for debugging."""
        symbols = {EMPTY: '.', BLACK: 'X', WHITE: 'O'}
        lines = []
        lines.append('  ' + ' '.join(str(i % 10) for i in range(BOARD_SIZE)))
        for i, row in enumerate(self.board):
            line = f"{i:2} " + ' '.join(symbols[cell] for cell in row)
            lines.append(line)
        return '\n'.join(lines)
