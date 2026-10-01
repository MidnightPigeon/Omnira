package com.mcmagic.omnira.world.structure;

/** Spawn samples use local Y=1 for the surrounding surface block, not local Y=0. */
public final class AuthoredStructurePlacement {
    private AuthoredStructurePlacement(){}
    public static int originY(int surfaceBlockY){return surfaceBlockY-1;}
}
