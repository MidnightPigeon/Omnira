package com.mcmagic.omnira.mana;

public record ManaState(double current, double maximum, int affinity) {
    public ManaState(double current,double maximum) {this(current,maximum,0);}
    public static final double DEFAULT_MAXIMUM = 100.0D;
    public static final int REGEN_INTERVAL = 4;
    public int color() { return Affinity.byId(affinity).color; }
    public String affinityTranslationKey() { return Affinity.byId(affinity).key(); }

    public ManaState {
        if (!Double.isFinite(current) || !Double.isFinite(maximum) || maximum < 0.000001D || maximum > 1_000_000) {
            throw new IllegalArgumentException("Invalid mana values");
        }
        current = Math.max(0, Math.min(current, maximum));
    }

    public static ManaState initial() {
        return new ManaState(DEFAULT_MAXIMUM, DEFAULT_MAXIMUM);
    }

    public ManaState regenerate() {
        return new ManaState(current + maximum * 0.01D, maximum,affinity);
    }

    public ManaState withCurrent(double amount) {
        return new ManaState(amount, maximum,affinity);
    }

    public ManaState withMaximum(double amount) {
        return new ManaState(current, amount,affinity);
    }

    public boolean canSpend(double amount) {
        return Double.isFinite(amount) && amount >= 0 && current >= amount;
    }

    public ManaState spend(double amount) {
        if (!canSpend(amount)) {
            throw new IllegalArgumentException("Invalid or unaffordable mana cost");
        }
        return withCurrent(current - amount);
    }
}
