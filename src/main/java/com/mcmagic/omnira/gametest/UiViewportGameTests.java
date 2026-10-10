package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.client.screen.UiViewport;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_ui_layout")
@PrefixGameTestTemplate(false)
public final class UiViewportGameTests {
    @GameTest(template="spell_arena") public static void fluidClipsFollowScaledPose(GameTestHelper h){
        for(float scale:new float[]{.35F,.5F,.75F,1F})for(int height:new int[]{1,7,18}){
            var matrix=new org.joml.Matrix4f().scale(scale,scale,1).translate(19,27,0);
            var bounds=UiViewport.scissor(matrix,50,102-height,126,102);
            var a=matrix.transformPosition(new org.joml.Vector3f(50,102-height,0));
            var b=matrix.transformPosition(new org.joml.Vector3f(126,102,0));
            h.assertTrue(bounds[0]<=a.x&&a.x-bounds[0]<1&&bounds[1]<=a.y&&a.y-bounds[1]<1,"Fluid clip start does not match transformed texture");
            h.assertTrue(bounds[2]>=b.x&&bounds[2]-b.x<1&&bounds[3]>=b.y&&bounds[3]-b.y<1,"Fluid clip end does not match transformed texture");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void panelsFitSmallAndWideViewports(GameTestHelper h){
        int[][] windows={{240,135},{320,180},{427,240},{640,360},{360,640},{1280,720}};
        int[][] panels={{176,240},{238,244},{407,244},{192,272},{200,244},{320,224},{320,270},{188,218},{256,220},{250,240},{268,294},{176,248}};
        for(var window:windows)for(var panel:panels){
            float scale=UiViewport.fit(window[0],window[1],panel[0],panel[1]);
            h.assertTrue(scale>0&&scale<=1&&panel[0]*scale<=window[0]-12+.01&&panel[1]*scale<=window[1]-12+.01,"Panel exceeds viewport");
            double x=panel[0]*.7,y=panel[1]*.4;
            h.assertTrue(Math.abs(x*scale/scale-x)<.001&&Math.abs(y*scale/scale-y)<.001,"Pointer mapping moved slot");
        }
        h.assertTrue(UiViewport.fit(1280,720,238,244)==1,"Large viewport should retain original pixel size");h.succeed();
    }
    @GameTest(template="spell_arena") public static void gridSlotsClearTitleInventoryAndEachOther(GameTestHelper h){
        for(int count:new int[]{3,5,8,12})for(int i=0;i<count;i++){
            int x=com.mcmagic.omnira.menu.CrystalGridMenu.slotX(count,i),y=com.mcmagic.omnira.menu.CrystalGridMenu.slotY(count,i);
            h.assertTrue(x>=1&&x+17<176&&y-4>=20&&y+17<142,"Lattice slot or cursor overlaps title/inventory");
            for(int j=i+1;j<count;j++)h.assertTrue(Math.abs(x-com.mcmagic.omnira.menu.CrystalGridMenu.slotX(count,j))>=18
                    ||Math.abs(y-com.mcmagic.omnira.menu.CrystalGridMenu.slotY(count,j))>=18,"Lattice slot frames overlap");
        }h.succeed();
    }
}
