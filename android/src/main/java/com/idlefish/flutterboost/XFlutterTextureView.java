package com.idlefish.flutterboost;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;
import io.flutter.embedding.engine.renderer.FlutterRenderer;
import io.flutter.embedding.engine.renderer.FlutterUiDisplayListener;
import io.flutter.embedding.engine.renderer.RenderSurface;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class XFlutterTextureView extends TextureView implements RenderSurface {
    private static final String TAG = "XFlutterTextureView";
    private boolean isSurfaceAvailableForRendering;
    private boolean isAttachedToFlutterRenderer;
    @Nullable
    private FlutterRenderer flutterRenderer;
    @NonNull
    private Set flutterUiDisplayListeners;
    private final SurfaceTextureListener surfaceTextureListener;

    public XFlutterTextureView(@NonNull Context context) {
        this(context, (AttributeSet)null);
    }

    public XFlutterTextureView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.isSurfaceAvailableForRendering = false;
        this.isAttachedToFlutterRenderer = false;
        this.flutterUiDisplayListeners = new HashSet();
        this.surfaceTextureListener = new SurfaceTextureListener() {
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                Log.v("FlutterTextureView", "SurfaceTextureListener.onSurfaceTextureAvailable()");
                XFlutterTextureView.this.isSurfaceAvailableForRendering = true;
                if (XFlutterTextureView.this.isAttachedToFlutterRenderer) {
                    XFlutterTextureView.this.connectSurfaceToRenderer();
                }

            }

            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                Log.v("FlutterTextureView", "SurfaceTextureListener.onSurfaceTextureSizeChanged()");
                if (XFlutterTextureView.this.isAttachedToFlutterRenderer) {
                    XFlutterTextureView.this.changeSurfaceSize(width, height);
                }

            }

            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            }

            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                Log.v("FlutterTextureView", "SurfaceTextureListener.onSurfaceTextureDestroyed()");
                XFlutterTextureView.this.isSurfaceAvailableForRendering = false;
                if (XFlutterTextureView.this.isAttachedToFlutterRenderer) {
                    XFlutterTextureView.this.disconnectSurfaceFromRenderer();
                }

                return true;
            }
        };
        this.init();
    }

    private void init() {
        this.setSurfaceTextureListener(this.surfaceTextureListener);
    }

    private final FlutterUiDisplayListener flutterUiDisplayListener = new FlutterUiDisplayListener() {
        @Override
        public void onFlutterUiDisplayed() {
            Log.v(TAG, "onFlutterUiDisplayed()");
            // Now that a frame is ready to display, take this SurfaceView from transparent to opaque.
            setAlpha(1.0f);

            if (flutterRenderer != null) {
                flutterRenderer.removeIsDisplayingFlutterUiListener(this);
            }
        }

        @Override
        public void onFlutterUiNoLongerDisplayed() {
            // no-op
        }
    };

    @Nullable
    @Override
    public FlutterRenderer getAttachedRenderer() {
        return flutterRenderer;
    }

    public void attachToRenderer(@NonNull FlutterRenderer flutterRenderer) {
        Log.v(TAG, "Attaching to FlutterRenderer.");
        if (this.flutterRenderer != null) {
            Log.v(TAG, "Already connected to a FlutterRenderer. Detaching from old one and attaching to new one.");
            this.flutterRenderer.stopRenderingToSurface();
            this.flutterRenderer.removeIsDisplayingFlutterUiListener(flutterUiDisplayListener);
        }

        this.flutterRenderer = flutterRenderer;
        isAttachedToFlutterRenderer = true;

        this.flutterRenderer.addIsDisplayingFlutterUiListener(flutterUiDisplayListener);

        // If we're already attached to an Android window then we're now attached to both a renderer
        // and the Android window. We can begin rendering now.
        if (isSurfaceAvailableForRendering) {
            Log.v(TAG, "Surface is available for rendering. Connecting FlutterRenderer to Android surface.");
            connectSurfaceToRenderer();
        }
    }

    public void detachFromRenderer() {
        if (flutterRenderer != null) {
            // If we're attached to an Android window then we were rendering a Flutter UI. Now that
            // this FlutterSurfaceView is detached from the FlutterRenderer, we need to stop rendering.
            // TODO(mattcarroll): introduce a isRendererConnectedToSurface() to wrap "getWindowToken() != null"
            if (getWindowToken() != null) {
                Log.v(TAG, "Disconnecting FlutterRenderer from Android surface.");
                disconnectSurfaceFromRenderer();
            }

            // Make the SurfaceView invisible to avoid showing a black rectangle.
            setAlpha(1.0f);

            this.flutterRenderer.removeIsDisplayingFlutterUiListener(flutterUiDisplayListener);

            flutterRenderer = null;
            isAttachedToFlutterRenderer = false;
        } else {
            Log.w(TAG, "detachFromRenderer() invoked when no FlutterRenderer was attached.");
        }
    }

    private void connectSurfaceToRenderer() {
        if (this.flutterRenderer != null && this.getSurfaceTexture() != null) {
            Surface surface= new Surface(this.getSurfaceTexture());
            //this.flutterRenderer.surfaceCreated(surface);
            this.flutterRenderer.startRenderingToSurface(surface);
            surface.release();
        } else {
            throw new IllegalStateException("connectSurfaceToRenderer() should only be called when flutterRenderer and getSurfaceTexture() are non-null.");
        }
    }

    private void changeSurfaceSize(int width, int height) {
        if (flutterRenderer == null) {
            throw new IllegalStateException("changeSurfaceSize() should only be called when flutterRenderer is non-null.");
        }

        Log.v(TAG, "Notifying FlutterRenderer that Android surface size has changed to " + width + " x " + height);
        flutterRenderer.surfaceChanged(width, height);
    }

    private void disconnectSurfaceFromRenderer() {
        if (flutterRenderer == null) {
            throw new IllegalStateException("disconnectSurfaceFromRenderer() should only be called when flutterRenderer is non-null.");
        }

        flutterRenderer.stopRenderingToSurface();
    }

    public void addFlutterUiDisplayListener(@NonNull FlutterUiDisplayListener listener) {
        this.flutterUiDisplayListeners.add(listener);
    }

    public void removeFlutterUiDisplayListener(@NonNull FlutterUiDisplayListener listener) {
        this.flutterUiDisplayListeners.remove(listener);
    }

    public void onFlutterUiDisplayed() {
        Log.v("FlutterTextureView", "onFlutterUiDisplayed()");
        Iterator var1 = this.flutterUiDisplayListeners.iterator();

        while(var1.hasNext()) {
            FlutterUiDisplayListener listener = (FlutterUiDisplayListener)var1.next();
            listener.onFlutterUiDisplayed();
        }

    }

    public void onFlutterUiNoLongerDisplayed(){
        // no-op
    }
}