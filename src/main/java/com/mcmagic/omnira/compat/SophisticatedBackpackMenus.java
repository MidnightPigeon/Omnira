package com.mcmagic.omnira.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;

/** Placed backpacks build a block context in their use interaction, including extra client data. */
public final class SophisticatedBackpackMenus {
    public static boolean isPlacedBackpack(BlockEntity entity) {
        return API != null && API.entity.isInstance(entity);
    }

    private record Api(Class<?> entity, Constructor<?> context, Constructor<?> menu, Method write, Method title, Method getContext) {}
    private static final Api API=load();

    private static Api load(){
        if(!ModList.get().isLoaded("sophisticatedbackpacks"))return null;
        String base="net.p3pp3rf1y.sophisticatedbackpacks.";
        try{
            var context=Class.forName(base+"common.gui.BackpackContext");
            return new Api(Class.forName(base+"backpack.BackpackBlockEntity"),
                    Class.forName(context.getName()+"$Block").getConstructor(BlockPos.class),
                    Class.forName(base+"common.gui.BackpackContainer").getConstructor(int.class,Player.class,context),
                    context.getMethod("toBuffer",FriendlyByteBuf.class,Player.class),context.getMethod("getDisplayName",Player.class),
                    Class.forName(base+"common.gui.BackpackContainer").getMethod("getBackpackContext"));
        }catch(ReflectiveOperationException ex){
            com.mojang.logging.LogUtils.getLogger().warn("Placed backpack menu API unavailable",ex);
            return null;
        }
    }

    public interface PendingLoad {
        void omnira$initializeStoredBackpack();
    }

    public static void prepare(BlockEntity target) {
        if (target instanceof PendingLoad backpack) backpack.omnira$initializeStoredBackpack();
    }

    public interface BoundContext {
        void omnira$bind(BlockEntity backpack);
        BlockEntity omnira$backpack();
    }
    public static BlockEntity boundBackpack(Object menu){
        if(API==null)return null;
        try{
            var context=API.getContext.invoke(menu);
            return context instanceof BoundContext bound?bound.omnira$backpack():null;
        }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
    }
    public static MenuProvider provider(BlockEntity target){
        if(API==null || !API.entity.isInstance(target))return null;
        return new MenuProvider(){
            private Object context;
            private Player viewer;
            public Component getDisplayName(){
                if(context!=null)try{return (Component)API.title.invoke(context,viewer);}
                catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
                return target.getBlockState().getBlock().getName();
            }
            public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){
                try{
                    prepare(target);
                    viewer=player;
                    context=API.context.newInstance(target.getBlockPos());
                    ((BoundContext)context).omnira$bind(target);
                    return (AbstractContainerMenu)API.menu.newInstance(id,player,context);
                }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
            }
            public void writeClientSideData(AbstractContainerMenu menu,RegistryFriendlyByteBuf buffer){
                try{
                    API.write.invoke(context,buffer,viewer);
                }
                catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
            }
        };
    }
    private SophisticatedBackpackMenus(){}
}
