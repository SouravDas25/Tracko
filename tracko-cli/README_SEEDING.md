# Tracko Database Seeding Script

This script seeds the Tracko database with sample data for testing and development purposes.

## Prerequisites

1. Tracko backend server running on `http://localhost:8080`
2. Existing user with credentials:
   - Username: `user@example.com`
   - Password: `password`

## Usage

### Run the seeding script

```bash
cd tracko-cli
python seed_database.py
```

### What gets created

The script creates the following sample data:

1. **Accounts** (5)
   - HDFC Savings
   - ICICI Credit Card
   - Cash Wallet
   - Paytm Wallet
   - Investment Account

2. **Categories** (12)
   - Food & Dining
   - Transportation
   - Shopping
   - Entertainment
   - Bills & Utilities
   - Healthcare
   - Education
   - Travel
   - Investments
   - Salary
   - Freelance
   - Other Income

3. **Contacts** (5)
   - Alice Johnson
   - Bob Smith
   - Charlie Brown
   - Diana Prince
   - Eve Wilson

4. **Transactions** (~36)
   - Generated over the last 3 months
   - Mix of income and expense transactions
   - Various amounts and categories

5. **Budget Allocations** (5-7)
   - Monthly budget allocations for expense categories
   - Based on available income

6. **Currency Configurations** (6)
   - EUR (0.85)
   - GBP (0.73)
   - JPY (110.0)
   - INR (74.0)
   - CAD (1.25)
   - AUD (1.35)

7. **Split Transactions** (0-3)
   - Sample split transactions with contacts

## Script Features

- **Error Handling**: Robust error handling with detailed logging
- **API Health Check**: Waits for the API to be available before starting
- **ID Resolution**: Handles different API response formats for entity IDs
- **Progress Tracking**: Shows progress during data creation
- **Summary Report**: Provides a complete summary of created data

## Authentication

The script uses the existing user credentials (`user@example.com` / `password`) to authenticate with the API. After successful login, it receives a JWT token that's used for all subsequent API calls.

## Output

The script provides:
- Real-time progress updates
- Success/failure logging for each operation
- Final summary with counts of all created entities
- Authentication token for CLI usage

## Using the CLI after seeding

After running the seeding script, you can use the Tracko CLI with the provided token:

```bash
# List accounts
python tracko_cli.py --base-url http://localhost:8080 --token <TOKEN> accounts list

# List transactions
python tracko_cli.py --base-url http://localhost:8080 --token <TOKEN> transactions list

# Get a transaction by ID
python tracko_cli.py --base-url http://localhost:8080 --token <TOKEN> transactions get --id 1

# Update a transaction by ID
python tracko_cli.py --base-url http://localhost:8080 --token <TOKEN> transactions update --id 1 --account-id 2 --category-id 2 --amount 300 --type expense --name "Lunch (updated)" --comments "Updated from CLI"

# Delete a transaction by ID
python tracko_cli.py --base-url http://localhost:8080 --token <TOKEN> transactions delete --id 1

# View budget
python tracko_cli.py --base-url http://localhost:8080 --token <TOKEN> budget view
```

## Troubleshooting

### API Not Available
If the script can't connect to the API, ensure:
- The Tracko backend is running on `http://localhost:8080`
- The server is healthy and ready to accept requests

### Authentication Issues
If login fails:
- Verify the user credentials are correct
- Check if the user exists in the database
- Ensure the backend authentication is working

### Insufficient Budget Funds
If budget allocations fail due to insufficient funds:
- The script will show available vs requested amounts
- This is normal behavior based on the income/expense ratio in generated transactions

### Split Creation Issues
If splits fail to create:
- This might be due to API validation rules
- The script logs the error details for debugging

## Customization

You can modify the script to:
- Change the sample data (names, amounts, etc.)
- Adjust the number of entities created
- Modify the time range for transactions
- Add different types of test data

## Notes

- The script is designed for development/testing purposes only
- It creates realistic but fictional data
- All operations are logged for debugging purposes
- The script handles API response format variations gracefully
