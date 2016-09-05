/*
    Copyright 2012 Nik Cain nik@showmehills.com
    
    This file is part of ShowMeHills.

    ShowMeHills is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ShowMeHills is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ShowMeHills.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.showmehills;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.support.annotation.NonNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraPreviewSurface extends SurfaceView {
    Context ctx;
    private String mCameraId;
    CameraManager mCameraManager;
    private boolean mGotSecondCallback;
    Handler mBackgroundHandler;
    CameraDevice mCamera;
    static final String TAG = "showmehills";
    SurfaceHolder previewHolder;
    ShowMeHillsActivity smh;
    SurfaceView mSurfaceView;
    CameraCaptureSession mCaptureSession;
    Size previewSize;
    boolean paused = false;

    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    static Size chooseBigEnoughSize(Size[] choices, int width, int height) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        for (Size option : choices) {
            if (option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
                Log.d(TAG, "size " + option);
            }
        }
        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    final CameraCaptureSession.StateCallback mCaptureSessionListener =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Log.i(TAG, "Finished configuring camera outputs");
                    mCaptureSession = session;
                    SurfaceHolder holder = mSurfaceView.getHolder();
                    if (holder != null) {
                        try {
                            // Build a request for preview footage
                            CaptureRequest.Builder requestBuilder =
                                    mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                            requestBuilder.addTarget(holder.getSurface());
                            CaptureRequest previewRequest = requestBuilder.build();
                            // Start displaying preview images
                            try {
                                session.setRepeatingRequest(previewRequest, /*listener*/null,
                                /*handler*/null);
                            } catch (CameraAccessException ex) {
                                Log.e(TAG, "Failed to make repeating preview request", ex);
                            }
                        } catch (CameraAccessException ex) {
                            Log.e(TAG, "Failed to build preview request", ex);
                        }
                    } else {
                        Log.e(TAG, "Holder didn't exist when trying to formulate preview request");
                    }
                }

                @Override
                public void onClosed(@NonNull CameraCaptureSession session) {
                    mCaptureSession = null;
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "Configuration error on device '" + mCamera.getId());
                }
            };

    final CameraDevice.StateCallback mCameraStateCallback =
            new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.i(TAG, "Successfully opened camera");
                    mCamera = camera;
                    try {
                        List<Surface> outputs = Arrays.asList(
                                mSurfaceView.getHolder().getSurface());//, mCaptureBuffer.getSurface());
                        camera.createCaptureSession(outputs, mCaptureSessionListener,
                                mBackgroundHandler);
                    } catch (CameraAccessException ex) {
                        Log.e(TAG, "Failed to create a capture session", ex);
                    }
                    catch (SecurityException se) {
                        Log.e(TAG, "security exception", se);
                    }
                    // Control flow continues in mCaptureSessionListener.onConfigured()
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.e(TAG, "Camera was disconnected");
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.e(TAG, "State error on device '" + camera.getId() + "': code " + error);
                }
            };

    SurfaceHolder.Callback surfaceHolderListener = new SurfaceHolder.Callback() {

        public void surfaceCreated(SurfaceHolder holder) {
            mCameraId = null;
            mGotSecondCallback = false;
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            Log.d(TAG, "surface changed");
            if (paused) {
                Log.d(TAG, "paused, do nothing");
                return;
            }
            if (mCameraId == null) {
                Log.d(TAG, "null camera, setting new one up");
                // Find the device's back-facing camera and set the destination buffer sizes
                try {
                    mGotSecondCallback = false;
                    mCameraManager = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
                    for (String cameraId : mCameraManager.getCameraIdList()) {
                        CameraCharacteristics cameraCharacteristics =
                                mCameraManager.getCameraCharacteristics(cameraId);
                        if (cameraCharacteristics.get(cameraCharacteristics.LENS_FACING) ==
                                CameraCharacteristics.LENS_FACING_BACK) {
                            Log.i(TAG, "Found a back-facing camera");
                            StreamConfigurationMap info = cameraCharacteristics
                                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                            Log.i(TAG, "SurfaceView size: " + getWidth() + 'x' + getHeight());
                            if (previewSize == null) {
                                previewSize = new Size(getWidth(), getHeight());
                            }

                            Size optimalSize = chooseBigEnoughSize(
                                    info.getOutputSizes(SurfaceHolder.class), previewSize.getWidth(), previewSize.getHeight());
                            // Set the SurfaceHolder to use the camera's largest supported size
                            Log.i(TAG, "Preview size: " + optimalSize);
                            SurfaceHolder surfaceHolder = getHolder();
                            surfaceHolder.setFixedSize(optimalSize.getWidth(), optimalSize.getHeight());
                            mCameraId = cameraId;
                        }
                    }
                } catch (CameraAccessException ex) {
                    Log.e(TAG, "Unable to list cameras", ex);
                } catch (SecurityException sx) {
                    Log.e(TAG, "permission failure", sx);
                }
                Log.e(TAG, "Didn't find any back-facing cameras");
                // This is the second time the method is being invoked: our size change is complete
            } else {
                if (!mGotSecondCallback) {
                    Log.d(TAG, "2nd callback");
                    if (mCamera != null) {
                        Log.e(TAG, "Aborting camera open because it hadn't been closed");
                        return;
                    }
                    // Open the camera device
                    try {
                        Log.d(TAG, "opening camera");
                        mCameraManager.openCamera(mCameraId, mCameraStateCallback,
                                mBackgroundHandler);
                    } catch (CameraAccessException ex) {
                        Log.e(TAG, "Failed to configure output surface", ex);
                    } catch (SecurityException sx) {
                        Log.e(TAG, "permission failure", sx);
                    }
                    mGotSecondCallback = true;
                    // Control flow continues in mCameraStateCallback.onOpened()
                }
            }

            smh.scrheight = h;
            smh.scrwidth = w;
        }

        public void surfaceDestroyed(SurfaceHolder arg0) {
        }
    };

    public void onResume() {
        paused = false;
    }

    public void onPause() {
        Log.d(TAG, "pausing surface");
        paused = true;
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.setFixedSize(10,10);

        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCamera != null) {
            mCamera.close();
            Log.d(TAG, "closed camera");
        }

        mCamera = null;
        mCameraId = null;
        mGotSecondCallback = false;
    }

    public CameraPreviewSurface(Context ct)
    {
        super(ct);
        ctx = ct;
        mSurfaceView = this;
    }

    public void init(ShowMeHillsActivity myapp) {
        smh = myapp;
        previewHolder = this.getHolder();
        previewHolder.addCallback(surfaceHolderListener);

    }

    public CameraPreviewSurface(Context ct, ShowMeHillsActivity myapp)
    {
        super(ct);
        ctx = ct;
        mSurfaceView = this;

        init(myapp);
    }

    public CameraPreviewSurface(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        ctx = context;
        mSurfaceView = this;
    }
    public CameraPreviewSurface(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        ctx = context;
        mSurfaceView = this;
    }
}

