package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.client.screen.EngineGuideTimeline;
import com.mcmagic.omnira.client.screen.VanillaFrameLayout;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_ui_consistency")
@PrefixGameTestTemplate(false)
public final class UiConsistencyGameTests {
    @GameTest(template="spell_arena") public static void containerFramesCloseAtEveryUsedWidth(GameTestHelper h){
        for(int width:new int[]{18,104,176,192,250,256,268,376})for(int height:new int[]{18,90,98,124,166,220,240}){
            var coverage=new int[width*height];
            VanillaFrameLayout.draw(width,height,(x,y,w,hh,u,v,sw,sh)->{
                h.assertTrue(x>=0&&y>=0&&x+w<=width&&y+hh<=height&&u>=0&&v>=0&&u+sw<=176&&v+sh<=166,"Frame blit escaped bounds");
                h.assertTrue(w==sw&&((x==0||x==width-4)&&y==4?sh==1:hh==sh),"Frame corners or top bevel were stretched");
                for(int yy=y;yy<y+hh;yy++)for(int xx=x;xx<x+w;xx++)coverage[yy*width+xx]++;
            });
            for(int y=0;y<height;y++)for(int x=0;x<width;x++)
                h.assertTrue(coverage[y*width+x]==(x<4||x>=width-4||y<4||y>=height-4?1:0),"Frame has a gap, duplicate or painted interior at "+width+"x"+height);
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void standardFrameKeepsOriginalVanillaPixels(GameTestHelper h){
        int width=176,height=240;var pixels=new int[width*height];
        VanillaFrameLayout.draw(width,height,(x,y,w,hh,u,v,sw,sh)->{
            for(int yy=0;yy<hh;yy++)for(int xx=0;xx<w;xx++)pixels[(y+yy)*width+x+xx]=(u+xx)+256*(v+yy*sh/hh)+1;
        });
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            int expected=y<4?x+256*y+1:y>=height-4?x+256*(162+y-height+4)+1:x<4||x>=172?x+256*50+1:0;
            h.assertTrue(pixels[y*width+x]==expected,"Original 176-wide frame changed");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void enginePreviewModesAndPlaybackStateAgree(GameTestHelper h){
        for(int stage=0;stage<EngineGuideTimeline.STAGES;stage++)for(boolean electrical:new boolean[]{false,true}){
            boolean electric=EngineGuideTimeline.electricOutput(stage,electrical);
            h.assertTrue(electric==(electrical&&stage!=4),"Native assembly preview advertises FE output");
            h.assertTrue(EngineGuideTimeline.outputUnit(electric).equals(electric?"FE/s":"MPU"),"Preview retains old SU unit");
        }
        h.assertTrue(EngineGuideTimeline.playbackGlyph(true).equals(">")&&EngineGuideTimeline.playbackGlyph(false).equals("||"),"Rebuilt paused control has wrong glyph");h.succeed();
    }
}
