package com.mcmagic.omnira.item;

import net.minecraft.world.item.Item;

public final class OrbUpgradeItem extends Item {
    public enum Kind {
        STACKING(2,0xC9A6F5), INFUSED_STACKING(4,0xC9A6F5),
        STABILIZATION(1,0xA9E8B9), LAVA(1,0xFFA34F), INTAKE(1,0x9FD8F1), OUTPUT(1,0x9FD8F1), SPEED(1,0xBF91ED);
        public final int capacityMultiplier;
        public final int displayColor;
        Kind(int capacityMultiplier,int displayColor) {
            this.capacityMultiplier=capacityMultiplier;this.displayColor=displayColor;
        }
        public boolean sameFamily(Kind other) {
            return this==other || other!=null && capacityMultiplier>1 && other.capacityMultiplier>1;
        }
    }
    private static final int[] INFUSED_COLORS={0xD7A85C,0x6FB3F4,0xFA7568,0xA4EBB4,0xFEF0A2,0xB181E0};
    private static final int[] STABILIZATION_COLORS={Kind.STABILIZATION.displayColor,0xFFFFFF,0x302B39};
    public final Kind kind;
    public OrbUpgradeItem(Properties properties,Kind kind) {super(properties.stacksTo(1));this.kind=kind;}
    @Override public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(
            net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(kind!=Kind.INTAKE && kind!=Kind.OUTPUT)return net.minecraft.world.InteractionResultHolder.pass(stack);
        if(!level.isClientSide) {
            int source=hand==net.minecraft.world.InteractionHand.OFF_HAND?40:player.getInventory().selected;
            player.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inventory,p)->
                    new com.mcmagic.omnira.menu.OrbUpgradeMenu(id,inventory,source,kind==Kind.INTAKE),stack.getHoverName()),
                    data->{data.writeInt(source);data.writeBoolean(kind==Kind.INTAKE);});
        }
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flag) {
        lines.add(net.minecraft.network.chat.Component.translatable("tooltip.omnira.orb."+kind.name().toLowerCase(java.util.Locale.ROOT)).withStyle(net.minecraft.ChatFormatting.GRAY));
        if(kind==Kind.INTAKE || kind==Kind.OUTPUT)lines.add(net.minecraft.network.chat.Component.translatable("gui.omnira.orb.configure_held").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
    @Override public void inventoryTick(net.minecraft.world.item.ItemStack stack,net.minecraft.world.level.Level level,net.minecraft.world.entity.Entity entity,int slot,boolean selected) {
        if(!level.isClientSide || !selected || (kind!=Kind.INFUSED_STACKING && kind!=Kind.STABILIZATION) || level.random.nextInt(8)!=0)return;
        int[] colors=kind==Kind.INFUSED_STACKING?INFUSED_COLORS:STABILIZATION_COLORS;
        int color=colors[level.random.nextInt(colors.length)];var position=entity.getEyePosition().add(entity.getLookAngle().scale(.45));
        level.addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F),.45F),
                position.x+(level.random.nextDouble()-.5)*.2,position.y-.35,position.z+(level.random.nextDouble()-.5)*.2,0,.008,0);
    }
}
