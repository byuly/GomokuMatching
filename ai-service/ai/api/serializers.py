"""
Serializers for AI API requests and responses.
"""
from rest_framework import serializers
from ai.game import BLACK, WHITE


class GetMoveRequest(serializers.Serializer):
    """Request serializer for getting AI move."""

    board_state = serializers.ListField(
        child=serializers.ListField(
            child=serializers.IntegerField(min_value=0, max_value=2)
        ),
        help_text="15x15 board state (0=empty, 1=black, 2=white)"
    )
    current_player = serializers.IntegerField(
        min_value=1,
        max_value=2,
        help_text="Current player (1=black, 2=white)"
    )
    difficulty = serializers.ChoiceField(
        choices=['easy', 'medium', 'hard', 'expert'],
        default='medium',
        help_text="AI difficulty level"
    )

    def validate_board_state(self, value):
        """Validate board dimensions."""
        if len(value) != 15:
            raise serializers.ValidationError("Board must have 15 rows")

        for row in value:
            if len(row) != 15:
                raise serializers.ValidationError("Each row must have 15 columns")

        return value


class MoveResponse(serializers.Serializer):
    """Response serializer for AI move."""

    row = serializers.IntegerField(
        min_value=0,
        max_value=14,
        help_text="Row index of AI move"
    )
    col = serializers.IntegerField(
        min_value=0,
        max_value=14,
        help_text="Column index of AI move"
    )
    difficulty = serializers.CharField(help_text="Difficulty level used")
    evaluation = serializers.FloatField(
        required=False,
        help_text="Position evaluation (if available)"
    )


class EvaluatePositionRequest(serializers.Serializer):
    """Request serializer for position evaluation."""

    board_state = serializers.ListField(
        child=serializers.ListField(
            child=serializers.IntegerField(min_value=0, max_value=2)
        ),
        help_text="15x15 board state"
    )
    current_player = serializers.IntegerField(
        min_value=1,
        max_value=2,
        help_text="Current player"
    )

    def validate_board_state(self, value):
        """Validate board dimensions."""
        if len(value) != 15:
            raise serializers.ValidationError("Board must have 15 rows")

        for row in value:
            if len(row) != 15:
                raise serializers.ValidationError("Each row must have 15 columns")

        return value


class PositionEvaluationResponse(serializers.Serializer):
    """Response serializer for position evaluation."""

    value = serializers.FloatField(
        help_text="Position value from -1 (losing) to 1 (winning)"
    )
    top_moves = serializers.ListField(
        child=serializers.DictField(),
        help_text="Top 5 recommended moves with probabilities"
    )


class ValidateMoveRequest(serializers.Serializer):
    """Request serializer for move validation."""

    board_state = serializers.ListField(
        child=serializers.ListField(
            child=serializers.IntegerField(min_value=0, max_value=2)
        ),
        help_text="15x15 board state"
    )
    row = serializers.IntegerField(
        min_value=0,
        max_value=14,
        help_text="Row index"
    )
    col = serializers.IntegerField(
        min_value=0,
        max_value=14,
        help_text="Column index"
    )


class ValidateMoveResponse(serializers.Serializer):
    """Response serializer for move validation."""

    is_valid = serializers.BooleanField(help_text="Whether the move is legal")
    reason = serializers.CharField(
        required=False,
        help_text="Reason if move is invalid"
    )


class CheckGameOverRequest(serializers.Serializer):
    """Request serializer for checking game over status."""

    board_state = serializers.ListField(
        child=serializers.ListField(
            child=serializers.IntegerField(min_value=0, max_value=2)
        ),
        help_text="15x15 board state"
    )
    last_move_row = serializers.IntegerField(
        required=False,
        min_value=0,
        max_value=14,
        help_text="Row of last move (optional)"
    )
    last_move_col = serializers.IntegerField(
        required=False,
        min_value=0,
        max_value=14,
        help_text="Column of last move (optional)"
    )


class GameOverResponse(serializers.Serializer):
    """Response serializer for game over check."""

    is_over = serializers.BooleanField(help_text="Whether the game is over")
    winner = serializers.IntegerField(
        required=False,
        allow_null=True,
        help_text="Winner (0=draw, 1=black, 2=white, null=ongoing)"
    )
    reason = serializers.CharField(
        required=False,
        allow_null=True,
        help_text="Reason for game over (win/draw)"
    )


class HealthCheckResponse(serializers.Serializer):
    """Response serializer for health check."""

    status = serializers.CharField(help_text="Service status")
    version = serializers.CharField(help_text="API version")
    model_loaded = serializers.BooleanField(help_text="Whether ML model is loaded")
    device = serializers.CharField(help_text="Compute device (cpu/cuda)")
