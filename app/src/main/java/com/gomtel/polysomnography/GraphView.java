package com.gomtel.polysomnography;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;


public class GraphView extends SurfaceView implements SurfaceHolder.Callback{
    private static final String TAG = "GraphView";
    float panelHeight;
	float panelWidth;
    private Paint paint = new Paint();
    private Path mPath = new Path();
    private RectF mRect = new RectF();
    private int mColors[] = new int[3 * 2];
    private float val = 0 ;
    private int  width;
    private int  height;
    ArrayList<Float> aPointsX = new ArrayList<Float>();
    private ArrayList<Float> aPointsY = new ArrayList<Float>();
    private ArrayList<Float> aPointsZ = new ArrayList<Float>();
    private ArrayList<Float> cPointsX = new ArrayList<Float>();
    private ArrayList<Float> cPointsY = new ArrayList<Float>();
    private ArrayList<Float> cPointsZ = new ArrayList<Float>();
    private float[] points ;
    private float[] cVals = new float[3]; // for ext mag field
    private float[] aVals = new float[3];
    private float x = 0;
    private float[]mPoints;
    private int counter = 0;
    public static final double STEP_LIMIT = 0.5;
//    private static MovingAverageStepDetector mStepDetector;
    private int posun = 0;

//    public static MovingAverageStepDetector getmStepDetector() {
//		return mStepDetector;
//	}


	public GraphView(Context context, AttributeSet attrs) {
    	super(context, attrs);
    	getHolder().addCallback(this);
    	setFocusable(true);          
         
    	mColors[0] = Color.argb(192, 255, 64, 64);           // red
    	mColors[1] = Color.argb(192, 64, 128, 64);           // green
    	mColors[2] = Color.argb(192, 64, 64, 255);           // blue 
    	mColors[3] = Color.argb(192, 64, 255, 255);          // light blue
    	mColors[4] = Color.argb(192, 128, 64, 128);          // violet
    	mColors[5] = Color.argb(192, 255, 255, 64);          // yellow

    	paint.setFlags(Paint.ANTI_ALIAS_FLAG);
    	mRect.set(-0.5f, -0.5f, 0.5f, 0.5f);
    	mPath.arcTo(mRect, 0, 1000);  
    	
    	DisplayMetrics metrics = this.getResources().getDisplayMetrics();
    	width = metrics.widthPixels;
    	height = metrics.heightPixels;
    	mPoints = new float[width * 5];
    	
    	aPointsX.add(Float.valueOf(0));
    	aPointsX.add(Float.valueOf(130));
    	aPointsY.add(Float.valueOf(0));
    	aPointsY.add(Float.valueOf(130));
    	aPointsZ.add(Float.valueOf(0));
    	aPointsZ.add(Float.valueOf(130));
   	
    	cPointsX.add(Float.valueOf(0));
    	cPointsX.add(Float.valueOf(380));
    	cPointsY.add(Float.valueOf(0));
    	cPointsY.add(Float.valueOf(380));
    	cPointsZ.add(Float.valueOf(0));
    	cPointsZ.add(Float.valueOf(380));
    	this.setKeepScreenOn(true);  	
    	
    	
    }
    

    public void drawGraph(Canvas canvas, float[] values) {
        aVals = values;
		drawGraph(canvas);
	}

      
    public void drawGraph(Canvas canvas) {
    	canvas.drawColor(0xFFFFFFFF);
    	int middle = height/2 + height/4 - posun;
    	int coef = (height/4 + height/16) / 2;
    	double stepLimit = STEP_LIMIT;
    	paint.setColor(0xFFAAAAAA);
    	canvas.drawLine(0, middle - posun, width, middle - posun, paint);
    	canvas.drawLine(5, middle - height/2 - height/8 - posun, 5, middle + height/32 - posun, paint);
    	canvas.drawLine(0, middle - Math.round(coef*stepLimit) - posun, width, middle - Math.round(coef*stepLimit) - posun, paint);
//    	canvas.drawText("Step limit", width - 70, middle - Math.round(coef*stepLimit) - 10 - posun, paint);
//    	canvas.drawText("0", 7, middle + 10 - posun, paint);
//    	canvas.drawText("5", 7, middle - height/2 - height/8 + 10 - posun, paint);
//    	canvas.drawText("Distance and acceleration values : ", 5, 15, paint);
//    	counter += 2;
    	
    	
    	
    	// Linear Acceleration values
    	// x values
//    	paint.setColor(mColors[2]);
//    	canvas.drawText("Integral : " + aVals[0] + " m.", 5, 30, paint);
    	//if (aVals[0] > 5) aVals[0] = 4; 
    	//if (aVals[0] < -5) aVals[0] = -4;
    	//mPoints = toArray(-aVals[0] * coef + middle - posun, aPointsX);
    	//canvas.drawLines(mPoints, paint);    	

    	//y values
    	//
//    	paint.setColor(mColors[0]);
//    	canvas.drawText("Multipled : " + aVals[1] + " m.", 175, 30, paint);
    	
//    	//z values
//    	paint.setColor(mColors[1]);
//    	canvas.drawText("StepDetect value " + aVals[2] , 300, 30, paint);
//    	if (aVals[2] > 5) aVals[2] =  4;
//    	if (aVals[2] < -5) aVals[2] = -4;
        float temp = Math.abs(aVals[0])+Math.abs(aVals[1])+Math.abs(aVals[2]);
    	mPoints = toArray(temp * coef + middle - posun, aPointsZ);
    	canvas.drawLines(mPoints,  paint); 
    	
    	// ext magnetic field
    	if (cVals[0] != 0 || cVals[1] != 0 || cVals[2] != 0) {
    		paint.setColor(mColors[0]);
    		canvas.drawText("EXTERNAL MAGNETIC FIELD PRESENT!!" , width/2 - width/4, 45, paint);
    	}
    		
    	
    }	

    private float[] toArray(float y, ArrayList<Float> mPoints) {
    	points = new float[width * 5];              
        if (counter >= width - 100) {
        	for (int i = 7 ; i <= mPoints.size(); i +=4){
        		val = mPoints.get(i);
        		mPoints.set(i-2, val);
        		mPoints.set(i-4, val);
        	}
        	mPoints.set(mPoints.size() - 2, (float) width-100);
        	mPoints.set(mPoints.size() - 1, y); 
        	
        } else {                
        	x = counter - 2;
            if (mPoints.size() == 2) {
            	mPoints.add(x);
            	mPoints.add(y);
            } else {
                mPoints.add(mPoints.get(mPoints.size() - 2));
                mPoints.add(mPoints.get(mPoints.size() - 2));
                mPoints.add(x);                
                mPoints.add(y);
            }
        } 
        for (int i = 0; i < mPoints.size(); i++) {
        	Float f = mPoints.get(i);
            points[i] = (f != null ? f : Float.NaN);
        }                                                                                                                                                                                                                                                                                                                                                
        return points;
    }
    
    @Override	
    public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
    	this.panelHeight = (float)height;
    	this.panelWidth = (float)width;
//    	Canvas c = new Canvas();
    	Canvas c = holder.lockCanvas();
    	drawGraph( c );
    	holder.unlockCanvasAndPost( c );
    }

	@Override
    public void surfaceCreated(SurfaceHolder holder) {
//        Polysomnography.graphView = this;
//        Log.e(TAG, "lixiang---surfaceCreated");
    }
    
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
//        Polysomnography.graphView = this;
    }
}
