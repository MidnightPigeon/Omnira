package com.mcmagic.omnira.client.renderer;

import com.google.gson.*;
import com.mcmagic.omnira.entity.spacetime.*;
import com.mojang.blaze3d.vertex.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.PathfinderMob;

/** Shared native cuboid loader; opaque anatomy and crystal membranes use separate passes. */
public final class SpacetimeFaunaRenderer<T extends PathfinderMob> extends MobRenderer<T,SpacetimeFaunaRenderer.Model<T>> {
    private final ResourceLocation texture;
    public SpacetimeFaunaRenderer(EntityRendererProvider.Context context,String id,float shadow){
        super(context,new Model<>(id),shadow);texture=ResourceLocation.parse("omnira:textures/entity/"+id+".png");
        addLayer(new RenderLayer<T,Model<T>>(this){
            @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,T entity,float walk,float amount,float partial,float age,float yaw,float pitch){
                if(!entity.isInvisible()){
                    var out=buffers.getBuffer(CrystalGlassLayer.entity(texture));
                    getParentModel().renderPass(pose,out,light,OverlayTexture.NO_OVERLAY,-1,true);
                    if(entity instanceof RiftstepDeer)getParentModel().renderLuminousDetails(pose,out);
                }
            }
        });
    }
    @Override public ResourceLocation getTextureLocation(T entity){return texture;}
    public static final class Model<T extends PathfinderMob> extends EntityModel<T> {
        private final ModelPart root;
        private final Map<String,ModelPart> parts=new HashMap<>();
        private final Set<String> glass=new HashSet<>();
        private final String id;
        private int wingColor=-1;
        private boolean glowingAntlers;
        private static boolean antler(String name){return name.startsWith("antler")||name.startsWith("branch")||name.startsWith("tine");}
        Model(String id){
            super(RenderType::entityCutoutNoCull);this.id=id;
            try(var stream=SpacetimeFaunaRenderer.class.getResourceAsStream("/assets/omnira/models/entity/"+id+".json")){
                if(stream==null)throw new IOException("Missing mesh "+id);
                var data=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
                var mesh=new MeshDefinition();Map<String,PartDefinition> definitions=new HashMap<>();definitions.put("root",mesh.getRoot());
                for(var element:data.getAsJsonArray("parts")){
                    var p=element.getAsJsonObject();String name=p.get("name").getAsString();var cubes=CubeListBuilder.create();
                    for(var value:p.getAsJsonArray("cubes")){var c=value.getAsJsonArray();cubes.texOffs(c.get(6).getAsInt(),c.get(7).getAsInt()).addBox(f(c,0),f(c,1),f(c,2),f(c,3),f(c,4),f(c,5));}
                    var pivot=p.getAsJsonArray("pivot");var a=p.getAsJsonArray("rotation");
                    definitions.put(name,definitions.get(p.get("parent").getAsString()).addOrReplaceChild(name,cubes,PartPose.offsetAndRotation(f(pivot,0),f(pivot,1),f(pivot,2),f(a,0),f(a,1),f(a,2))));
                    if(p.has("glass")&&p.get("glass").getAsBoolean())glass.add(name);
                }
                root=LayerDefinition.create(mesh,data.get("width").getAsInt(),data.get("height").getAsInt()).bakeRoot();parts.put("root",root);
                for(var element:data.getAsJsonArray("parts")){var p=element.getAsJsonObject();String name=p.get("name").getAsString();parts.put(name,parts.get(p.get("parent").getAsString()).getChild(name));}
            }catch(IOException ex){throw new IllegalStateException(ex);}
        }
        private static float f(JsonArray a,int n){return a.get(n).getAsFloat();}
        @Override public void setupAnim(T entity,float walk,float amount,float age,float yaw,float pitch){
            root.getAllParts().forEach(ModelPart::resetPose);wingColor=-1;glowingAntlers=false;
            if(entity instanceof AgecoilSnail snail){
                int ticks=snail.shelterTicks();
                if(ticks>0){
                    float remaining=ticks-Math.clamp(age-entity.tickCount,0F,1F);
                    float tuck=Math.clamp(Math.min(20F-remaining,remaining)/3F,0F,1F);
                    tuck=tuck*tuck*(3F-2F*tuck);
                    var head=parts.get("head");
                    head.xScale=head.yScale=head.zScale=1F-.55F*tuck;
                    head.z+=7F*tuck;
                    head.y-=4F*tuck;
                    var foot=parts.get("foot");
                    foot.xScale=1F-.55F*tuck;
                    foot.zScale=1F-.65F*tuck;
                    foot.y-=4F*tuck;
                    foot.z+=tuck;
                    parts.get("body").y+=2F*tuck;
                }
                for(int sign=-1;sign<=1;sign+=2){
                    parts.get("clock_minute"+sign).xRot+=sign*age*(float)(Math.PI*2/1200);
                    parts.get("clock_hour"+sign).xRot+=sign*age*(float)(Math.PI*2/7200);
                }
                parts.get("antenna_left").xRot+=(float)Math.sin(age*.08)*.12F;parts.get("antenna_right").xRot-=(float)Math.sin(age*.08)*.12F;
            }else if(entity instanceof RiftstepDeer deer){
                glowingAntlers=deer.angry();
                for(int i=0;i<8;i++){
                    var mote=parts.get("mote"+i);
                    // Stagger the drifting centers along the torso so the whole cloud never converges.
                    double time=age+entity.getId()*13.0;
                    double phase=i*2.39996;
                    mote.z=-4.2F+i*1.34F+(float)Math.sin(time*(.023+i*.001)+phase)*.55F;
                    mote.x=(float)(1.55*Math.sin(phase)+.85*Math.sin(time*(.031+i*.002)+phase*2));
                    mote.y=.45F+(float)(.95*Math.cos(phase)+.85*Math.sin(time*(.019+i*.003)+phase));
                    float size=.52F+(i*3%7)*.055F;
                    mote.xScale=mote.yScale=mote.zScale=size;
                    mote.yRot=(float)(phase+time*.023);
                }
                float lower=deer.headLowering(age-entity.tickCount);
                parts.get("shoulders").xRot=.85F*lower;
                parts.get("head").yRot=yaw*(float)Math.PI/180;parts.get("head").xRot+=pitch*(float)Math.PI/180*(1-lower)+.25F*lower;
                // Vanilla walk accumulates horizontal travel. Keep full strides at walking speed.
                float stride=Math.min(1F,amount*4F);
                for(int i=0;i<4;i++){
                    double phase=walk*1.25+(i==0||i==3?0:Math.PI);
                    var leg=parts.get("leg"+i);
                    leg.xRot+=(float)Math.cos(phase)*stride*.65F;
                    leg.y-=(float)Math.max(0,Math.sin(phase))*stride*.8F;
                }
            }else if(entity instanceof RiftMantis mantis){
                for(int i=0;i<4;i++)parts.get("leg"+i).xRot+=(float)Math.cos(walk*.8+i*Math.PI)*amount*.3F;
                float raise=mantis.windup()>0?.7F:0;parts.get("scythe_left").xRot-=raise;parts.get("scythe_right").xRot-=raise;
                parts.get("scythe_left").yRot-=raise*.42F; parts.get("scythe_right").yRot+=raise*.42F;
                parts.get("head").yRot=yaw*.01F;
            }else if(entity instanceof RiftMoth moth){
                float flap=(float)Math.sin(age*.65F)*.55F;parts.get("wing_left").zRot+=flap;parts.get("wing_right").zRot-=flap;
                parts.get("body").y+=(float)Math.sin(age*.13)*.35F;
                int region=moth.region();float blend=region==1?0:region==2?1:(float)(Math.sin(age*.035)+1)*.5F;
                int r=(int)(224-68*blend),g=(int)(246-41*blend),b=(int)(174+81*blend);wingColor=0xD0000000|r<<16|g<<8|b;
            }
        }
        public void renderPass(PoseStack pose,VertexConsumer out,int light,int overlay,int color,boolean translucent){
            parts.forEach((name,part)->part.skipDraw=glass.contains(name)!=translucent||(glowingAntlers&&antler(name))||name.startsWith("mote"));
            root.render(pose,out,light,overlay,translucent?wingColor:color);
        }
        public void renderLuminousDetails(PoseStack pose,VertexConsumer out){
            parts.forEach((name,part)->part.skipDraw=!(name.startsWith("mote")||(glowingAntlers&&antler(name))));
            root.render(pose,out,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,-1);
        }
        @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,int color){renderPass(pose,out,light,overlay,color,false);}
    }
}
