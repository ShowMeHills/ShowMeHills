package com.showmehills;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreviewSurface extends SurfaceView {
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

	public CameraPreviewSurface(Context ctx, ShowMeHillsActivity myapp)
	{
		super(ctx);
		smh = myapp;
		previewHolder = this.getHolder();
		previewHolder.setType 
		(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		previewHolder.addCallback(surfaceHolderListener);
	}
}

