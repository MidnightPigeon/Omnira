package com.mcmagic.omnira.client.renderer;

/** The same six-tick lift, impact and return are used in-world and in the guide. */
public final class AssemblyHammerMotion {
    private static final double[][] KEYS={{0,0,0,0},{.3,2,-5,25},{.55,0,-5,-16},{.75,2,-5,15},{1,0,0,0}};
    public record Pose(double lift,double reach,double swing) {}
    public static Pose at(double age) {
        if(age<0 || age>=6)return new Pose(0,0,0);
        double t=age/6;
        for(int i=1;i<KEYS.length;i++)if(t<=KEYS[i][0]) {
            var a=KEYS[i-1];var b=KEYS[i];double u=(t-a[0])/(b[0]-a[0]);u=u*u*(3-2*u);
            return new Pose(a[1]+(b[1]-a[1])*u,a[2]+(b[2]-a[2])*u,a[3]+(b[3]-a[3])*u);
        }
        return new Pose(0,0,0);
    }
    private AssemblyHammerMotion() {}
}
