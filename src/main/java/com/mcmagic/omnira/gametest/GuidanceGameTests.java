package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.guidance.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.alchemy.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_guidance")
@PrefixGameTestTemplate(false)
public final class GuidanceGameTests {
    private static GuidanceOrbEntity orb(GameTestHelper h,int stage,Direction facing){
        var p=h.absolutePos(new BlockPos(4,3,4));h.getLevel().removeBlock(p,false);h.getLevel().setBlockAndUpdate(p,GuidanceContent.BLOCK.get().defaultBlockState()
                .setValue(GuidanceOrbBlock.STAGE,stage).setValue(GuidanceOrbBlock.FACING,facing));
        return (GuidanceOrbEntity)h.getLevel().getBlockEntity(p);
    }
    private static ItemStack item(String name){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("omnira:"+name)));}
    @GameTest(template="spell_arena") public static void bookBookmarksPersistAndStayIndependent(GameTestHelper h){
        var a=item("guide_book");var b=item("guide_book");
        var aId=GuideBookmark.identity(a);var bId=GuideBookmark.identity(b);
        var saved=new GuideBookmark("intake_upgrade",7);
        h.assertTrue(saved.save(a,aId)&&GuideBookmark.read(a).page()==6,"Book spread not saved");
        h.assertTrue(!saved.save(b,aId)&&GuideBookmark.read(b).destination().isEmpty(),"A different book was overwritten");
        h.assertTrue(!aId.equals(bId),"Books share identity");
        var reloaded=ItemStack.parseOptional(h.getLevel().registryAccess(),(net.minecraft.nbt.CompoundTag)a.save(h.getLevel().registryAccess()));
        h.assertTrue(GuideBookmark.identity(reloaded).equals(aId)&&GuideBookmark.read(reloaded).equals(saved),"Bookmark lost on item serialization");
        var orb=orb(h,1,Direction.NORTH);orb.interact(ItemStack.EMPTY,Direction.NORTH);orb.interact(ItemStack.EMPTY,Direction.UP);
        h.assertTrue(GuideBookmark.read(a).equals(saved)&&GuideBookmark.read(b).destination().isEmpty(),"Orb navigation changed book progress");
        h.assertTrue(!saved.save(new ItemStack(Items.STICK),aId),"Non-book accepted a bookmark");
        h.assertTrue(new GuideBookmark("",-1).page()==0&&new GuideBookmark("",999999).page()==8192,"Bookmark page bounds wrong");h.succeed();
    }
    @GameTest(template="spell_arena") public static void bookmarkUpdatesPreserveOtherBookData(GameTestHelper h){
        var book=item("guide_book");var tag=new net.minecraft.nbt.CompoundTag();tag.putString("CustomNote","keep");
        book.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
        var id=GuideBookmark.identity(book);new GuideBookmark("dream_industry",4).save(book,id);
        h.assertTrue(book.get(DataComponents.CUSTOM_DATA).copyTag().getString("CustomNote").equals("keep"),"Bookmark erased unrelated data");
        new GuideBookmark("orb_upgrade_plugins",0).save(book,id);
        h.assertTrue(GuideBookmark.read(book).destination().equals("orb_upgrade_plugins"),"Second visit did not update bookmark");h.succeed();
    }
    @GameTest(template="spell_arena") public static void sequentialActivationWithoutConsumption(GameTestHelper h){
        var orb=orb(h,0,Direction.NORTH);
        orb.interact(item("genesis_particles"),Direction.DOWN);h.assertTrue(orb.stage()==0,"Skipped stages");
        h.assertTrue(orb.projections().length==2,"Dormant projections missing");
        for(var name:java.util.List.of("spiritual_crystal","dream_crystal_bedrock","time_warp_point","genesis_particles")){
            var stack=item(name);int before=orb.stage();orb.interact(stack,Direction.NORTH);
            h.assertTrue(orb.stage()==before+1&&stack.getCount()==1&&orb.view()==0,"Activation consumed an item or failed");
        }
        orb.interact(ItemStack.EMPTY,Direction.UP);h.assertTrue(orb.messageKey().endsWith("terminal")&&!orb.buttonsVisible(),"Final stage not under construction");
        h.assertTrue(GuidanceOrbEntity.advances(2,item("space_warp_point")),"Space alternative missing");h.succeed();
    }
    @GameTest(template="spell_arena") public static void directionalNavigationAndPersistentHint(GameTestHelper h){
        for(var face:Direction.Plane.HORIZONTAL){
            var orb=orb(h,1,face);orb.interact(ItemStack.EMPTY,face);
            h.assertTrue(orb.target(face).equals("initial_spells")&&orb.target(face.getClockWise()).equals("initial_alchemy")
                    &&orb.target(face.getOpposite()).equals("initial_industry")&&orb.target(face.getCounterClockWise()).equals("simple_storage"),"Rotated routes wrong");
            h.assertTrue(orb.interact(ItemStack.EMPTY,Direction.UP).equals("visit_dream")&&!orb.buttonsVisible(),"Dream guidance did not open entry and hide buttons");
            var copy=new GuidanceOrbEntity(orb.getBlockPos(),orb.getBlockState());copy.setLevel(h.getLevel());copy.loadWithComponents(orb.getUpdateTag(h.getLevel().registryAccess()),h.getLevel().registryAccess());
            h.assertTrue(copy.projections().length==1&&copy.projections()[0].equals("dream_crystal_bedrock"),"Hint lost on reload");
            copy.interact(new ItemStack(Items.STICK),face);h.assertTrue(copy.view()==0,"Other item did not dismiss hint");
        }h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=190) public static void transitionHintPersistsUntilDismissed(GameTestHelper h){
        var orb=orb(h,2,Direction.NORTH);orb.interact(ItemStack.EMPTY,Direction.NORTH);
        h.assertTrue(orb.target(Direction.WEST).equals("pocket_magic_bottle")&&orb.target(Direction.EAST).equals("spacetime_entry"),"Transition routes wrong");
        orb.interact(ItemStack.EMPTY,Direction.UP);h.assertTrue(!orb.buttonsVisible()&&orb.messageKey().endsWith("spacetime_hint"),"Persistent hint absent");
        h.assertTrue(java.util.Arrays.equals(orb.projections(),new String[]{"time_warp_point","space_warp_point"}),"Both transition projections required");
        h.runAfterDelay(162,()->{
            h.assertTrue(orb.view()==GuidanceOrbEntity.HINT,"Hint expired without interaction");
            var copy=new GuidanceOrbEntity(orb.getBlockPos(),orb.getBlockState());copy.setLevel(h.getLevel());
            copy.loadWithComponents(orb.getUpdateTag(h.getLevel().registryAccess()),h.getLevel().registryAccess());
            h.assertTrue(copy.view()==GuidanceOrbEntity.HINT&&copy.projections().length==2,"Reload lost transition hint");
            copy.interact(new ItemStack(Items.STICK),Direction.SOUTH);h.assertTrue(copy.view()==GuidanceOrbEntity.IDLE,"Held item did not dismiss hint");
            orb.interact(ItemStack.EMPTY,Direction.UP);h.assertTrue(orb.view()==GuidanceOrbEntity.IDLE,"Empty hand did not dismiss hint");
            orb.interact(ItemStack.EMPTY,Direction.NORTH);h.assertTrue(orb.buttonsVisible(),"Navigation did not reopen");
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atLowerCornerOf(orb.getBlockPos()).add(.5,1.2,.5),Direction.NORTH,orb.getBlockPos(),false);
            h.assertTrue(GuidanceOrbBlock.interactionFace(orb.getBlockPos(),hit)==Direction.UP,"Raised platform side must activate top entry");
            h.succeed();
        });
    }
    @GameTest(template="spell_arena") public static void advancedHintAndMinedStage(GameTestHelper h){
        var orb=orb(h,3,Direction.NORTH);orb.interact(ItemStack.EMPTY,Direction.NORTH);
        h.assertTrue(orb.target(Direction.NORTH).equals("spacetime_terrain")&&orb.target(Direction.EAST).equals("advanced_industry")
                &&orb.target(Direction.SOUTH).equals("large_alchemy")&&orb.target(Direction.WEST).equals("advanced_spells"),"Advanced routes wrong");
        orb.interact(ItemStack.EMPTY,Direction.UP);h.assertTrue(orb.projections()[0].equals("genesis_particles"),"Genesis projection absent");
        var drops=Block.getDrops(orb.getBlockState(),h.getLevel(),orb.getBlockPos(),orb);
        h.assertTrue(drops.size()==1&&drops.getFirst().is(GuidanceContent.ITEM.get()),"Orb drop missing");
        var state=drops.getFirst().get(DataComponents.BLOCK_STATE);
        h.assertTrue(state!=null&&state.apply(GuidanceContent.BLOCK.get().defaultBlockState()).getValue(GuidanceOrbBlock.STAGE)==3,"Mined stage lost");h.succeed();
    }
    private static ReactionRecipe recipe(GameTestHelper h,String id){return (ReactionRecipe)h.getLevel().getRecipeManager()
            .byKey(ResourceLocation.parse("omnira:reaction/"+id)).orElseThrow().value();}
    private static int count(net.neoforged.neoforge.items.IItemHandler items,String id){int n=0;for(int i=0;i<items.getSlots();i++)if(items.getStackInSlot(i).is(item(id).getItem()))n+=items.getStackInSlot(i).getCount();return n;}
    @GameTest(template="spell_arena") public static void bothMicrocoreReactionsFinishInCauldron(GameTestHelper h){
        for(String id:java.util.List.of("water_to_fire_microcore","water_fire_to_earth_air")){
            var p=h.absolutePos(new BlockPos(7,4,7));h.getLevel().removeBlock(p,false);h.getLevel().setBlockAndUpdate(p,ModBlocks.PRIMITIVE_REACTION_CAULDRON.get().defaultBlockState());
            var v=(RoughAlchemyCauldronBlockEntity)h.getLevel().getBlockEntity(p);var r=recipe(h,id);
            for(int i=0;i<r.items().size();i++)v.items.setStackInSlot(i,r.items().get(i).ingredient().getItems()[0].copy());
            if(!r.fluid().isEmpty())v.tank.fill(r.fluid(),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            h.assertTrue(r.support().isEmpty()&&r.matches(new ReactionRecipe.Input(v),h.getLevel()),"Uncatalysed recipe not accepted");
            for(int i=0;i<2001;i++)RoughAlchemyCauldronBlockEntity.tick(h.getLevel(),p,v.getBlockState(),v);
            if(id.equals("water_to_fire_microcore"))h.assertTrue(count(v.items,"fire_microcore")==1&&v.tank.isEmpty()&&r.fluid().getAmount()==500,"Lava conversion accounting wrong");
            else h.assertTrue(count(v.items,"earth_microcore")==1&&count(v.items,"air_microcore")==1&&count(v.items,"paradox_dust")==0,"Paired outputs not guaranteed");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void poolConvertsBothOutputsTogether(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(5,3,5));h.assertTrue(ReactionPoolBlock.place(h.getLevel(),p,Direction.NORTH),"Pool placement failed");
        var pool=ReactionPoolBlock.controller(h.getLevel(),p);var r=recipe(h,"water_fire_to_earth_air");
        for(int i=0;i<r.items().size();i++)pool.items.setStackInSlot(i,r.items().get(i).ingredient().getItems()[0].copy());
        h.assertTrue(pool.start(r),"Pool conversion rejected");pool.advance(2000);
        h.assertTrue(count(pool.items,"earth_microcore")==1&&count(pool.items,"air_microcore")==1&&!pool.reacting(),"Pool lost one guaranteed result");h.succeed();
    }
    @GameTest(template="spell_arena") public static void rarityAndReachableGuideDestinations(GameTestHelper h)throws Exception{
        h.assertTrue(item("deflection_field").getRarity()==Rarity.UNCOMMON,"Shield rarity wrong");
        var rarities=new Rarity[]{Rarity.COMMON,Rarity.UNCOMMON,Rarity.RARE,Rarity.EPIC};
        for(var list:java.util.List.of(ModItems.NAIL_UPGRADES,ModItems.NEEDLE_UPGRADES))for(int i=0;i<4;i++)h.assertTrue(new ItemStack(list.get(i).get()).getRarity()==rarities[i],"Weapon rarity wrong");
        for(String language:java.util.List.of("zh_cn","en_us")){
            var book=guide(language);var ids=new java.util.HashSet<String>();collect(book,ids);
            for(String id:java.util.List.of("initial_spells","initial_alchemy","initial_industry","simple_storage","visit_dream","pocket_magic_bottle","spacetime_entry","corridor_ritual","spacetime_terrain","advanced_industry","large_alchemy","advanced_spells"))
                h.assertTrue(ids.contains(id),"Orb links to absent guide destination: "+id);
        }h.succeed();
    }
    static com.google.gson.JsonObject guide(String language)throws java.io.IOException{return com.mcmagic.omnira.client.guide.GuideResourceTree.load(path->{
        try(var in=GuidanceGameTests.class.getResourceAsStream("/assets/omnira/guide/"+language+"/"+path)){
            if(in==null)throw new java.io.IOException(path);return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
    });}
    private static void collect(com.google.gson.JsonObject node,java.util.Set<String> ids){
        if(node.has("id"))ids.add(node.get("id").getAsString());
        for(String kind:java.util.List.of("chapters","entries"))if(node.has(kind))for(var child:node.getAsJsonArray(kind))collect(child.getAsJsonObject(),ids);
    }
    static com.google.gson.JsonObject find(com.google.gson.JsonObject node,String id){
        if(node.has("id")&&node.get("id").getAsString().equals(id))return node;
        for(String kind:java.util.List.of("chapters","entries"))if(node.has(kind))for(var child:node.getAsJsonArray(kind)){var found=find(child.getAsJsonObject(),id);if(found!=null)return found;}
        return null;
    }
}
