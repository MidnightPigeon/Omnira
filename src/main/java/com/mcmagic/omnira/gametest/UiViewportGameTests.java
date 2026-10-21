package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.client.screen.UiViewport;
import com.mcmagic.omnira.client.screen.GuideBookLayout;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_ui_layout")
@PrefixGameTestTemplate(false)
public final class UiViewportGameTests {
    @GameTest(template="spell_arena") public static void aspectRatioAndGuiScaleMatrix(GameTestHelper h){
        int[][] resolutions={{480,270},{640,360},{1024,768},{1280,720},{1280,800},{1600,1200},{1920,1080},{1920,1200},
                {2560,1080},{2560,1440},{3440,1440},{3840,1080},{3840,2160},{5120,1440},{720,1280},{1080,1920}};
        int[][] panels={{176,166,0,0},{176,217,0,0},{176,240,0,0},{176,241,0,0},{176,244,0,0},{176,248,0,0},
                {176,244,31,0},{176,244,0,31},{176,244,31,31},{176,244,29,0},{176,166,0,122},{376,244,31,0},
                {192,272,0,0},{200,244,0,0},{250,240,0,0},{256,220,0,0},{256,124,0,0},{268,294,0,0},
                {GuideBookLayout.WIDTH,GuideBookLayout.HEIGHT,0,0},{320,270,0,0},{188,218,0,0},{320,330,0,0}};
        int checked=0;
        for(var resolution:resolutions)for(int requested:new int[]{0,1,2,3,4,6,8})for(boolean unicode:new boolean[]{false,true}){
            int gui=1;while(gui!=requested&&resolution[0]/(gui+1)>=320&&resolution[1]/(gui+1)>=240)gui++;
            if(unicode&&gui%2!=0)gui++;
            int w=(int)Math.ceil(resolution[0]/(double)gui),height=(int)Math.ceil(resolution[1]/(double)gui);
            for(var p:panels){var view=UiViewport.layout(w,height,p[0]+p[2]+p[3],p[1]);
                int left=(view.width()-p[0]-p[2]-p[3])/2+p[2],top=(view.height()-p[1])/2;
                h.assertTrue((left-p[2])*view.scale()>=4&&(left+p[0]+p[3])*view.scale()<=w-4
                        &&top*view.scale()>=4&&(top+p[1])*view.scale()<=height-4,"Viewport or extension escaped screen");
                for(double fraction:new double[]{.05,.5,.95}){
                    double x=left+p[0]*fraction,y=top+p[1]*fraction;
                    h.assertTrue(Math.abs(x*view.scale()/view.scale()-x)<.0001&&Math.abs(y*view.scale()/view.scale()-y)<.0001,"Scaled pointer drift");
                }
                h.assertTrue(view.equals(UiViewport.layout(w,height,p[0]+p[2]+p[3],p[1])),"Rebuild changed viewport");checked++;
            }
        }
        h.assertTrue(checked==4928,"Incomplete aspect-ratio matrix");h.succeed();
    }
    @GameTest(template="spell_arena") public static void sundialPopupRemainsWithinItsPanel(GameTestHelper h){
        for(int count:new int[]{1,2,8,9,24,48,80,120,255}){
            float radius=(com.mcmagic.omnira.time.SundialLayout.choiceSide(count)*10+2)*com.mcmagic.omnira.time.SundialLayout.choiceScale(count);
            h.assertTrue(137-radius>=0&&137+radius<=200&&66-radius>=0&&66+radius<=151,"Popup crosses panel or inventory boundary");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void compactSpellHudClearsManaAndScreenEdges(GameTestHelper h){
        for(int width:new int[]{240,320,360})for(int height:new int[]{135,180,240,360,640})for(int occupied:new int[]{57,75,93,111}){
            var view=UiViewport.spellHud(width,height,occupied);
            float top=(view.y()-48)*view.scale(),bottom=(view.y()+75)*view.scale();
            h.assertTrue(top>=6&&bottom<=Math.max(20,height-occupied-6)+1,"Compact spell HUD escaped its safe area");
            h.assertTrue(92*view.scale()<Math.max(2,width/2-91)||bottom<=Math.max(2,height-occupied-2)-3,"Spell HUD overlaps raised mana bar");
        }
        var wide=UiViewport.spellHud(640,360,93);h.assertTrue(wide.scale()==1&&wide.x()==46&&wide.y()==268,"Wide HUD changed");h.succeed();
    }
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
        int[][] panels={{176,240},{238,244},{407,244},{192,272},{200,244},{GuideBookLayout.WIDTH,GuideBookLayout.HEIGHT},{320,270},{188,218},{256,220},{250,240},{268,294},{176,248},{320,330}};
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
