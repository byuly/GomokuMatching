"""
Integration tests for REST API endpoints.
"""
from django.test import TestCase
from rest_framework.test import APIClient
from rest_framework import status
from ai.game import EMPTY, BLACK, WHITE


class TestAIAPI(TestCase):
    """Test AI REST API endpoints."""

    def setUp(self):
        """Create API client for each test."""
        self.client = APIClient()
        self.empty_board = [[EMPTY] * 15 for _ in range(15)]

    def test_health_check(self):
        """Test health check endpoint."""
        response = self.client.get('/api/health/')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['status'], 'healthy')
        self.assertIn('version', response.data)
        self.assertIn('model_loaded', response.data)
        self.assertIn('device', response.data)

    def test_get_ai_move_success(self):
        """Test getting AI move with valid request."""
        board = self.empty_board
        board[7][7] = BLACK

        data = {
            'board_state': board,
            'current_player': WHITE,
            'difficulty': 'easy'
        }

        response = self.client.post('/api/move/', data, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn('row', response.data)
        self.assertIn('col', response.data)
        self.assertIn('difficulty', response.data)
        self.assertEqual(response.data['difficulty'], 'easy')

        # Move should be valid
        row = response.data['row']
        col = response.data['col']
        self.assertGreaterEqual(row, 0)
        self.assertLess(row, 15)
        self.assertGreaterEqual(col, 0)
        self.assertLess(col, 15)

    def test_get_ai_move_invalid_board(self):
        """Test AI move with invalid board dimensions."""
        data = {
            'board_state': [[EMPTY] * 10 for _ in range(10)],  # Wrong size
            'current_player': BLACK,
            'difficulty': 'easy'
        }

        response = self.client.post('/api/move/', data, format='json')

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_get_ai_move_invalid_player(self):
        """Test AI move with invalid player."""
        data = {
            'board_state': self.empty_board,
            'current_player': 3,  # Invalid
            'difficulty': 'easy'
        }

        response = self.client.post('/api/move/', data, format='json')

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_validate_move_valid(self):
        """Test move validation for valid move."""
        data = {
            'board_state': self.empty_board,
            'row': 7,
            'col': 7
        }

        response = self.client.post('/api/validate/', data, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(response.data['is_valid'])

    def test_validate_move_occupied(self):
        """Test move validation for occupied cell."""
        board = self.empty_board
        board[7][7] = BLACK

        data = {
            'board_state': board,
            'row': 7,
            'col': 7
        }

        response = self.client.post('/api/validate/', data, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertFalse(response.data['is_valid'])
        self.assertIn('reason', response.data)

    def test_validate_move_out_of_bounds(self):
        """Test move validation for out of bounds."""
        data = {
            'board_state': self.empty_board,
            'row': 20,
            'col': 20
        }

        response = self.client.post('/api/validate/', data, format='json')

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_check_game_over_win(self):
        """Test game over check with win."""
        board = self.empty_board
        # Create 5 in a row
        for col in range(5):
            board[7][col] = BLACK

        data = {
            'board_state': board,
            'last_move_row': 7,
            'last_move_col': 4
        }

        response = self.client.post('/api/game-over/', data, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(response.data['is_over'])
        self.assertEqual(response.data['winner'], BLACK)
        self.assertEqual(response.data['reason'], 'win')

    def test_check_game_over_ongoing(self):
        """Test game over check with ongoing game."""
        board = self.empty_board
        board[7][7] = BLACK

        data = {
            'board_state': board,
            'last_move_row': 7,
            'last_move_col': 7
        }

        response = self.client.post('/api/game-over/', data, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertFalse(response.data['is_over'])
        self.assertIsNone(response.data['winner'])

    def test_check_game_over_draw(self):
        """Test game over check with full board (draw)."""
        board = [[BLACK if (i + j) % 2 == 0 else WHITE for j in range(15)] for i in range(15)]

        # Make sure no wins
        data = {
            'board_state': board
        }

        response = self.client.post('/api/game-over/', data, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        # Should be over (board is full)
        self.assertTrue(response.data['is_over'])

    def test_evaluate_position(self):
        """Test position evaluation endpoint."""
        board = self.empty_board
        board[7][7] = BLACK
        board[7][8] = BLACK
        board[7][9] = BLACK

        data = {
            'board_state': board,
            'current_player': BLACK
        }

        response = self.client.post('/api/evaluate/', data, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn('value', response.data)
        self.assertIn('top_moves', response.data)
        self.assertEqual(len(response.data['top_moves']), 5)

        # Check top move format
        top_move = response.data['top_moves'][0]
        self.assertIn('row', top_move)
        self.assertIn('col', top_move)
        self.assertIn('probability', top_move)

    def test_different_difficulties(self):
        """Test all difficulty levels."""
        board = self.empty_board
        board[7][7] = BLACK

        for difficulty in ['easy', 'medium', 'hard', 'expert']:
            data = {
                'board_state': board,
                'current_player': WHITE,
                'difficulty': difficulty
            }

            response = self.client.post('/api/move/', data, format='json')

            self.assertEqual(response.status_code, status.HTTP_200_OK)
            self.assertEqual(response.data['difficulty'], difficulty)


if __name__ == '__main__':
    import django
    django.setup()
    from django.test.utils import get_runner
    from django.conf import settings
    TestRunner = get_runner(settings)
    test_runner = TestRunner()
    failures = test_runner.run_tests(["ai.tests"])
