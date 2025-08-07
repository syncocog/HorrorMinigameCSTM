# Blood Ties - Minecraft Social Deduction Minigame

A fully-featured social deduction minigame for Minecraft servers where players must survive and identify the Monster among them.

## 🎮 Game Overview

**Blood Ties** is a horror-themed social deduction game where players awaken in an abandoned hospital. Among them lurks a **Monster** disguised as a player. Each player is randomly assigned a hidden role with unique abilities. Every few minutes, players must vote to sacrifice one or two members. The goal: survive, discover the Monster, and escape—or manipulate and eliminate others if you are the Monster.

## ✨ Features

### 🎭 **5 Unique Roles**
- **🎭 Monster**: Can disguise, sabotage, and kill players
- **🩹 Healer**: Can restore players and sense deception
- **🧙 Wizard**: Can cast spells to reveal roles and vote counts
- **😶 Silent One**: Cannot speak but immune to votes once per game
- **🧍 Survivor**: Base role with investigation abilities

### 🎯 **3 Game Modes**
- **Solo**: No trusted allies. Full paranoia.
- **Duo**: You spawn with one known partner. But are they who they say they are?
- **Squad (4)**: Form blood pacts. Betrayal hurts more here.

### 🗳️ **Advanced Voting System**
- GUI-based voting interface
- Vote trading system (`/bt sellvote`)
- Private whisper chat (`/bt whisper`)
- Role-specific vote modifications

### ⚖️ **Karma System**
- Persistent player statistics
- Unlock cosmetics and effects
- Track role-specific wins
- Vote accuracy tracking

### 🔊 **Immersive Audio**
- Dynamic horror sound effects
- Heartbeat near the Monster
- Ambient hospital sounds
- Role-specific audio cues

## 📦 Installation

### Requirements
- **Spigot 1.21.4** or higher
- **Java 17** or higher
- **Maven** (for building from source)

### Quick Start
1. Download the latest release JAR file
2. Place it in your server's `plugins` folder
3. Start/restart your server
4. Configure the plugin in `plugins/BloodTies/config.yml`
5. Use `/bt join <mode>` to start playing!

### Building from Source
```bash
git clone https://github.com/your-repo/blood-ties.git
cd blood-ties
mvn clean package
```

## 🎮 Commands

### Player Commands
- `/bt join <mode>` - Join a game queue (solo/duo/squad)
- `/bt leave` - Leave current game or queue
- `/bt vote <player>` - Vote for a player during voting phase
- `/bt sellvote <player> <info>` - Offer your vote for trade
- `/bt useability` - Use your role's special ability
- `/bt stats` - View your statistics and karma
- `/bt roles` - View descriptions of all roles
- `/bt whisper <player> <message>` - Send private message
- `/bt team` - View team information (Duo/Squad only)
- `/bt help` - Show help menu

### Admin Commands
- `/bt admin forcestart <mode>` - Force start a game
- `/bt admin stopall` - Stop all active games
- `/bt admin kick <player>` - Kick player from their game
- `/bt admin stats` - View server statistics

## ⚙️ Configuration

### Main Configuration (`config.yml`)
```yaml
game:
  min_players: 6
  max_players: 20
  round_duration: 180  # 3 minutes
  vote_duration: 30    # 30 seconds
  lobby_duration: 60   # 1 minute

roles:
  monster_kill_cooldown: 300  # 5 minutes
  healer_heal_cooldown: 600   # 10 minutes
  wizard_spell_cooldown: 120  # 2 minutes

karma:
  vote_monster: 10     # Karma for voting out Monster
  vote_innocent: -3    # Karma for voting innocent
  betray_team: -5      # Karma for team betrayal
  survive_round: 1     # Karma for surviving

sounds:
  enabled: true
  volume: 0.5
  pitch: 1.0
```

### Permissions
- `bloodties.play` - Basic player permissions (default: true)
- `bloodties.admin` - Admin commands (default: op)
- `bloodties.staff` - Staff moderation (default: op)

## 🎯 Game Flow

### 1. **Lobby Phase**
- Players join queue for desired game mode
- Auto-start when minimum players reached
- 60-second countdown before game begins

### 2. **Spawn Phase**
- Players spawn in different hospital rooms
- 30 seconds of exploration before action begins
- Role assignments revealed to each player

### 3. **Action Phase**
- Players search for clues, use abilities, form alliances
- Monster can kill players (with cooldown)
- Random ambient horror events
- 3-minute rounds

### 4. **Voting Phase**
- GUI opens for all players
- Vote for who you think is the Monster
- Role abilities may affect voting
- 30-second voting window

### 5. **Consequence Phase**
- Chosen player(s) sacrificed
- Role revealed to all players
- Karma awarded/penalized
- Next round begins or game ends

## 🏆 Win Conditions

### Survivors Win When:
- Monster is identified and voted out
- Monster is killed by other means

### Monster Wins When:
- All Survivors are eliminated
- Monster escapes undetected

### Secret Win Conditions:
- **Wizard**: Collect all 3 soul shards
- **Silent One**: Survive until the end unnoticed

## 🎨 Customization

### Cosmetics
- Particle trails
- Custom sacrifice animations
- Alternate role skins
- Unlocked with karma

### Maps
- Procedurally generated hospital
- Multiple spawn points
- Secret passages (Monster only)
- Blood rune chambers for sacrifices

## 🔧 Development

### Project Structure
```
src/main/java/com/bloodties/
├── BloodTiesPlugin.java          # Main plugin class
├── commands/
│   └── BloodTiesCommand.java     # Command handler
├── game/
│   ├── Game.java                 # Main game logic
│   ├── GameManager.java          # Game management
│   ├── GamePlayer.java           # Player data
│   ├── GameState.java            # Game states
│   ├── GameMode.java             # Game modes
│   └── Role.java                 # Role definitions
├── gui/
│   └── VotingGUI.java            # Voting interface
├── listeners/
│   ├── GameListener.java         # Game events
│   └── LobbyListener.java        # Lobby events
├── managers/
│   ├── ConfigManager.java        # Configuration
│   ├── DataManager.java          # Data persistence
│   └── SoundManager.java         # Audio system
└── utils/
    └── Logger.java               # Logging utility
```

### Adding New Features
1. **New Roles**: Add to `Role.java` enum
2. **New Abilities**: Extend `GamePlayer.java`
3. **New Commands**: Add to `BloodTiesCommand.java`
4. **New Sounds**: Add to `SoundManager.java`

## 🐛 Troubleshooting

### Common Issues
1. **Plugin won't load**: Check Java version (requires 17+)
2. **Commands not working**: Verify permissions
3. **Sounds not playing**: Check sound settings in config
4. **Games not starting**: Ensure minimum players in queue

### Debug Mode
Enable debug mode in config to see detailed logs:
```yaml
debug_mode: true
```

## 📊 Statistics

The plugin tracks comprehensive statistics:
- Games played/won per role
- Vote accuracy
- Karma progression
- Kill counts
- Win rates

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

- **Issues**: Report bugs on GitHub
- **Discord**: Join our community server
- **Wiki**: Check the documentation wiki

---

**Blood Ties** - Where trust is your greatest weapon and your greatest weakness.
