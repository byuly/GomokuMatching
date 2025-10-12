# AI Service Dockerfile
# Python Flask microservice for Gomoku AI move calculation

FROM python:3.12-slim

# Set working directory
WORKDIR /app

# Install system dependencies (for curl in health check)
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Copy requirements first (for better layer caching)
COPY requirements.txt .

# Install Python dependencies
# Install torch CPU-only version to reduce image size
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu && \
    pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY app.py .
COPY model-inference/ ./model-inference/
COPY README.md .

# Expose port
EXPOSE 8000

# Health check
HEALTHCHECK --interval=10s --timeout=5s --retries=3 --start-period=10s \
    CMD curl -f http://localhost:8000/api/health/ || exit 1

# Set environment variables
ENV FLASK_APP=app.py
ENV FLASK_ENV=production
ENV PYTHONUNBUFFERED=1

# Run Flask application
CMD ["python", "-m", "flask", "run", "--host=0.0.0.0", "--port=8000"]
