#!/bin/sh
set -e

# Flyway is disabled by default because this project has an existing database
# with a failed migration history. Enable it explicitly after repairing that
# history and reviewing the pending migrations.
if [ -z "$SPRING_FLYWAY_ENABLED" ]; then
  export SPRING_FLYWAY_ENABLED=false
  echo "SPRING_FLYWAY_ENABLED not set — defaulting to false"
else
  echo "SPRING_FLYWAY_ENABLED=$SPRING_FLYWAY_ENABLED"
fi

# Warn if datasource URL is not set (common cause of connecting to localhost)
if [ -z "$SPRING_DATASOURCE_URL" ]; then
  echo "Warning: SPRING_DATASOURCE_URL not set. Application will use defaults from application.yml (often localhost)."
  echo "Set SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME and SPRING_DATASOURCE_PASSWORD in your deployment environment for production."
else
  echo "SPRING_DATASOURCE_URL is set"
fi

# If a datasource URL is provided, attempt to wait for the host:port to be reachable
if [ -n "$SPRING_DATASOURCE_URL" ]; then
  # Extract host and port from JDBC URL like jdbc:mysql://host:3306/dbname
  # This is a simple parse and may not cover all JDBC variants.
  hostport=$(echo "$SPRING_DATASOURCE_URL" | sed -E 's#jdbc:[a-z]+://([^/]+).*#\1#')
  host=$(echo "$hostport" | cut -d':' -f1)
  port=$(echo "$hostport" | cut -s -d':' -f2)
  if [ -n "$host" ] && [ -n "$port" ]; then
    echo "Waiting for database $host:$port to be reachable (timeout 60s)..."
    # Use bash TCP check if available
    if command -v bash >/dev/null 2>&1; then
      timeout=60
      until bash -c "</dev/tcp/$host/$port" >/dev/null 2>&1; do
        timeout=$((timeout-1))
        if [ "$timeout" -le 0 ]; then
          echo "Timed out waiting for $host:$port"
          break
        fi
        sleep 1
      done
      echo "Continuing startup."
    else
      echo "bash not found in image; skipping DB wait check."
    fi
  fi
fi

# Exec the jar (allow passing extra JVM or app args)
exec java -jar /app/app.jar "$@"
