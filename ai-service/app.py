"""
AI Service Flask Application for Gomoku

Provides REST API for AI move calculation using minimax algorithm.
Communicates with Spring Boot backend via HTTP.
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import sys
import os

# Add model-inference to path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'model-inference'))

from minimax_ai.gomoku_minimax import GomokuMinimaxAI

app = Flask(__name__)
CORS(app)  # Enable CORS for backend communication

# Initialize AI instances for each difficulty
ai_instances = {
    'easy': GomokuMinimaxAI(difficulty='easy'),
    'medium': GomokuMinimaxAI(difficulty='medium'),
    'hard': GomokuMinimaxAI(difficulty='hard'),
    'expert': GomokuMinimaxAI(difficulty='expert')
}

VERSION = "1.0.0"


@app.route('/api/health/', methods=['GET'])
def health_check():
    """
    Health check endpoint for Docker and monitoring.

    Returns:
        JSON with service status
    """
    return jsonify({
        'status': 'healthy',
        'version': VERSION,
        'model_loaded': True,
        'device': 'cpu'
    }), 200


@app.route('/api/move/', methods=['POST'])
def get_ai_move():
    """
    Get AI's next move for the given board state.

    Request JSON:
    {
        "board_state": [[0,0,...], ...],  // 15x15 int array (0=empty, 1=black, 2=white)
        "current_player": 2,               // 1 or 2
        "difficulty": "medium"             // easy, medium, hard, expert
    }

    Response JSON:
    {
        "row": 7,
        "col": 8,
        "difficulty": "medium"
    }
    """
    try:
        data = request.get_json()

        # Validate request
        if not data:
            return jsonify({'error': 'No JSON data provided'}), 400

        board_state = data.get('board_state')
        current_player = data.get('current_player')
        difficulty = data.get('difficulty', 'medium')

        # Validate required fields
        if board_state is None:
            return jsonify({'error': 'Missing board_state'}), 400
        if current_player is None:
            return jsonify({'error': 'Missing current_player'}), 400

        # Validate board dimensions
        if not isinstance(board_state, list) or len(board_state) != 15:
            return jsonify({'error': 'board_state must be 15x15 array'}), 400

        for row in board_state:
            if not isinstance(row, list) or len(row) != 15:
                return jsonify({'error': 'board_state must be 15x15 array'}), 400

        # Validate current_player
        if current_player not in [1, 2]:
            return jsonify({'error': 'current_player must be 1 or 2'}), 400

        # Validate difficulty
        if difficulty not in ai_instances:
            return jsonify({'error': f'Invalid difficulty. Must be one of: {list(ai_instances.keys())}'}), 400

        # Get AI move
        ai = ai_instances[difficulty]
        row, col = ai.get_move(board_state, current_player)

        app.logger.info(f"AI move calculated: difficulty={difficulty}, player={current_player}, move=({row},{col})")

        return jsonify({
            'row': row,
            'col': col,
            'difficulty': difficulty
        }), 200

    except ValueError as e:
        app.logger.error(f"Invalid move request: {str(e)}")
        return jsonify({'error': str(e)}), 400
    except Exception as e:
        app.logger.error(f"Error calculating AI move: {str(e)}")
        return jsonify({'error': 'Internal server error'}), 500


@app.route('/api/validate/', methods=['POST'])
def validate_move():
    """
    Validate if a move is legal (optional endpoint).
    Backend already handles this, but provided for completeness.

    Request JSON:
    {
        "board_state": [[...]],
        "row": 7,
        "col": 8
    }

    Response JSON:
    {
        "is_valid": true,
        "reason": null
    }
    """
    try:
        data = request.get_json()

        if not data:
            return jsonify({'error': 'No JSON data provided'}), 400

        board_state = data.get('board_state')
        row = data.get('row')
        col = data.get('col')

        if board_state is None or row is None or col is None:
            return jsonify({'error': 'Missing required fields'}), 400

        # Check bounds
        if not (0 <= row < 15 and 0 <= col < 15):
            return jsonify({
                'is_valid': False,
                'reason': 'Move out of bounds'
            }), 200

        # Check if position is empty
        if board_state[row][col] != 0:
            return jsonify({
                'is_valid': False,
                'reason': 'Position already occupied'
            }), 200

        return jsonify({
            'is_valid': True,
            'reason': None
        }), 200

    except Exception as e:
        app.logger.error(f"Error validating move: {str(e)}")
        return jsonify({'error': 'Internal server error'}), 500


@app.errorhandler(404)
def not_found(error):
    """Handle 404 errors"""
    return jsonify({'error': 'Endpoint not found'}), 404


@app.errorhandler(500)
def internal_error(error):
    """Handle 500 errors"""
    return jsonify({'error': 'Internal server error'}), 500


if __name__ == '__main__':
    # Development server
    app.run(host='0.0.0.0', port=8000, debug=True)
