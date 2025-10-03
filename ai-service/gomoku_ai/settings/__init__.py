import os

# Default to development settings
DJANGO_SETTINGS_MODULE = os.environ.get('DJANGO_SETTINGS_MODULE', 'gomoku_ai.settings.development')
