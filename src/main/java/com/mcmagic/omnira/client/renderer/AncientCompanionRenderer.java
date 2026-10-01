package com.mcmagic.omnira.client.renderer;

import com.google.gson.*;
import com.mcmagic.omnira.ancient.*;
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

public final class AncientCompanionRenderer<T extends AncientCompanion> extends MobRenderer<T,AncientCompanionRenderer.Model<T>> {
    private final ResourceLocation texture;
    public AncientCompanionRenderer(EntityRendererProvider.Context context, boolean spirit) {
        super(context,new Model<>(spirit),spirit ? .15F : .45F);
        texture=ResourceLocation.fromNamespaceAndPath("omnira","textures/entity/"+(spirit?"archaeopteryx_spirit":"velociraptor")+".png");
    }
    @Override public ResourceLocation getTextureLocation(T entity) { return texture; }

    public static final class Model<T extends AncientCompanion> extends EntityModel<T> {
        private final ModelPart root;
        private final boolean spirit;
        Model(boolean spirit) {
            super(spirit ? CrystalGlassLayer::entity : net.minecraft.client.renderer.RenderType::entityCutoutNoCull);
            this.spirit=spirit;
            String name=spirit?"archaeopteryx_spirit":"velociraptor";
            try (InputStream stream=AncientCompanionRenderer.class.getResourceAsStream("/assets/omnira/models/entity/"+name+".json")) {
                if(stream==null)throw new IOException("Missing companion mesh: "+name);
                JsonObject data=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
                MeshDefinition mesh=new MeshDefinition();
                Map<String,PartDefinition> parts=new HashMap<>();
                parts.put("root",mesh.getRoot());
                for(JsonElement element:data.getAsJsonArray("parts")) {
                    JsonObject part=element.getAsJsonObject();
                    CubeListBuilder cubes=CubeListBuilder.create();
                    for(JsonElement cube:part.getAsJsonArray("cubes")) {
                        JsonArray c=cube.getAsJsonArray();
                        cubes.texOffs(c.get(6).getAsInt(),c.get(7).getAsInt()).addBox(f(c,0),f(c,1),f(c,2),f(c,3),f(c,4),f(c,5));
                    }
                    JsonArray p=part.getAsJsonArray("pivot"),r=part.getAsJsonArray("rotation");
                    parts.put(part.get("name").getAsString(),parts.get(part.get("parent").getAsString()).addOrReplaceChild(
                            part.get("name").getAsString(),cubes,PartPose.offsetAndRotation(f(p,0),f(p,1),f(p,2),f(r,0),f(r,1),f(r,2))));
                }
                root=LayerDefinition.create(mesh,data.get("width").getAsInt(),data.get("height").getAsInt()).bakeRoot();
            } catch(IOException exception) { throw new IllegalStateException(exception); }
        }
        private static float f(JsonArray array,int index) { return array.get(index).getAsFloat(); }
        private ModelPart part(String name) { return root.getChild("body").getChild(name); }
        @Override public void setupAnim(T entity,float walk,float amount,float age,float yaw,float pitch) {
            root.getAllParts().forEach(ModelPart::resetPose);
            ModelPart body=root.getChild("body"),head=part("head"),tail=part("tail");
            head.yRot=yaw*Mth.DEG_TO_RAD;
            head.xRot+=pitch*Mth.DEG_TO_RAD*.6F;
            tail.yRot=Mth.sin(age*.09F)*.12F;
            if(spirit) {
                float flap=Mth.sin(age*.6F)*.65F;
                part("left_wing").zRot+=flap;
                part("right_wing").zRot-=flap;
                body.y+=Mth.sin(age*.15F)*.6F;
                body.xRot+=Mth.sin(age*.1F)*.04F;
            } else {
                part("left_leg").xRot=Mth.cos(walk*.6662F)*1.2F*amount;
                part("right_leg").xRot=Mth.cos(walk*.6662F+Mth.PI)*1.2F*amount;
                if(entity.isInSittingPose()) { body.y+=5; part("left_leg").xRot=-1;part("right_leg").xRot=-1; }
                head.getChild("jaw").xRot=((Velociraptor)entity).bite(0)*.6F;
            }
        }
        @Override public void renderToBuffer(PoseStack pose,VertexConsumer consumer,int light,int overlay,int color) {
            root.render(pose,consumer,light,overlay,color);
        }
    }
}
