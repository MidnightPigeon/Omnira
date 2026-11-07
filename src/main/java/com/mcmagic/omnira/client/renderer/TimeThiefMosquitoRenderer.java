package com.mcmagic.omnira.client.renderer;

import com.google.gson.*;
import com.mcmagic.omnira.entity.TimeThiefMosquito;
import com.mojang.blaze3d.vertex.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class TimeThiefMosquitoRenderer extends MobRenderer<TimeThiefMosquito,TimeThiefMosquitoRenderer.Model> {
    public static final ResourceLocation TEXTURE=ResourceLocation.parse("omnira:textures/entity/time_thief_mosquito.png");
    public TimeThiefMosquitoRenderer(EntityRendererProvider.Context context){super(context,new Model(),.22F);}
    @Override public ResourceLocation getTextureLocation(TimeThiefMosquito entity){return TEXTURE;}
    @Override protected net.minecraft.client.renderer.RenderType getRenderType(TimeThiefMosquito entity,boolean visible,boolean translucent,boolean glowing){
        return entity.opacity(0)<1 ? CrystalGlassLayer.entity(TEXTURE) : super.getRenderType(entity,visible,translucent,glowing);
    }
    public static final class Model extends EntityModel<TimeThiefMosquito> {
        private final ModelPart root;
        private float opacity=1;
        Model(){
            super(net.minecraft.client.renderer.RenderType::entityCutoutNoCull);
            try(var stream=TimeThiefMosquitoRenderer.class.getResourceAsStream("/assets/omnira/models/entity/time_thief_mosquito.json")){
                if(stream==null)throw new IOException("Missing mosquito mesh");
                var data=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
                var mesh=new MeshDefinition();Map<String,PartDefinition> parts=new HashMap<>();parts.put("root",mesh.getRoot());
                for(var element:data.getAsJsonArray("parts")){
                    var part=element.getAsJsonObject();var cubes=CubeListBuilder.create();
                    for(var cube:part.getAsJsonArray("cubes")){var c=cube.getAsJsonArray();cubes.texOffs(c.get(6).getAsInt(),c.get(7).getAsInt()).addBox(f(c,0),f(c,1),f(c,2),f(c,3),f(c,4),f(c,5));}
                    var p=part.getAsJsonArray("pivot");var a=part.getAsJsonArray("rotation");
                    parts.put(part.get("name").getAsString(),parts.get(part.get("parent").getAsString()).addOrReplaceChild(part.get("name").getAsString(),cubes,PartPose.offsetAndRotation(f(p,0),f(p,1),f(p,2),f(a,0),f(a,1),f(a,2))));
                }
                root=LayerDefinition.create(mesh,data.get("width").getAsInt(),data.get("height").getAsInt()).bakeRoot();
            }catch(IOException ex){throw new IllegalStateException(ex);}
        }
        private static float f(JsonArray a,int n){return a.get(n).getAsFloat();}
        @Override public void setupAnim(TimeThiefMosquito entity,float walk,float amount,float age,float yaw,float pitch){
            root.getAllParts().forEach(ModelPart::resetPose);var body=root.getChild("body");
            float partial=age-entity.tickCount;opacity=entity.opacity(partial);
            float flap=(float)Math.sin(age*2.2F)*(entity.attached()?.12F:.55F);
            body.getChild("wing_left").zRot+=flap;body.getChild("wing_right").zRot-=flap;
            if(!entity.attached())body.y+=(float)Math.sin(age*.14F)*.45F;
            else {
                body.xRot=-.35F;
                // Leave the proboscis in contact while keeping the face and monocle outside the host.
                body.z+=8;
            }
            for(int i=0;i<6;i++)body.getChild("leg"+i).xRot+=(float)Math.sin(age*.17F+i)*.08F;
        }
        @Override public void renderToBuffer(PoseStack pose,VertexConsumer consumer,int light,int overlay,int color){
            int alpha=(int)((color>>>24)*opacity);root.render(pose,consumer,light,overlay,(color&0xFFFFFF)|(alpha<<24));
        }
    }
}
