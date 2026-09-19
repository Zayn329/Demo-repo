FROM python:3.12-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc libpq-dev \
    && rm -rf /var/lib/apt/lists/*

# Copy dependency definitions
COPY pyproject.toml .

# Install dependencies
RUN pip install --no-cache-dir fastapi uvicorn pydantic pydantic-settings pyjwt sqlalchemy psycopg2-binary

# Copy application source code
COPY app/ app/

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
