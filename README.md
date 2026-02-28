# Alphabet Soup

A logic puzzle game for Android inspired by Sudoku. Fill an N×N grid with letters so that every row, column, and diagonal contains each letter exactly once — plus exactly one empty cell.

## How It Works

Clues are shown around the edges of the grid. Each clue reveals the **first visible letter** seen from that direction (empty cells are skipped). Use the clues and logical deduction to reconstruct the full grid.

The game offers five grid sizes: **4×4**, **5×5**, **6×6**, **7×7**, and **8×8**.

Additional features:
- **Pencil mode** — note candidate letters in a cell without committing
- **Undo** — step back through your moves
- **Timer** — track how long each puzzle takes you
- **Share** — brag about your solve time when you finish

## Requirements

- Android Studio **Meerkat** or later
- Android SDK **API 24** (Android 7.0) or higher
- JDK 11+

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/sfluegel05/alphabet-soup.git
   cd alphabet-soup
   ```

2. Open the project in Android Studio, or build from the command line:
   ```bash
   ./gradlew build
   ```

## Running the App

**From Android Studio:**
Select your target device or emulator and click **Run** (or press `Shift+F10`).

**From the command line:**
```bash
./gradlew installDebug
```
This builds a debug APK and installs it on a connected device or running emulator.

## Running Tests

**Unit tests** (run on your development machine):
```bash
./gradlew test
```

**Instrumented tests** (run on a connected device or emulator):
```bash
./gradlew connectedAndroidTest
```

## Bug Reports

Found a bug? Please [open an issue](https://github.com/sfluegel05/alphabet-soup/issues) and include:
- A description of what happened and what you expected
- Steps to reproduce the problem
- Your Android version and device model

## Contributing

Contributions are welcome! To get started:

1. Fork the repository and create a branch for your change.
2. Make your changes, following the existing code style (Kotlin + Jetpack Compose).
3. Add or update tests where appropriate.
4. Open a pull request with a clear description of what you changed and why.

## License

This project is licensed under the [MIT License](LICENSE).
