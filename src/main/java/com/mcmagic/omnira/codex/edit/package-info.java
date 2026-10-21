/**
 * Isolated Myriad Codex execution. No recipes, inventory consumption, UI or source-entity lifecycle.
 *
 * <p>Parent integration: {@code CodexEdits.get(level).submit(center, diameter, operation, axis,
 * hollow, sourceStack, targetStack, biome, owner)}. Consume inputs only when accepted; save jobId
 * on the source and use active(jobId). The automatic per-level Post subscriber owns advancement.
 * Terminal records expire after 1200 game ticks even when their source no longer exists.
 *
 * <p>Mode 0 maps to MATTER; mode 1 shape 0/1/2 maps to SHAPE/INSCRIBED/PLANE; mode 2 to ECOLOGY.
 * Axis is X/Y/Z. Diameters are 7,9,11,13,15. Plane rings have radial thickness two. Placement
 * order is nearest-first with stable ordering inside a voxel. Block targets use default states,
 * including incomplete multipart targets; ordinary items retain exemplar components. One item
 * is dropped per selected voxel. MATTER changes whole block types and dropped item types:
 * drop-to-drop preserves total item count and splits at the target's maximum stack size with a
 * persisted split cursor; drop-to-block removes each matched
 * stack and places one block per occupied voxel. A moved/picked-up source drop is skipped.
 *
 * <p>Submission validates loaded chunks plus a neighbour halo, height and world border before any
 * changes. Ecology constructs at most two detached landscapes per level tick from biome profiles,
 * without searching for native terrain, requesting chunks or invoking feature placement.
 * Selected biome, seed, cached volume, planned mutations and cursors persist. Old pending searches
 * retain their selection and identity; already planned mutations continue unchanged.
 * Missing profiles use climate/tags and bounded inspection of configured tree providers, never a reroll.
 * The biome registry and world biome data are never modified.
 *
 * <p>Per level, at most 256 planned mutation actions are spent per tick (entity-to-block actions
 * reserve two). Vanilla neighbour shapes are deferred until placement completes and have recursion
 * depth one; ordinary scheduled world physics and mod callbacks keep their native behaviour.
 * This is not a cap on the world's own physics. Filled block entities are intentionally overwritten.
 * Jobs pause when their destination unloads. The system is resumable, not a cross-file transaction
 * between region saves, entity saves and SavedData after a process crash.
 *
 * <p>Biome extensions: omnira:codex/ecology/GROUP and omnira:codex/ecology/exclude/GROUP,
 * with lowercase group names. OMNI includes every registered biome. Loot overrides use biome tags
 * omnira:codex/ecology/loot/NAMESPACE/PATH; baseline palette surface overrides use
 * omnira:codex/ecology/palette/NAMESPACE/BLOCK_PATH. Those referenced blocks/tables must exist.
 * Landscape profiles: data/omnira/codex/ecology/BIOME_NAMESPACE/BIOME_PATH.json, with base/surface
 * block states and up to eight decorations (tree, spire, column, patch; block and optional crown).
 * The flat ground/liquid surface sits in the lower third. Terrain is placed first, decoration next,
 * and the treasure last. Containers/entities are not copied; exactly one central crystal ball receives a deferred
 * loot table and a saved seed. Loading a completed job never rerolls treasure.
 */
package com.mcmagic.omnira.codex.edit;
