/*
 * The Compukters Developers
 * Copyright 2026 Vsevolod Petrov (lazyhat)
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example.compukters

import example.ExampleAddonContract
import example.ExampleCapabilityHandler
import net.neoforged.fml.common.Mod
import ru.lazyhat.compukters.api.addon.AddonCallResult
import ru.lazyhat.compukters.api.addon.addonCompleted
import ru.lazyhat.compukters.api.addon.minecraft.CompuktersAddonRegistry

@Mod("example_addon")
class ExampleAddonMod {
    init {
        CompuktersAddonRegistry.register(ExampleAddonContract.guestApi(ExampleAddonMod::class.java)) { _ ->
            ExampleAddonContract.host(
                object : ExampleCapabilityHandler {
                    override fun hello(): AddonCallResult<String> = addonCompleted("Hello from my Compukters addon!")
                },
            )
        }
    }
}
