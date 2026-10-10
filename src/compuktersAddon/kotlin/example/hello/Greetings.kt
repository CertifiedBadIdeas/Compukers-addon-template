/*
 * The Compukters Developers
 * Copyright 2026 Vsevolod Petrov (lazyhat)
 * SPDX-License-Identifier: Apache-2.0
 */

package example.hello

/** This object is available to Kotlin programs running inside Compukters. */
public object Greetings {
    public fun hello(): String = GreetingBindings.hello()
}

// Only the actual host boundary is external. The SDK generates its typed handler.
private object GreetingBindings {
    external fun hello(): String
}
