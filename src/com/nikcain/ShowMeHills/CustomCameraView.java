package com.nikcain.ShowMeHills;

import java.util.List;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CustomCameraView extends SurfaceView {
	Camera camera;
	SurfaceHolder previewHolder;
	ShowMeHillsActivity smh;

	SurfaceHolder.Callback surfaceHolderListener = new SurfaceHolder.Callback() {

		public void surfaceCreated(SurfaceHolder holder) {

			camera=Camera.open();
			try {
				camera.setPreviewDisplay(previewHolder);
			}
			catch (Throwable t){ }
		}

	    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h)
		{
			int rot = 0;
			try
			{
				rot = smh.GetRotation();				
			}
			catch (Throwable t) {
				Log.d("showmehills", "couldn't get field of views");
			}
			/*
			 *  we're not doing this anymore - easier to stick with one orientation!!!
			switch (rot)
			{
				case Surface.ROTATION_0: 
					camera.setDisplayOrientation(90);
					// switch fov
					smh.vfov = camera.getParameters().getHorizontalViewAngle();
					smh.hfov = camera.getParameters().getVerticalViewAngle();
					break;
				case Surface.ROTATION_90: 
					camera.setDisplayOrientation(0);
					smh.hfov = camera.getParameters().getHorizontalViewAngle();
					smh.vfov = camera.getParameters().getVerticalViewAngle();
					break;
				case Surface.ROTATION_180: 
					camera.setDisplayOrientation(270);
					// switch fov
					smh.vfov = camera.getParameters().getHorizontalViewAngle();
					smh.hfov = camera.getParameters().getVerticalViewAngle();
					break;
				case Surface.ROTATION_270: 
					camera.setDisplayOrientation(180);
					smh.hfov = camera.getParameters().getHorizontalViewAngle();
					smh.vfov = camera.getParameters().getVerticalViewAngle();
					break;
				default: 
			}
*/
			try
			{
				Parameters params = camera.getParameters();
				
				smh.scrheight = h;
				smh.scrwidth = w;
				
				params.setPreviewSize((w>h)?w:h, (w>h)?h:w);
				params.setPictureFormat(PixelFormat.JPEG);
				camera.setParameters(params);
			}
			catch (Throwable t) {
				Log.d("showmehills", "couldn't set camera params: smh.scrwidth="+smh.scrwidth+" w="+w+" smh.scrheight="+smh.scrheight+" h="+h);
			}

			camera.startPreview();
		}
	    
		public void surfaceDestroyed(SurfaceHolder arg0)
		{
			camera.stopPreview();
			camera.release();   
		}
	};

	public CustomCameraView(Context ctx, ShowMeHillsActivity myapp)
	{
		super(ctx);
		smh = myapp;
		previewHolder = this.getHolder();
		previewHolder.setType 
		(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		previewHolder.addCallback(surfaceHolderListener);
	}
}

