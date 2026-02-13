#!/bin/bash
# MySQL Setup Script for Money Transfer System
# Run this script to set up MySQL database and user

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}Money Transfer System - MySQL Setup${NC}"
echo "======================================"

# Check if MySQL is installed
if ! command -v mysql &> /dev/null; then
    echo -e "${YELLOW}MySQL is not installed. Please install MySQL first.${NC}"
    exit 1
fi

# Get MySQL root password
read -sp "Enter MySQL root password: " ROOT_PASSWORD
echo ""

# Create database and user
echo -e "${BLUE}Setting up database...${NC}"

mysql -u root -p"$ROOT_PASSWORD" << EOF
CREATE DATABASE IF NOT EXISTS money_transfer_system;

-- Create a dedicated user for the application (optional)
CREATE USER IF NOT EXISTS 'mts_user'@'localhost' IDENTIFIED BY 'mts_password';
GRANT ALL PRIVILEGES ON money_transfer_system.* TO 'mts_user'@'localhost';
FLUSH PRIVILEGES;

-- Show confirmation
SELECT 'Database setup complete!' as status;
EOF

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Database setup completed successfully!${NC}"
    echo ""
    echo "Database: money_transfer_system"
    echo "Application User: mts_user"
    echo "Application Password: mts_password"
    echo ""
    echo -e "${YELLOW}Update your application.yml with these credentials!${NC}"
else
    echo -e "${YELLOW}Error during database setup. Please verify MySQL is running and credentials are correct.${NC}"
    exit 1
fi
