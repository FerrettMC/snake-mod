# Snake Mod

A Minecraft Forge mod that brings the classic Snake game to life inside your world using blocks and player movement.

---

## How It Works

Type `snake 10` in chat and a full Snake board is instantly built around you — checkered green floor, glass walls, and a small control platform you're teleported to. Step in any direction on the platform to start moving your snake. Eat the apples to grow. Don't hit the walls or yourself.

---

## Commands

| Command | Description |
|---|---|
| `snake <size>` | Start a game (size 4–100) |
| `snake <size> <speed>` | Start with custom speed (1–6) |
| `stop` | Quit your current game |
| `clear` | Remove the board after a game |
| `help` | Show command info |

---

## Features

- Fully in-world Snake built from Minecraft blocks
- Checkerboard green concrete floor with glass walls
- Alternating cyan and light blue wool body
- Apples spawn randomly — more appear every 5 points, up to 5 at once
- Score displayed above the hotbar throughout gameplay
- Game over title screen with final score and a firework
- Speed boost effect while playing for responsive controls
- Fully customizable board size and speed per game
- Multiplayer support — every player runs a completely independent game simultaneously

---

## Building

**Requirements**
- Java 21
- Forge MDK for Minecraft 1.21

```bash
git clone https://github.com/yourname/snakemod
cd snakemod
./gradlew build
```

The built jar will be at `build/libs/snakemod-<version>.jar`.

---

## Installation

1. Install [Forge for Minecraft 1.21](https://files.minecraftforge.net)
2. Drop the jar into your `mods/` folder
3. Launch Minecraft with the Forge profile

---

## About

Built as part of a modding content series — the first in a series of videos where increasingly cursed and creative Minecraft mods get coded from scratch. More coming soon.
