package com.mcmagic.omnira.compat;

import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

/** Mekanism machines open through their GUI attribute and write tile-specific opening data. */
public final class MekanismRemoteMenus {
    private record Api(Class<?> tile, Class<?> gui, Method attribute, Method provider, Method encode,
                       Method hasGui, Object security, Method access) {}

    private static Api load() {
        if (!ModList.get().isLoaded("mekanism")) return null;
        try {
            var tile = Class.forName("mekanism.common.tile.base.TileEntityMekanism");
            var gui = Class.forName("mekanism.common.block.attribute.AttributeGui");
            var attributes = Class.forName("mekanism.common.block.attribute.Attribute");
            var security = Class.forName("mekanism.api.security.IBlockSecurityUtils");
            return new Api(tile, gui, attributes.getMethod("get", BlockState.class, Class.class),
                    gui.getMethod("getProvider", tile, boolean.class),
                    tile.getMethod("encodeExtraContainerData", RegistryFriendlyByteBuf.class), tile.getMethod("hasGui"),
                    security.getField("INSTANCE").get(null),
                    security.getMethod("canAccess", Player.class, Level.class, BlockPos.class, BlockEntity.class));
        } catch (ReflectiveOperationException | LinkageError ex) {
            com.mojang.logging.LogUtils.getLogger().warn("Mekanism remote menu integration unavailable", ex);
            return null;
        }
    }

    private static final Api API = load();

    public static boolean isMachine(BlockEntity target) { return API != null && API.tile.isInstance(target); }

    public static boolean permits(Player player, BlockEntity target) {
        if (API == null || !API.tile.isInstance(target)) return true;
        try {
            return (boolean) API.access.invoke(API.security, player, target.getLevel(), target.getBlockPos(), target);
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    public static MenuProvider provider(BlockEntity target) {
        if (API == null || !API.tile.isInstance(target)) return null;
        try {
            if (!(boolean) API.hasGui.invoke(target)) return null;
            var attribute = API.attribute.invoke(null, target.getBlockState(), API.gui);
            if (attribute == null) return null;
            var nativeProvider = (MenuProvider) API.provider.invoke(attribute, target, true);
            if (nativeProvider == null) return null;
            return new MenuProvider() {
                public Component getDisplayName() { return nativeProvider.getDisplayName(); }
                public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                    return nativeProvider.createMenu(id, inventory, player);
                }
                public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
                    buffer.writeBlockPos(target.getBlockPos());
                    try {
                        API.encode.invoke(target, buffer);
                    } catch (ReflectiveOperationException ex) {
                        throw new IllegalStateException("Could not encode Mekanism menu opening data", ex);
                    }
                }
            };
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private MekanismRemoteMenus() {}
}
