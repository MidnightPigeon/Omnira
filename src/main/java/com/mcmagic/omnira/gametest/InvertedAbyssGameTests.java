package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.space.abyss.*;
import com.mcmagic.omnira.shop.KirisameShopPlan;
import com.mcmagic.omnira.spacetime.*;
import com.google.gson.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_abyss")
@PrefixGameTestTemplate(false)
public final class InvertedAbyssGameTests {
    @GameTest(template="spell_arena") public static void sprintParticlesFollowPhysicalFeet(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(10,5,10));int middle=paint(h,p);
        var normal=h.spawn(EntityType.PIG,new BlockPos(10,5,10));normal.setNoAi(true);
        normal.setPos(p.getX()+.5,middle-12,p.getZ()+.5);
        h.assertTrue(Math.abs(AbyssGravity.footParticleY(normal,.1)-(normal.getY()+.1))<1e-6,"Ordinary sprint particle origin changed");
        var inverted=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        inverted.setPos(p.getX()+.5,middle+12,p.getZ()+.5);
        h.assertTrue(AbyssGravity.inverted(inverted),"Player is not inverted");
        h.assertTrue(Math.abs(AbyssGravity.footParticleY(inverted,.1)-(inverted.getBoundingBox().maxY-.1))<1e-6,"Inverted sprint particles remain near the head");
        inverted.setPose(net.minecraft.world.entity.Pose.CROUCHING);inverted.refreshDimensions();
        h.assertTrue(Math.abs(AbyssGravity.footParticleY(inverted,.1)-(inverted.getBoundingBox().maxY-.1))<1e-6,"Crouching height ignored");
        var spectator=h.makeMockPlayer(net.minecraft.world.level.GameType.SPECTATOR);
        spectator.setPos(p.getX()+.5,middle+12,p.getZ()+.5);
        h.assertTrue(Math.abs(AbyssGravity.footParticleY(spectator,.1)-(spectator.getY()+.1))<1e-6,"Spectator particle origin changed");
        normal.discard();h.succeed();
    }
    public static int paint(GameTestHelper h,BlockPos p){
        var level=h.getLevel();int middle=((p.getY()+40)>>2)<<2;
        var registry=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
        var abyss=registry.getHolderOrThrow(AbyssContent.BIOME);var plain=registry.getHolderOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS);
        for(int cx=(p.getX()-12)>>4;cx<=(p.getX()+12)>>4;cx++)for(int cz=(p.getZ()-12)>>4;cz<=(p.getZ()+12)>>4;cz++)
            level.getChunk(cx,cz).fillBiomesFromNoise((x,y,z,s)->y*4>=middle-32&&y*4<middle+32?abyss:plain,level.getChunkSource().randomState().sampler());
        return middle;
    }
    @GameTest(template="spell_arena") public static void signedGravityCeilingSupportAndGaze(GameTestHelper h){
        h.assertTrue(AbyssGravity.band(62.499,65)==1&&AbyssGravity.band(62.5,65)==0&&AbyssGravity.band(67.499,65)==0&&AbyssGravity.band(67.5,65)==-1,"Five-block band boundaries");
        var p=h.absolutePos(new BlockPos(10,5,10));int mid=paint(h,p);
        var pig=h.spawn(EntityType.PIG,new BlockPos(10,5,10));pig.setNoAi(true);
        pig.setPos(p.getX()+.5,mid-12,p.getZ()+.5);h.assertTrue(AbyssGravity.direction(pig)==1,"Lower gravity inverted");
        pig.setPos(p.getX()+.5,mid+12,p.getZ()+.5);h.assertTrue(pig.getGravity()<0,"Upper gravity not inverted");
        pig.setNoGravity(true);h.assertTrue(pig.getGravity()==0,"No-gravity flag overwritten");pig.setNoGravity(false);
        int top=mid+15;var ceiling=new BlockPos(p.getX(),top,p.getZ());h.getLevel().setBlockAndUpdate(ceiling,Blocks.STONE.defaultBlockState());
        pig.setPos(p.getX()+.5,top-pig.getBbHeight()-.01,p.getZ()+.5);pig.setDeltaMovement(0,.2,0);
        pig.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,.2,0));
        h.assertTrue(pig.onGround()&&pig.getOnPos().equals(ceiling),"Ceiling collision: ground="+pig.onGround()+", vertical="+pig.verticalCollision+", motion="+pig.getDeltaMovement()+", box="+pig.getBoundingBox()+", support="+pig.getOnPos()+", ceiling="+ceiling+", gravity="+AbyssGravity.direction(pig));
        h.assertTrue(Math.abs(pig.getEyeY()-(pig.getY()+pig.getBbHeight()-pig.getEyeHeight()))<1e-5,"Upside-down eyes do not match model");
        h.getLevel().setBlockAndUpdate(ceiling.east().below(),Blocks.STONE_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE,net.minecraft.world.level.block.state.properties.SlabType.TOP));
        pig.setDeltaMovement(1,.08,0);pig.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(1,.08,0));
        h.assertTrue(pig.getX()>p.getX()+1.4&&Math.abs(pig.getBoundingBox().maxY-(top-.5))<.001,"Inverted slab stepping failed: "+pig.getBoundingBox());
        var jumper=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);jumper.setPos(p.getX()+.5,top-jumper.getBbHeight(),p.getZ()+.5);jumper.setOnGround(true);jumper.jumpFromGround();
        h.assertTrue(jumper.getDeltaMovement().y<0,"Ceiling jump still points toward ceiling");
        jumper.setYRot(0);jumper.setXRot(0);jumper.turn(10,20);
        h.assertTrue(Math.abs(jumper.getYRot()+1.5)<.001&&Math.abs(jumper.getXRot()+3)<.001,"Inverted mouse input disagrees with camera");
        pig.setYRot(0);pig.setXRot(-45);var forward=AbyssGravity.input(pig,new Vec3(0,0,1),.1F);
        h.assertTrue(forward.y>0&&forward.z>0&&Math.abs(forward.length()-.1)<1e-6,"Weightless forward ignores pitch");
        pig.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void massHasStrongAndWeakAttractionWithinSphere(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(10,5,10));int mid=paint(h,p);
        BlockPos mass=null;
        for(int y=mid-6;y<=mid+6;y++){var at=new BlockPos(p.getX(),y,p.getZ());if(AbyssGravity.direction(level,Vec3.atCenterOf(at))==0){mass=at;break;}}
        h.assertTrue(mass!=null,"Actual biome volume has no weightless band");
        level.setBlockAndUpdate(mass,AbyssContent.MASS.get().defaultBlockState());var center=Vec3.atCenterOf(mass);
        h.assertTrue(VoidAttraction.acceleration(level,center.add(4,0,0)).x<0,"Mass does not attract entities");
        h.assertTrue(VoidAttraction.acceleration(level,center.add(5.01,0,0)).lengthSqr()==0,"Mass attraction exceeds sphere");
        level.setBlockAndUpdate(mass,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(mass.above(10),AbyssContent.MASS.get().defaultBlockState());
        h.assertTrue(Math.abs(VoidAttraction.acceleration(level,center.add(0,10,2)).length()-.005)<1e-6,"Mass outside zero gravity must use weak attraction");
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);player.setPos(center.x,mid+12,center.z);
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("charm",0,new net.minecraft.world.item.ItemStack(com.mcmagic.omnira.registry.ModItems.SPACETIME_ANCHORING_SIGIL.get()));
        h.assertTrue(player.getGravity()<0,"Amulet immunizes terrain gravity");h.succeed();
    }
    @GameTest(template="spell_arena") public static void towerLootSymmetryAndTerrainIsolation(GameTestHelper h){
        for(int height:new int[]{44,56,68}){
            var cells=SkyreachPlan.build(height);int items=0,fluids=0,masses=0;
            var indexed=new java.util.HashMap<BlockPos,KirisameShopPlan.Cell>();for(var c:cells)indexed.put(c.pos(),c);
            for(var c:cells){
                KirisameShopPlan.state(c);
                h.assertTrue(!c.name().equals("omnira:void_mirror_rock"),"Tower masonry must not use void-infested rock");
                if(c.name().equals("omnira:crystal_ball")){items++;h.assertTrue(c.nbt().contains("LootTable"),"Orb missing loot");}
                if(c.name().equals("omnira:liquid_crystal_ball")){fluids++;h.assertTrue(c.nbt().contains("FluidTreasure:1b"),"Liquid orb not treasure");}
                if(c.name().equals("omnira:dense_void_mass"))masses++;
                if(c.name().equals("omnira:engraved_mirror_rock_slab")){
                    int direction=c.pos().getY()<height/2?1:-1;
                    var above=indexed.get(c.pos().offset(0,direction,0));
                    // Corner treads can overlap vertically; only the exposed walking surface needs headroom.
                    if(above!=null&&above.name().equals(c.name()))continue;
                    for(int step=1;step<=2;step++){
                        var space=indexed.get(c.pos().offset(0,direction*step,0));
                        h.assertTrue(space!=null&&space.name().equals("minecraft:air"),"Stairway lacks two-block headroom at "+c.pos()+" height="+height);
                    }
                }
            }
            h.assertTrue(items==4&&fluids==2&&masses==1,"Tower contents not symmetric");
        }
        boolean found=false;
        for(int x=-1024;x<=1024&&!found;x+=32)for(int z=-1024;z<=1024&&!found;z+=32)if(AbyssTerrain.biome(x,z)){
            found=true;h.assertTrue(CorridorLayout.block(x,23,z).is(com.mcmagic.omnira.registry.ModBlocks.PURE_SOLIDIFIED_SPACETIME.get()),"Lower seal changed");
            h.assertTrue(CorridorLayout.block(x,103,z).is(com.mcmagic.omnira.registry.ModBlocks.PURE_SOLIDIFIED_SPACETIME.get()),"Cave roof seal changed");
            h.assertTrue(CorridorLayout.block(x,68,z).isAir(),"Abyss passage obstructed");
        }
        h.assertTrue(found,"Abyss region absent");
        int authoredItems=0,authoredFluids=0;
        for(var c:com.mcmagic.omnira.space.abyss.SkyreachPiece.cells(56)) {
            KirisameShopPlan.state(c);
            h.assertTrue(!c.name().equals("omnira:void_mirror_rock"),"Authored tower retains void masonry");
            if(c.name().equals("omnira:crystal_ball")){authoredItems++;h.assertTrue(c.nbt().contains("LootTable")&&!c.nbt().contains("StorageIdentity"),"Authored treasure must remain unopened and unique");}
            if(c.name().equals("omnira:liquid_crystal_ball")){authoredFluids++;h.assertTrue(c.nbt().contains("FluidTreasure:1b"),"Authored fluid treasure missing");}
        }
        h.assertTrue(authoredItems==4&&authoredFluids==2,"Authored tower treasure count changed");
        for(int height:new int[]{44,56,68}){
            var authored=com.mcmagic.omnira.space.abyss.SkyreachPiece.cells(height);
            var indexed=new java.util.HashMap<BlockPos,KirisameShopPlan.Cell>();for(var c:authored)indexed.put(c.pos(),c);
            int upright=0,inverted=0;
            for(var c:authored){
                if(!c.name().equals("omnira:light_crystal_torch"))continue;
                var state=KirisameShopPlan.state(c);var direction=state.getValue(com.mcmagic.omnira.block.LightCrystalTorchBlock.MOUNT);
                if(state.getValue(com.mcmagic.omnira.block.LightCrystalTorchBlock.HALF)!=net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER)continue;
                if(direction==Direction.UP)upright++;else if(direction==Direction.DOWN)inverted++;else h.fail("Tower torch must face into its gallery");
                var tip=indexed.get(c.pos().relative(direction));var support=indexed.get(c.pos().relative(direction.getOpposite()));
                h.assertTrue(tip!=null&&tip.name().equals(c.name())&&KirisameShopPlan.state(tip).getValue(com.mcmagic.omnira.block.LightCrystalTorchBlock.HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER&&KirisameShopPlan.state(tip).getValue(com.mcmagic.omnira.block.LightCrystalTorchBlock.MOUNT)==direction,"Tower torch pair broken");
                h.assertTrue(support!=null&&KirisameShopPlan.state(support).isFaceSturdy(h.getLevel(),h.absolutePos(support.pos()),direction),"Tower torch lacks support");
            }
            h.assertTrue(upright==5&&inverted==5,"Tower must preserve five lower and five upper lamps");
        }
        try{
            var list=new JsonArray();for(var c:SkyreachPlan.build(56)){
                var j=new JsonObject();j.addProperty("x",c.pos().getX());j.addProperty("y",c.pos().getY());j.addProperty("z",c.pos().getZ());j.addProperty("name",c.name());
                if(!c.properties().isEmpty())j.add("properties",new Gson().toJsonTree(c.properties()));if(!c.nbt().isEmpty())j.addProperty("nbt",c.nbt());list.add(j);
            }
            var dir=java.nio.file.Path.of("../../build/abyss");java.nio.file.Files.createDirectories(dir);
            java.nio.file.Files.writeString(dir.resolve("skyreach_tower.json"),new GsonBuilder().setPrettyPrinting().create().toJson(list));
        }catch(java.io.IOException e){throw new IllegalStateException(e);}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void fissuresReachBothSealsWithoutCrossingBiomes(GameTestHelper h){
        int lower=0,upper=0,slopes=0,outside=0;
        for(int tx=-1024;tx<=1024;tx+=32)for(int tz=-1024;tz<=1024;tz+=32)
        for(int dx=0;dx<32;dx+=8)for(int dz=0;dz<32;dz+=8) {
            int x=tx+dx,z=tz+dz;
            int floor=com.mcmagic.omnira.space.FoldedTerrain.floor(x,z),roof=com.mcmagic.omnira.space.FoldedTerrain.roof(x,z);
            int cutFloor=AbyssTerrain.riftFloor(x,z),cutRoof=AbyssTerrain.riftRoof(x,z);
            if(!AbyssTerrain.biome(x,z)) {
                h.assertTrue(cutFloor==floor&&cutRoof==roof,"Fissure entered another biome");outside++;continue;
            }
            if(cutFloor==23){lower++;h.assertTrue(CorridorLayout.block(x,24,z).isAir(),"Lower fissure does not reach the seal");}
            if(cutRoof==103){upper++;h.assertTrue(CorridorLayout.block(x,102,z).isAir(),"Upper fissure does not reach the seal");}
            if(cutFloor>23&&cutFloor<floor || cutRoof>roof&&cutRoof<103)slopes++;
            h.assertTrue(CorridorLayout.block(x,23,z).is(com.mcmagic.omnira.registry.ModBlocks.PURE_SOLIDIFIED_SPACETIME.get()),"Lower seal removed");
            h.assertTrue(CorridorLayout.block(x,103,z).is(com.mcmagic.omnira.registry.ModBlocks.PURE_SOLIDIFIED_SPACETIME.get()),"Upper seal removed");
        }
        h.assertTrue(lower>20&&upper>20&&slopes>20&&outside>0,"Fissure coverage or banks absent");
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=80) public static void sableForceChangesPerBody(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("sable")){h.succeed();return;}
        AbyssSableChecks.run(h);
    }
    @GameTest(template="spell_arena") public static void floatingMarkersHaveSixRoughBlocksAndOneExcitedCore(GameTestHelper h){
        boolean found=false;
        for(int x=-50;x<=50&&!found;x++)for(int z=-50;z<=50&&!found;z++){
            var p=AbyssTerrain.cluster(x,z);if(p==null)continue;found=true;
            h.assertTrue(AbyssTerrain.block(p.getX(),p.getY(),p.getZ()).is(com.mcmagic.omnira.registry.DreamContent.EXCITED_SPATIAL_CRYSTAL.get()),"Marker center not excited");
            for(var d:Direction.values()){
                var side=p.relative(d);
                h.assertTrue(AbyssTerrain.block(side.getX(),side.getY(),side.getZ()).is(com.mcmagic.omnira.registry.DreamContent.SPATIAL_CRYSTAL.get()),"Marker does not have six rough neighbors");
                h.assertTrue(AbyssGravity.band(side.getY()+.5,AbyssTerrain.middle(side.getX(),side.getZ()))==0,"Marker leaves zero-gravity layer");
            }
        }
        h.assertTrue(found,"No marker groups generated");h.succeed();
    }
    @GameTest(template="spell_arena") public static void smoothGravityAndBoundaryDebounce(GameTestHelper h){
        var state=new GravityTransition.State(1,0);
        state.update(-1,1);state.update(1,2);state.update(-1,3);state.update(1,4);
        h.assertTrue(state.gravity(4)==1&&state.roll(4)==0,"Boundary flicker started a flip");
        for(int t=5;t<=9;t++)state.update(-1,t);
        h.assertTrue(state.gravity(9)==1&&state.roll(9)==0,"Gravity snapped at transition start");
        h.assertTrue(Math.abs(state.gravity(21))<.001&&Math.abs(state.roll(21)-90)<.001,"Flip does not interpolate");
        h.assertTrue(state.gravity(33)==-1&&state.roll(33)==180,"Flip never completes");
        state.update(0,34);state.update(0,38);
        h.assertTrue(state.roll(62)==180&&state.gravity(62)==0,"Zero gravity changes attitude");
        state.update(1,63);state.update(1,67);
        h.assertTrue(state.roll(79)==90&&state.roll(91)==0,"Leaving abyss snaps orientation");h.succeed();
    }
    @GameTest(template="spell_arena") public static void orbMountsAndMapColours(GameTestHelper h){
        var audit=new JsonArray();int states=0;
        for(var entry:net.minecraft.core.registries.BuiltInRegistries.BLOCK.entrySet()){
            if(!entry.getKey().location().getNamespace().equals("omnira"))continue;
            var block=entry.getValue();var state=block.defaultBlockState();
            for(var variant:block.getStateDefinition().getPossibleStates()){
                states++;
                h.assertTrue(variant.getMapColor(h.getLevel(),h.absolutePos(BlockPos.ZERO))!=net.minecraft.world.level.material.MapColor.NONE,"Missing map colour: "+entry.getKey()+" "+variant);
                if(variant.getRenderShape()==net.minecraft.world.level.block.RenderShape.INVISIBLE)
                    h.assertTrue(block instanceof net.minecraft.world.level.block.LiquidBlock||block instanceof com.mcmagic.omnira.block.DreamPortalBlock||block instanceof com.mcmagic.omnira.spacetime.CorridorGatewayBlock||block instanceof com.mcmagic.omnira.time.WonderlandPokerStandBlock,"Unreviewed map-invisible block: "+entry.getKey());
            }
            var row=new JsonObject();row.addProperty("block",entry.getKey().location().toString());row.addProperty("states",block.getStateDefinition().getPossibleStates().size());row.addProperty("map_color",state.getMapColor(h.getLevel(),h.absolutePos(BlockPos.ZERO)).col);row.addProperty("render_shape",state.getRenderShape().name());audit.add(row);
            if(!(block instanceof com.mcmagic.omnira.block.CrystalBallBlock)&&!(block instanceof com.mcmagic.omnira.shop.MarisaOrbBlock))continue;
            for(var face:Direction.values()){
                var mounted=state.setValue(com.mcmagic.omnira.block.CrystalBallBlock.MOUNT,face);
                var shape=mounted.getShape(h.getLevel(),h.absolutePos(BlockPos.ZERO));
                h.assertTrue(!shape.isEmpty(),"Mounted orb has no collision shape");
                var base=com.mcmagic.omnira.block.OrbMount.point(face,new Vec3(.5,0,.5));
                h.assertTrue(base.distanceTo(new Vec3(.5,.5,.5).subtract(Vec3.atLowerCornerOf(face.getNormal()).scale(.5)))<1e-6,"Orb detached from clicked face");
                h.assertTrue(mounted.getRenderShape()!=net.minecraft.world.level.block.RenderShape.INVISIBLE,"Xaero ignores orb");
            }
        }
        try{
            var report=new JsonObject();report.addProperty("blocks",audit.size());report.addProperty("states",states);report.add("entries",audit);
            java.nio.file.Files.writeString(java.nio.file.Path.of("../../build/xaero-block-audit.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
        }catch(java.io.IOException e){throw new IllegalStateException(e);}
        h.assertTrue(Blocks.AIR.defaultBlockState().getMapColor(h.getLevel(),BlockPos.ZERO)==net.minecraft.world.level.material.MapColor.NONE,"Vanilla map colour changed");h.succeed();
    }
}
