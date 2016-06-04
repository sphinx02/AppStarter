package de.belu.appstarter.gui;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Animate change of width
 * https://stackoverflow.com/questions/16641539/animate-change-width-of-android-relativelayout
 */
public class ResizeWidthAnimation extends Animation
{
    private int mWidth;
    private int mStartWidth;
    private View mView;

    public ResizeWidthAnimation(View view, int width)
    {
        mView = view;
        mWidth = width;
        mStartWidth = view.getWidth();
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t)
    {
        int newWidth = mStartWidth + (int) ((mWidth - mStartWidth) * interpolatedTime);

        mView.getLayoutParams().width = newWidth;
        mView.requestLayout();
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight)
    {
        super.initialize(width, height, parentWidth, parentHeight);
    }

    @Override
    public boolean willChangeBounds()
    {
        return true;
    }
}