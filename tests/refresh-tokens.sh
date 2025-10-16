#!/bin/bash

# Script to refresh JWT tokens for test users
# Run this when tokens expire (after 24 hours)

API_BASE="http://localhost:8080/api"

echo "=========================================="
echo "Refreshing JWT Tokens for Test Users"
echo "=========================================="
echo ""

# Login Player 1
echo "Player 1 (player_one):"
P1_RESPONSE=$(curl -s -X POST "$API_BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail": "player_one", "password": "Password123@"}')

P1_TOKEN=$(echo $P1_RESPONSE | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
P1_ID=$(echo $P1_RESPONSE | sed -n 's/.*"userId":"\([^"]*\)".*/\1/p')

echo "  User ID: $P1_ID"
echo "  Token: $P1_TOKEN"
echo ""

# Login Player 2
echo "Player 2 (player_two):"
P2_RESPONSE=$(curl -s -X POST "$API_BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail": "player_two", "password": "Password123@"}')

P2_TOKEN=$(echo $P2_RESPONSE | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
P2_ID=$(echo $P2_RESPONSE | sed -n 's/.*"userId":"\([^"]*\)".*/\1/p')

echo "  User ID: $P2_ID"
echo "  Token: $P2_TOKEN"
echo ""

echo "=========================================="
echo "Update test-websocket.html with these:"
echo "=========================================="
echo ""
echo "Player 1 Token (paste into HTML):"
echo "$P1_TOKEN"
echo ""
echo "Player 2 Token (paste into HTML):"
echo "$P2_TOKEN"
echo ""
echo "Player 2 ID (paste into HTML):"
echo "$P2_ID"
echo ""

# Optional: Automatically update the HTML file
read -p "Update test-websocket.html automatically? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    # Backup first
    cp tests/test-websocket.html tests/test-websocket.html.bak

    # Update tokens in HTML
    sed -i '' "s/id=\"player1Token\" size=\"100\" value=\"[^\"]*\"/id=\"player1Token\" size=\"100\" value=\"$P1_TOKEN\"/" tests/test-websocket.html
    sed -i '' "s/id=\"player2Token\" size=\"100\" value=\"[^\"]*\"/id=\"player2Token\" size=\"100\" value=\"$P2_TOKEN\"/" tests/test-websocket.html
    sed -i '' "s/id=\"player2Id\" size=\"40\" value=\"[^\"]*\"/id=\"player2Id\" size=\"40\" value=\"$P2_ID\"/" tests/test-websocket.html

    echo "✅ test-websocket.html updated!"
    echo "   Backup saved to test-websocket.html.bak"
else
    echo "Skipped automatic update. Copy tokens manually to HTML."
fi
