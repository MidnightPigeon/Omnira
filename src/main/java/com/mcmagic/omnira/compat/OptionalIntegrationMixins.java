package com.mcmagic.omnira.compat;

import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Runs before ModList exists; never resolves classes from an optional mod. */
public final class OptionalIntegrationMixins implements IMixinConfigPlugin {
    private String modId;

    @Override public void onLoad(String mixinPackage) {
        modId = switch (mixinPackage) {
            case "com.mcmagic.omnira.compat.create.mixin" -> "create";
            case "com.mcmagic.omnira.compat.sable.mixin" -> "sable";
            case "com.mcmagic.omnira.compat.simulated.mixin" -> "simulated";
            default -> throw new IllegalArgumentException("Unknown integration package: " + mixinPackage);
        };
    }

    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        var mods = LoadingModList.get();
        return mods.getModFileById(modId) != null
                && (!modId.equals("simulated") || mods.getModFileById("create") != null);
    }
    @Override public String getRefMapperConfig() { return null; }
    @Override public List<String> getMixins() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
