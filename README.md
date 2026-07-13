# Servermatica

A Litematica companion mod for servers. Enables easy place, fixes crafter block interactions, splits large schematic packets so they work over standard server connections, and more.

## Features

- **Easy Place** — place schematic blocks without switching items manually
- **Easy Place Protocol** — server-side handshake so the server knows the client is using Litematica and can cooperate with block placement
- **Packet Splitter** — splits large schematic upload packets that would otherwise be rejected by servers
- **Crafter fixes** — corrects Litematica's interaction with the crafter block
- **Material List** — accurate block count for placement tasks
- **Carpet compatibility** — integrates with the Carpet mod where present

## Requirements

| Dependency | Side |
|---|---|
| [Litematica](https://modrinth.com/mod/litematica) | Client |
| [MaLiLib](https://modrinth.com/mod/malilib) | Client |
| [Carpet](https://modrinth.com/mod/carpet) | Server |

## Installation

Install on **both client and server**. The client-side features (easy place, schematic rendering) require Litematica. The server-side features (Easy Place Protocol, packet splitter) work independently.

## Usage

All features are configurable through the Litematica config screen (`M` → `Config`). The Easy Place Protocol activates automatically when both sides have Servermatica installed.

## License

MIT
