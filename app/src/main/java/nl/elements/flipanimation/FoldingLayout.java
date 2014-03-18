/*
 * Copyright 2014 Element Interactive
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.elements.flipanimation;



import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

/**
*   An extension of FrameLayout that allows changing the visible child using Folding animations from
 *   page to page
*/
public class FoldingLayout extends FrameLayout {

    public static final int TOP_FRONT = 0;
    public static final int TOP_BACK = 1;
    public static final int BOTTOM_FRONT = 2;
    public static final int BOTTOM_BACK = 3;

    public static final int FOLD_DOWN=-1;
    public static final int FOLD_UP=1;


    private View[] views;
    private int currentView = 0;
    private Bitmap[] rectangles = new Bitmap[4];
    private boolean folding;
    private long startTime;
    private float interpolatedTime;

    private Interpolator interpolator;


    private long duration = 500;
    private float deltaTime;
    private int nextView;
    private int foldingDirection=FOLD_DOWN;

    private int halfHeight;
    private int halfWidth;

    Paint p=new Paint();



    private OnFoldListener onFoldListener;


    public FoldingLayout(Context context) {
        super(context);
        init();
    }

    public FoldingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FoldingLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        invalidate();

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!folding) {
            views = new View[getChildCount()];
            for (int i = 0; i < getChildCount(); i++) {
                views[i] = getChildAt(i);
            }
            showSingleChild(0);
        }
    }

    /**
     * Sets all views gone except one
     *
     * @param i The index of the view to show
     */

    private void showSingleChild(int i) {
        hideAllViews();
        views[i].setVisibility(VISIBLE);
    }

    /**
     * Sets all views as Invisible
     */
    private void hideAllViews() {
        for (View view : views) {
            view.setVisibility(INVISIBLE);
        }
    }

    /**
     * Starts a fold
     */
    public void fold() {
        if (folding) {
            return;
        }
        if (interpolator==null) {
            interpolator=new LinearInterpolator();
        }
        nextView=(currentView+foldingDirection);
        if (nextView<0) {
            nextView=views.length-1;
        }
        if (nextView>views.length-1) {
            nextView=0;
        }
        if (foldingDirection==FOLD_UP) {
            generateRectangles(views[currentView], views[nextView]);
        }else {
            generateRectangles( views[nextView],views[currentView]);
        }
        hideAllViews();
        invalidate();
        folding = true;


        startTime = System.currentTimeMillis();

        invalidate();



    }

    /**
     * Reverses direction and folds
     */
    public void foldReverse() {
        reverseDirection();
        fold();
    }


    /**
     * Folds into a determinate direction. The direction remains settled after fold
     * @param direction the new direction
     */
    public void foldDirection(int direction) {
        setDirection(direction);
        fold();
    }

    /**
     * Sets the direction
     * @param direction
     */
    public void setDirection(int direction) {
        foldingDirection=direction;
    }

    /**
     * change the current direction, no animation is done
     */
    public void reverseDirection() {
        foldingDirection=-foldingDirection;
    }


    @Override
    protected void onDraw(Canvas canvas) {


        if (folding) {
            halfHeight=getMeasuredHeight()/2;
            halfWidth=getMeasuredWidth()/2;
            deltaTime = System.currentTimeMillis() - startTime;

            interpolatedTime = interpolator.getInterpolation ((float) deltaTime / duration);
            if (foldingDirection==FOLD_DOWN) {
                interpolatedTime=1-interpolatedTime;
            }

            if (deltaTime >= duration) {
                stopAnimation();
            }else {
                //Clear the canvas and draw the two static halves
                canvas.drawColor(0x00000000);
                canvas.drawBitmap(rectangles[TOP_BACK], 0, 0, null);
                canvas.drawBitmap(rectangles[BOTTOM_FRONT], 0, halfHeight, null);


                if (interpolatedTime < 0.5) {
                    //First half of fold
                    Matrix matrix = new Matrix();
                    Camera camera = new Camera();
                    camera.save();
                    camera.rotateX(-interpolatedTime * 180);
                    camera.getMatrix(matrix);
                    //Bitmap rotation center should be moved to 0,0,0 position before rotation
                    matrix.preTranslate(-halfWidth, -halfHeight );
                    //And then put it back
                    matrix.postTranslate(halfWidth, halfHeight );

                    //Apply light
                    p.setColorFilter(applyLightness((int) (-interpolatedTime * 100)));
                    canvas.drawBitmap(rectangles[TOP_FRONT], matrix, p);
                    camera.restore();

                } else {
                    //Second half of fold
                    Matrix matrix = new Matrix();
                    Camera camera = new Camera();
                    camera.save();
                    camera.rotateX(((1f - interpolatedTime) * 180));

                    camera.getMatrix(matrix);
                    //Bitmap rotation center should be moved to 0,0,0 position before rotation
                    matrix.preTranslate(-halfWidth, 0);
                    //And then put it back and move it down
                    matrix.postTranslate(halfWidth, halfHeight );

                    //Apply light
                    p.setColorFilter(applyLightness((int) (interpolatedTime * 100 - 100)));
                    canvas.drawBitmap(rectangles[BOTTOM_BACK], matrix, p);
                    camera.restore();
                }
                //Invoke next onDraw
                invalidate();
            }
        }else{
            super.onDraw(canvas);
        }


    }

    /**
     * Setup and releases all things to stop the animation
     */
    private void stopAnimation() {
        folding = false;
        currentView=nextView;
        showSingleChild(currentView);
        invalidate();
        if (onFoldListener != null) {
            onFoldListener.onFoldFinished(currentView, views[currentView], foldingDirection);
        }
    }

    /**
     * Generates a PorterDuff that will light or darken the applied paint
     * @param progress The amount of light. -100 for dark, 100 for light, 0 for normal
     * @return The PorterDuffColorFilter with the requested light ready to apply
     */
    public static PorterDuffColorFilter applyLightness(int progress) {

        if(progress>0)
        {
            int value = (int) progress*255/100;
            return new PorterDuffColorFilter(Color.argb(value, 255, 255, 255), PorterDuff.Mode.SRC_OVER);
        } else {
            int value = (int) (progress*-1)*255/100;
            return new PorterDuffColorFilter(Color.argb(value, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);
        }

    }

    /**
     * Generates the rectangles arrays with the top and bottom half of each view
     *
     * @param frontView The frontview for the animation
     * @param backView  The backView for the animation
     */
    private void generateRectangles(View frontView, View backView) {
        rectangles[TOP_FRONT] = clipBitmap(frontView, 0, frontView.getHeight() / 2);
        rectangles[BOTTOM_FRONT] = clipBitmap(frontView, frontView.getHeight() / 2, frontView.getHeight() );
        rectangles[TOP_BACK] = clipBitmap(backView, 0, backView.getHeight() / 2);
        rectangles[BOTTOM_BACK] = clipBitmap(backView, backView.getHeight() / 2, backView.getHeight());
    }

    /**
     * returns a new Bitmap with a clipped vertical area of the original view cache
     *
     * @param view   The view to clip
     * @param top    Top Y coordinate where to start to clip
     * @param bottom Lower Y coordinate where to end the clip
     * @return A new Bitmap with the clipped area
     */
    private Bitmap clipBitmap(View view, int top, int bottom) {
        view.buildDrawingCache(false);

        Bitmap fullImage = view.getDrawingCache();

        Bitmap clipView = Bitmap.createBitmap(fullImage.getWidth(), fullImage.getHeight() / 2, Bitmap.Config.ARGB_8888);

        Canvas c = new Canvas(clipView);

        Rect source = new Rect(0, top, fullImage.getWidth(), bottom);
        Rect target = new Rect(0, 0, clipView.getWidth(), clipView.getHeight());

        c.drawBitmap(fullImage, source, target, null);
        view.destroyDrawingCache();

        return clipView;
    }

    public interface OnFoldListener{
        public void onFoldFinished(int position, View view, int direction);
    }

    /**
     * How long this animation should last
     * @return the duration in milliseconds of the animation
     */
    public long getDuration() {
        return duration;
    }

    /**
     * How long this animation should last. The duration cannot be negative.
     * @param duration Duration in milliseconds
     */
    public void setDuration(long duration) {
        this.duration = duration;
    }

    /**
     * Sets the acceleration curve for this animation. Defaults to a linear interpolation.
     * @param interpolator The interpolator which defines the acceleration curve
     */
    public void setInterpolator(Interpolator interpolator) {
        this.interpolator = interpolator;
    }

    /**
     * Gets the acceleration curve type for this animation.
     * @return the Interpolator associated to this animation
     */
    public Interpolator getInterpolator() {
        return interpolator;
    }

    /**
     * The listener of the fold process ending
     * @return The current OnFoldListener object assigned
     */

    public OnFoldListener getOnFoldListener() {
        return onFoldListener;
    }

    /**
     * Who will be notified once the folding is finished
     * @param onFoldListener Any object implementing OnFoldListener interface
     */
    public void setOnFoldListener(OnFoldListener onFoldListener) {
        this.onFoldListener = onFoldListener;
    }
}
