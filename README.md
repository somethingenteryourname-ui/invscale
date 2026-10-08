# InvScale

**Independent, precise scaling for the hotbar, inventory, container screens and every major HUD element — without touching Minecraft's GUI Scale.**

Client-side Fabric mod for **Minecraft 1.21.11**. Built for PvP: run a tiny hotbar with a huge inventory.

```
Hotbar      0.80x
Inventory   3.50x
Containers  2.50x
HUD         0.90x
```

## Features

- **Four independent scales** — Hotbar, Inventory, Containers, HUD. Changing one never changes another.
- **Real scaling, correct hitboxes** — scaled screens get their own coordinate system: rendering is scaled with the GUI matrix and mouse input is transformed by the exact inverse, so clicking, dragging, shift-clicking, dropping, tooltips, buttons, the recipe book, armor and crafting slots all line up perfectly.
- **Per-screen scales** — Player Inventory, Creative, Chest, Large Chest, Shulker Box, Crafting Table, Furnace, Blast Furnace, Smoker, Anvil, Enchanting Table, Brewing Stand, Hopper, Dispenser, Dropper, Loom, Stonecutter, Smithing Table, Grindstone, Cartography Table, Beacon, Villager Trading, Horse, Crafter and modded containers. Anything without an override uses the Container (or Inventory) scale.
- **Per-element HUD** — Crosshair, Health, Armor, Hunger, Air, Mount Health, XP bar, Item name, Action bar, Status effects, Boss bar, Scoreboard, Titles, Player list: each with on/off, show/hide, scale and X/Y offset.
- **Hotbar placement** — Center / Left / Right + custom X/Y offset. Health, hunger, armor and XP can follow the hotbar.
- **Presets** — Default, PvP, Crystal PvP, Mace PvP, Minimal, Large Inventory, Competitive, plus your own (save, load, rename, delete, reset).
- **Live preview** — the real HUD updates while you drag (the menu fades out while dragging), and container previews show the exact size.
- **Never inaccessible** — screens that would not fit are reduced automatically; HUD elements always stay on screen; every tab has Reset.
- **Sharp items** — item icons are rendered at higher resolution when shown larger.
- **Two scale modes** — Relative (multiplies your GUI Scale, 1.00x = vanilla) or Absolute (exact pixel size, 4.00x = GUI Scale 4).

## Commands (client-side)

| Command | |
|---|---|
| `/invscale` | Open the settings |
| `/invscale reset` | Reset everything to Default |
| `/invscale preset <name>` | Load a preset |
| `/invscale presets` | List presets |
| `/invscale save <name>` | Save current settings as a preset |
| `/invscale hotbar\|inventory\|containers\|hud <scale>` | Set a scale |
| `/invscale toggle` | Turn InvScale on/off |
| `/invscale status` | Show current scales |
| `/invscale reload` | Reload `config/invscale.json` |

## Keybinds

Options → Controls → Key Binds → **InvScale**

- Open InvScale — `I`
- Toggle PvP Preset — unbound
- Toggle Between Profiles — unbound (profiles chosen in the Presets tab)

## Requirements

- Minecraft 1.21.11, Fabric Loader ≥ 0.17.3, Fabric API, Java 21
- Mod Menu optional (adds a config button)

## Fair play

Purely visual. InvScale sends no packets, adds nothing to the server and changes no gameplay — no reach, aim, automation or any other advantage. Works on any server without server-side installation.

## Building

```
./gradlew build
```

The jar is written to `build/libs/`. GitHub Actions builds every push and uploads the jar as an artifact.
