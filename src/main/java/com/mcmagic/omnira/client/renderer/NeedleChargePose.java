package com.mcmagic.omnira.client.renderer;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Camera-space grip and orientation; shared with the offline pose check. */
public final class NeedleChargePose {
    public record Frame(double x,double y,double z,Quaternionf rotation) {}
    public static Frame frame(boolean left,float charge,double time) {
        int side=left?-1:1;float blend=Math.min(1,charge*4);
        double x=side*(.63+(.30-.63)*blend),y=-.60+(.32)*blend,z=-.76+(-.06+.12*charge)*blend;
        x+=Math.sin(time*2.8)*.012*charge;y+=Math.cos(time*3.1)*.008*charge;
        var direction=new Vector3f((float)-x,(float)-y,(float)(-8-z)).normalize();
        var aim=new Quaternionf().rotationTo(new Vector3f(0,1,0),direction);
        var rest=new Quaternionf().rotationY((float)Math.toRadians(-90)).rotateZ((float)Math.toRadians(-20));
        rest.slerp(aim,blend);
        return new Frame(x,y,z,rest);
    }
    private NeedleChargePose() {}
}
