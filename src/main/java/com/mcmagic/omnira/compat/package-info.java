/**
 * Optional integrations are entered only behind a ModList presence check.
 * Create factories provide kinetic blocks only when Create is loaded; ordinary
 * Omnira blocks remain available without it. Do not reference optional types in
 * unconditional event subscriber signatures or superclass declarations.
 *
 * Early mixins are separated into create, sable and simulated configurations.
 * OptionalIntegrationMixins checks LoadingModList, not the later runtime ModList.
 * Present integrations require their injection points; absent ones are excluded.
 *
 * Sable reads data/omnira/physics_block_properties and floating_materials itself.
 * Keep these loader-owned paths intact: moving JSON under a compat directory
 * makes it invisible. No Sable runtime is necessary to register Omnira blocks.
 */
package com.mcmagic.omnira.compat;
