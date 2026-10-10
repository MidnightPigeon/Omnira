package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.registry.ModCreativeModeTabs;
import java.util.List;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value=CreativeModeTabRegistry.class,remap=false)
public abstract class CreativeTabGroupingMixin {
    @ModifyVariable(method="setCreativeModeTabOrder",at=@At("HEAD"),argsOnly=true)
    private static List<CreativeModeTab> omnira$groupTabs(List<CreativeModeTab> tabs){
        return ModCreativeModeTabs.groupTabs(tabs);
    }
}
