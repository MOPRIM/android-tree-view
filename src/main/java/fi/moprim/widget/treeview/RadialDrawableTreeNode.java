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
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;

/**
 * Created by mineraud on 04/01/17.
 */
class RadialDrawableTreeNode extends DrawableTreeNode {

    private final RectF innerCircle;
    private final RectF outerCircle;

    private RadialDrawableTreeNode(Context context, TreeNode node, RectF innerCircle, RectF outerCircle,
                                   float start, float sweep,
                                   Paint backgroundPaint, Bitmap iconBitmap, float iconSize) {
        super(context, node, start, sweep, backgroundPaint, iconBitmap, iconSize);
        this.innerCircle = innerCircle;
        this.outerCircle = outerCircle;
    }

//    static RadialDrawableTreeNode getInstance(Context context, TreeNode node, RectF innerCircle, RectF outerCircle,
//                                              int backgroundColorResId, int iconResId, float iconSize) {
//        Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        backgroundPaint.setStyle(Paint.Style.FILL);
//        backgroundPaint.setColor(ContextCompat.getColor(context, backgroundColorResId));
//
//        Bitmap iconBitmap = null;
//        if (iconResId != -1) {
//            iconBitmap = BitmapFactory.decodeResource(context.getResources(), iconResId);
//        }
//        return new RadialDrawableTreeNode(context, node, innerCircle, outerCircle,
//                backgroundPaint, iconBitmap, iconSize);
//    }

    static RadialDrawableTreeNode getInstance(Context context, TreeNode node, RectF innerCircle, RectF outerCircle,
                                              float start, float sweep,
                                              int backgroundColorResId, int iconResId, float iconSize, int colorIcon) {
        Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(ContextCompat.getColor(context, backgroundColorResId));
        Bitmap iconBitmap = null;
        if (iconResId != -1) {
            if (colorIcon != -1) {

                Drawable icon = ContextCompat.getDrawable(context, iconResId);
                icon.mutate(); // A mutable drawable is guaranteed to not share its state with any other drawable
                DrawableCompat.setTint(icon,
                        ContextCompat.getColor(context, android.R.color.white));
                iconBitmap = DrawableTreeNode.drawableToBitmap(icon);
            } else {
                iconBitmap = BitmapFactory.decodeResource(context.getResources(), iconResId);
            }
        }
        return new RadialDrawableTreeNode(context, node, innerCircle, outerCircle, start, sweep,
                backgroundPaint, iconBitmap, iconSize);
    }

    @Override
    void setBackgroundPath() {
        backgroundPath.reset();
        backgroundPath.arcTo(outerCircle, getStart(), getSweep(), false);
        backgroundPath.arcTo(innerCircle, getStart() + getSweep(), -getSweep(), false);
        backgroundPath.close();
    }

    @Override
    void setMiddlePointAndLength() {
        float left = (outerCircle.left + innerCircle.left) / 2;
        float top = (outerCircle.top + innerCircle.top) / 2;
        float right = (outerCircle.right + innerCircle.right) / 2;
        float bottom = (outerCircle.bottom + innerCircle.bottom) / 2;
        RectF middleCircle = new RectF(left, top, right, bottom);
        Path path = new Path();
        path.arcTo(middleCircle, getStart(), getSweep() / 2, false);
        PathMeasure pm = new PathMeasure(path, false);
        // coordinates will be here
        float coordinates[] = {0f, 0f};
        //get point at the end
        pm.getPosTan(pm.getLength(), coordinates, null);

        this.length = pm.getLength() * 2;
        this.middlePoint.set(coordinates[0], coordinates[1]);
    }
}
