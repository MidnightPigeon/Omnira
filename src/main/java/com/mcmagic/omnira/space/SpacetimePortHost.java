package com.mcmagic.omnira.space;

public interface SpacetimePortHost {
    SpacetimePortState port();
    default void detachPortKinetics() {}
    default void attachPortKinetics() {}
}
