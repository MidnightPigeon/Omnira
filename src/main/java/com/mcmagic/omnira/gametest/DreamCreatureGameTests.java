package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.item.bottle.*;
import com.mcmagic.omnira.registry.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("omnira_dream_creatures")
@PrefixGameTestTemplate(false)
public final class DreamCreatureGameTests {
    @GameTest(template="spell_arena")
    public static void lightSpawnSurface(GameTestHelper h) {
        var floor=h.absolutePos(new BlockPos(5,3,5));
        for(int y=1;y<=10;y++) h.getLevel().setBlockAndUpdate(floor.above(y),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        for(var block:new net.minecraft.world.level.block.Block[]{DreamContent.LIGHT_CONDENSATE.get(),
                DreamContent.LIGHT_SOURCE_CRYSTAL.get(),DreamContent.LIGHT_CRYSTAL_CORE.get()}) {
            h.getLevel().setBlockAndUpdate(floor,block.defaultBlockState());
            for(int y=1;y<=8;y++) h.assertTrue(com.mcmagic.omnira.entity.LightSpirit.hasSpawnSurface(h.getLevel(),floor.above(y)),"Valid flying spawn rejected");
            h.assertTrue(!com.mcmagic.omnira.entity.LightSpirit.hasSpawnSurface(h.getLevel(),floor.above(9)),"Spawn too far above island");
            h.assertTrue(!com.mcmagic.omnira.entity.LightSpirit.hasSpawnSurface(h.getLevel(),floor),"Spawn inside crystal accepted");
        }
        h.getLevel().setBlockAndUpdate(floor,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        h.assertTrue(!com.mcmagic.omnira.entity.LightSpirit.hasSpawnSurface(h.getLevel(),floor.above()),"Non-crystal surface accepted");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void sustainedFlight(GameTestHelper h) {
        var light=ModEntityTypes.LIGHT_SPIRIT.get().create(h.getLevel());
        var ghost=ModEntityTypes.SHADOW_GHOST.get().create(h.getLevel());
        var origin=h.absolutePos(new BlockPos(5,6,5));
        for(var mob:new net.minecraft.world.entity.Mob[]{light,ghost}) {
            mob.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+.5);
            double startZ=mob.getZ(),startY=mob.getY();
            mob.getMoveControl().setWantedPosition(mob.getX(),startY+3,startZ+7,1);
            for(int i=0;i<20;i++) {mob.tickCount++;mob.tick();}
            h.assertTrue(mob.getZ()>startZ+1 && mob.getY()>startY+.2,"One flight command did not sustain movement: "+mob.getType()
                    +" dz="+(mob.getZ()-startZ)+" dy="+(mob.getY()-startY)+" target="+mob.getTarget()+" speed="+mob.getSpeed()+" wanted="+mob.getMoveControl().hasWanted());
            mob.discard();
        }
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void bossesCannotEnterBottles(GameTestHelper h) {
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"bottle-boss-test"));
        for(var mode:new GameType[]{GameType.SURVIVAL,GameType.CREATIVE}) {
            player.setGameMode(mode);
            for(var type:new EntityType<?>[]{EntityType.ENDER_DRAGON,EntityType.WITHER}) {
                var boss=type.create(h.getLevel());
                var bottle=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());
                h.assertTrue(BottleCaptureRules.forbidden(boss),"Boss was not classified");
                h.assertTrue(!PocketBottleItem.capture(player,boss,bottle),"Boss capture succeeded");
                h.assertTrue(!boss.isRemoved() && !PocketBottleItem.filled(bottle),"Rejected capture mutated boss or bottle");
                if(boss instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon)
                    h.assertTrue(BottleCaptureRules.forbidden(dragon.getSubEntities()[0]),"Dragon part bypassed protection");
                boss.discard();
            }
        }
        var pig=EntityType.PIG.create(h.getLevel());
        var bottle=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());
        h.assertTrue(PocketBottleItem.capture(player,pig,bottle) && pig.isRemoved(),"Ordinary capture broken");
        BottleStorage.get(h.getLevel()).remove(bottle.get(ModDataComponents.BOTTLE_CAPTURE));
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void creatureAttributesAndCharge(GameTestHelper h) {
        var light=ModEntityTypes.LIGHT_SPIRIT.get().create(h.getLevel());
        h.assertTrue(light.getMaxHealth()==8,"Light spirit health not reduced");
        h.assertTrue(light.noPhysics && !light.isInWall(),"Light spirit cannot phase");
        light.discard();
        var ghost=ModEntityTypes.SHADOW_GHOST.get().create(h.getLevel());
        h.assertTrue(ghost.getMaxHealth()==20 && ghost.getAttributeValue(Attributes.ARMOR)==0
                && ghost.getAttributeValue(Attributes.ATTACK_DAMAGE)==3 && ghost.getAttributeValue(Attributes.FOLLOW_RANGE)==16,"Ghost attributes wrong");
        // NeoForge fake players are invulnerable; record attacks explicitly for this movement test.
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"ghost-target")) {
            @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {return false;}
            @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount) {
                setHealth(getHealth()-amount);return true;
            }
        };
        player.setGameMode(GameType.SURVIVAL);
        var pos=h.absolutePos(new BlockPos(5,4,5));
        ghost.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+3.5);
        ghost.setYRot(0);ghost.setYHeadRot(0);ghost.setXRot(0);
        h.assertTrue(ghost.withinView(player),"Forward target not visible");
        ghost.setYRot(180);ghost.setYHeadRot(180);
        h.assertTrue(!ghost.withinView(player),"Rear target incorrectly visible");
        ghost.setYRot(0);ghost.setYHeadRot(0);
        for(int x=-1;x<=1;x++) for(int y=0;y<3;y++)
            h.getLevel().setBlockAndUpdate(pos.offset(x,y,2),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        h.assertTrue(ghost.withinView(player),"Wall blocked directional detection");
        h.assertTrue(ghost.noPhysics && !ghost.isInWall(),"Ghost cannot phase");
        ghost.setTarget(player);
        for(int tick=0;tick<35;tick++) {
            player.invulnerableTime=0;
            ghost.tickCount++;ghost.tick();
        }
        h.assertTrue(player.getHealth()<20 && player.getHealth()>=17,"Charge missed or hit repeatedly: " + player.getHealth());
        ghost.discard();h.succeed();
    }
}
