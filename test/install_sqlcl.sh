#!/bin/bash
# test/install_sqlcl.sh - Setup script for Oracle SQLcl

echo "Starting SQLcl setup..."

# Basic check for Java, which is required by SQLcl
if command -v java &> /dev/null; then
    echo "Java is installed: $(java -version 2>&1 | head -n 1)"
else
    echo "Error: Java is not found. SQLcl requires Java to run."
fi

# Check if sqlcl is already in path and is actually Oracle SQLcl
IS_SQLCL=false
if command -v sql &> /dev/null; then
    if timeout 5s sql -version 2>&1 | grep -q "SQLcl"; then
        IS_SQLCL=true
    fi
fi

if [ "$IS_SQLCL" = true ]; then
    echo "SQLcl (sql) is already installed."
else
    echo "SQLcl not found in PATH. Attempting to install..."

    SQLCL_DIR="$HOME/sqlcl"
    if [ ! -d "$SQLCL_DIR" ]; then
        mkdir -p "$SQLCL_DIR"
        echo "Downloading SQLcl..."
        curl -L https://download.oracle.com/otn_software/java/sqldeveloper/sqlcl-latest.zip -o sqlcl-latest.zip
        echo "Unzipping SQLcl..."
        unzip -q sqlcl-latest.zip -d "$SQLCL_DIR"
        rm sqlcl-latest.zip
    fi

    # Find the bin directory
    SQLCL_BIN=$(find "$SQLCL_DIR" -name "sql" -type f | grep "/bin/sql" | head -n 1)
    if [ -n "$SQLCL_BIN" ]; then
        # Ensure we have an absolute path
        SQLCL_BIN_DIR=$(cd "$(dirname "$SQLCL_BIN")" && pwd)
        echo "SQLcl found at $SQLCL_BIN_DIR"

        # If not already in PATH, add it
        if [[ ":$PATH:" != *":$SQLCL_BIN_DIR:"* ]]; then
            echo "Adding $SQLCL_BIN_DIR to PATH (prepended)"
            export PATH="$SQLCL_BIN_DIR:$PATH"
        fi

        # If in GitHub Actions, add to GITHUB_PATH
        if [ -n "$GITHUB_PATH" ]; then
            echo "$SQLCL_BIN_DIR" >> "$GITHUB_PATH"
            echo "Added $SQLCL_BIN_DIR to GITHUB_PATH"
        fi
        IS_SQLCL=true
    else
        echo "Error: Could not find sql binary in $SQLCL_DIR."
        exit 1
    fi
fi

echo "SQLcl setup script finished."

# Output version
if [ -n "$SQLCL_BIN" ]; then
    "$SQLCL_BIN" -version
elif [ "$IS_SQLCL" = true ]; then
    sql -version
fi
