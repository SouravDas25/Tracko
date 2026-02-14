#!/usr/bin/env python3
"""
Database seeding script for Tracko
Uses the tracko_cli module to populate the database with sample data
"""

import json
import sys
import time
import uuid
from datetime import datetime, timedelta
import tracko_cli


def log(message):
    """Print timestamped log message"""
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{timestamp}] {message}")


def wait_for_api(base_url: str, max_attempts: int = 30):
    """Wait for API to be available"""
    log(f"Waiting for API at {base_url}...")
    
    for attempt in range(max_attempts):
        try:
            result = tracko_cli.http_request("GET", f"{base_url}/api/health")
            if result.get("ok"):
                log("API is ready!")
                return True
        except Exception as e:
            pass
        
        log(f"Attempt {attempt + 1}/{max_attempts} - API not ready, waiting 2 seconds...")
        time.sleep(2)
    
    log("ERROR: API not available after maximum attempts")
    return False


def login_existing_user(base_url: str):
    """Login using existing user credentials"""
    log("Logging in with existing user credentials...")
    
    url = tracko_cli._join_url(base_url, "/api/login")
    body = {
        "username": "user@example.com",
        "password": "password"
    }
    
    result = tracko_cli.http_request("POST", url, json_body=body)
    
    if result.get("ok"):
        token = result.get("json", {}).get("token")
        if token:
            log("Login successful!")
            return token
        else:
            log("Login failed: No token in response")
    else:
        log(f"Login failed: {result.get('text', 'Unknown error')}")
    
    return None


def create_accounts(base_url: str, token: str):
    """Create sample accounts"""
    log("Creating sample accounts...")
    
    accounts = [
        {"name": "HDFC Savings"},
        {"name": "ICICI Credit Card"},
        {"name": "Cash Wallet"},
        {"name": "Paytm Wallet"},
        {"name": "Investment Account"}
    ]
    
    account_ids = []
    url = tracko_cli._join_url(base_url, "/api/accounts")
    
    for account_data in accounts:
        result = tracko_cli.http_request("POST", url, token=token, json_body=account_data)
        if result.get("ok"):
            response_data = result.get("json", {})
            # Handle different response formats
            account_id = None
            if "result" in response_data and response_data["result"]:
                account_id = response_data["result"].get("id")
            elif "id" in response_data:
                account_id = response_data["id"]
            
            if account_id:
                account_ids.append(account_id)
                log(f"Created account: {account_data['name']} (ID: {account_id})")
            else:
                log(f"Created account: {account_data['name']} (ID: None - response format issue)")
                # Still add None to maintain list structure
                account_ids.append(None)
        else:
            log(f"Failed to create account {account_data['name']}: {result.get('text', 'Unknown error')}")
    
    return account_ids


def create_categories(base_url: str, token: str):
    """Create sample categories"""
    log("Creating sample categories...")
    
    categories = [
        {"name": "Food & Dining"},
        {"name": "Transportation"},
        {"name": "Shopping"},
        {"name": "Entertainment"},
        {"name": "Bills & Utilities"},
        {"name": "Healthcare"},
        {"name": "Education"},
        {"name": "Travel"},
        {"name": "Investments"},
        {"name": "Salary"},
        {"name": "Freelance"},
        {"name": "Other Income"}
    ]
    
    category_ids = []
    url = tracko_cli._join_url(base_url, "/api/categories")
    
    for category_data in categories:
        result = tracko_cli.http_request("POST", url, token=token, json_body=category_data)
        if result.get("ok"):
            response_data = result.get("json", {})
            # Handle different response formats
            category_id = None
            if "result" in response_data and response_data["result"]:
                category_id = response_data["result"].get("id")
            elif "id" in response_data:
                category_id = response_data["id"]
            
            if category_id:
                category_ids.append(category_id)
                log(f"Created category: {category_data['name']} (ID: {category_id})")
            else:
                log(f"Created category: {category_data['name']} (ID: None - response format issue)")
                # Still add None to maintain list structure
                category_ids.append(None)
        else:
            log(f"Failed to create category {category_data['name']}: {result.get('text', 'Unknown error')}")
    
    return category_ids


def create_contacts(base_url: str, token: str):
    """Create sample contacts"""
    log("Creating sample contacts...")
    
    contacts = [
        {"name": "Alice Johnson", "phoneNo": "9876543210", "email": "alice@example.com"},
        {"name": "Bob Smith", "phoneNo": "9876543211", "email": "bob@example.com"},
        {"name": "Charlie Brown", "phoneNo": "9876543212", "email": "charlie@example.com"},
        {"name": "Diana Prince", "phoneNo": "9876543213", "email": "diana@example.com"},
        {"name": "Eve Wilson", "phoneNo": "9876543214", "email": "eve@example.com"}
    ]
    
    contact_ids = []
    url = tracko_cli._join_url(base_url, "/api/contacts")
    
    for contact_data in contacts:
        result = tracko_cli.http_request("POST", url, token=token, json_body=contact_data)
        if result.get("ok"):
            response_data = result.get("json", {})
            # Handle different response formats
            contact_id = None
            if "result" in response_data and response_data["result"]:
                contact_id = response_data["result"].get("id")
            elif "id" in response_data:
                contact_id = response_data["id"]
            
            if contact_id:
                contact_ids.append(contact_id)
                log(f"Created contact: {contact_data['name']} (ID: {contact_id})")
            else:
                log(f"Created contact: {contact_data['name']} (ID: None - response format issue)")
                # Still add None to maintain list structure
                contact_ids.append(None)
        else:
            log(f"Failed to create contact {contact_data['name']}: {result.get('text', 'Unknown error')}")
    
    return contact_ids


def create_transactions(base_url: str, token: str, account_ids: list, category_ids: list):
    """Create sample transactions"""
    log("Creating sample transactions...")
    
    # Filter out None IDs and get valid ones
    valid_account_ids = [aid for aid in account_ids if aid is not None]
    valid_category_ids = [cid for cid in category_ids if cid is not None]
    
    if not valid_account_ids:
        log("No valid account IDs available, skipping transactions")
        return 0
    
    if not valid_category_ids:
        log("No valid category IDs available, skipping transactions")
        return 0
    
    # Generate transactions over the last 3 months
    now = datetime.now()
    transactions = []
    
    # Expense transactions
    expense_data = [
        {"name": "Grocery Shopping", "category_idx": 0, "amount_range": (50, 200)},
        {"name": "Uber Ride", "category_idx": 1, "amount_range": (10, 50)},
        {"name": "Amazon Purchase", "category_idx": 2, "amount_range": (20, 150)},
        {"name": "Movie Tickets", "category_idx": 3, "amount_range": (15, 40)},
        {"name": "Electricity Bill", "category_idx": 4, "amount_range": (80, 200)},
        {"name": "Doctor Visit", "category_idx": 5, "amount_range": (50, 300)},
        {"name": "Online Course", "category_idx": 6, "amount_range": (30, 100)},
        {"name": "Flight Booking", "category_idx": 7, "amount_range": (200, 800)},
    ]
    
    # Income transactions
    income_data = [
        {"name": "Monthly Salary", "category_idx": 9, "amount_range": (3000, 5000)},
        {"name": "Freelance Project", "category_idx": 10, "amount_range": (500, 2000)},
        {"name": "Investment Returns", "category_idx": 11, "amount_range": (100, 500)},
    ]
    
    # Generate transactions for the previous 3 months from current month
    current_year = now.year
    current_month = now.month
    
    log(f"Generating transactions for previous 3 months from {current_month}/{current_year}")
    
    # Calculate the 3 previous months
    for months_back in range(1, 4):  # 1, 2, 3 months back
        target_month = current_month - months_back
        target_year = current_year
        
        # Handle year rollover
        if target_month <= 0:
            target_month += 12
            target_year -= 1
        
        log(f"Generating transactions for month: {target_month}/{target_year} ({months_back} month(s) back)")
        
        # Generate transactions for each day of the target month
        days_in_month = 31  # Simplified, will be adjusted by datetime
        try:
            # Find the actual last day of the month
            if target_month == 12:
                next_month_first_day = datetime(target_year + 1, 1, 1)
            else:
                next_month_first_day = datetime(target_year, target_month + 1, 1)
            last_day_of_month = (next_month_first_day - timedelta(days=1)).day
            days_in_month = last_day_of_month
        except:
            days_in_month = 30  # Fallback
        
        for day in range(1, days_in_month + 1):
            # Random transaction generation - not every day has transactions
            if tracko_cli.random.random() < 0.7:  # 70% chance of transactions on any given day
                transaction_date = datetime(target_year, target_month, day, 
                                          tracko_cli.random.randint(9, 21),  # Random hour 9-21
                                          tracko_cli.random.randint(0, 59))  # Random minute
                
                # Generate 1-3 transactions per day
                num_transactions = tracko_cli.random.randint(1, 3)
                
                for i in range(num_transactions):
                    # Expense transactions (more frequent)
                    if tracko_cli.random.random() < 0.7:  # 70% chance of expense
                        expense = tracko_cli.random.choice(expense_data)
                        transactions.append({
                            "transactionType": 1,  # DEBIT/EXPENSE
                            "name": expense["name"],
                            "comments": f"Sample expense transaction from {target_month}/{target_year}",
                            "date": int(transaction_date.timestamp() * 1000),
                            "accountId": tracko_cli.random.choice(valid_account_ids),
                            "categoryId": tracko_cli.random.choice(valid_category_ids[:8]),  # Expense categories
                            "isCountable": 1,
                            "amount": round(tracko_cli.random.uniform(*expense["amount_range"]), 2)
                        })
                    
                    # Income transactions (less frequent)
                    elif tracko_cli.random.random() < 0.3:  # 30% chance of income
                        income = tracko_cli.random.choice(income_data)
                        transactions.append({
                            "transactionType": 2,  # CREDIT/INCOME
                            "name": income["name"],
                            "comments": f"Sample income transaction from {target_month}/{target_year}",
                            "date": int(transaction_date.timestamp() * 1000),
                            "accountId": valid_account_ids[0],  # Usually to main account
                            "categoryId": tracko_cli.random.choice(valid_category_ids[8:]),  # Income categories
                            "isCountable": 1,
                            "amount": round(tracko_cli.random.uniform(*income["amount_range"]), 2)
                        })
    
    log(f"Generated {len(transactions)} transactions across the previous 3 months")
    
    # Create transactions in batches
    url = tracko_cli._join_url(base_url, "/api/transactions")
    created_count = 0
    
    for tx in transactions:
        result = tracko_cli.http_request("POST", url, token=token, json_body=tx)
        if result.get("ok"):
            created_count += 1
            if created_count % 10 == 0:
                log(f"Created {created_count} transactions...")
        else:
            log(f"Failed to create transaction {tx['name']}: {result.get('text', 'Unknown error')}")
    
    log(f"Created {created_count} transactions total")
    return created_count


def create_budget_allocations(base_url: str, token: str, category_ids: list):
    """Create sample budget allocations for current month"""
    log("Creating sample budget allocations...")
    
    # Filter out None IDs and get valid ones
    valid_category_ids = [cid for cid in category_ids if cid is not None]
    
    if not valid_category_ids:
        log("No valid category IDs available, skipping budget allocations")
        return 0
    
    now = datetime.now()
    allocations = [
        {"categoryId": valid_category_ids[0], "amount": 500.0, "name": "Food & Dining"},
        {"categoryId": valid_category_ids[1] if len(valid_category_ids) > 1 else valid_category_ids[0], "amount": 200.0, "name": "Transportation"},
        {"categoryId": valid_category_ids[2] if len(valid_category_ids) > 2 else valid_category_ids[0], "amount": 300.0, "name": "Shopping"},
        {"categoryId": valid_category_ids[3] if len(valid_category_ids) > 3 else valid_category_ids[0], "amount": 150.0, "name": "Entertainment"},
        {"categoryId": valid_category_ids[4] if len(valid_category_ids) > 4 else valid_category_ids[0], "amount": 250.0, "name": "Bills & Utilities"},
        {"categoryId": valid_category_ids[5] if len(valid_category_ids) > 5 else valid_category_ids[0], "amount": 100.0, "name": "Healthcare"},
        {"categoryId": valid_category_ids[6] if len(valid_category_ids) > 6 else valid_category_ids[0], "amount": 50.0, "name": "Education"},
    ]
    
    url = tracko_cli._join_url(base_url, "/api/budget/allocate")
    created_count = 0
    
    for allocation in allocations:
        body = {
            "month": now.month,
            "year": now.year,
            "categoryId": allocation["categoryId"],
            "amount": allocation["amount"]
        }
        
        result = tracko_cli.http_request("POST", url, token=token, json_body=body)
        if result.get("ok"):
            created_count += 1
            log(f"Allocated ${allocation['amount']} to {allocation['name']}")
        else:
            log(f"Failed to allocate budget for {allocation['name']}: {result.get('text', 'Unknown error')}")
    
    log(f"Created {created_count} budget allocations")
    return created_count


def create_currencies(base_url: str, token: str):
    """Create sample currency configurations"""
    log("Creating sample currency configurations...")
    
    currencies = [
        {"currencyCode": "EUR", "exchangeRate": 0.85},
        {"currencyCode": "GBP", "exchangeRate": 0.73},
        {"currencyCode": "JPY", "exchangeRate": 110.0},
        {"currencyCode": "INR", "exchangeRate": 74.0},
        {"currencyCode": "CAD", "exchangeRate": 1.25},
        {"currencyCode": "AUD", "exchangeRate": 1.35},
    ]
    
    url = tracko_cli._join_url(base_url, "/api/user-currencies")
    created_count = 0
    
    for currency in currencies:
        result = tracko_cli.http_request("POST", url, token=token, json_body=currency)
        if result.get("ok"):
            created_count += 1
            log(f"Added currency: {currency['currencyCode']} (Rate: {currency['exchangeRate']})")
        else:
            log(f"Failed to add currency {currency['currencyCode']}: {result.get('text', 'Unknown error')}")
    
    log(f"Created {created_count} currency configurations")
    return created_count


def create_sample_splits(base_url: str, token: str, contact_ids: list, account_ids: list, category_ids: list):
    """Create sample split transactions"""
    log("Creating sample split transactions...")
    
    # Filter out None IDs and get valid ones
    valid_contact_ids = [cid for cid in contact_ids if cid is not None]
    valid_account_ids = [aid for aid in account_ids if aid is not None]
    valid_category_ids = [cid for cid in category_ids if cid is not None]
    
    if not valid_contact_ids:
        log("No valid contact IDs available, skipping splits")
        return 0
    
    if not valid_account_ids:
        log("No valid account IDs available, skipping splits")
        return 0
    
    if not valid_category_ids:
        log("No valid category IDs available, skipping splits")
        return 0
    
    # First create a transaction that can be split
    url = tracko_cli._join_url(base_url, "/api/transactions")
    
    transaction_data = {
        "transactionType": 1,  # DEBIT/EXPENSE
        "name": "Group Dinner",
        "comments": "Dinner with friends - to be split",
        "date": int((datetime.now() - timedelta(days=5)).timestamp() * 1000),
        "accountId": valid_account_ids[0],
        "categoryId": valid_category_ids[0],  # Food & Dining
        "isCountable": 1,
        "amount": 300.0
    }
    
    result = tracko_cli.http_request("POST", url, token=token, json_body=transaction_data)
    if not result.get("ok"):
        log("Failed to create transaction for splits")
        return 0
    
    response_data = result.get("json", {})
    transaction_id = None
    if "result" in response_data and response_data["result"]:
        transaction_id = response_data["result"].get("id")
    elif "id" in response_data:
        transaction_id = response_data["id"]
    
    if not transaction_id:
        log("No transaction ID returned")
        return 0
    
    # Now create splits for this transaction
    splits_url = tracko_cli._join_url(base_url, "/api/splits")
    split_count = 0
    
    # Create splits with different contacts
    split_amounts = [100.0, 75.0, 125.0]  # Should sum to 300
    
    for i, contact_id in enumerate(valid_contact_ids[:3]):
        split_data = {
            "transactionId": transaction_id,
            "userId": None,  # Can be null for contact splits
            "amount": split_amounts[i],
            "contactId": contact_id,
            "isSettled": i % 2 == 0  # Alternate settled/unsettled
        }
        
        result = tracko_cli.http_request("POST", splits_url, token=token, json_body=split_data)
        if result.get("ok"):
            split_count += 1
            status = "settled" if split_data["isSettled"] else "unsettled"
            log(f"Created split: ${split_amounts[i]} for contact {contact_id} ({status})")
        else:
            log(f"Failed to create split: {result.get('text', 'Unknown error')}")
    
    log(f"Created {split_count} splits")
    return split_count


def main():
    """Main seeding function"""
    base_url = "http://localhost:8080"
    
    log("Starting Tracko database seeding...")
    log(f"Target API: {base_url}")
    
    # Wait for API to be available
    if not wait_for_api(base_url):
        sys.exit(1)
    
    # Login with existing user credentials
    token = login_existing_user(base_url)
    if not token:
        log("Failed to login, aborting...")
        sys.exit(1)
    
    log("Successfully authenticated!")
    
    # Create sample data
    account_ids = create_accounts(base_url, token)
    category_ids = create_categories(base_url, token)
    contact_ids = create_contacts(base_url, token)
    
    # Create transactions (requires accounts and categories)
    if account_ids and category_ids:
        transaction_count = create_transactions(base_url, token, account_ids, category_ids)
    else:
        transaction_count = 0
        log("Skipping transactions due to missing accounts or categories")
    
    # Create budget allocations (requires categories)
    if category_ids:
        budget_count = create_budget_allocations(base_url, token, category_ids)
    else:
        budget_count = 0
        log("Skipping budget allocations due to missing categories")
    
    # Create currency configurations
    currency_count = create_currencies(base_url, token)
    
    # Create sample splits (requires contacts, accounts, categories)
    if contact_ids and account_ids and category_ids:
        split_count = create_sample_splits(base_url, token, contact_ids, account_ids, category_ids)
    else:
        split_count = 0
        log("Skipping splits due to missing contacts, accounts, or categories")
    
    # Summary
    log("\n" + "="*60)
    log("DATABASE SEEDING COMPLETE!")
    log("="*60)
    log(f"User: user@example.com")
    log(f"Accounts created: {len(account_ids)}")
    log(f"Categories created: {len(category_ids)}")
    log(f"Contacts created: {len(contact_ids)}")
    log(f"Transactions created: {transaction_count}")
    log(f"Budget allocations created: {budget_count}")
    log(f"Currency configurations created: {currency_count}")
    log(f"Splits created: {split_count}")
    log("="*60)
    log("\nYou can now use the CLI with:")
    log(f"python tracko_cli.py --base-url {base_url} --token {token} [command]")
    log("\nExample commands:")
    log("python tracko_cli.py accounts list")
    log("python tracko_cli.py transactions list")
    log("python tracko_cli.py budget view")


if __name__ == "__main__":
    # Add missing random import to tracko_cli if not present
    if not hasattr(tracko_cli, 'random'):
        import random
        tracko_cli.random = random
    
    main()
