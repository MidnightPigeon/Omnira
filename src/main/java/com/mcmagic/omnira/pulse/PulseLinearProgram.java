package com.mcmagic.omnira.pulse;

import java.util.List;

/** Two-phase simplex used only for source-dependent, lossy transport constraints. */
final class PulseLinearProgram {
    private static final double EPS=1e-9;
    private int m;private final int n;private int[] basic;private final int[] nonbasic;private double[][] t;
    PulseLinearProgram(List<double[]> a,List<Double> b,double[] c){
        m=b.size();n=c.length;basic=new int[m];nonbasic=new int[n+1];t=new double[m+2][n+2];
        for(int i=0;i<m;i++){System.arraycopy(a.get(i),0,t[i],0,n);basic[i]=n+i;t[i][n]=-1;t[i][n+1]=b.get(i);}
        for(int j=0;j<n;j++){nonbasic[j]=j;t[m][j]=-c[j];}nonbasic[n]=-1;t[m+1][n]=1;
    }
    private void pivot(int r,int s){
        double inverse=1/t[r][s];
        for(int i=0;i<m+2;i++)if(i!=r)for(int j=0;j<n+2;j++)if(j!=s)t[i][j]-=t[r][j]*t[i][s]*inverse;
        for(int j=0;j<n+2;j++)if(j!=s)t[r][j]*=inverse;
        for(int i=0;i<m+2;i++)if(i!=r)t[i][s]*=-inverse;
        t[r][s]=inverse;int old=basic[r];basic[r]=nonbasic[s];nonbasic[s]=old;
    }
    private boolean optimize(int phase){
        int row=phase==1?m+1:m;
        for(int iteration=0;iteration<100000;iteration++){
            int s=-1;
            for(int j=0;j<=n;j++)if(!(phase==2&&nonbasic[j]==-1)&&t[row][j]<-EPS&&(s==-1||nonbasic[j]<nonbasic[s]))s=j;
            if(s==-1)return true;
            int r=-1;
            for(int i=0;i<m;i++)if(t[i][s]>EPS&&(r==-1||t[i][n+1]/t[i][s]<t[r][n+1]/t[r][s]-EPS
                    ||Math.abs(t[i][n+1]/t[i][s]-t[r][n+1]/t[r][s])<=EPS&&basic[i]<basic[r]))r=i;
            if(r==-1)return false;pivot(r,s);
        }
        throw new IllegalStateException("Pulse transport did not converge");
    }
    double[] solve(){
        int r=0;for(int i=1;i<m;i++)if(t[i][n+1]<t[r][n+1])r=i;
        if(m>0&&t[r][n+1]<-EPS){
            pivot(r,n);if(!optimize(1)||Math.abs(t[m+1][n+1])>EPS)return null;
            for(int i=0;i<m;i++)if(basic[i]==-1){
                int s=-1;for(int j=0;j<=n;j++)if(Math.abs(t[i][j])>EPS&&(s==-1||nonbasic[j]<nonbasic[s]))s=j;
                if(s!=-1)pivot(i,s);
            }
        }
        if(!optimize(2))throw new IllegalStateException("Unbounded pulse transport");
        return solution();
    }
    /** Keep the feasible basis while freezing a previous optimum and selecting the next load. */
    void constrain(double[] coefficients,double bound){
        var row=new double[n+2];row[n+1]=bound;
        for(int j=0;j<=n;j++)if(nonbasic[j]>=0&&nonbasic[j]<n)row[j]=coefficients[nonbasic[j]];
        for(int i=0;i<m;i++)if(basic[i]>=0&&basic[i]<n){double coefficient=coefficients[basic[i]];
            for(int j=0;j<n+2;j++)row[j]-=coefficient*t[i][j];}
        if(row[n+1]<-1e-6)throw new IllegalArgumentException("New pulse constraint excludes the current optimum");
        row[n+1]=Math.max(0,row[n+1]);var next=new double[m+3][];System.arraycopy(t,0,next,0,m);
        next[m]=row;next[m+1]=t[m];next[m+2]=t[m+1];t=next;basic=java.util.Arrays.copyOf(basic,m+1);basic[m]=n+m;m++;
    }
    double[] maximize(double[] objective){
        java.util.Arrays.fill(t[m],0);
        for(int j=0;j<=n;j++)if(nonbasic[j]>=0&&nonbasic[j]<n)t[m][j]=-objective[nonbasic[j]];
        for(int i=0;i<m;i++)if(basic[i]>=0&&basic[i]<n){double coefficient=objective[basic[i]];
            for(int j=0;j<n+2;j++)t[m][j]+=coefficient*t[i][j];}
        if(!optimize(2))throw new IllegalStateException("Unbounded pulse transport");return solution();
    }
    private double[] solution(){double[] result=new double[n];for(int i=0;i<m;i++)if(basic[i]>=0&&basic[i]<n)result[basic[i]]=Math.max(0,t[i][n+1]);return result;}
}
