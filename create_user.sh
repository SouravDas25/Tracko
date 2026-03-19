#!/bin/bash
# Quick user creation script for Tracko

read -p "Name: " name
read -p "Email: " email
read -p "Phone: " phone
read -p "Base currency (e.g. INR, USD) [INR]: " currency
currency=${currency:-INR}
read -sp "Password: " password
echo

python -m cli user upsert \
  --name "$name" \
  --email "$email" \
  --phone "$phone" \
  --base-currency "$currency" \
  --password "$password"
