# Fatsan Utilities for Server

Fatsan Utilities for Server is a Folia-first, Paper-compatible utility plugin for Minecraft 1.21.11 servers. It was built as a practical core utility layer for survival servers that want economy, teleport, chat, and admin quality-of-life features without depending on large all-in-one plugins that may not behave well on Folia.

The plugin focuses on the features servers actually use every day: balance commands, player payments, TPA, spawn, RTP, homes, warps, item and inventory chat showcase, inventory viewing, a configurable in-game help menu, and a built-in economy that can either work on its own or integrate into an existing Vault ecosystem.

Instead of replacing your whole server stack, it provides a compact and modular base designed around Folia-safe scheduling, clean YAML configuration, persistent storage where needed, and realistic server-owner control over how each feature behaves.

## Features

- Folia-first design with Paper 1.21.11 compatibility
- Built around Folia-safe scheduler usage instead of legacy scheduler assumptions
- Built-in SQLite-backed economy provider with BigDecimal precision
- Vault and VaultUnlocked bridge support
- Economy modes:
  - `auto` to consume an existing provider if present, otherwise register the built-in one
  - `provider` to always use the built-in SQLite economy
  - `consumer` to only use an external provider
- Atomic player-to-player money transfers
- Persistent daily payment limit support
- `/balance`, `/balancetop`, `/pay`, and `/eco` economy features
- `/tpa`, `/tpaccept`, `/tpdeny`, `/spawn`, `/rtp`, `/home`, `/sethome`, `/delhome`, `/homes`, `/warp`, and `/setwarp`
- Persistent warp storage in `warps.yml`
- Persistent home storage in `homes.yml`
- Persistent configurable spawn storage in `spawn.yml`
- Config-driven `/help` GUI that overrides the default help command
- In-game help menu that closes normally with `ESC` or `E`
- Chat showcase commands for held items and inventory context
- Admin inventory viewer with optional ender chest access
- Runtime module toggles for economy, teleport, social, admin, and chat features
- Turkish and English language support
- Per-player language selection through permissions or client locale follow mode
- Async audit logging to `logs/audit.log`
- Thread-safe internal state for cooldowns, rate limits, TPA requests, and pay limits
- Player quit cleanup to avoid stale requests and temporary-state leaks
- STARTUP load order so economy registration happens early for dependent plugins

## Safe Teleport Cost Handling

Delayed teleports are handled carefully so players do not lose money just because a teleport was cancelled before completion.

The plugin first checks whether the player can afford the teleport, waits through the configured delay, cancels if movement rules are violated, and only charges right before the teleport attempt is made. If a charged teleport attempt fails at that point, the cost is refunded automatically.

This applies to delayed teleport flows such as homes and warps, and avoids the common problem where movement cancellation still consumes money.

## Commands

- `/balance [player]`
- `/balancetop [page]`
- `/pay <player> <amount>`
- `/eco <give|take|set|reset> <player> [amount]`
- `/tpa <player>`
- `/tpaccept`
- `/tpdeny`
- `/spawn`
- `/setspawn`
- `/rtp [world]`
- `/home [name]`
- `/sethome [name]`
- `/delhome [name]`
- `/homes`
- `/warp <name>`
- `/setwarp <name>`
- `/itemchat`
- `/invchat`
- `/invsee <player> [ender]`
- `/help`
- `/fudebug`
- `/fumodule <module> <on|off|status>`
- `/futilitiesreload`

## Permissions

- `fatsanutilities.balance`
- `fatsanutilities.balancetop`
- `fatsanutilities.pay`
- `fatsanutilities.admin.eco`
- `fatsanutilities.tpa`
- `fatsanutilities.tpa.accept`
- `fatsanutilities.tpa.deny`
- `fatsanutilities.spawn`
- `fatsanutilities.admin.setspawn`
- `fatsanutilities.rtp`
- `fatsanutilities.rtp.world.<world>`
- `fatsanutilities.home`
- `fatsanutilities.sethome`
- `fatsanutilities.delhome`
- `fatsanutilities.homes`
- `fatsanutilities.homes.max.<number>`
- `fatsanutilities.homes.unlimited`
- `fatsanutilities.warp`
- `fatsanutilities.admin.setwarp`
- `fatsanutilities.itemchat`
- `fatsanutilities.invchat`
- `fatsanutilities.admin.invsee`
- `fatsanutilities.help`
- `fatsanutilities.admin.debug`
- `fatsanutilities.admin.module`
- `fatsanutilities.admin.reload`
- `fatsanutilities.lang.tr`
- `fatsanutilities.lang.en`

## Configuration

The plugin is split into focused YAML files so server owners can manage behavior without editing one oversized config.

You can control things like:

- server default language
- whether player language should follow client locale
- which major modules are enabled
- chat format
- per-command rate limits
- economy mode and currency settings
- starting balance
- payment limits and balance-top cache settings
- teleport command costs
- TPA expiry and behavior
- RTP world settings, denied biomes, denied ground materials, and attempt counts
- home limits, cooldowns, delays, and movement cancellation
- warp delay, movement cancellation, and cooldown
- the `/help` menu title, size, filler items, slots, icons, and text content

## Config Files

- `config.yml` for general settings, modules, chat format, rate limits, and help menu layout
- `economy.yml` for economy mode, currency settings, pay limits, and balance-top settings
- `teleport.yml` for teleport costs, TPA, RTP, home, and warp behavior
- `cooldowns.yml` for command cooldown timings
- `messages_tr.yml` for Turkish messages
- `messages_en.yml` for English messages

## Runtime Data Files

- `data/economy.sqlite` for built-in economy storage when running in provider mode
- `spawn.yml` for the configured spawn location
- `homes.yml` for player homes
- `warps.yml` for named warps
- `logs/audit.log` for audit records

## Requirements

- Java 21
- Folia or Paper 1.21.11

## Optional Integrations

- Vault
- VaultUnlocked
- FoliaPerms
- LuckPerms

## Notes

This plugin is intended to be a modular survival utility base, not a full replacement for every admin or gameplay plugin on your server.

The built-in economy works well on its own, but the plugin can also run in consumer mode if your server already uses another Vault-compatible economy plugin.

The `/help` command is intentionally handled by the plugin and opens a configurable in-game GUI instead of the default help output.

RTP, homes, spawn, warps, and TPA behavior are configurable, so the default setup is meant to be a practical starting point rather than a fixed gameplay design.

The plugin ships with both Turkish and English localisation files, and missing language keys fall back safely.

## Development

```bash
./gradlew build
./gradlew test
./gradlew runServer
```

## License

See [LICENSE](LICENSE).
