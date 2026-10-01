package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.spell.visual.SpatialBladeGeometry;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_blade_visual")
@PrefixGameTestTemplate(false)
public final class SpatialBladeVisualGameTests {
    @GameTest(template="spell_arena")
    public static void horizontalBladeFollowsEveryLaunchDirection(GameTestHelper h){
        for(int yaw=0;yaw<360;yaw+=45)for(int pitch:new int[]{-90,-60,0,60,90}){
            double y=Math.toRadians(yaw),p=Math.toRadians(pitch);
            var forward=new Vec3(-Math.sin(y)*Math.cos(p),-Math.sin(p),Math.cos(y)*Math.cos(p));
            var basis=SpatialBladeGeometry.basis(forward);
            h.assertTrue(basis.point(0,0,1).distanceTo(forward)<1e-7,"Visual forward differs from flight direction");
            h.assertTrue(Math.abs(basis.right().dot(forward))<1e-7,"Blade is not transverse");
            h.assertTrue(Math.abs(basis.up().dot(forward))<1e-7&&Math.abs(basis.up().dot(basis.right()))<1e-7,"Non-orthogonal blade axes");
            h.assertTrue(Math.abs(basis.right().y)<1e-7,"Horizontal span rolled vertically");
            var top=basis.point(.5,SpatialBladeGeometry.THICKNESS/2,-.2);
            var bottom=basis.point(.5,-SpatialBladeGeometry.THICKNESS/2,-.2);
            h.assertTrue(Math.abs(top.distanceTo(bottom)-1.0/32)<1e-7,"Blade thickness drifted");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void crescentAndTrailingRibbonStayBehindBlade(GameTestHelper h){
        h.assertTrue(SpatialBladeGeometry.THICKNESS<.04,"Blade remains too thick");
        for(int column=0;column<8;column++){
            h.assertTrue(SpatialBladeGeometry.front(column)==SpatialBladeGeometry.front(7-column),"Crescent edge asymmetric");
            h.assertTrue(SpatialBladeGeometry.back(column)<SpatialBladeGeometry.front(column),"Inverted blade ribbon");
        }
        for(float age:new float[]{0,4,8,9,10,16,100}){
            double length=SpatialBladeGeometry.trailLength(age,8);
            h.assertTrue(length>=0&&length<=2.4,"Unbounded trail");
            if(age<=8)h.assertTrue(length==0,"Trail appears before launch");
            var basis=SpatialBladeGeometry.basis(new Vec3(1,1,1));
            var edge=basis.point(0,0,SpatialBladeGeometry.back(3));
            var tail=basis.point(0,0,SpatialBladeGeometry.back(3)-length);
            h.assertTrue(tail.subtract(edge).dot(basis.forward())<=1e-7,"Trail points ahead instead of backward");
        }
        h.succeed();
    }
}
