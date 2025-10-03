"""
AI service that orchestrates game logic, neural network, and minimax algorithm.
"""
from typing import Tuple, Dict, Optional
import torch
from ai.game import Board, BLACK, WHITE
from ai.ml import create_model
from ai.services.minimax import MinimaxAI


class AIService:
    """
    Main service for AI move generation.

    Manages the neural network model and coordinates with Minimax algorithm.
    """

    def __init__(self, model_path: Optional[str] = None):
        """
        Initialize AI service.

        Args:
            model_path: Optional path to pretrained model weights
        """
        # Determine device (CPU or CUDA)
        self.device = 'cuda' if torch.cuda.is_available() else 'cpu'

        # Load or create model
        self.model = create_model(device=self.device, num_residual_blocks=5)

        if model_path:
            try:
                self.model.load_state_dict(torch.load(model_path, map_location=self.device))
                self.model.eval()
            except Exception as e:
                print(f"Warning: Could not load model from {model_path}: {e}")
                print("Using untrained model.")

        # Cache for AI instances by difficulty
        self._ai_cache: Dict[str, MinimaxAI] = {}

    def get_ai_move(
        self,
        board_state: list,
        current_player: int,
        difficulty: str = 'medium'
    ) -> Tuple[int, int]:
        """
        Get the best AI move for the current position.

        Args:
            board_state: 2D list (15x15) representing the board
            current_player: 1 (BLACK) or 2 (WHITE)
            difficulty: 'easy', 'medium', 'hard', or 'expert'

        Returns:
            Tuple (row, col) of the best move

        Raises:
            ValueError: If board_state is invalid or difficulty is unknown
        """
        # Validate inputs
        if current_player not in [BLACK, WHITE]:
            raise ValueError(f"Invalid player: {current_player}")

        # Create board instance
        try:
            board = Board(board_state)
        except Exception as e:
            raise ValueError(f"Invalid board state: {e}")

        # Get or create AI instance for this difficulty
        if difficulty not in self._ai_cache:
            use_model = difficulty in ['hard', 'expert']
            model = self.model if use_model else None
            self._ai_cache[difficulty] = MinimaxAI(difficulty=difficulty, model=model)

        ai = self._ai_cache[difficulty]

        # Get best move
        move = ai.get_best_move(board, current_player)

        return move

    def evaluate_position(
        self,
        board_state: list,
        current_player: int
    ) -> Dict[str, any]:
        """
        Evaluate a board position using the neural network.

        Args:
            board_state: 2D list representing the board
            current_player: 1 (BLACK) or 2 (WHITE)

        Returns:
            Dictionary with:
                - 'value': Position evaluation (-1 to 1)
                - 'top_moves': List of top 5 moves with probabilities
        """
        from ai.ml import board_to_tensor

        board_tensor = board_to_tensor(board_state, current_player)
        policy, value = self.model.predict(board_tensor)

        # Get top 5 moves
        top_indices = torch.topk(policy, k=5).indices
        top_moves = []

        for idx in top_indices:
            row = idx.item() // 15
            col = idx.item() % 15
            prob = policy[idx].item()
            top_moves.append({
                'row': row,
                'col': col,
                'probability': round(prob, 4)
            })

        return {
            'value': round(value, 4),
            'top_moves': top_moves
        }

    def validate_move(
        self,
        board_state: list,
        row: int,
        col: int
    ) -> bool:
        """
        Validate if a move is legal.

        Args:
            board_state: 2D list representing the board
            row: Row index
            col: Column index

        Returns:
            True if move is valid
        """
        try:
            board = Board(board_state)
            return board.is_valid_move(row, col)
        except Exception:
            return False

    def check_game_over(
        self,
        board_state: list,
        last_move: Optional[Tuple[int, int]] = None
    ) -> Dict[str, any]:
        """
        Check if the game is over.

        Args:
            board_state: 2D list representing the board
            last_move: Optional (row, col) of last move

        Returns:
            Dictionary with:
                - 'is_over': Boolean
                - 'winner': 0 (draw), 1 (BLACK), 2 (WHITE), or None
                - 'reason': 'win', 'draw', or None
        """
        board = Board(board_state)

        # Check for win if last move provided
        if last_move is not None:
            row, col = last_move
            if board.check_win(row, col):
                winner = board.board[row, col]
                return {
                    'is_over': True,
                    'winner': int(winner),
                    'reason': 'win'
                }

        # Check for draw
        if board.is_full():
            return {
                'is_over': True,
                'winner': 0,
                'reason': 'draw'
            }

        return {
            'is_over': False,
            'winner': None,
            'reason': None
        }


# Singleton instance
_ai_service_instance: Optional[AIService] = None


def get_ai_service(model_path: Optional[str] = None) -> AIService:
    """
    Get or create the singleton AI service instance.

    Args:
        model_path: Optional path to model weights (only used on first call)

    Returns:
        AIService instance
    """
    global _ai_service_instance

    if _ai_service_instance is None:
        _ai_service_instance = AIService(model_path=model_path)

    return _ai_service_instance
