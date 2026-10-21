package com.mcmagic.omnira.codex;

/** Shared codex state on native and optional kinetic hosts. */
public interface CodexAccess {
    MyriadCodexEntity codex();
    default float shaftSpeed(){return 0;}
    default double mechanicalPotential(){return 0;}
}
