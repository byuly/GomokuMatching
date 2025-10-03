"""
Unit tests for Minimax AI algorithm.
"""
import unittest
from ai.game import Board, BLACK, WHITE
from ai.services import MinimaxAI


class TestMinimaxAI(unittest.TestCase):
    """Test Minimax AI functionality."""

    def setUp(self):
        """Create AI instance for each test."""
        self.easy_ai = MinimaxAI(difficulty='easy')
        self.medium_ai = MinimaxAI(difficulty='medium')

    def test_ai_initialization(self):
        """Test AI initializes with correct parameters."""
        self.assertEqual(self.easy_ai.max_depth, 1)
        self.assertEqual(self.medium_ai.max_depth, 2)

    def test_first_move_is_center(self):
        """Test that first move is in the center."""
        board = Board()
        move = self.easy_ai.get_best_move(board, BLACK)

        # Should be center or very close
        self.assertEqual(move, (7, 7))

    def test_blocks_opponent_win(self):
        """Test AI blocks opponent's winning move."""
        board = Board()

        # Create a situation where WHITE has 4 in a row
        for col in range(4):
            board.make_move(7, col, WHITE)

        # AI should block at (7, 4)
        move = self.medium_ai.get_best_move(board, BLACK)
        self.assertEqual(move, (7, 4))

    def test_takes_winning_move(self):
        """Test AI takes immediate winning move."""
        board = Board()

        # Create a situation where BLACK can win
        for col in range(4):
            board.make_move(7, col, BLACK)

        # AI should win at (7, 4)
        move = self.easy_ai.get_best_move(board, BLACK)
        self.assertEqual(move, (7, 4))

    def test_returns_valid_move(self):
        """Test that AI always returns valid move."""
        board = Board()

        # Make some random moves
        board.make_move(7, 7, BLACK)
        board.make_move(7, 8, WHITE)
        board.make_move(8, 7, BLACK)

        move = self.medium_ai.get_best_move(board, WHITE)

        # Move should be valid
        self.assertTrue(board.is_valid_move(*move))

    def test_nodes_evaluated_counter(self):
        """Test that nodes_evaluated counter works."""
        board = Board()
        board.make_move(7, 7, BLACK)

        self.easy_ai.get_best_move(board, WHITE)

        # Should have evaluated some nodes
        self.assertGreater(self.easy_ai.nodes_evaluated, 0)

    def test_heuristic_evaluation(self):
        """Test heuristic evaluation function."""
        board = Board()

        # Place some BLACK stones
        board.make_move(7, 7, BLACK)
        board.make_move(7, 8, BLACK)
        board.make_move(7, 9, BLACK)

        # Evaluate for BLACK
        score = self.easy_ai._evaluate_heuristic(board, BLACK)

        # Should be positive for BLACK
        self.assertGreater(score, 0)

    def test_pattern_counting(self):
        """Test pattern counting in heuristic."""
        board = Board()

        # Create 3 in a row for BLACK
        for col in range(3):
            board.make_move(7, col, BLACK)

        score = self.easy_ai._count_patterns(board, BLACK)

        # Should detect the pattern
        self.assertGreater(score, 0)


if __name__ == '__main__':
    unittest.main()
