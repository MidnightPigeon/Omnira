package com.mcmagic.omnira.client.renderer;

import com.google.gson.*;
import com.mcmagic.omnira.space.sea.*;
import com.mojang.blaze3d.vertex.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class StarSeaCreatureRenderer<T extends StarSeaCreature> extends MobRenderer<T,StarSeaCreatureRenderer.Model<T>> {
    private final ResourceLocation texture;
    public StarSeaCreatureRenderer(EntityRendererProvider.Context context,boolean jelly){
        super(context,new Model<>(jelly),jelly?.25F:.8F);texture=ResourceLocation.parse("omnira:textures/entity/"+(jelly?"star_jellyfish":"void_manta")+".png");
    }
    @Override public ResourceLocation getTextureLocation(T entity){return texture;}
    @Override protected int getBlockLightLevel(T entity,net.minecraft.core.BlockPos p){return Math.max(super.getBlockLightLevel(entity,p),entity instanceof StarJellyfish?10:5);}
    public static final class Model<T extends StarSeaCreature> extends EntityModel<T> {
        private final ModelPart root;
        private final boolean jelly;
        Model(boolean jelly){
            super(jelly?CrystalGlassLayer::entity:net.minecraft.client.renderer.RenderType::entityCutoutNoCull);this.jelly=jelly;
            String name=jelly?"star_jellyfish":"void_manta";
            try(var stream=StarSeaCreatureRenderer.class.getResourceAsStream("/assets/omnira/models/entity/"+name+".json")){
                if(stream==null)throw new IOException("Missing mesh "+name);
                var data=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
                MeshDefinition mesh=new MeshDefinition();Map<String,PartDefinition> parts=new HashMap<>();parts.put("root",mesh.getRoot());
                for(var element:data.getAsJsonArray("parts")){
                    var part=element.getAsJsonObject();var cubes=CubeListBuilder.create();
                    for(var cube:part.getAsJsonArray("cubes")){var c=cube.getAsJsonArray();cubes.texOffs(c.get(6).getAsInt(),c.get(7).getAsInt()).addBox(f(c,0),f(c,1),f(c,2),f(c,3),f(c,4),f(c,5));}
                    var p=part.getAsJsonArray("pivot");
                    parts.put(part.get("name").getAsString(),parts.get(part.get("parent").getAsString()).addOrReplaceChild(part.get("name").getAsString(),cubes,PartPose.offset(f(p,0),f(p,1),f(p,2))));
                }
                root=LayerDefinition.create(mesh,data.get("width").getAsInt(),data.get("height").getAsInt()).bakeRoot();
            }catch(IOException ex){throw new IllegalStateException(ex);}
        }
        private static float f(JsonArray a,int n){return a.get(n).getAsFloat();}
        @Override public void setupAnim(T entity,float walk,float amount,float age,float yaw,float pitch){
            root.getAllParts().forEach(ModelPart::resetPose);var body=root.getChild("body");
            if(jelly){
                double vy=entity.getDeltaMovement().y,horizontal=entity.getDeltaMovement().horizontalDistance();
                animateJellyfish(age,((StarJellyfish)entity).charge()/20F);
                body.xRot=(float)Math.clamp(horizontal*2-vy*.8,-.45,.65);
            }else{
                float flap=Mth.sin(age*(entity.isInWater()?.12F:.19F))*.24F;
                body.getChild("left_wing").zRot=flap;body.getChild("right_wing").zRot=-flap;
                body.getChild("tail").yRot=Mth.sin(age*.08F)*.12F;
                body.y+=Mth.sin(age*.12F)*.3F;
            }
        }
        void animateJellyfish(float age,float charge){
            root.getAllParts().forEach(ModelPart::resetPose);var body=root.getChild("body");
            float pulse=Mth.sin(age*.16F);
            body.xScale=body.zScale=1+pulse*.055F-charge*.12F;body.yScale=1-pulse*.045F;
            for(int i=0;i<8;i++){var tentacle=body.getChild("tentacle"+i);tentacle.xRot=Mth.sin(age*.12F+i*.8F)*.19F;tentacle.zRot=Mth.cos(age*.12F+i*.8F)*.19F;}
        }
        @Override public void renderToBuffer(PoseStack p,VertexConsumer c,int light,int overlay,int color){root.render(p,c,light,overlay,color);}
    }
}
