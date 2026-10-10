# Compukters addon template

A minimal NeoForge 1.21.1 addon exposing `example.hello.Greetings.hello()` to programs running inside Compukters.
It includes a Guest API, a typed host implementation, a committed ABI lock and a runnable Guest example.

## Requirements

- JDK 25 to run Gradle and the Guest API compiler.
- JDK 21 installed as a Gradle toolchain for Minecraft 1.21.1 and the addon.
- Internet access for ordinary Gradle, Kotlin and NeoForge dependencies and the first SDK download.

The addon and Compukters must both be installed in Minecraft. Create, Sable and Propulsion are not required.

## Build with a published SDK

Use GitHub's **Use this template** button and clone your new repository. `compuktersRelease` in `gradle.properties`
selects the exact Compukters release. Run:

```sh
./gradlew build
./gradlew runClient
```

On Windows, use `gradlew.bat`. The first build downloads `compukters-addon-development-<version>.zip` and its SHA256
inventory from the selected public GitHub Release. No GitHub token or external Compukters Maven service is required.
The ZIP contains an isolated file repository used by Gradle for the SDK plugin, compiler, platform, API and named
development mod. Kotlin and NeoForge continue to resolve from their ordinary public repositories.

The download cache is separated by product version and archive hash. Set `compuktersSdkSha256` to the SDK SHA256
from the release's `checksums.sha256` to pin that identity in your repository. Use `--offline` only after the SDK and
the remaining dependencies have been downloaded. A missing or invalid SDK fails the build; it never switches to sources.

**Availability:** 0.6.0 is the first planned release containing this archive. Until it is published, use the local
development steps below. The existing 0.5.0 release does not contain a downloadable SDK.

The verified production JAR is under `build/libs/`. Install it alongside the matching Compukters mod; the template
does not package Compukters, its APIs, Minecraft or NeoForge classes into your addon.

## Local development against Compukters sources

Source mode also requires the Rust toolchain used by the main repository. Clone Compukters separately,
then initialize its pinned submodules over public HTTPS:

```sh
git clone --branch dev https://github.com/CertifiedBadIdeas/Compukters.git ../Compukters
git -C ../Compukters -c url.https://github.com/.insteadOf=git@github.com: submodule update --init --recursive
cd ../Compukters
./gradlew stageAddonDevelopmentDependencies
cd ../your-addon
./gradlew build -PcompuktersWorkspace=../Compukters
./gradlew runClient -PcompuktersWorkspace=../Compukters
```

This explicitly selects the local SDK and composes the main source checkout. API, Guest compiler, platform bundle
and development mod rebuild through included-build task dependencies. Run the staging task again after changing the
SDK plugin itself. No development releases or moving Git tags are used.

To work with a locally unpacked SDK without any main source checkout, use
`-PcompuktersSdkDirectory=/absolute/path/to/unpacked-sdk` instead. Do not combine this with `compuktersWorkspace`.

## Try the Guest program

Launch the dev client, place and open a Compukters computer, then open the IDE. Open the included `examples/hello`
project by its full path, resolve dependencies, build, and deploy it to the computer. It prints:

```text
Hello from my Compukters addon!
```

The Guest code calls `Greetings.hello()`. The generated contract routes that call to `ExampleCapabilityHandler.hello()`
in the addon. The handler returns `addonCompleted(...)`; no Minecraft objects cross the Guest boundary.

## Make it your own

- Choose your mod ID, display name and Kotlin host package in the metadata and host source.
- Rename the Guest namespace `example` in its source paths and packages, `compuktersAddon.register`, the generated
  contract imports, and the Guest example manifest. The mod ID and Guest namespace are different identifiers.
- Replace the greeting operation with your own Guest API and host implementation. Generated files under `build/`
  are outputs; do not edit or commit them.
- After an intentional API change, run `./gradlew updateCompuktersAddonAbiLock` and review/commit the resulting
  `src/compuktersAddon/addon.lock`. Ordinary builds validate this lock and never update it silently.
- Keep the required Compukters dependency and API classes compile-only. Each live Guest program needs a fresh host
  instance, as in the example's registration factory.

See the [addon development guide](https://certifiedbadideas.github.io/Compukters/ADDON-DEVELOPMENT/) for asynchronous
operations, peripheral contracts, versioning and lifecycle ownership.
