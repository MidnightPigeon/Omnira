package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.event.LightDarkConversion;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_light_dark")
@PrefixGameTestTemplate(false)
public final class LightDarkConversionGameTests {
    @GameTest(template="spell_arena")
    public static void droppedDarkCoreUsesExactlyOneLightBlock(GameTestHelper h) {
        var local=new BlockPos(3,2,3);h.setBlock(local,DreamContent.LIGHT_CRYSTAL_CORE.get());
        var pos=h.absolutePos(local);var item=new ItemEntity(h.getLevel(),pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,new ItemStack(ModItems.DARK_MICROCORE.get(),3));
        h.getLevel().addFreshEntity(item);
        h.assertTrue(LightDarkConversion.convertDropped(item),"Resting item contact not detected");
        h.assertTrue(h.getLevel().getBlockState(pos).is(DreamContent.LIGHT_CONDENSATE.get()),"Core did not become condensate");
        h.assertTrue(item.getItem().is(ModItems.DARK_MICROCORE.get()) && item.getItem().getCount()==2,"Whole stack converted");
        h.assertTrue(!LightDarkConversion.convertDropped(item),"Same block converted twice");
        var light=h.getLevel().getEntitiesOfClass(ItemEntity.class,item.getBoundingBox().inflate(1),e->e.getItem().is(ModItems.LIGHT_MICROCORE.get()));
        h.assertTrue(light.size()==1 && light.getFirst().getItem().getCount()==1,"Wrong output count");
        light.forEach(ItemEntity::discard);item.discard();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void mistConversionPersistsAndFades(GameTestHelper h) {
        var pos=h.absoluteVec(new Vec3(5,3,5));var mist=ModEntityTypes.SHADOW_MIST.get().create(h.getLevel());mist.setPos(pos);h.getLevel().addFreshEntity(mist);
        var item=new ItemEntity(h.getLevel(),pos.x,pos.y+.2,pos.z,new ItemStack(ModItems.LIGHT_MICROCORE.get(),4));h.getLevel().addFreshEntity(item);
        float opacity=mist.opacity(0),diameter=mist.diameter(0);
        h.assertTrue(LightDarkConversion.convertDropped(item) && mist.collapsing(),"Mist conversion failed");
        h.assertTrue(item.getItem().getCount()==3 && !LightDarkConversion.convertDropped(item),"Mist converted whole stack or repeated");
        h.assertTrue(mist.opacity(0)==opacity && mist.diameter(0)==diameter,"Conversion abruptly changed mist visuals");
        var save=new net.minecraft.nbt.CompoundTag();mist.saveWithoutId(save);
        var restored=ModEntityTypes.SHADOW_MIST.get().create(h.getLevel());restored.load(save);
        h.assertTrue(restored.collapsing() && !restored.consumeForConversion(),"Reload resets consumed state");
        for(int i=0;i<10;i++) restored.tick();
        h.assertTrue(restored.opacity(0)>0 && restored.opacity(0)<opacity && !restored.isRemoved(),"Mist not fading gradually");
        for(int i=0;i<10;i++) restored.tick();
        h.assertTrue(restored.isRemoved(),"Fast fade exceeds one second");
        h.getLevel().getEntitiesOfClass(ItemEntity.class,item.getBoundingBox().inflate(1)).forEach(ItemEntity::discard);mist.discard();h.succeed();
    }
    private static net.neoforged.neoforge.common.util.FakePlayer player(GameTestHelper h) {
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"conversion-test"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);return player;
    }
    @GameTest(template="spell_arena")
    public static void rightClickBothDirectionsAndWalls(GameTestHelper h) {
        var player=player(h);var local=new BlockPos(3,2,3);h.setBlock(local,DreamContent.LIGHT_CRYSTAL_CORE.get());var pos=h.absolutePos(local);
        player.setPos(pos.getCenter().add(0,1,-2));player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.DARK_MICROCORE.get()));
        var event=new PlayerInteractEvent.RightClickBlock(player,InteractionHand.MAIN_HAND,pos,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false));
        LightDarkConversion.useBlock(event);
        h.assertTrue(event.isCanceled() && player.getMainHandItem().is(ModItems.LIGHT_MICROCORE.get()),"Right click block failed");
        var mist=ModEntityTypes.SHADOW_MIST.get().create(h.getLevel());mist.setPos(h.absoluteVec(new Vec3(5,4,5)));h.getLevel().addFreshEntity(mist);
        player.setPos(h.absoluteVec(new Vec3(5,3,2)));player.setYRot(0);player.setXRot(0);
        h.setBlock(new BlockPos(5,4,3),net.minecraft.world.level.block.Blocks.STONE);
        LightDarkConversion.useItem(new PlayerInteractEvent.RightClickItem(player,InteractionHand.MAIN_HAND));
        h.assertTrue(!mist.collapsing() && player.getMainHandItem().is(ModItems.LIGHT_MICROCORE.get()),"Converted mist through a wall");
        h.setBlock(new BlockPos(5,4,3),net.minecraft.world.level.block.Blocks.AIR);
        for(int i=0;i<20;i++) mist.tick();
        LightDarkConversion.useItem(new PlayerInteractEvent.RightClickItem(player,InteractionHand.MAIN_HAND));
        h.assertTrue(mist.collapsing() && player.getMainHandItem().is(ModItems.DARK_MICROCORE.get()),"Right click mist failed");
        mist.discard();h.succeed();
    }
}
