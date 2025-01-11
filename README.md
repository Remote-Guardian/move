<h1><img alt="RG-Logo.svg" src="cli/src/asciidoc/remoteGuardianLogo_darkmode.svg" width="200"/>
<br>
Move</h1>

Move files with confidence!

### What is it?
`move` is a cli tool from Remote Guardian's Cybersecurity software portfolio. Use `move` to be certain your files have maintained their integrity while moving them to a new location. `move` uses one of a number of hash algorithms to validate the integrity of the files as they are moved.

### Installation

Download the binary for your platform from [move's latest release](https://github.com/RemoteGuardian/move/releases).

### Usage

```bash
Usage: move [-hV] -o=<outputDirectory> [-i=<input>[,<input>...]]...
recursively moves files and directories
  -h, --help      Show this help message and exit.
  -i, -F, --input=<input>[,<input>...]
                  File path(s) or directory path
  -o, --output, --destination=<outputDirectory>
                  output directory
  -V, --version   Print version information and exit
```




