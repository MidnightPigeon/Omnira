package com.mcmagic.omnira.gametest;

import com.google.gson.JsonParser;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.sign.*;
import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_signs")
@PrefixGameTestTemplate(false)
public final class CrystalSignGameTests {
    @GameTest(template="spell_arena") public static void fullPoolRightClickCycles(GameTestHelper h){
        var sign=sign(h,true,3);
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var hit=new BlockHitResult(Vec3.atCenterOf(sign.getBlockPos()),Direction.SOUTH,sign.getBlockPos(),false);
        int count=WisdomStatements.pool().size();
        String last="";
        for(int cycle=0;cycle<3;cycle++){
            var seen=new HashSet<String>();
            for(int i=0;i<count;i++){
                if(i%2==0)sign.getBlockState().useWithoutItem(h.getLevel(),player,hit);
                else sign.getBlockState().useItemOn(new ItemStack(Items.STICK),h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
                String text=Arrays.stream(sign.getFrontText().getMessages(false)).map(c->c.getString()).collect(java.util.stream.Collectors.joining("\n"));
                h.assertTrue(seen.add(text),"Repeated statement before full pool exhausted at draw "+i);
                h.assertTrue(!text.equals(last),"Adjacent cycles repeated the same statement");last=text;
                h.assertTrue(sign.remainingCount()==count-i-1,"Draw consumed wrong number of statements");
                var saved=sign.saveWithFullMetadata(h.getLevel().registryAccess());
                sign.loadWithComponents(saved,h.getLevel().registryAccess());
            }
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void importedSignCountsDisplayedStatement(GameTestHelper h){
        var sign=sign(h,true,3);var pool=List.of("甲","乙","丙");
        sign.setText(new SignText().setMessage(1,net.minecraft.network.chat.Component.literal("甲")),true);
        var seen=new HashSet<String>();seen.add("甲");
        for(int i=0;i<2;i++){
            sign.draw(pool);
            h.assertTrue(seen.add(sign.getFrontText().getMessage(1,false).getString()),"Imported visible statement repeated before cycle exhausted");
        }
        h.assertTrue(sign.remainingCount()==0,"Imported sign failed to complete remaining cycle");h.succeed();
    }
    @GameTest(template="spell_arena") public static void crystalSignCrafting(GameTestHelper h){
        var slots=net.minecraft.core.NonNullList.withSize(9,ItemStack.EMPTY);
        for(int i=0;i<6;i++)slots.set(i,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get()));
        slots.set(7,new ItemStack(Items.STICK));
        var input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,slots);
        var recipe=h.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,h.getLevel()).orElseThrow().value();
        var result=recipe.assemble(input,h.getLevel().registryAccess());
        h.assertTrue(result.is(ModItems.CRYSTAL_SIGN.get())&&result.getCount()==3,"Six crystals and a stick must craft three signs");
        slots.set(0,ItemStack.EMPTY);
        h.assertTrue(!recipe.matches(net.minecraft.world.item.crafting.CraftingInput.of(3,3,slots),h.getLevel()),"Missing crystal accepted");h.succeed();
    }
    @GameTest(template="spell_arena") public static void validationAndLinePlacement(GameTestHelper h){
        String longLine="一二三四五六七八九十甲乙丙丁";
        h.assertTrue(WisdomStatements.normalize(longLine).equals("一二三四五六七八九十甲乙"),"Full-width line did not truncate at twelve");
        h.assertTrue(WisdomStatements.normalize("Boy♂Next♂Door").equals("Boy♂Next♂Door"),"Half-width text unnecessarily truncated");
        h.assertTrue(WisdomStatements.normalize("1\n2\n3\n4\n5")==null,"Five-line statement accepted");
        h.assertTrue(Arrays.equals(WisdomStatements.positioned("一行"),new String[]{"","一行","",""}),"Single line not on second row");
        h.assertTrue(Arrays.equals(WisdomStatements.positioned("甲\n乙"),new String[]{"","甲","乙",""}),"Two lines not centered");
        h.assertTrue(Arrays.equals(WisdomStatements.positioned("甲\n乙\n丙"),new String[]{"甲","乙","丙",""}),"Three rows shifted");
        h.assertTrue(Arrays.equals(WisdomStatements.positioned("甲\n乙\n丙\n丁"),new String[]{"甲","乙","丙","丁"}),"Four rows lost");
        var json=new com.google.gson.JsonObject();var entries=new com.google.gson.JsonArray();
        entries.add("有效");entries.add("有效");entries.add("1\n2\n3\n4\n5");entries.add(123);entries.add(" ");
        json.add("statements",entries);h.assertTrue(WisdomStatements.parse(json).equals(List.of("有效")),"Invalid statements not discarded");
        try(var reader=new java.io.InputStreamReader(CrystalSignGameTests.class.getResourceAsStream("/omnira-wisdom-defaults.json"),java.nio.charset.StandardCharsets.UTF_8)){
            h.assertTrue(WisdomStatements.parse(JsonParser.parseReader(reader)).size()==58,"Bundled statement pool incomplete");
        }catch(java.io.IOException ex){throw new RuntimeException(ex);}
        h.succeed();
    }
    private static CrystalSignEntity sign(GameTestHelper h,boolean wisdom,int x){
        var pos=h.absolutePos(new BlockPos(x,3,5));h.getLevel().setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos,(wisdom?ModBlocks.WISDOM_SIGN:ModBlocks.CRYSTAL_SIGN).get().defaultBlockState());
        return (CrystalSignEntity)h.getLevel().getBlockEntity(pos);
    }
    @GameTest(template="spell_arena") public static void shuffleBagPersistenceAndConfigChange(GameTestHelper h){
        var sign=sign(h,true,3);var pool=List.of("甲","乙","丙");var seen=new HashSet<String>();
        sign.draw(pool);seen.add(sign.getFrontText().getMessage(1,false).getString());
        var copy=new CrystalSignEntity(sign.getBlockPos(),sign.getBlockState());copy.setLevel(h.getLevel());
        copy.loadWithComponents(sign.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(copy.remainingCount()==2,"Draw bag lost on reload");
        for(int i=0;i<2;i++){copy.draw(pool);seen.add(copy.getFrontText().getMessage(1,false).getString());}
        h.assertTrue(seen.size()==3&&copy.remainingCount()==0,"Statement repeated before cycle completed");
        copy.draw(pool);h.assertTrue(copy.remainingCount()==2,"Exhausted pool did not refill");
        h.assertTrue(copy.getFrontText().hasGlowingText()&&copy.getFrontText().getColor()==DyeColor.BLACK,"Wisdom text not glowing black");
        h.assertTrue(copy.getFrontText().getMessage(1,false).equals(copy.getBackText().getMessage(1,false)),"Faces do not match");
        copy.draw(List.of("新池"));h.assertTrue(copy.getFrontText().getMessage(1,false).getString().equals("新池"),"Config change retained stale draw bag");
        copy.draw(List.of());h.assertTrue(copy.getFrontText().getMessage(1,false).getString().isEmpty(),"Empty valid pool retained stale text");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void placementModesAndSupportRemoval(GameTestHelper h){
        var level=h.getLevel();var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"sign_placement"));
        var floor=h.absolutePos(new BlockPos(4,2,4));level.setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
        player.setPos(floor.getX()+.5,floor.getY()+1,floor.getZ()+2.5);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.WISDOM_SIGN.get(),3));
        var floorContext=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(floor),Direction.UP,floor,false));
        h.assertTrue(ModItems.WISDOM_SIGN.get().useOn(floorContext).consumesAction(),"Floor placement failed");
        var floorSign=(CrystalSignEntity)level.getBlockEntity(floor.above());
        h.assertTrue(floorSign!=null&&floorSign.remainingCount()==WisdomStatements.pool().size()-1&&floorSign.getPlayerWhoMayEdit()==null,"Placement did not draw or opened editor");
        var ceiling=h.absolutePos(new BlockPos(8,6,4));level.setBlockAndUpdate(ceiling,Blocks.STONE.defaultBlockState());
        var ceilingContext=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(ceiling),Direction.DOWN,ceiling,false));
        h.assertTrue(ModItems.WISDOM_SIGN.get().useOn(ceilingContext).consumesAction()&&level.getBlockState(ceiling.below()).is(ModBlocks.WISDOM_HANGING_SIGN.get()),"Hanging placement failed");
        var wall=h.absolutePos(new BlockPos(6,3,7));level.setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
        player.setPos(wall.getX()+.5,wall.getY(),wall.getZ()+2.5);player.setXRot(0);player.setYRot(180);
        var wallContext=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(wall),Direction.SOUTH,wall,false));
        h.assertTrue(ModItems.WISDOM_SIGN.get().useOn(wallContext).consumesAction()&&level.getBlockState(wall.south()).is(ModBlocks.WISDOM_WALL_SIGN.get()),"Wall placement failed");
        level.removeBlock(ceiling,false);h.assertTrue(level.getBlockState(ceiling.below()).isAir(),"Hanging sign survived missing support");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.CRYSTAL_SIGN.get()));
        var forbidden=h.absolutePos(new BlockPos(10,6,4));level.setBlockAndUpdate(forbidden,Blocks.STONE.defaultBlockState());
        h.assertTrue(!ModItems.CRYSTAL_SIGN.get().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(forbidden),Direction.DOWN,forbidden,false))).consumesAction(),"Ordinary crystal sign hung from ceiling");
        h.assertTrue(ModBlocks.CRYSTAL_SIGN.get().defaultBlockState().getLightEmission()==12&&ModBlocks.WISDOM_SIGN.get().defaultBlockState().getLightEmission()==15,"Incorrect sign luminance");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void editingGlowAndAssembly(GameTestHelper h){
        var sign=sign(h,false,4);var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"sign_edit"));
        sign.setAllowedPlayerEditor(player.getUUID());
        var lines=List.of(net.minecraft.server.network.FilteredText.passThrough("编辑"),net.minecraft.server.network.FilteredText.passThrough(""),net.minecraft.server.network.FilteredText.passThrough(""),net.minecraft.server.network.FilteredText.passThrough(""));
        sign.updateSignText(player,true,lines);h.assertTrue(sign.getFrontText().getMessage(0,false).getString().equals("编辑"),"Native front editing failed");
        sign.setAllowedPlayerEditor(player.getUUID());sign.updateSignText(player,false,lines);
        h.assertTrue(sign.getBackText().getMessage(0,false).getString().isEmpty(),"Back text edited on single-face sign");
        var glow=(SignApplicator)Items.GLOW_INK_SAC;
        h.assertTrue(glow.tryApplyToSign(h.getLevel(),sign,true,player)&&sign.getFrontText().hasGlowingText(),"Glow ink failed");
        sign.setWaxed(true);sign.setAllowedPlayerEditor(player.getUUID());
        var blocked=List.of(net.minecraft.server.network.FilteredText.passThrough("改动"),lines.get(1),lines.get(2),lines.get(3));
        sign.updateSignText(player,true,blocked);h.assertTrue(sign.getFrontText().getMessage(0,false).getString().equals("编辑"),"Wax did not block editing");
        var wisdom=sign(h,true,8);wisdom.draw(List.of("智慧"));wisdom.setAllowedPlayerEditor(player.getUUID());wisdom.updateSignText(player,true,lines);
        h.assertTrue(wisdom.getPlayerWhoMayEdit()==null&&wisdom.getFrontText().getMessage(1,false).getString().equals("智慧"),"Wisdom sign editable");
        player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
        var hit=new BlockHitResult(Vec3.atCenterOf(wisdom.getBlockPos()),Direction.SOUTH,wisdom.getBlockPos(),false);
        var ink=new ItemStack(Items.INK_SAC,2);
        wisdom.getBlockState().useItemOn(ink,h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(wisdom.remainingCount()==WisdomStatements.pool().size()-1&&ink.getCount()==2&&wisdom.getFrontText().hasGlowingText(),"Right click did not redraw independently of editing/ink");
        wisdom.getBlockState().useWithoutItem(h.getLevel(),player,hit);
        h.assertTrue(wisdom.remainingCount()==WisdomStatements.pool().size()-2,"Empty hand did not redraw exactly once");
        var container=new net.minecraft.world.SimpleContainer(7);
        for(int i=0;i<6;i++)container.setItem(i,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()));
        container.setItem(6,new ItemStack(ModItems.CRYSTAL_SIGN.get()));
        var recipe=h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","assembly/marvelous_wisdom_sign")).orElseThrow().value();
        h.assertTrue(recipe instanceof com.mcmagic.omnira.recipe.AssemblyRecipe assembly&&assembly.matches(new com.mcmagic.omnira.recipe.AssemblyRecipe.Input(container),h.getLevel())
                &&assembly.assemble(new com.mcmagic.omnira.recipe.AssemblyRecipe.Input(container),h.getLevel().registryAccess()).is(ModItems.WISDOM_SIGN.get()),"Wisdom assembly recipe invalid");
        h.succeed();
    }
}
