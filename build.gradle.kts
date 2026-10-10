/*
 * The Compukters Developers
 * Copyright 2026 Vsevolod Petrov (lazyhat)
 * SPDX-License-Identifier: Apache-2.0
 */

import java.util.Properties
import java.util.jar.Manifest
import java.util.zip.ZipFile

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.10"
    id("dev.architectury.loom") version "1.17.491"
    id("architectury-plugin") version "3.5.169"
    id("ru.lazyhat.compukters.addon")
}

group = "org.example.compukters"
version = providers.gradleProperty("addonVersion").get()
base { archivesName.set("compukters-example-1.21.1-neoforge") }

val sdkDirectory = file(gradle.extra["compuktersSdkDirectory"].toString())
val sdk = Properties().apply { sdkDirectory.resolve("sdk.properties").inputStream().use(::load) }
val sdkVersion = sdk.getProperty("sdkVersion")
val modVersion = sdk.getProperty("modVersion")
val modBuildVersion = sdk.getProperty("modBuildVersion")
val modUpperBound = modVersion.split('.').let { "${it[0]}.${it[1].toInt() + 1}.0" }

repositories {
    exclusiveContent {
        forRepository { maven { url = uri(sdkDirectory.resolve("repository")) } }
        filter { includeGroup("ru.lazyhat.compukters") }
    }
    mavenCentral()
    maven("https://maven.architectury.dev/")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.neoforged.net/releases/")
}

kotlin {
    jvmToolchain(21)
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
}
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
architectury {
    minecraft = "1.21.1"
    platformSetupLoomIde()
    neoForge()
}
compuktersAddon {
    register("example")
    toolingCoordinate.set("ru.lazyhat.compukters:compukters-addon-tooling:$sdkVersion")
    platformCoordinate.set("ru.lazyhat.compukters:compukters-guest-platform:$sdkVersion@cpb")
    commonApiCoordinate.set("ru.lazyhat.compukters:compukters-addon-api:$sdkVersion")
    adapterApiCoordinate.set("ru.lazyhat.compukters:compukters-addon-neoforge-1.21.1:$sdkVersion")
}

providers.gradleProperty("compuktersWorkspace").orNull?.let { path ->
    val workspace = file(path)
    val mainBuild = gradle.includedBuild("Compukters")
    configurations.named("compuktersAddonTooling") {
        withDependencies {
            clear()
            add(project.dependencies.create(files(workspace.resolve(
                "modules/common/compiler-k2-engine/build/devlibs/compukters-addon-tooling-development.jar",
            )).builtBy(mainBuild.task(":compiler-k2-engine:developmentAddonToolingJar"))))
        }
    }
    configurations.named("compuktersAddonPlatform") {
        withDependencies {
            clear()
            add(project.dependencies.create(files(workspace.resolve(
                "modules/common/guest-platform/build/platform/compukters-platform.cpb",
            )).builtBy(mainBuild.task(":guest-platform:assemblePlatformBundle"))))
        }
    }
    configurations.named("runtimeOnly") {
        withDependencies {
            removeIf { it.group == "ru.lazyhat.compukters" && it.name == "compukters-neoforge-1.21.1-dev" }
            add(project.dependencies.create(files(workspace.resolve(
                "modules/minecraft/v1_21_1/v1_21_1-neoforge/build/devlibs/compukters-development-mod.jar",
            )).builtBy(mainBuild.task(":v1_21_1-neoforge:developmentModJar"))))
        }
    }
}

dependencies {
    minecraft("net.minecraft:minecraft:1.21.1")
    mappings(loom.officialMojangMappings())
    neoForge("net.neoforged:neoforge:21.1.252")
    runtimeOnly("ru.lazyhat.compukters:compukters-neoforge-1.21.1-dev:$modVersion")
    listOf(
        "org.jetbrains.kotlin:kotlin-stdlib:2.4.10",
        "io.github.oshai:kotlin-logging-jvm:8.0.4",
        "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.11.0",
        "org.tukaani:xz:1.12",
    ).forEach { forgeRuntimeLibrary(it) { isTransitive = false } }
}

loom {
    mods { maybeCreate("example_addon").sourceSet("main") }
    runs {
        named("client") { runDir("run/client") }
        named("server") { runDir("run/server") }
    }
}

tasks.processResources {
    inputs.property("addonVersion", project.version)
    inputs.property("compuktersRange", "[$modBuildVersion,$modUpperBound)")
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("addon_version" to project.version, "compukters_range" to "[$modBuildVersion,$modUpperBound)")
    }
}

tasks.jar { archiveClassifier.set("dev") }
tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") { archiveClassifier.set("") }

tasks.register("verifyProductionJar") {
    val jar = tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar")
    dependsOn(jar)
    inputs.file(jar.flatMap { it.archiveFile })
    doLast {
        ZipFile(jar.get().archiveFile.get().asFile).use { archive ->
            val names = archive.entries().asSequence().map { it.name }.toSet()
            check("META-INF/compukters/addons/example.cagb" in names)
            check("META-INF/neoforge.mods.toml" in names)
            check("org/example/compukters/ExampleAddonMod.class" in names)
            check(names.none { it.startsWith("ru/lazyhat/compukters/") || it.startsWith("net/minecraft/") }) {
                "Base-mod or Minecraft implementation classes leaked into the addon"
            }
        }
    }
}
tasks.named("check") { dependsOn("verifyProductionJar") }

tasks.register("verifyDevelopmentRuntime") {
    val runtime = configurations.named("runtimeClasspath")
    inputs.files(runtime)
    doLast {
        val mod = runtime.get().files.single {
            it.name == "compukters-development-mod.jar" || it.name == "compukters-neoforge-1.21.1-dev-$modVersion.jar"
        }
        ZipFile(mod).use { archive ->
            check(archive.getEntry("META-INF/neoforge.mods.toml") != null)
            check(archive.getEntry("ru/lazyhat/compukters/api/addon/minecraft/CompuktersAddonRegistry.class") != null)
            val manifest = archive.getInputStream(archive.getEntry("META-INF/MANIFEST.MF")).use {
                Manifest(it)
            }
            check(manifest.mainAttributes.getValue("Fabric-Loom-Remap") == "false") {
                "The development mod must already use named Minecraft mappings"
            }
        }
    }
}
tasks.named("check") { dependsOn("verifyDevelopmentRuntime") }
