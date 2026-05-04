# DSCPMARK

An Android application for setting DSCP (Differentiated Services Code Point) marks for outgoing traffic of specific applications.

## Features
- List of all installed applications with search capability.
- Assign custom DSCP marks to selected applications.
- Automatic rule application on device boot.
- Uses `iptables` for traffic marking.

## Requirements
- **Root access**: Required to modify `iptables` rules.
- **iptables**: Must be installed on the system.
- **Kernel support**: The kernel must support the `DSCP` target in the `mangle` table.

## How it works
The application creates a custom `DSCPMARK` chain in the `mangle` table of `iptables`. For each selected application, a rule is added that matches packets by the user's UID (`--uid-owner`) and sets the specified DSCP mark (`-j DSCP --set-dscp`).

A main rule in the `OUTPUT` chain redirects traffic to the `DSCPMARK` chain:
```bash
iptables -t mangle -A OUTPUT -j DSCPMARK
```

## Changelog
### v0.0.2 (2026-05-04)
- An updated algorithm for storing app information.

### v0.0.1 (2026-04-24)
- Initial release.
- Application listing and search.
- DSCP marking via iptables.
- Persistent rules across reboots.
