package com.mcmagic.omnira.client;

import java.util.*;
import com.google.gson.*;
import com.mcmagic.omnira.client.renderer.*;
import com.mcmagic.omnira.myriad.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class MiniatureWorldClient {
    private record Quad(float[] vertices,float u,float v,int color,float nx,float ny,float nz){}
    private static final Map<String,MiniatureTransitionMesh> TRANSITIONS=new HashMap<>();
    private static final Map<String,MiniatureWorldBlocks> NATIVE_BLOCKS=new HashMap<>();
    private static final Map<String,Map<ResourceLocation,List<Quad>>> MESHES=meshes();
    private static final Map<WorldDevelopmentRecipe.Material,List<ItemStack>> EXAMPLES=new HashMap<>();
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){
        event.registerBlockEntityRenderer(MiniatureWorldContent.ENTITY.get(),context->new BlockEntityRenderer<MiniatureWorldBlockEntity>(){
            @Override public void render(MiniatureWorldBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
                if(be.getLevel()!=null)draw(be,be.stage(),be.getLevel().getGameTime()+partial,pose,buffers,light,overlay);
            }
            @Override public AABB getRenderBoundingBox(MiniatureWorldBlockEntity be){var p=be.getBlockPos();return new AABB(p.getX()-.1,p.getY(),p.getZ()-.1,p.getX()+5.1,p.getY()+5,p.getZ()+5.1);}
            @Override public int getViewDistance(){return 64;}
        });
    }
    private static Map<String,Map<ResourceLocation,List<Quad>>> meshes(){
        try(var reader=new java.io.InputStreamReader(Objects.requireNonNull(MiniatureWorldClient.class.getResourceAsStream("/assets/omnira/geometry/miniature_world.json")),java.nio.charset.StandardCharsets.UTF_8)){
            var json=JsonParser.parseReader(reader).getAsJsonObject();var result=new HashMap<String,Map<ResourceLocation,List<Quad>>>();
            for(var entry:json.getAsJsonObject("scene_blocks").entrySet())NATIVE_BLOCKS.put(entry.getKey(),new MiniatureWorldBlocks(entry.getValue().getAsJsonArray()));
            result.put("frame",bake(json.getAsJsonArray("frame")));
            result.put("void_floor",bake(json.getAsJsonArray("void_floor")));
            result.put("dream_floor",bake(json.getAsJsonArray("dream_floor")));
            result.put("spacetime_floor",bake(json.getAsJsonArray("spacetime_floor")));
            for(var entry:json.getAsJsonObject("scenes").entrySet()){
                var parts=entry.getValue().getAsJsonArray();result.put(entry.getKey(),bake(parts));
                TRANSITIONS.put(entry.getKey(),new MiniatureTransitionMesh(parts));
            }
            return Map.copyOf(result);
        }catch(java.io.IOException ex){throw new IllegalStateException("Missing miniature world mesh",ex);}
    }
    private static Map<ResourceLocation,List<Quad>> bake(JsonArray parts){
        var result=new LinkedHashMap<ResourceLocation,List<Quad>>();
        // Local constant avoids static initialization order coupling with the cached meshes.
        int[][] faces={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
        for(var element:parts){
            var p=element.getAsJsonObject();var a=p.getAsJsonArray("a");var b=p.getAsJsonArray("b");
            float x=a.get(0).getAsFloat(),y=a.get(1).getAsFloat(),z=a.get(2).getAsFloat(),X=b.get(0).getAsFloat(),Y=b.get(1).getAsFloat(),Z=b.get(2).getAsFloat();
            float[][] vertices={{x,y,z},{X,y,z},{X,Y,z},{x,Y,z},{x,y,Z},{X,y,Z},{X,Y,Z},{x,Y,Z}};
            float density=p.get("density").getAsFloat();int color=(int)Long.parseLong(p.get("color").getAsString(),16),mask=p.get("faces").getAsInt();
            var list=result.computeIfAbsent(ResourceLocation.parse(p.get("texture").getAsString()),k->new ArrayList<>());
            for(int face=0;face<6;face++){
                if((mask&(1<<face))==0)continue;var ids=faces[face];var start=vertices[ids[0]];
                float uLength=(face==2||face==3?Z-z:X-x)*density,vLength=(face>=4?Z-z:Y-y)*density;
                float[] du=new float[3],dv=new float[3];for(int n=0;n<3;n++){du[n]=vertices[ids[3]][n]-start[n];dv[n]=vertices[ids[1]][n]-start[n];}
                var normal=new org.joml.Vector3f(dv).cross(new org.joml.Vector3f(du)).normalize();
                for(float u=0;u<uLength;u+=1)for(float v=0;v<vLength;v+=1){
                    float U=Math.min(u+1,uLength),V=Math.min(v+1,vLength);float[] quad=new float[12];
                    float[] us={u,u,U,U},vs={v,V,V,v};
                    for(int i=0;i<4;i++)for(int n=0;n<3;n++)quad[i*3+n]=start[n]+du[n]*us[i]/uLength+dv[n]*vs[i]/vLength;
                    list.add(new Quad(quad,U-u,V-v,color,normal.x,normal.y,normal.z));
                }
            }
        }return result;
    }
    private static void mesh(String name,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        mesh(name,pose,buffers,light,overlay,Float.POSITIVE_INFINITY);
    }
    private static void mesh(String name,PoseStack pose,MultiBufferSource buffers,int light,int overlay,float ceiling){
        mesh(name,pose,buffers,light,overlay,ceiling,0xFFFFFFFF);
    }
    private static void mesh(String name,PoseStack pose,MultiBufferSource buffers,int light,int overlay,float ceiling,int tint){
        var atlas=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        for(var entry:MESHES.getOrDefault(name,Map.of()).entrySet()){
            var sprite=atlas.apply(entry.getKey());boolean crystal=entry.getKey().getNamespace().equals("omnira");
            boolean fluid=entry.getKey().getPath().contains("water")||entry.getKey().getPath().contains("lava");
            var out=buffers.getBuffer(crystal||fluid?CrystalGlassLayer.ATLAS:RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
            int lit=entry.getKey().getPath().contains("glow")||entry.getKey().getPath().contains("lava")?LightTexture.FULL_BRIGHT:light;
            for(var q:entry.getValue())if((q.vertices[1]+q.vertices[4]+q.vertices[7]+q.vertices[10])*.25F<=ceiling)for(int i=0;i<4;i++)out.addVertex(pose.last(),q.vertices[i*3],q.vertices[i*3+1],q.vertices[i*3+2])
                    .setColor((q.color>>>16&255)*(tint>>>16&255)/255,(q.color>>>8&255)*(tint>>>8&255)/255,(q.color&255)*(tint&255)/255,q.color>>>24)
                    .setUv(sprite.getU(i>=2?q.u:0),sprite.getV(i==1||i==2?q.v:0)).setOverlay(overlay).setLight(lit).setNormal(pose.last(),q.nx,q.ny,q.nz);
        }
    }
    public static void draw(MiniatureWorldBlockEntity be,MiniatureWorldBlockEntity.Stage stage,double time,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        double elapsed=be==null?1000:Math.max(0,time-be.changedAt());
        float progress=be==null||!be.developing()?1:Math.clamp((float)elapsed/Math.max(1,be.duration()),0,1);
        int theme=stage==MiniatureWorldBlockEntity.Stage.RIFT&&progress<.5F?MiniatureWorldBlockEntity.Stage.BASTION.theme():stage.theme();
        boolean tearing=stage==MiniatureWorldBlockEntity.Stage.SPACETIME_EMPTY&&be!=null&&be.previousStage()==MiniatureWorldBlockEntity.Stage.DREAM_LAYERS&&progress<1;
        if(tearing&&progress<.78F)theme=MiniatureWorldBlockEntity.Stage.DREAM_LAYERS.theme();
        mesh("frame",pose,buffers,light,overlay,Float.POSITIVE_INFINITY,theme);
        floor(tearing&&progress<.78F?MiniatureWorldBlockEntity.Stage.DREAM_LAYERS:stage,progress,pose,buffers,overlay);
        pose.pushPose();
        pose.translate(2.5,MiniatureWorldMotion.bob(time),2.5);
        pose.mulPose(Axis.YP.rotationDegrees(MiniatureWorldMotion.rotation(time)));
        pose.translate(-2.5,0,-2.5);
        if(MiniatureCodexRenderer.contains(stage)){
            MiniatureCodexRenderer.draw(be==null?stage:be.previousStage(),stage,progress,time,pose,buffers,light,overlay);
        }else if(stage==MiniatureWorldBlockEntity.Stage.PORTAL&&be!=null&&be.previousStage()==MiniatureWorldBlockEntity.Stage.RUINS&&progress<1){
            mesh("portal_ground",pose,buffers,light,overlay);
            if(progress<.3F)mesh("portal_ruins",pose,buffers,light,overlay,2.3F-progress*5);
            if(progress>.32F)mesh("portal_frame",pose,buffers,light,overlay,1.2F+(progress-.32F)*2.1F);
            if(progress>.88F)netherPortal(time,pose,buffers,overlay);
        }else if(stage==MiniatureWorldBlockEntity.Stage.SPACETIME_EMPTY&&be!=null&&be.previousStage()==MiniatureWorldBlockEntity.Stage.DREAM_LAYERS&&progress<1){
            var old=TRANSITIONS.get("dream_layers");if(old!=null)old.swallow(progress,pose,buffers,light,overlay);
            MiniatureWorldInhabitants.draw(MiniatureWorldBlockEntity.Stage.DREAM_LAYERS,time,pose,buffers,light,1-MiniatureWorldMotion.smooth(progress/.6F));
            rift(time,progress,pose,buffers);
        }else if(stage==MiniatureWorldBlockEntity.Stage.PROBABILITY_EXPLOSION){
            float collapse=1-MiniatureWorldMotion.smooth(progress/.65F);
            if(collapse>.001F){pose.pushPose();pose.translate(2.5,2.5,2.5);pose.mulPose(Axis.YP.rotationDegrees(progress*420));pose.scale(collapse,collapse,collapse);pose.translate(-2.5,-2.5,-2.5);scene(MiniatureWorldBlockEntity.Stage.SPACETIME_LAYERS,time,pose,buffers,light,overlay);pose.popPose();}
            if(progress>=.65F)for(int i=0;i<48;i++){
                float p=(progress-.65F)/.35F;double a=i*2.39996,r=Math.sin(p*Math.PI)*1.7,lat=1-2*(i+.5)/48;
                StellarVisuals.cube(pose,buffers.getBuffer(CrystalGlassLayer.COLOR),2.5F+(float)(Math.cos(a)*r*Math.sqrt(1-lat*lat)),2.5F+(float)(lat*r),2.5F+(float)(Math.sin(a)*r*Math.sqrt(1-lat*lat)),.04F*(1-p),0xFFE8C5FF);
            }
        }else if(stage==MiniatureWorldBlockEntity.Stage.VOID){
            float scale=(float)Math.pow(1-progress,1.6);
            if(scale>.001F){pose.pushPose();pose.translate(2.5,2,2.5);pose.mulPose(Axis.YP.rotationDegrees(progress*240));pose.scale(scale,scale,scale);pose.translate(-2.5,-2,-2.5);scene(MiniatureWorldBlockEntity.Stage.CITY,time,pose,buffers,light,overlay);pose.popPose();}
        }else if(stage==MiniatureWorldBlockEntity.Stage.RIFT){
            var old=TRANSITIONS.get("bastion");if(old!=null)old.draw(MiniatureWorldTransition.Preset.WAVE,progress,false,true,MiniatureWorldMotion.bob(time),pose,buffers,light,overlay);
            MiniatureWorldInhabitants.draw(MiniatureWorldBlockEntity.Stage.BASTION,time,pose,buffers,light,MiniatureWorldTransition.gateAmount(progress,2.5F,1.3F+MiniatureWorldMotion.bob(time),2.95F));
        }else if(progress<1&&be!=null){
            transition(be.previousStage(),stage,time,progress,pose,buffers,light,overlay);
        }else scene(stage,time,pose,buffers,light,overlay);
        pose.popPose();
        if(stage==MiniatureWorldBlockEntity.Stage.RIFT)endRift(pose,buffers,progress);
        var stars=buffers.getBuffer(CrystalGlassLayer.COLOR);
        for(int i=0;i<9;i++){
            double phase=time*.008+i*Math.PI*2/9;float x=2.5F+(float)Math.cos(phase)*1.85F,z=2.5F+(float)Math.sin(phase)*1.85F;
            StellarVisuals.cube(pose,stars,x,4.2F+(float)Math.sin(phase*2)*.04F,z,.025F,theme);
        }
        if(be!=null){
            for(int i=0;i<be.size();i++){double angle=time*.008+i*Math.PI*2/9;item(be.material(i),2.5+Math.cos(angle)*1.85,4.2,2.5+Math.sin(angle)*1.85,.34F,time*.5,pose,buffers,light,overlay);}
            var offerings=be.offerings();
            if(progress<.65F)for(int i=0;i<offerings.size();i++){
                double p=Math.min(1,progress/.65),angle=time*.008+i*Math.PI*2/Math.max(1,offerings.size())+p*4;
                item(offerings.get(i),2.5+Math.cos(angle)*1.85*(1-p),4.2-2.8*p,2.5+Math.sin(angle)*1.85*(1-p),.34F*(float)(1-p),time*2,pose,buffers,light,overlay);
            }
            hint(be,time,pose,buffers,overlay);
        }
        if(stage.complete()){
            float reveal=stage==MiniatureWorldBlockEntity.Stage.CODEX_COMPLETE?MiniatureWorldMotion.smooth((progress-.90F)/.10F):1;
            item(stage.product(),2.5,2.5+Math.sin(time*.035)*.1,2.5,(stage==MiniatureWorldBlockEntity.Stage.PROBABILITY_COMPLETE?1.6F:.8F)*reveal,time*.65,pose,buffers,LightTexture.FULL_BRIGHT,overlay);
            int[] colors={0xFFCFB477,0xFF68B6F1,0xFFED845D,0xFFC4EEDF,0xFFFFEDA3,0xFF9F79D3,0xFFB5E7AE,0xFF81D6E7};
            for(int i=0;i<24;i++){double a=time*.025+i*Math.PI/12;float r=.6F+(i%3)*.15F;StellarVisuals.cube(pose,stars,2.5F+(float)Math.cos(a)*r,2.5F+(float)Math.sin(a*2+i)*.38F,2.5F+(float)Math.sin(a)*r,.035F*reveal,colors[i%8]);}
        }
    }
    private static void floor(MiniatureWorldBlockEntity.Stage stage,float progress,PoseStack pose,MultiBufferSource buffers,int overlay){
        switch(stage){
            case DREAM_EMPTY,DREAM_LAYERS -> mesh("dream_floor",pose,buffers,LightTexture.FULL_BRIGHT,overlay);
            case SPACETIME_EMPTY,SPACETIME_LAYERS,PROBABILITY_EXPLOSION -> mesh("spacetime_floor",pose,buffers,LightTexture.FULL_BRIGHT,overlay);
            case NETHER_EMPTY,NETHER,FORTRESS,BASTION -> mesh("void_floor",pose,buffers,LightTexture.FULL_BRIGHT,overlay);
            case RIFT -> mesh("void_floor",pose,buffers,LightTexture.FULL_BRIGHT,overlay);
            case END_EMPTY,END,CHORUS,CITY,VOID -> endFloor(pose,buffers);
            default -> { }
        }
    }
    private static void endFloor(PoseStack pose,MultiBufferSource buffers){
        var out=buffers.getBuffer(RenderType.endPortal());
        // Above the glass bottom (.03125), below all terrain; one upward-facing plane.
        for(int i=0;i<4;i++)out.addVertex(pose.last().pose(),i>=2?4.875F:.125F,.0625F,i==1||i==2?4.875F:.125F);
    }
    private static void transition(MiniatureWorldBlockEntity.Stage previous,MiniatureWorldBlockEntity.Stage next,double time,float progress,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var preset=MiniatureWorldTransition.preset(next.name().toLowerCase(Locale.ROOT));
        var old=TRANSITIONS.get(previous.name().toLowerCase(Locale.ROOT));var fresh=TRANSITIONS.get(next.name().toLowerCase(Locale.ROOT));
        if(old!=null)old.draw(preset,progress,false,false,0,pose,buffers,light,overlay);
        if(fresh!=null)fresh.draw(preset,progress,true,false,0,pose,buffers,light,overlay);
        if(preset==MiniatureWorldTransition.Preset.STARDUST&&old!=null)old.grains(fresh,progress,pose,buffers);
        if(preset==MiniatureWorldTransition.Preset.WAVE){
            float r=2.35F*progress;
            for(int layer=0;layer<(next==MiniatureWorldBlockEntity.Stage.SPACETIME_LAYERS?3:1);layer++)for(int i=0;i<24;i++){
                double a=i*Math.PI/12;StellarVisuals.cube(pose,buffers.getBuffer(CrystalGlassLayer.COLOR),2.5F+(float)Math.cos(a)*r,.85F+layer*1.25F,2.5F+(float)Math.sin(a)*r,.018F*(float)Math.sin(progress*Math.PI),0xFFCDEFF5);
            }
        }
        float out=MiniatureWorldTransition.actorAmount(progress,false),in=MiniatureWorldTransition.actorAmount(progress,true);
        if(out>0){
            nativeBlocks(previous,out,pose,buffers,light,overlay);
            MiniatureWorldInhabitants.draw(previous,time,pose,buffers,light,out);
            if(previous==MiniatureWorldBlockEntity.Stage.DREAM_PORTAL)dreamGateway(time,out,pose,buffers,light);
            if(previous==MiniatureWorldBlockEntity.Stage.PORTAL){pose.pushPose();pose.translate(2.4375,1.6875,2.5);pose.scale(out,out,out);pose.translate(-2.4375,-1.6875,-2.5);netherPortal(time,pose,buffers,overlay);pose.popPose();}
        }
        if(in>0){
            nativeBlocks(next,in,pose,buffers,light,overlay);
            if(next==MiniatureWorldBlockEntity.Stage.BASTION)beam(pose,buffers);
            if(next==MiniatureWorldBlockEntity.Stage.DREAM_PORTAL)dreamGateway(time,in,pose,buffers,light);
            MiniatureWorldInhabitants.draw(next,time,pose,buffers,light,in);
        }
    }
    private static void scene(MiniatureWorldBlockEntity.Stage stage,double time,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        mesh(stage.name().toLowerCase(Locale.ROOT),pose,buffers,light,overlay);
        nativeBlocks(stage,1,pose,buffers,light,overlay);
        if(stage==MiniatureWorldBlockEntity.Stage.DREAM_PORTAL)dreamGateway(time,1,pose,buffers,light);
        if(stage==MiniatureWorldBlockEntity.Stage.PORTAL)netherPortal(time,pose,buffers,overlay);
        if(stage==MiniatureWorldBlockEntity.Stage.BASTION)beam(pose,buffers);
        MiniatureWorldInhabitants.draw(stage,time,pose,buffers,light);
    }
    private static void nativeBlocks(MiniatureWorldBlockEntity.Stage stage,float amount,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var blocks=NATIVE_BLOCKS.get(stage.name().toLowerCase(Locale.ROOT));
        if(blocks!=null)blocks.draw(amount,pose,buffers,light,overlay);
    }
    private static void rift(double time,float progress,PoseStack pose,MultiBufferSource buffers){
        if(MiniatureWorldTransition.riftOpening(progress)<=.001F)return;
        var out=buffers.getBuffer(CrystalGlassLayer.COLOR);
        for(int row=0;row<32;row++){
            var tear=MiniatureWorldTransition.tear(progress,row);
            for(int col=0;col<8;col++)for(int side:new int[]{-1,1})for(int n=0;n<4;n++){
                int i=side>0?n:3-n;float x=tear.x()-tear.halfWidth()+tear.halfWidth()*2*(col+(i>=2?1:0))/8;
                out.addVertex(pose.last().pose(),x,i==1||i==2?tear.top():tear.bottom(),2.5F+side*tear.depth()).setColor(MiniatureWorldTransition.riftColor(time,row,col));
            }
            for(int side:new int[]{-1,1})StellarVisuals.cube(pose,buffers.getBuffer(CrystalGlassLayer.COLOR),tear.x()+side*tear.halfWidth(),(tear.top()+tear.bottom())/2,2.5F,
                    .025F*MiniatureWorldTransition.riftOpening(progress),row%3==0?0xFFE4D2FF:0xFF9EDFE2);
        }
    }
    private static void dreamGateway(double time,float amount,PoseStack pose,MultiBufferSource buffers,int light){
        if(amount<=0)return;pose.pushPose();pose.translate(2.5,1.75,2.5);pose.scale(.5F*amount,.5F*amount,.5F*amount);
        DreamPortalRenderer.swirl(pose,buffers,time,CrystalGlassLayer.COLOR);pose.popPose();
    }
    private static void netherPortal(double time,PoseStack pose,MultiBufferSource buffers,int overlay){
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.withDefaultNamespace("block/nether_portal"));
        var out=buffers.getBuffer(CrystalGlassLayer.ATLAS);
        for(int side:new int[]{-1,1})for(int i=0;i<4;i++){
            int j=side>0?i:3-i;float x=j>=2?2.6875F:2.1875F,y=j==1||j==2?2F:1.375F;
            out.addVertex(pose.last(),x,y,2.5F+side*.005F).setColor(0xDDA88CFF).setUv(sprite.getU(j>=2?1:0),sprite.getV(j==1||j==2?0:1)).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(),0,0,side);
        }
    }
    private static void beam(PoseStack pose,MultiBufferSource buffers){pose.pushPose();pose.translate(2.5,2.4,2.5);pose.scale(.06F,1.25F,.06F);StellarVisuals.cube(pose,buffers.getBuffer(CrystalGlassLayer.COLOR),0,0,0,1,0x998B56DF);pose.popPose();}
    private static void endRift(PoseStack pose,MultiBufferSource buffers,float progress){
        float r=MiniatureWorldTransition.gateRadius(progress),y=MiniatureWorldTransition.gateHeight(progress);
        var out=buffers.getBuffer(RenderType.endPortal());
        for(int side:new int[]{-1,1})for(int n=0;n<32;n++){
            double a=(side>0?n+1:n)*Math.PI/16,b=(side>0?n:n+1)*Math.PI/16;
            out.addVertex(pose.last().pose(),2.5F,y+side*.004F,2.5F);out.addVertex(pose.last().pose(),2.5F,y+side*.004F,2.5F);
            out.addVertex(pose.last().pose(),2.5F+(float)Math.cos(a)*r,y+side*.004F,2.5F+(float)Math.sin(a)*r);
            out.addVertex(pose.last().pose(),2.5F+(float)Math.cos(b)*r,y+side*.004F,2.5F+(float)Math.sin(b)*r);
        }
    }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){EXAMPLES.clear();MiniatureWorldInhabitants.clear();}
    private static void hint(MiniatureWorldBlockEntity be,double time,PoseStack pose,MultiBufferSource buffers,int overlay){
        if(be.hint()<0)return;
        if(be.stage()==MiniatureWorldBlockEntity.Stage.EMPTY){initialHints(be,time,pose,buffers,overlay);return;}
        var recipes=be.recipes();if(be.hint()>=recipes.size())return;
        var requirements=recipes.get(be.hint()).materials();var mc=Minecraft.getInstance();
        pose.pushPose();pose.translate(2.5,3.3,2.5);pose.mulPose(Axis.YP.rotationDegrees(-be.hintFace().toYRot()));pose.translate(0,0,2.535);
        text(Component.translatable("miniature.omnira.next"),0,.4F,pose,buffers,true);
        for(int i=0;i<requirements.size();i++){
            var requirement=requirements.get(i);var choices=EXAMPLES.computeIfAbsent(requirement,WorldDevelopmentRecipe.Material::examples);if(choices.isEmpty())continue;
            var stack=choices.get(Math.floorMod((int)(time/40),choices.size()));float y=-i*.34F;
            pose.pushPose();pose.translate(-1.5,y,0);pose.scale(.25F,.25F,.25F);mc.getItemRenderer().renderStatic(stack,ItemDisplayContext.GUI,LightTexture.FULL_BRIGHT,overlay,pose,CrystalGlassLayer.contents(buffers),mc.level,i);pose.popPose();
            text(Component.literal(stack.getCount()+" x ").append(stack.getHoverName()),-1.25F,y+.075F,pose,buffers,false);
        }
        pose.popPose();
    }
    private static void initialHints(MiniatureWorldBlockEntity be,double time,PoseStack pose,MultiBufferSource buffers,int overlay){
        var series=be.initialSeries();var mc=Minecraft.getInstance();
        pose.pushPose();pose.translate(2.5,0,2.5);pose.mulPose(Axis.YP.rotationDegrees(-be.hintFace().toYRot()));pose.translate(0,0,2.51);
        for(int panel=0;panel<WorldDevelopmentSeries.PANEL_COUNT;panel++){
            pose.pushPose();pose.translate(0,WorldDevelopmentSeries.panelY(panel),0);
            float w=(float)WorldDevelopmentSeries.PANEL_WIDTH,h=(float)WorldDevelopmentSeries.PANEL_HEIGHT;
            int color=panel<series.size()?0xFFB9EAF5:0x66556B78;
            panelEdge(pose,buffers,-w/2,-h/2,w,.035F,color);panelEdge(pose,buffers,-w/2,h/2-.035F,w,.035F,color);
            panelEdge(pose,buffers,-w/2,-h/2,.035F,h,color);panelEdge(pose,buffers,w/2-.035F,-h/2,.035F,h,color);
            if(panel<series.size()){
                var family=series.get(panel);var alternatives=family.at(MiniatureWorldBlockEntity.Stage.EMPTY);
                int selected=Math.floorMod(be.initialAlternative(panel),alternatives.size());
                text(Component.translatable(family.key()).append("  "+(selected+1)+"/"+alternatives.size()),0,.45F,pose,buffers,true);
                var requirements=alternatives.get(selected).materials();
                for(int i=0;i<requirements.size();i++){
                    var choices=EXAMPLES.computeIfAbsent(requirements.get(i),WorldDevelopmentRecipe.Material::examples);if(choices.isEmpty())continue;
                    var stack=choices.get(Math.floorMod((int)(time/40),choices.size()));
                    boolean rows=requirements.size()>4;int row=rows?i/5:0,col=rows?i%5:i,count=rows?Math.min(5,requirements.size()-row*5):requirements.size();
                    float x=(col-(count-1)/2F)*(w-.3F)/count,y=rows?.08F-row*.37F:.02F;
                    float size=rows?.18F:.27F;
                    pose.pushPose();pose.translate(x,y,0);pose.scale(size,size,size);mc.getItemRenderer().renderStatic(stack,ItemDisplayContext.GUI,LightTexture.FULL_BRIGHT,overlay,pose,CrystalGlassLayer.contents(buffers),mc.level,i);pose.popPose();
                    panelText(Component.literal(Integer.toString(stack.getCount())),x+size*.75F,y+.06F,size*.7F,pose,buffers);
                    panelText(stack.getHoverName(),x,rows?y-.11F:-.22F,(w-.65F)/count,pose,buffers);
                }
            }
            pose.popPose();
        }
        pose.popPose();
    }
    private static void panelEdge(PoseStack pose,MultiBufferSource buffers,float x,float y,float w,float h,int color){
        pose.pushPose();pose.translate(x+w/2,y+h/2,.01);pose.scale(w,h,.02F);StellarVisuals.cube(pose,buffers.getBuffer(CrystalGlassLayer.COLOR),0,0,0,.5F,color);pose.popPose();
    }
    private static void panelText(Component text,float x,float y,float width,PoseStack pose,MultiBufferSource buffers){
        var font=Minecraft.getInstance().font;float scale=Math.min(.012F,width/Math.max(1,font.width(text)+4));
        pose.pushPose();pose.translate(x,y,.04);pose.scale(scale,-scale,scale);
        font.drawInBatch8xOutline(text.getVisualOrderText(),-font.width(text)/2F,0,0xFFE7F2FF,0xFF23303A,pose.last().pose(),buffers,LightTexture.FULL_BRIGHT);pose.popPose();
    }
    private static void text(Component text,float x,float y,PoseStack pose,MultiBufferSource buffers,boolean centered){
        var font=Minecraft.getInstance().font;float scale=Math.min(.017F,3.0F/Math.max(1,font.width(text)));
        pose.pushPose();pose.translate(x,y,.015);pose.scale(scale,-scale,scale);
        font.drawInBatch8xOutline(text.getVisualOrderText(),centered?-font.width(text)/2F:0,0,0xFFE7F2FF,0xFF23303A,pose.last().pose(),buffers,LightTexture.FULL_BRIGHT);pose.popPose();
    }
    private static void item(ItemStack stack,double x,double y,double z,float scale,double angle,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(stack.isEmpty())return;pose.pushPose();pose.translate(x,y,z);pose.mulPose(Axis.YP.rotationDegrees((float)angle));pose.scale(scale,scale,scale);
        var mc=Minecraft.getInstance();var renderer=mc.getItemRenderer();
        var model=renderer.getModel(stack,mc.level,null,0);var contents=CrystalGlassLayer.contents(buffers);
        for(int copy=0;copy<MiniatureWorldMotion.stackCopies(stack.getCount());copy++){
            var offset=MiniatureWorldMotion.stackOffset(copy);
            pose.pushPose();pose.translate(offset.x(),offset.y(),offset.z());
            renderer.render(stack,ItemDisplayContext.NONE,false,pose,contents,light,overlay,model);pose.popPose();
        }
        pose.popPose();
    }
    @SubscribeEvent public static void items(RegisterClientExtensionsEvent event){event.registerItem(new IClientItemExtensions(){
        private BlockEntityWithoutLevelRenderer renderer;
        @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){
            if(renderer==null)renderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
                @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
                    var mc=Minecraft.getInstance();double time=mc.level==null?0:mc.level.getGameTime();var stage=MiniatureWorldBlockEntity.Stage.EMPTY;
                    var data=stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
                    if(data!=null)try{stage=MiniatureWorldBlockEntity.Stage.valueOf(data.copyTag().getString("Stage"));}catch(IllegalArgumentException ignored){}
                    pose.pushPose();pose.translate(.5,0,.5);pose.scale(.2F,.2F,.2F);pose.translate(-2.5,0,-2.5);
                    draw(null,stage,time,pose,buffers,light,overlay);pose.popPose();
                }
            };return renderer;
        }
    },MiniatureWorldContent.ITEM.get());}
    private MiniatureWorldClient(){}
}
