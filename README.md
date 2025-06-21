# Level up table Plugin

A comprehensive RuneLite plugin that displays all skill unlocks for every OSRS skill level (1-99). Features a modern UI with search, filtering, and direct wiki integration to help players plan their progression efficiently.

## Features

### 🎯 Core Features
- **Complete Skill Unlocks**: Browse all unlocks for every skill (1-99) including items, spells, quests, and activities
- **Modern UI Design**: Clean, intuitive interface with smooth animations and visual feedback
- **Smart Categorization**: Unlocks are automatically categorized by type (Item, Spell, Prayer, Quest, Location, Activity, Ability)
- **Live Player Stats**: Automatically updates based on your current skill levels

### 🔍 Search & Filter
- **Global Search**: Find any unlock across all skills instantly
- **Advanced Filtering**: Filter by unlock status, type, or proximity to your level
- **Milestone Grouping**: Unlocks organized into level ranges (1-9, 10-24, 25-49, 50-74, 75-98, 99)

### 🎨 Visual Features
- **Color-Coded Progress**:
  - 🟢 Green: Already unlocked
  - 🟡 Yellow: Next unlock (coming soon)
  - ⚪ Gray: Future unlocks
- **Expandable Cards**: Click any unlock for detailed requirements and quick actions
- **Progress Indicators**: Visual gauges showing overall skill completion
- **Interactive Buttons**: Quick access to wiki pages and clipboard copying

### ⚡ Performance
- **Wiki Data Integration**: Fetches live data from OSRS Wiki's level up tables
- **Smart Caching**: Reduces API calls and improves load times
- **Concurrent Loading**: All skills load simultaneously for faster startup

## Installation

### From RuneLite Plugin Hub (Recommended)
1. Open RuneLite
2. Click on the Configuration icon (wrench)
3. Select "Plugin Hub"
4. Search for "Level up table"
5. Click Install

### Manual Installation (Development)
1. Clone this repository
2. Run `./gradlew build`
3. Use the provided launcher scripts:
   - **macOS**: Double-click `RuneLite-LevelUpTable.command` or the `.app` bundle
   - **Windows/Linux**: Run `./gradlew runClient`

## Usage

1. Once installed, click on the Level up table icon in the RuneLite sidebar
2. Select a skill by clicking on its icon in the grid at the top
3. Browse through the unlocks for that skill
4. Use the search bar to find specific unlocks across all levels
5. Click "Refresh Wiki Data" to manually update the data from the wiki

## Configuration

The plugin provides several configuration options:

- **Refresh on startup**: Force refresh skill data from wiki when the plugin starts
- **Show only unlocked**: Filter to show only items you have already unlocked
- **Highlight next unlock**: Highlight the next upcoming unlock for each skill
- **Cache expiry**: How long to cache wiki data before refreshing (in hours)

## Development

### Requirements
- Java 11 or higher
- Gradle
- RuneLite development environment

### Building
```bash
./gradlew build
```

### Testing
```bash
./gradlew test
```

### Running in Developer Mode
```bash
# Build and run with test client
./gradlew runClient

# Or use the convenient launcher (macOS)
./RuneLite-LevelUpTable.command
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Technical Details

### Architecture
- **Data Source**: OSRS Wiki API (MediaWiki) - parses level up tables
- **Caching**: Local JSON cache in `~/.runelite/level-up-table/`
- **Parser**: Custom wiki markup parser supporting templates and links
- **UI Framework**: Swing with custom modern components

### Key Components
- `WikiClient`: Handles API communication with OSRS Wiki
- `WikiTextParser`: Parses wiki markup and extracts unlock data
- `UnlockRepository`: Manages data storage and caching
- `ModernSkillUnlockPanel`: Main UI with search, filters, and cards

### Performance Optimizations
- Concurrent skill data fetching
- Lazy loading of skill content
- Virtual scrolling for large unlock lists
- Debounced search input

## License

This project is licensed under the BSD 2-Clause License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [RuneLite](https://runelite.net) developers for the plugin framework
- [OSRS Wiki](https://oldschool.runescape.wiki) contributors for maintaining skill unlock data
- The OSRS community for feedback and suggestions

## Screenshots

![Level up table Main View](https://i.imgur.com/placeholder1.png)
*Main plugin interface showing skill selection and unlock cards*

![Search and Filter](https://i.imgur.com/placeholder2.png)
*Search functionality and filter options*

![Expanded Card View](https://i.imgur.com/placeholder3.png)
*Detailed view with requirements and actions*