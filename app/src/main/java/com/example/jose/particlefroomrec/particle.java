package com.example.jose.particlefroomrec;

import java.util.Random;

/**
 * Created by jose on 4/19/2017.
 */

public class particle {
    public floorPlanGraph floorPlan;
    public double x;    //position in X axis
    public double y;    //position in Y axis
    //Likelihood to determine Weights
    public double indLikehood[];  //NLR model likelihood (ranging)
    public double weight;             //Weight associated with likelihood
    public particle(int pnumberAN)
    {
         indLikehood=new double[pnumberAN];
        //Initialize particle in a random position
        Random rand = new Random();
        //Initial position coordinates
        this.x= rand.nextInt(18)+0; //X coordinate
        this.y= rand.nextInt(16)+0;//Y coordinate

    }


}
