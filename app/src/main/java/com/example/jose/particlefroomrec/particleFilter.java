package com.example.jose.particlefroomrec;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import weka.classifiers.Classifier;
import weka.classifiers.lazy.KStar;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Created by jose on 4/18/2017.
 */

public class particleFilter {


    private static final double sigma_u=Math.sqrt(5);
    private static final int sigma_pl=8;
      Context context;
     public particle[] particleList;
    private double[] distance;
    private double[] m;
    wifiANs[] AN;
    //Machine Learning Classification Model
    Classifier cls;//=new KStar();



    private static final int numberParticles=1000;             //Number of particles to use
    int numberAN;
    public particleFilter(Context pcontext,wifiANs[]pAN){
        context=pcontext;
        numberAN=pAN.length;
        AN=pAN;
        distance=new double[numberAN];
        m=new double[numberAN];
        particleList = new particle[numberParticles];
        for (int i = 0; i < numberParticles; i++) {
            particleList[i] = new particle(numberAN);
            particleList[i].weight =1.0f/numberParticles; //initial weight, same in this particle
        }
        spreadParticles();
    }
    public void spreadParticles(){
        Random rand = new Random();
        int position;
        for (int i = 0; i < numberParticles; i++) {
          for(int j=0;j<numberAN;j++){
               particleList[i].indLikehood[j]=1;
            }

            particleList[i].x= rand.nextInt(18)+0; //X coordinate
            particleList[i].y= rand.nextInt(16)+0;//Y coordinate
            particleList[i].weight =1.0f/numberParticles; //initial weight, same in this particle
        }

    }


    //Particle without movement
    //RSS[] contains WiFi received signal strength
    //earthMag[] contains magnetic field values on earth coordinates
    public double[] stateUpdate(double RSS[],float earthMag[]){
        int predictedRoom;
        double[] state =new double[11]; //0,1 x and y coordinates, the remaining are the room histogram
        //1. Update particle state (movement model)
        //To implement
        //2. Calculate individual likehood (ranging)
        this.distanceRSSI(RSS); //calculate distance NLR
        this.indLikehood();

        //3. Calculate likelihood based on room recognition
        //this.roomLikelihood(RSS,earthMag); //Not implemented
        //4.Update unnormalized weights
        this.unnormalizedWeights();
        //5. Normalized weights.
        this.normalizedWeights();
        //6. Resampling
        this.systematicResampling();
        //7. Calculate Neff
        while (this.neff() < (0.5 * numberParticles)) {
            this.systematicResampling();
        }
        //8. Compute the estimated state
        for (int i = 0; i < numberParticles; i++) {
            state[0] = state[0] + particleList[i].weight * particleList[i].x;
            state[1] = state[1] + particleList[i].weight * particleList[i].y;

        }
        return state;
    }



    //public void systematic resampling cumulative weights
    public void systematicResampling(){
        //Log.d(TAG,"RESAMPLING");
        try {
            ArrayList<particle> particleLists = new ArrayList<>();
            double threshold = 0;  //to delete particles with weight zero
            double sum = 0;

            for (int i = 0; i < numberParticles; i++) {
                if (particleList[i].weight > threshold) {
                    particleLists.add(particleList[i]);
                }
            }

            //Normalize weights
            double totalWeight=0;
            for (int i = 0; i < particleLists.size(); i++) {
                totalWeight=totalWeight+particleLists.get(i).weight;
            }

            for (int i = 0; i < particleLists.size(); i++) {
                particleLists.get(i).weight=particleLists.get(i).weight/totalWeight;
            }
            //Cumulative sum

            double[] cumSum=new double[particleLists.size()];
            for (int i = 0; i < particleLists.size(); i++) {
                sum=sum+particleLists.get(i).weight;
                cumSum[i]=sum;
            }
            double[] index=new double[numberParticles];
            for(int i=0;i < numberParticles; i++) {
                index[i] =  Math.random();
            }
            //Find the particle to be duplicated
            // particle[] particleListT=new particle[numberParticles];
            for(int i=0;i <index.length  ;i++) {
                for(int j=0;j<cumSum.length-1;j++){
                    if (index[i]>=cumSum[j] && index[i]<cumSum[j+1]){
                        // particleListT[i] = particleLists.get(j+1);
                        particleList[i].x=particleLists.get(j+1).x;
                        particleList[i].y=particleLists.get(j+1).y;
                    }
                }
            }
            //Assign the same weight to each particle after resampling
            for (int i = 0; i < numberParticles; i++) {
                particleList[i].weight =1.0f/numberParticles; //initial weight, same in this particle

            }
            //normalizedWeights();
            // particleList=particleListT;
        }catch (Exception e)
        {
            Log.d("Resampling","ERROR en RESAMPLING: "+e.getMessage());
        }
    }


    //6. Calculate Neff to resampling
    public double neff(){
        double neff=0;
        for (int i = 0; i < numberParticles; i++) {
            neff=neff+ Math.pow(particleList[i].weight,2);
        }
        return 1/neff;
    }
    //5.Normalize weights
    public void normalizedWeights(){
        double totalWeight=0;
        for (int i = 0; i < numberParticles; i++) {
            totalWeight=totalWeight+particleList[i].weight;
        }
        for (int i = 0; i < numberParticles; i++) {
            particleList[i].weight=particleList[i].weight/totalWeight;
        }
        //  Log.d(TAG,"Total: "+totalWeight);
    }
    //4. Update unnormalized weights
    public void unnormalizedWeights(){
        double W;
        for (int i = 0; i < numberParticles; i++) {
        W=1;
        for(int j=0;j<numberAN;j++) {
              W =W* Math.pow(particleList[i].indLikehood[j],m[j]);  //Weighted distance.
        }
            /*******    Included NLR likelihood and room recognition  ******/
          particleList[i].weight=W;
        }
    }

    //2. Calculate individual likelihood distance
    public void indLikehood(){

        double range_offset;
        double xi,yi;                  //x, y of the particle
        for(int i=0;i<numberParticles;i++){
            xi=particleList[i].x;
            yi=particleList[i].y;
            for(int j=0;j<numberAN;j++) {
                range_offset=Math.pow((distance[j] - Math.sqrt(Math.pow(xi - AN[j].positionX, 2) + Math.pow(yi - AN[j].positionY, 2))), 2);
                range_offset=range_offset/(2*Math.pow(sigma_u,2));
                particleList[i].indLikehood[j]=(1/(sigma_u*Math.sqrt(2*Math.PI)))*Math.pow(Math.E,-range_offset);

                //  particleList[i].indLikehood[j]=1; ///JUST TO TEST UNTIL VEL IS CALCULATED
            }
        }
    }
    //Calculate distance from RSSI
    //Apply NLR model
    public double[] distanceRSSI(double RSS[]){
         //NLR non linear regression model
        //d=alfa*e^(beta*RSSI)
        try {
            double invtotalDistance = 0;
            for (int i = 0; i < RSS.length; i++) {
                distance[i] = AN[i].alpha * Math.pow(Math.E, (AN[i].betha * RSS[i]));


                invtotalDistance = invtotalDistance + (1 / distance[i]);

            }
            for (int i = 0; i < RSS.length; i++) {
                m[i] = (1 / distance[i]) / (invtotalDistance);
                //  m[i] = 1 / 5; //just to test particle filter without ranging
            }
        }
        catch (Exception e) {
        }
        return distance;
    }


}
