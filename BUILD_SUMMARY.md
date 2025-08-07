# Blood Ties Plugin - Build Summary

## 🎉 Successfully Built!

The **Blood Ties** Minecraft social deduction minigame plugin has been successfully compiled and packaged.

## 📦 Build Output

- **JAR File**: `target/blood-ties-1.0.0.jar` (65.9 KB)
- **Version**: 1.0.0
- **Target**: Spigot 1.21.4
- **Java Version**: 17

## 🚀 Installation

1. **Download**: Copy `blood-ties-1.0.0.jar` to your server's `plugins/` folder
2. **Start Server**: The plugin will auto-generate configuration files
3. **Configure**: Edit `plugins/BloodTies/config.yml` as needed
4. **Restart**: Restart your server to load the plugin

## 🎮 Quick Start

1. **Join Queue**: `/bt join solo` (or duo/squad)
2. **Wait**: Plugin auto-starts when minimum players reached
3. **Play**: Follow in-game instructions and role assignments
4. **Vote**: Use GUI during voting phases
5. **Win**: Survive as Survivors or eliminate all as Monster

## ✨ Features Implemented

### ✅ Core Game Systems
- **5 Unique Roles**: Monster, Healer, Wizard, Silent One, Survivor
- **3 Game Modes**: Solo, Duo, Squad
- **Advanced Voting**: GUI-based voting with role modifications
- **Karma System**: Persistent player statistics and progression
- **Sound System**: Immersive horror audio effects

### ✅ Commands
- `/bt join <mode>` - Join game queue
- `/bt leave` - Leave game/queue
- `/bt vote <player>` - Vote during voting phase
- `/bt sellvote <player> <info>` - Trade votes for information
- `/bt useability` - Use role-specific abilities
- `/bt stats` - View personal statistics
- `/bt roles` - View role descriptions
- `/bt whisper <player> <message>` - Private messaging
- `/bt team` - Team information (Duo/Squad)
- `/bt help` - Command help

### ✅ Admin Commands
- `/bt admin forcestart <mode>` - Force start game
- `/bt admin stopall` - Stop all active games
- `/bt admin kick <player>` - Kick player from game
- `/bt admin stats` - Server statistics

### ✅ Game Features
- **Role Assignment**: Random role distribution
- **Voting GUI**: Interactive voting interface
- **Ability System**: Role-specific powers and cooldowns
- **Win Conditions**: Multiple victory paths
- **Statistics Tracking**: Comprehensive player data
- **Ambient Effects**: Horror atmosphere and sounds

## 🔧 Configuration

The plugin generates these configuration files:

- `config.yml` - Main game settings
- `data.yml` - Player statistics and game history

### Key Settings
```yaml
game:
  min_players: 6
  max_players: 20
  round_duration: 180  # 3 minutes
  vote_duration: 30    # 30 seconds

roles:
  monster_kill_cooldown: 300  # 5 minutes
  healer_heal_cooldown: 600   # 10 minutes
  wizard_spell_cooldown: 120  # 2 minutes

karma:
  vote_monster: 10     # Karma for voting out Monster
  vote_innocent: -3    # Karma for voting innocent
  betray_team: -5      # Karma for team betrayal
```

## 🎯 Game Flow

1. **Lobby**: Players join queue, auto-start when ready
2. **Spawn**: 30-second exploration phase
3. **Action**: 3-minute rounds with role abilities
4. **Voting**: GUI-based voting phase
5. **Consequence**: Sacrifice chosen players
6. **Repeat**: Continue until win condition met

## 🏆 Win Conditions

- **Survivors Win**: Identify and vote out the Monster
- **Monster Wins**: Eliminate all Survivors
- **Secret Wins**: Wizard collects soul shards, Silent One survives unnoticed

## 📊 Statistics Tracked

- Games played/won per role
- Vote accuracy
- Karma progression
- Kill counts
- Win rates
- Role-specific achievements

## 🎨 Customization

- **Cosmetics**: Unlockable particle trails and animations
- **Sounds**: Configurable audio settings
- **Timing**: Adjustable round and voting durations
- **Balancing**: Role cooldown and karma adjustments

## 🔍 Technical Details

- **Architecture**: Modular design with separate managers
- **Performance**: Optimized for large player counts
- **Compatibility**: Spigot 1.21.4+, Java 17+
- **Dependencies**: Minimal external dependencies
- **Data Storage**: YAML-based configuration and statistics

## 🐛 Troubleshooting

### Common Issues
1. **Plugin won't load**: Check Java version (requires 17+)
2. **Commands not working**: Verify permissions
3. **Sounds not playing**: Check sound settings in config
4. **Games not starting**: Ensure minimum players in queue

### Debug Mode
Enable in config.yml:
```yaml
debug_mode: true
```

## 📈 Future Enhancements

The plugin is designed for easy extension:
- New roles can be added to `Role.java`
- Additional abilities in `GamePlayer.java`
- Custom commands in `BloodTiesCommand.java`
- Enhanced sounds in `SoundManager.java`

## 🎉 Ready for Production!

The Blood Ties plugin is fully production-ready with:
- ✅ Complete game mechanics
- ✅ GUI voting system
- ✅ Role-based abilities
- ✅ Statistics tracking
- ✅ Admin controls
- ✅ Comprehensive documentation
- ✅ Error handling
- ✅ Performance optimization

**Install the JAR file and start playing Blood Ties today!**

---

*Blood Ties - Where trust is your greatest weapon and your greatest weakness.*