# Trivia Game - Mac Terminal Commands

## Step 1: Navigate to Project Directory
```bash
cd /Users/nilka/nhd5-IT114-005/Project
```

## Step 2: Compile All Java Files
```bash
javac -cp . Client/*.java Common/*.java Server/*.java Exceptions/*.java
```

## Step 3: Run the Server (Terminal 1)
```bash
cd /Users/nilka/nhd5-IT114-005/Project
java Server.Server
```

**Expected Output:**
```
Server starting on port 3000...
Created Room: lobby
```

## Step 4: Run First Client (Terminal 2)
```bash
cd /Users/nilka/nhd5-IT114-005/Project
java Client.Client
```

**What to do:**
1. Enter your name (e.g., "dave")
2. Click "Set Name" button
3. Click "Connect" button
4. Click "READY" button

## Step 5: Run Second Client (Terminal 3)
```bash
cd /Users/nilka/nhd5-IT114-005/Project
java Client.Client
```

**What to do:**
1. Enter your name (e.g., "bob")
2. Click "Set Name" button
3. Click "Connect" button
4. Click "READY" button

## Step 6: Game Should Start!
When both players click READY, the game will automatically start with the first question.

---

## Quick Test Commands (All in One)

### Compile and Run Server:
```bash
cd /Users/nilka/nhd5-IT114-005/Project && javac -cp . Client/*.java Common/*.java Server/*.java Exceptions/*.java && java Server.Server
```

### Compile and Run Client:
```bash
cd /Users/nilka/nhd5-IT114-005/Project && javac -cp . Client/*.java Common/*.java Server/*.java Exceptions/*.java && java Client.Client
```

---

## Troubleshooting

### If compilation fails:
```bash
# Clean old class files
rm -rf Client/*.class Common/*.class Server/*.class Exceptions/*.class

# Recompile
javac -cp . Client/*.java Common/*.java Server/*.java Exceptions/*.java
```

### Check if server is running:
```bash
# Check if port 3000 is in use
lsof -i :3000
```

### Stop the server:
Press `Ctrl+C` in the server terminal

