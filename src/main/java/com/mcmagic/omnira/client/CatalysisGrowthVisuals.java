package com.mcmagic.omnira.client;

import com.mcmagic.omnira.alchemy.catalysis.*;
import com.mcmagic.omnira.client.renderer.*;
import com.mcmagic.omnira.paradox.ParadoxContent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class CatalysisGrowthVisuals {
    private record PlantModel(BlockState stem,BlockState foliage,CatalysisPlantMesh mesh) {}
    // Block identities and states survive resource reloads; baked models/textures are never cached here.
    private static final java.util.Map<Block,PlantModel> TREES=new java.util.IdentityHashMap<>();
    private static final java.util.Map<Block,PlantModel> FUNGI=new java.util.IdentityHashMap<>();
    private static PlantModel fungusModel(Block plant){
        boolean crimson=plant==Blocks.CRIMSON_FUNGUS,warped=plant==Blocks.WARPED_FUNGUS;
        boolean nether=crimson||warped,red=plant==Blocks.RED_MUSHROOM;
        var stem=(crimson?Blocks.CRIMSON_STEM:warped?Blocks.WARPED_STEM:Blocks.MUSHROOM_STEM).defaultBlockState();
        var hat=(crimson?Blocks.NETHER_WART_BLOCK:warped?Blocks.WARPED_WART_BLOCK:red?Blocks.RED_MUSHROOM_BLOCK:Blocks.BROWN_MUSHROOM_BLOCK).defaultBlockState();
        if(!nether)for(var face:java.util.List.of(BlockStateProperties.UP,BlockStateProperties.DOWN,BlockStateProperties.NORTH,
                BlockStateProperties.SOUTH,BlockStateProperties.EAST,BlockStateProperties.WEST)){
            hat=hat.setValue(face,true);stem=stem.setValue(face,face!=BlockStateProperties.UP&&face!=BlockStateProperties.DOWN);
        }
        return new PlantModel(stem,hat,nether?CatalysisPlantMesh.NETHER_FUNGUS:red?CatalysisPlantMesh.RED_MUSHROOM:CatalysisPlantMesh.BROWN_MUSHROOM);
    }
    private static PlantModel treeModel(Block sapling){
        var id=BuiltInRegistries.BLOCK.getKey(sapling);String family=id.getPath().replace("_sapling","").replace("_propagule","");
        String logs=family.equals("azalea")||family.equals("flowering_azalea")?"oak":family;
        var wood=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),logs+"_log"));
        var leaves=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),family+"_leaves"));
        return new PlantModel(wood.defaultBlockState(),leaves.defaultBlockState(),family.equals("spruce")?CatalysisPlantMesh.CONIFER:CatalysisPlantMesh.TREE);
    }
    public static boolean fungal(CatalysisGrowth.Plan plan){
        var plant=plan.plant();
        return plant.is(Blocks.CRIMSON_FUNGUS)||plant.is(Blocks.WARPED_FUNGUS)||plant.is(Blocks.RED_MUSHROOM)||plant.is(Blocks.BROWN_MUSHROOM);
    }
    public static void fungus(CatalysisGrowth.Plan plan,float stage,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(stage<2){block(plan.plant(),.5,1.22,.5,.34F,.34F,.34F,pose,buffers,light,overlay);return;}
        var model=FUNGI.computeIfAbsent(plan.plant().getBlock(),CatalysisGrowthVisuals::fungusModel);
        float fraction=Math.clamp((stage-1)/Math.max(1,plan.stages()-1),0,1),cell=(.42F+.42F*fraction)/5;
        drawPlant(model,cell,pose,buffers,light,overlay);
    }
    public static boolean sealed(ItemStack stack){return stack.is(ParadoxContent.STABLE_SOURCE.get())||stack.is(ParadoxContent.STABLE_AGGREGATE.get());}
    public static void sealedGrowth(CatalysisBlockEntity be,double time,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,1.38,.5);pose.scale(.65F,.65F,.65F);
        ParadoxSourceRenderer.core(be.items.getStackInSlot(0),time,pose,buffers,light,overlay);pose.popPose();
        boolean genesis=be.items.getStackInSlot(0).is(ParadoxContent.STABLE_AGGREGATE.get());
        if(!be.items.getStackInSlot(1).is(genesis?ParadoxContent.GENESIS.get():com.mcmagic.omnira.registry.ModItems.PARADOX_DUST.get()))return;
        float growth=(be.visualStage(time)-1)/3F;
        int grains=Math.max(1,(int)(1+growth*15));
        var mc=Minecraft.getInstance();var model=mc.getModelManager().getModel(ParadoxMaterialRenderer.model(genesis?"genesis_grain":"grain"));
        var out=CrystalGlassLayer.contents(buffers).getBuffer(Sheets.cutoutBlockSheet());
        for(int i=0;i<grains;i++){
            double angle=i*2.39996;
            pose.pushPose();pose.translate(.5+Math.cos(angle)*.035*Math.sqrt(i),1.51+(i/5)*.035,.5+Math.sin(angle)*.035*Math.sqrt(i));
            float size=.3F+growth*.45F;pose.scale(size,size,size);pose.translate(-.5,-.5,-.5);
            mc.getItemRenderer().renderModelLists(model,ItemStack.EMPTY,LightTexture.FULL_BRIGHT,overlay,pose,out);pose.popPose();
        }
    }
    public static void tree(CatalysisGrowth.Plan plan,float stage,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var sapling=plan.plant();
        if(stage<2){block(sapling,.5,1.22,.5,.34F,.34F,.34F,pose,buffers,light,overlay);return;}
        var model=TREES.computeIfAbsent(sapling.getBlock(),CatalysisGrowthVisuals::treeModel);
        if(model.stem.is(Blocks.AIR)||model.foliage.is(Blocks.AIR)){block(sapling,.5,1.22,.5,.34F,.34F,.34F,pose,buffers,light,overlay);return;}
        float fraction=Math.clamp((stage-1)/Math.max(1,plan.stages()-1),0,1),size=.42F+.42F*fraction;
        float cell=size/5;
        drawPlant(model,cell,pose,buffers,light,overlay);
    }
    private static void drawPlant(PlantModel model,float size,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var contents=CrystalGlassLayer.contents(buffers);
        for(int i=0;i<model.mesh.size();i++){
            var cell=model.mesh.cell(i);
            block(cell.foliage()?model.foliage:model.stem,.5+cell.x()*size,1.22+cell.y()*size,.5+cell.z()*size,
                    size,size,size,pose,contents,light,overlay);
        }
    }
    private static void block(net.minecraft.world.level.block.state.BlockState state,double x,double y,double z,float sx,float sy,float sz,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(x-sx/2,y,z-sz/2);pose.scale(sx,sy,sz);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state,pose,CrystalGlassLayer.contents(buffers),light,overlay);pose.popPose();
    }
    private CatalysisGrowthVisuals(){}
}
