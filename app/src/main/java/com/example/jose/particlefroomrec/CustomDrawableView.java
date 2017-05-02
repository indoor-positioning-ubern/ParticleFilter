package com.example.jose.particlefroomrec;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

/**
 * Created by jose on 4/13/2017.
 * Class to draw the position in the screen
 */

public class CustomDrawableView {
    Paint paint = new Paint();
    private final static float planWidht=18.9f; //Real widht of the floor in meters.
    private final static float planHeight=16.6f; //Reall height of the floor in meters.
    BitmapFactory.Options options;
    Rect dest;
    Canvas canvas;
    Context context;
    Bitmap bit;
    int imageHeight;
    int imageWidth;
    int scaleXaxis;
    int scaleYaxis;
    int idPlan; //id of the floor plan png drawable resource.
    int screenFactor;
    public CustomDrawableView( Context pcontext,String pfloorPlan,int pwidth)
    {
        context=pcontext;
        options=new BitmapFactory.Options();
        options.inJustDecodeBounds=true;
        bit =BitmapFactory.decodeResource(context.getResources(), context.getResources().getIdentifier(pfloorPlan,"drawable",context.getPackageName()), options);
        idPlan=context.getResources().getIdentifier(pfloorPlan,"drawable", context.getPackageName());

        paint.setFilterBitmap(true);
        screenFactor=Math.round(pwidth/options.outWidth );
        imageHeight=options.outHeight*screenFactor;    //screenFactor fit the map in the screen;
        imageWidth=options.outWidth*screenFactor;
        dest=new Rect(0,0,imageWidth,imageHeight);
        //scaling image, convert pixels to meters
        scaleXaxis=Math.round(imageWidth/planWidht);
        scaleYaxis=Math.round(imageHeight/planHeight);

    };
    public Bitmap draw(wifiANs[] WiFiAN,double[] pedState,particleFilter objParticleFilter) {
        Bitmap bmp= Bitmap.createBitmap(dest.width(),dest.height(), Bitmap.Config.ARGB_8888);
        canvas=new Canvas(bmp);
        paint.setStyle(Paint.Style.FILL);
        //*************Draw floor Plan***********//
        canvas.drawBitmap(BitmapFactory.decodeResource(context.getResources(),idPlan), null, dest, paint);

        //***********Draw WiFi anchor nodes ************//
        paint.setTextSize(17);
        int count=0;
        for(wifiANs AN: WiFiAN){
            count++;
            paint.setColor(Color.YELLOW);
            canvas.drawCircle(Math.round(AN.positionX * scaleXaxis), Math.round(imageHeight - AN.positionY * scaleYaxis), 20, paint);
            paint.setColor(Color.BLACK);
            canvas.drawText("AN"+Integer.toString(count) , Math.round(AN.positionX * scaleXaxis) - 10, Math.round(imageHeight - AN.positionY * scaleYaxis), paint);
        }

        //***********Draw Pedestrian Position ************//
        paint.setColor(Color.GREEN);
        paint.setTextSize(10*screenFactor);
        canvas.drawCircle(Math.round(pedState[0] * scaleXaxis), Math.round(imageHeight - (pedState[1] * scaleYaxis)), 6*screenFactor, paint);
        paint.setColor(Color.BLACK);
        canvas.drawText(":)", Math.round(pedState[0] * scaleXaxis) - 10, Math.round(imageHeight - (pedState[1] * scaleYaxis)) + 10, paint);

        //**************Draw Particles ********************//
        paint.setColor(Color.RED);
        paint.setTextSize(20);
        paint.setStyle(Paint.Style.FILL);
        for(int i=0;i<objParticleFilter.particleList.length;i++)
        {
            canvas.drawText("o", Math.round(objParticleFilter.particleList[i].x*scaleXaxis), Math.round(imageHeight-(objParticleFilter.particleList[i].y*scaleYaxis)), paint);
        }

        canvas.save();
        return bmp;
      //  canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.plan_thirdroom), null, dest, paint);
    }
}
