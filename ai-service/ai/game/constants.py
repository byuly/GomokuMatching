"""
Game constants for Gomoku
"""

# Board configuration
BOARD_SIZE = 15
WIN_LENGTH = 5

# Cell states
EMPTY = 0
BLACK = 1
WHITE = 2

# Direction vectors for win checking (horizontal, vertical, diagonal, anti-diagonal)
DIRECTIONS = [
    (0, 1),   # Horizontal
    (1, 0),   # Vertical
    (1, 1),   # Diagonal
    (1, -1),  # Anti-diagonal
]
