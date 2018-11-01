/*
 * Copyright 2017 Moprim
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * 	http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package fi.moprim.widget.treeview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Keep;
import android.view.View;

/**
 * Created by mineraud on 04/01/17.
 * Draw a tree node
 */
abstract class DrawableTreeNode extends View {

    private final TreeNode node;
    protected Path backgroundPath;
    protected PointF middlePoint;
    protected float length;
    private float start;
    private float sweep;
    private Paint backgroundPaint;
    private Bitmap iconBitmap;
    private int iconSize;
    private Rect src;
    private Rect dst;

    DrawableTreeNode(Context context, TreeNode node,
                     float start, float sweep, Paint backgroundPaint,
                     Bitmap iconBitmap, float iconSize) {
        super(context, null);
        this.start = start;
        this.sweep = sweep;
        this.backgroundPaint = backgroundPaint;
        this.middlePoint = new PointF(0f, 0f);
        this.length = 0f;
        this.iconBitmap = iconBitmap;
        this.iconSize = Math.round(iconSize);
        this.backgroundPath = new Path();
        this.src = new Rect();
        this.dst = new Rect();
        this.node = node;
    }

    DrawableTreeNode(Context context, TreeNode node, Paint backgroundPaint, Bitmap iconBitmap, float iconSize) {
        this(context, node, 0f, 0f, backgroundPaint, iconBitmap, iconSize);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {

        Bitmap bitmap;

        // Handle the case for drawable bitmap
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public TreeNode getNode() {
        return node;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.node.getWeight() <= 0.00001) {
            return;
        }
        setBackgroundPath();
        setMiddlePointAndLength();
        if (backgroundPath != null) {
            canvas.drawPath(backgroundPath, this.backgroundPaint);
            // Now draw the icon if present and enough size
            if (this.iconBitmap != null && this.iconSize < this.length) {
                this.src.set(0, 0, iconBitmap.getWidth() - 1, iconBitmap.getHeight() - 1);
                this.dst.set(
                        (int) (this.middlePoint.x - this.iconSize / 2),
                        (int) (this.middlePoint.y - this.iconSize / 2),
                        (int) (this.middlePoint.x + this.iconSize / 2),
                        (int) (this.middlePoint.y + this.iconSize / 2));
                canvas.drawBitmap(this.iconBitmap, src, dst, null);
            }
        }
    }

    abstract void setBackgroundPath();

    abstract void setMiddlePointAndLength();

    public float getStart() {
        return start;
    }

    public void setStart(float start) {
        this.start = start;
    }

    public float getSweep() {
        return sweep;
    }

    public void setSweep(float sweep) {
        this.sweep = sweep;
    }

}
