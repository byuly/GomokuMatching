"""
Custom exception handling for AI API.
"""
from rest_framework.views import exception_handler
from rest_framework.response import Response
from rest_framework import status


def custom_exception_handler(exc, context):
    """
    Custom exception handler for consistent error responses.

    Args:
        exc: Exception instance
        context: Request context

    Returns:
        Response with error details
    """
    # Call REST framework's default exception handler first
    response = exception_handler(exc, context)

    if response is not None:
        # Standardize error response format
        error_data = {
            'error': {
                'message': str(exc),
                'type': exc.__class__.__name__,
            }
        }

        if hasattr(response, 'data'):
            error_data['error']['details'] = response.data

        response.data = error_data

    else:
        # Handle non-DRF exceptions
        error_data = {
            'error': {
                'message': str(exc),
                'type': exc.__class__.__name__,
            }
        }

        response = Response(
            error_data,
            status=status.HTTP_500_INTERNAL_SERVER_ERROR
        )

    return response
