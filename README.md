# WGExtender

## Downloads

- Releases: https://github.com/sokolovskiyoleg/WGExtender/releases
- ---

WGExtender is a plugin that extends WorldGuard with additional region protection features, claim helpers, and configurable chat messages.

This repository is a maintained fork with updated dependencies, support for modern Paper versions, and configurable message formatting via `config.yml`.

## Fork Highlights

- Updated for Paper `1.21.11`
- Built with Java `21`
- Uses WorldGuard `7.0.15`
- Uses WorldEdit `7.4.1`
- Supports both legacy color codes and MiniMessage
- Keeps player-facing chat messages in `config.yml`

## Main Features

- Additional region protection behavior for liquids, fire, and explosions
- Claim command enhancements
- Automatic vertical expansion for selected regions
- Region size and block limit checks
- Command restrictions inside regions
- Extended WorldEdit wand support
- Configurable player messages and item display names

## Message Formatting

Messages are configured in [`resources/config.yml`](/home/oleg/IdeaProjects/WGExtender/resources/config.yml).

Supported serializers:

- `LEGACY` for classic `&` color codes
- `MINIMESSAGE` for Adventure MiniMessage tags like `<green>`, `<bold>`

All player-visible chat messages are loaded from the config, so message text and styling can be changed without editing Java code.

## Requirements

- Java `21`
- Paper `1.21.11`
- WorldGuard `7.0.15`
- WorldEdit `7.4.1`
- Vault, if your setup uses permission-based limits or integrations

## Installation

1. Install WorldEdit and WorldGuard on your server.
2. Install Vault if your permissions setup depends on it.
3. Download the latest JAR from Releases or build the plugin yourself.
4. Put the JAR into the server `plugins/` directory.
5. Start or restart the server.
6. Review `plugins/WGExtender/config.yml` and adjust settings if needed.

## Build

Build from the project root with Maven:

```bash
mvn package
```

In this fork, the project is also built in IntelliJ IDEA with Maven using Java 21.

Output artifact:

```text
target/wgextender-3.2.1.jar
```

## Configuration

The main configuration file is:

[`resources/config.yml`](/home/oleg/IdeaProjects/WGExtender/resources/config.yml)

It contains:

- claim limits
- region protection toggles
- automatic flags
- restricted commands
- wand settings
- message formatting and texts

## License

This project is licensed under the GNU AGPL v3.0. For details, see [LICENSE](./LICENSE).

## Fork Repository

[sokolovskiyoleg/WGExtender](https://github.com/sokolovskiyoleg/WGExtender)
