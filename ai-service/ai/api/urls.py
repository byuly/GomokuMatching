"""
URL routing for AI API endpoints.
"""
from django.urls import path
from .views import (
    GetAIMoveView,
    EvaluatePositionView,
    ValidateMoveView,
    CheckGameOverView,
    HealthCheckView,
)

urlpatterns = [
    path('move/', GetAIMoveView.as_view(), name='ai-move'),
    path('evaluate/', EvaluatePositionView.as_view(), name='evaluate-position'),
    path('validate/', ValidateMoveView.as_view(), name='validate-move'),
    path('game-over/', CheckGameOverView.as_view(), name='check-game-over'),
    path('health/', HealthCheckView.as_view(), name='health-check'),
]
