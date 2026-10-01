package com.mcmagic.omnira.spell;

public record SpellTargetingRule(
        SpellAnchorPolicy anchorPolicy,
        double maxRange,
        boolean requiresHit,
        boolean allowsAirEndpoint
) {
    public static final double SELF_RANGE = 0.0D;
    public static final double GROUND_MAX_RANGE = 12.0D;
    public static final double TARGET_MAX_RANGE = 12.0D;
    public static final double AIM_MAX_RANGE = 16.0D;
    public static final double PROJECTILE_SPAWN_RANGE = 0.0D;

    public static SpellTargetingRule forPattern(SpellTargetKeyword targetKeyword, SpellShapeKeyword shapeKeyword) {
        if (targetKeyword == SpellTargetKeyword.GROUND) {
            // Range is a per-column vertical search limit, not a horizontal targeting ray.
            return new SpellTargetingRule(SpellAnchorPolicy.GROUND, GROUND_MAX_RANGE, false, false);
        }
        if (shapeKeyword == SpellShapeKeyword.PROJECTILE) {
            if (targetKeyword == SpellTargetKeyword.SELF) return new SpellTargetingRule(SpellAnchorPolicy.SELF_PROJECTILE, 0, false, false);
            return new SpellTargetingRule(SpellAnchorPolicy.PROJECTILE,
                    targetKeyword == SpellTargetKeyword.TARGET ? TARGET_MAX_RANGE : PROJECTILE_SPAWN_RANGE,
                    targetKeyword == SpellTargetKeyword.TARGET, false);
        }

        return switch (targetKeyword) {
            case SELF -> new SpellTargetingRule(SpellAnchorPolicy.CASTER, SELF_RANGE, false, false);
            case GROUND -> new SpellTargetingRule(SpellAnchorPolicy.GROUND, GROUND_MAX_RANGE, true, false);
            case TARGET -> new SpellTargetingRule(SpellAnchorPolicy.REQUIRED_HIT, TARGET_MAX_RANGE, true, false);
            case AIM -> new SpellTargetingRule(SpellAnchorPolicy.RAY_ENDPOINT, AIM_MAX_RANGE, false, true);
        };
    }
}
