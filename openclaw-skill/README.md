# Trako OpenClaw Skill 💰

An [OpenClaw](https://github.com/openclaw/openclaw) skill for managing personal finances through the [Trako CLI](https://github.com/SouravDas25/Tracko).

## What It Does

Talk to your OpenClaw agent in natural language and it will manage your expenses, income, transfers, budgets, splits, and more — all through the Trako CLI.

**Supported operations:**
- List/add/update/delete transactions (expenses, income, transfers)
- List accounts and balances
- List categories
- View and allocate budgets
- View splits and settle debts
- Spending statistics and summaries

## Prerequisites

- [OpenClaw](https://github.com/openclaw/openclaw) installed and running
- `trako` CLI installed and on your PATH ([download](https://github.com/SouravDas25/Tracko/releases/latest))
- Logged in via `trako auth login`

## Installation

### Linux / macOS

```bash
curl -sL https://github.com/SouravDas25/Tracko/archive/refs/heads/main.tar.gz \
  | tar xz --strip-components=1 -C /tmp Tracko-main/openclaw-skill
cp -r /tmp/openclaw-skill ~/.openclaw/skills/trako
rm -rf /tmp/openclaw-skill
```

### Windows (PowerShell)

```powershell
Invoke-WebRequest -Uri "https://github.com/SouravDas25/Tracko/archive/refs/heads/main.zip" -OutFile "$env:TEMP\tracko.zip"
Expand-Archive "$env:TEMP\tracko.zip" -DestinationPath "$env:TEMP\tracko" -Force
Copy-Item -Recurse "$env:TEMP\tracko\Tracko-main\openclaw-skill" "$env:USERPROFILE\.openclaw\skills\trako"
Remove-Item -Recurse -Force "$env:TEMP\tracko", "$env:TEMP\tracko.zip"
```

### Then configure your agent

Add `trako` to your agent's `config.yaml`:

```yaml
skills:
  - trako
```

Restart your agent:

```bash
openclaw restart
```

## Usage Examples

Just talk to your agent:

- "I spent 200 on dinner at a restaurant"
- "Show my account balances"
- "How much did I spend this month?"
- "Transfer 5000 from Savings to Cash"
- "What's my budget for March?"
- "Who owes me money?"

## Configuration

The Trako CLI reads its config from `~/.tracko-cli.json`. Make sure you have a valid profile with a token:

```bash
trako auth login --username your_username
trako config list   # verify active profile
```
