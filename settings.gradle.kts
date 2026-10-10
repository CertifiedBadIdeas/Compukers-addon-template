/*
 * The Compukters Developers
 * Copyright 2026 Vsevolod Petrov (lazyhat)
 * SPDX-License-Identifier: Apache-2.0
 */

check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_25)) {
    "Run Gradle on JDK 25 or newer; the addon itself targets Java 21."
}
apply(from = "gradle/compukters-sdk.settings.gradle.kts")
rootProject.name = "compukters-example-addon"
