"""
API views for AI service endpoints.
"""
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from drf_yasg.utils import swagger_auto_schema
from drf_yasg import openapi

from ai.services import get_ai_service
from .serializers import (
    GetMoveRequest,
    MoveResponse,
    EvaluatePositionRequest,
    PositionEvaluationResponse,
    ValidateMoveRequest,
    ValidateMoveResponse,
    CheckGameOverRequest,
    GameOverResponse,
    HealthCheckResponse,
)


class GetAIMoveView(APIView):
    """
    Get the best AI move for the current position.

    POST /api/move/
    """

    @swagger_auto_schema(
        request_body=GetMoveRequest,
        responses={
            200: MoveResponse,
            400: 'Invalid request',
            500: 'Internal server error'
        },
        operation_description="Get the best AI move for the current board state"
    )
    def post(self, request):
        """Get AI move."""
        serializer = GetMoveRequest(data=request.data)

        if not serializer.is_valid():
            return Response(
                {'error': serializer.errors},
                status=status.HTTP_400_BAD_REQUEST
            )

        try:
            ai_service = get_ai_service()

            board_state = serializer.validated_data['board_state']
            current_player = serializer.validated_data['current_player']
            difficulty = serializer.validated_data.get('difficulty', 'medium')

            # Get AI move
            row, col = ai_service.get_ai_move(
                board_state=board_state,
                current_player=current_player,
                difficulty=difficulty
            )

            response_data = {
                'row': row,
                'col': col,
                'difficulty': difficulty
            }

            return Response(response_data, status=status.HTTP_200_OK)

        except ValueError as e:
            return Response(
                {'error': str(e)},
                status=status.HTTP_400_BAD_REQUEST
            )
        except Exception as e:
            return Response(
                {'error': f'Internal error: {str(e)}'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )


class EvaluatePositionView(APIView):
    """
    Evaluate a board position using neural network.

    POST /api/evaluate/
    """

    @swagger_auto_schema(
        request_body=EvaluatePositionRequest,
        responses={
            200: PositionEvaluationResponse,
            400: 'Invalid request',
            500: 'Internal server error'
        },
        operation_description="Evaluate a board position and get top move suggestions"
    )
    def post(self, request):
        """Evaluate position."""
        serializer = EvaluatePositionRequest(data=request.data)

        if not serializer.is_valid():
            return Response(
                {'error': serializer.errors},
                status=status.HTTP_400_BAD_REQUEST
            )

        try:
            ai_service = get_ai_service()

            board_state = serializer.validated_data['board_state']
            current_player = serializer.validated_data['current_player']

            # Evaluate position
            evaluation = ai_service.evaluate_position(
                board_state=board_state,
                current_player=current_player
            )

            return Response(evaluation, status=status.HTTP_200_OK)

        except Exception as e:
            return Response(
                {'error': f'Internal error: {str(e)}'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )


class ValidateMoveView(APIView):
    """
    Validate if a move is legal.

    POST /api/validate/
    """

    @swagger_auto_schema(
        request_body=ValidateMoveRequest,
        responses={
            200: ValidateMoveResponse,
            400: 'Invalid request'
        },
        operation_description="Check if a move is legal on the current board"
    )
    def post(self, request):
        """Validate move."""
        serializer = ValidateMoveRequest(data=request.data)

        if not serializer.is_valid():
            return Response(
                {'error': serializer.errors},
                status=status.HTTP_400_BAD_REQUEST
            )

        try:
            ai_service = get_ai_service()

            board_state = serializer.validated_data['board_state']
            row = serializer.validated_data['row']
            col = serializer.validated_data['col']

            is_valid = ai_service.validate_move(
                board_state=board_state,
                row=row,
                col=col
            )

            response_data = {
                'is_valid': is_valid,
            }

            if not is_valid:
                response_data['reason'] = 'Cell is occupied or out of bounds'

            return Response(response_data, status=status.HTTP_200_OK)

        except Exception as e:
            return Response(
                {'error': f'Internal error: {str(e)}'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )


class CheckGameOverView(APIView):
    """
    Check if the game is over.

    POST /api/game-over/
    """

    @swagger_auto_schema(
        request_body=CheckGameOverRequest,
        responses={
            200: GameOverResponse,
            400: 'Invalid request'
        },
        operation_description="Check if the game is over (win or draw)"
    )
    def post(self, request):
        """Check game over."""
        serializer = CheckGameOverRequest(data=request.data)

        if not serializer.is_valid():
            return Response(
                {'error': serializer.errors},
                status=status.HTTP_400_BAD_REQUEST
            )

        try:
            ai_service = get_ai_service()

            board_state = serializer.validated_data['board_state']
            last_move_row = serializer.validated_data.get('last_move_row')
            last_move_col = serializer.validated_data.get('last_move_col')

            last_move = None
            if last_move_row is not None and last_move_col is not None:
                last_move = (last_move_row, last_move_col)

            result = ai_service.check_game_over(
                board_state=board_state,
                last_move=last_move
            )

            return Response(result, status=status.HTTP_200_OK)

        except Exception as e:
            return Response(
                {'error': f'Internal error: {str(e)}'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )


class HealthCheckView(APIView):
    """
    Health check endpoint.

    GET /api/health/
    """

    @swagger_auto_schema(
        responses={200: HealthCheckResponse},
        operation_description="Check service health and status"
    )
    def get(self, request):
        """Health check."""
        try:
            ai_service = get_ai_service()

            response_data = {
                'status': 'healthy',
                'version': '1.0.0',
                'model_loaded': ai_service.model is not None,
                'device': ai_service.device,
            }

            return Response(response_data, status=status.HTTP_200_OK)

        except Exception as e:
            return Response(
                {
                    'status': 'unhealthy',
                    'error': str(e)
                },
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )
