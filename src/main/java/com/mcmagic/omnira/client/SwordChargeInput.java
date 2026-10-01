package com.mcmagic.omnira.client;

import com.mcmagic.omnira.item.RitualSwordItem;
import com.mcmagic.omnira.network.SwordChargePayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class SwordChargeInput {
    private static boolean charging;
    private static boolean mining;
    private static boolean blockAttack,blockUse;
    private static int ticks;
    private static net.minecraft.world.item.ItemStack weapon;
    public static void interrupted(int actions) {
        var mc=Minecraft.getInstance();
        if((actions&1)!=0) {charging=false;ticks=0;weapon=null;blockAttack=mc.options.keyAttack.isDown();}
        if((actions&2)!=0 && mc.player!=null) {
            com.mcmagic.omnira.item.SwordFocus.stop(mc.player);
            mc.player.stopUsingItem();
            blockUse=mc.options.keyUse.isDown();
        }
    }
    public static float progress(float partial) {return charging && eligible()?Math.min(1,(ticks+partial)/20F):0;}
    private static boolean eligible() {
        var mc=Minecraft.getInstance();return mc.player!=null && mc.screen==null && mc.isWindowActive() && mc.player.isAlive() && !mc.player.isSpectator()
                && !mc.player.isUsingItem() && !com.mcmagic.omnira.item.LifeEnderItem.held(mc.player) && !com.mcmagic.omnira.item.SwordActions.weaponUnavailable(mc.player)
                && mc.player.getMainHandItem().getItem() instanceof RitualSwordItem;
    }
    @SubscribeEvent public static void attack(InputEvent.InteractionKeyMappingTriggered event) {
        var player=Minecraft.getInstance().player;
        if(event.isAttack() && player!=null && com.mcmagic.omnira.item.SwordActions.weaponUnavailable(player)) {
            event.setCanceled(true);event.setSwingHand(false);return;
        }
        if(event.isAttack() && player!=null && player.getMainHandItem().getItem() instanceof RitualSwordItem
                && (mining || aimingAtBlock())) {
            cancelCharge();mining=true;return;
        }
        if(event.isAttack() && blockAttack || event.isUseItem() && blockUse) {
            event.setCanceled(true);event.setSwingHand(false);return;
        }
        if(event.isAttack() && eligible()) {
            if(!charging){charging=true;ticks=0;weapon=Minecraft.getInstance().player.getMainHandItem();PacketDistributor.sendToServer(new SwordChargePayload(0));}
            else {event.setCanceled(true);event.setSwingHand(false);}
        }
    }
    private static boolean aimingAtBlock() {
        var hit=Minecraft.getInstance().hitResult;
        return hit!=null && hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK;
    }
    private static void cancelCharge() {
        if(charging && Minecraft.getInstance().getConnection()!=null)PacketDistributor.sendToServer(new SwordChargePayload(2));
        charging=false;ticks=0;weapon=null;
    }
    private static void release() {
        if(!charging)return;
        var mc=Minecraft.getInstance();
        if(mc.getConnection()!=null)PacketDistributor.sendToServer(new SwordChargePayload(eligible() && !aimingAtBlock() && mc.player.getMainHandItem()==weapon?1:2));
        charging=false;
    }
    @SubscribeEvent public static void mouseRelease(InputEvent.MouseButton.Post event) {
        if(event.getAction()!=org.lwjgl.glfw.GLFW.GLFW_RELEASE)return;
        var options=Minecraft.getInstance().options;
        if(options.keyAttack.matchesMouse(event.getButton())) {release();blockAttack=false;mining=false;}
        if(options.keyUse.matchesMouse(event.getButton()))blockUse=false;
    }
    @SubscribeEvent public static void keyRelease(InputEvent.Key event) {
        if(event.getAction()!=org.lwjgl.glfw.GLFW.GLFW_RELEASE)return;
        var options=Minecraft.getInstance().options;
        if(options.keyAttack.matches(event.getKey(),event.getScanCode())) {release();blockAttack=false;mining=false;}
        if(options.keyUse.matches(event.getKey(),event.getScanCode()))blockUse=false;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(mc.player==null) {charging=false;weapon=null;blockAttack=false;blockUse=false;mining=false;return;}
        if(!mc.options.keyAttack.isDown()) {blockAttack=false;mining=false;}
        if(!mc.options.keyUse.isDown())blockUse=false;
        if(!charging)return;
        if(aimingAtBlock()) {cancelCharge();mining=true;return;}
        if(!eligible() || mc.player.getMainHandItem()!=weapon) {
            if(mc.getConnection()!=null)PacketDistributor.sendToServer(new SwordChargePayload(2));
            charging=false;return;
        }
        if(!mc.options.keyAttack.isDown()) {
            release();return;
        }
        if(++ticks==20)mc.player.playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,.5F,1.4F);
        if(weapon.getItem() instanceof RitualSwordItem sword && sword.kind==RitualSwordItem.Kind.NAIL) {
            var center=mc.player.getEyePosition().add(mc.player.getLookAngle().scale(.8)).add(0,-.35,0);
            for(int i=0;i<3;i++) {
                double angle=ticks*.45+i*Math.PI*2/3,radius=.3*(1-progress(0))+.1;
                double dx=Math.cos(angle)*radius,dz=Math.sin(angle)*radius;
                mc.particleEngine.createParticle(com.mcmagic.omnira.registry.ModParticles.NAIL_FOCUS.get(),center.x+dx,center.y,center.z+dz,-dx/10,.005,-dz/10);
            }
        }
    }
}
