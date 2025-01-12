<h1><img alt="RG-Logo.svg" src="cli/src/asciidoc/remoteGuardianLogo_darkmode.svg" width="200"/>
<br>
Move</h1>

Move files with confidence!

### What is it?
`move` is a cli tool from Remote Guardian's Cybersecurity software portfolio. Use `move` to be certain your files have maintained their integrity while moving them to a new location. `move` uses one of a number of hash algorithms to validate the integrity of the files as they are moved.

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

### Installation

Download the binary for your platform from [move's latest release](https://github.com/RemoteGuardian/move/releases).


### Build from source

metadata has to be gathered using the graal native agent prior to building. Without this, packages will not be found 
you go to run the executable. Not providing this metadata will not trip any errors, but the executable will not function
properly. 

All the following instructions are performed from the root of the repository.

#### Create the metadata

```bash
 ./gradlew -Pagent :cli:run
 ```

#### Copy the metadata to the cli submodule's META-INF/native-image directory
```bash
./gradlew :cli:metadataCopy --task run --dir /src/main/resources/META-INF/native-image
```
> [!NOTE] 
> Notice that the flags relative to the cli submodule no longer reference paths from the root of the repository
> (ie `/src/main/resources/META-INF/native-image` instead of `/cli/src/main/resources/META-INF/native-image` or `--task run` instead of `:cli:run`).
> 
### Build Executable with added metadata

```bash
./gradlew :cli:nativeCompile
```