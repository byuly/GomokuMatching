"""
Unit tests for Board class and game logic.
"""
import unittest
from ai.game import Board, BLACK, WHITE, EMPTY


class TestBoard(unittest.TestCase):
    """Test Board class functionality."""

    def setUp(self):
        """Create a fresh board for each test."""
        self.board = Board()

    def test_initial_board(self):
        """Test that new board is empty."""
        self.assertEqual(self.board.move_count, 0)
        self.assertTrue(self.board.is_valid_move(7, 7))

    def test_make_move(self):
        """Test making a valid move."""
        result = self.board.make_move(7, 7, BLACK)
        self.assertTrue(result)
        self.assertEqual(self.board.board[7, 7], BLACK)
        self.assertEqual(self.board.move_count, 1)

    def test_invalid_move_occupied(self):
        """Test that can't move on occupied cell."""
        self.board.make_move(7, 7, BLACK)
        result = self.board.make_move(7, 7, WHITE)
        self.assertFalse(result)

    def test_invalid_move_out_of_bounds(self):
        """Test that can't move out of bounds."""
        self.assertFalse(self.board.is_valid_move(-1, 7))
        self.assertFalse(self.board.is_valid_move(7, 15))
        self.assertFalse(self.board.is_valid_move(20, 20))

    def test_horizontal_win(self):
        """Test horizontal win detection."""
        # Place 5 stones horizontally
        for col in range(5):
            self.board.make_move(7, col, BLACK)

        # Check win on last move
        self.assertTrue(self.board.check_win(7, 4))

    def test_vertical_win(self):
        """Test vertical win detection."""
        # Place 5 stones vertically
        for row in range(5):
            self.board.make_move(row, 7, WHITE)

        # Check win on last move
        self.assertTrue(self.board.check_win(4, 7))

    def test_diagonal_win(self):
        """Test diagonal win detection."""
        # Place 5 stones diagonally
        for i in range(5):
            self.board.make_move(i, i, BLACK)

        # Check win on last move
        self.assertTrue(self.board.check_win(4, 4))

    def test_anti_diagonal_win(self):
        """Test anti-diagonal win detection."""
        # Place 5 stones anti-diagonally
        for i in range(5):
            self.board.make_move(i, 4 - i, WHITE)

        # Check win on last move
        self.assertTrue(self.board.check_win(4, 0))

    def test_no_win_with_four(self):
        """Test that 4 in a row doesn't win."""
        for col in range(4):
            self.board.make_move(7, col, BLACK)

        self.assertFalse(self.board.check_win(7, 3))

    def test_get_valid_moves(self):
        """Test getting all valid moves."""
        # Empty board should have 225 valid moves
        moves = self.board.get_valid_moves()
        self.assertEqual(len(moves), 225)

        # After one move, should have 224
        self.board.make_move(7, 7, BLACK)
        moves = self.board.get_valid_moves()
        self.assertEqual(len(moves), 224)

    def test_get_smart_moves(self):
        """Test smart move generation."""
        # First move should be center
        moves = self.board.get_smart_moves(BLACK)
        self.assertEqual(moves[0], (7, 7))

        # After some moves, should return nearby cells
        self.board.make_move(7, 7, BLACK)
        self.board.make_move(7, 8, WHITE)
        moves = self.board.get_smart_moves(BLACK, limit=10)

        # All moves should be near existing stones
        for row, col in moves:
            # Check if within 2 cells of (7,7) or (7,8)
            near_77 = abs(row - 7) <= 2 and abs(col - 7) <= 2
            near_78 = abs(row - 7) <= 2 and abs(col - 8) <= 2
            self.assertTrue(near_77 or near_78)

    def test_is_full(self):
        """Test board full detection."""
        self.assertFalse(self.board.is_full())

        # Fill board
        player = BLACK
        for row in range(15):
            for col in range(15):
                self.board.make_move(row, col, player)
                player = WHITE if player == BLACK else BLACK

        self.assertTrue(self.board.is_full())

    def test_copy(self):
        """Test board copy."""
        self.board.make_move(7, 7, BLACK)
        board_copy = self.board.copy()

        # Modify copy
        board_copy.make_move(7, 8, WHITE)

        # Original should be unchanged
        self.assertEqual(self.board.board[7, 8], EMPTY)
        self.assertEqual(board_copy.board[7, 8], WHITE)

    def test_to_list(self):
        """Test board to list conversion."""
        self.board.make_move(0, 0, BLACK)
        self.board.make_move(1, 1, WHITE)

        board_list = self.board.to_list()

        self.assertEqual(len(board_list), 15)
        self.assertEqual(len(board_list[0]), 15)
        self.assertEqual(board_list[0][0], BLACK)
        self.assertEqual(board_list[1][1], WHITE)

    def test_board_from_state(self):
        """Test creating board from existing state."""
        initial_state = [[EMPTY] * 15 for _ in range(15)]
        initial_state[7][7] = BLACK
        initial_state[7][8] = WHITE

        board = Board(initial_state)

        self.assertEqual(board.board[7, 7], BLACK)
        self.assertEqual(board.board[7, 8], WHITE)
        self.assertEqual(board.move_count, 2)


if __name__ == '__main__':
    unittest.main()
