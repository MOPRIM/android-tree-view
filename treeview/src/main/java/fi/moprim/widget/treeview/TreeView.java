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

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by mineraud on 03/01/17.
 * The tree view widget intend to visually represent tree views
 */
public class TreeView extends View implements TreeAdapter.TreeNodeChangeListener, ValueAnimator.AnimatorUpdateListener {

    private static final String TAG = "TreeView";
    private final boolean radialView;
    private final float innerRadiusWeight;
    private final float layerHeight;
    private final float skipLength;
    private final boolean doAnimate;
    private final boolean addShadow;
    private final int colorIcons;
    private final GestureDetector gestureDetector;
    private float calculatedLayerHeight;
    private TreeAdapter adapter;
    private TreeMap<String, DrawableTreeNode> drawableTreeNodes;
    private int width;
    private int height;
    private AnimatorSet animatorSet;
    private Collection<Animator> animators;
    private List<OnClickListener> listeners;
    private final Path shadowPath;
    private final Paint shadowPaint;
    private final float shadowOffset;

    public TreeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.TreeView,
                0, 0);
        int shadowColor;
        try {
            // The default is to use radial view
            radialView = a.getBoolean(R.styleable.TreeView_radial_view, true);
            doAnimate = a.getBoolean(R.styleable.TreeView_animate, true);
            innerRadiusWeight = a.getFloat(R.styleable.TreeView_inner_radius_weight, 2f);
            skipLength = a.getFloat(R.styleable.TreeView_skip_length, 1f);
            layerHeight = a.getDimension(R.styleable.TreeView_layer_height, 50f);
            colorIcons = a.getResourceId(R.styleable.TreeView_color_icons, -1);
            addShadow = a.getBoolean(R.styleable.TreeView_shadow, true);
            shadowOffset = a.getFloat(R.styleable.TreeView_shadow_offset, 15f);
            shadowColor = a.getResourceId(R.styleable.TreeView_shadow_color, android.R.color.darker_gray);
        } finally {
            a.recycle();
        }
        adapter = null;
        this.shadowPath = new Path();
        this.shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setStyle(Paint.Style.STROKE);
        shadowPaint.setColor(ContextCompat.getColor(context, shadowColor));
        this.drawableTreeNodes = new TreeMap<>();
        this.animatorSet = new AnimatorSet();
        this.animators = new ArrayList<>();
        this.listeners = new ArrayList<>();
        this.gestureDetector = new GestureDetector(context, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent motionEvent) {
                Log.d(TAG, "onDown");
                return true;
            }

            @Override
            public void onShowPress(MotionEvent motionEvent) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent motionEvent) {
                Log.d(TAG, "onSingleTapUp");
                //fires on drag and click
                boolean clickedSomething = false;
                for (DrawableTreeNode drawableTreeNode : drawableTreeNodes.values()) {
                    if (clicked(drawableTreeNode.backgroundPath, motionEvent)) {
                        for (OnClickListener listener : listeners) {
                            listener.onNodeClicked(drawableTreeNode.getNode());
                        }
                        clickedSomething = true;
                        break;
                    }
                }
                if (!clickedSomething) {
                    for (OnClickListener listener : listeners) {
                        listener.onNodeClicked(null);
                    }
                }
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent motionEvent) {
                Log.d(TAG, "onLongPress");
            }

            @Override
            public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
                return false;
            }
        });
        this.gestureDetector.setIsLongpressEnabled(true);
    }

    public void addOnClickListener(OnClickListener listener) {
        listeners.add(listener);
    }

    public void setTreeMapAdapter(TreeAdapter adapter) {
        if (this.adapter != null) {
            this.adapter.removeTreeNodeChangeListener(this);
        }
        this.adapter = adapter;
        this.adapter.addTreeNodeChangeListener(this);
        this.adapter.notifyDataSetChanged();
    }

    public TreeAdapter getTreeAdapter() {
        return adapter;
    }

    public boolean isRadialView() {
        return radialView;
    }

    private void loadDrawablesFromAdapter() {
        drawableTreeNodes.clear();
        // Log.d(TAG, "loadDrawablesFromAdapter");
        if (isRadialView()) {
            // The radial view is over 360 degrees
            if (addShadow) {
                setRadialShadow(shadowOffset);
            }
            makeDrawableTreeNodes(adapter.getRootNodes(), 0, 360, -1);
        } else {
            // While the rectangular view uses percentages
            makeDrawableTreeNodes(adapter.getRootNodes(), 0, 100, -1);
        }
        // then invalidate and finally request layout
        invalidate();
        requestLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (addShadow) {
            canvas.drawPath(shadowPath, shadowPaint);
        }
        for (DrawableTreeNode drawableTreeNode : drawableTreeNodes.values()) {
            drawableTreeNode.draw(canvas);
        }
    }

    public boolean clicked(Path path, MotionEvent e) {
        PathMeasure pm = new PathMeasure(path, false);
        int numberOfPoints = 10;
        float length = pm.getLength();
        float distance = 0f;
        float speed = length / numberOfPoints;
        int counter = 0;
        float[][] coordinates = new float[numberOfPoints][2];


        while ((distance < length) && (counter < numberOfPoints)) {
            // get point from the path
            pm.getPosTan(distance, coordinates[counter], null);
            counter++;
            distance = distance + speed;
        }

        int j = coordinates.length - 1;
        boolean oddNodes = false;

        for (int i = 0; i < coordinates.length; i++) {
            if ((coordinates[i][1] < e.getY() && coordinates[j][1] >= e.getY() ||
                    coordinates[j][1] < e.getY() && coordinates[i][1] >= e.getY())
                    && (coordinates[i][0] <= e.getX() || coordinates[j][0] <= e.getX())) {
                if (coordinates[i][0] + (e.getY() - coordinates[i][1])
                        / (coordinates[j][1] - coordinates[i][1])
                        * (coordinates[j][0] - coordinates[i][0]) < e.getX()) {
                    oddNodes = !oddNodes;
                }
            }
            j = i;
        }

        return oddNodes;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    private RectF getInnerCircleForDepth(int depth) {
        float centerX = this.width / 2;
        float centerY = this.height / 2;
        float offset = depth == 1 ? 0 : (depth - 1) * calculatedLayerHeight + calculatedLayerHeight * 0.05f;

        float innerRadius = innerRadiusWeight * calculatedLayerHeight;
        return new RectF(
                centerX - innerRadius - offset, centerY - innerRadius - offset,
                centerX + innerRadius + offset, centerY + innerRadius + offset);
    }

    private RectF getOuterCircleForDepth(int depth) {
        float centerX = this.width / 2;
        float centerY = this.height / 2;
        float offset = depth * calculatedLayerHeight;
        float innerRadius = innerRadiusWeight * calculatedLayerHeight;
        return new RectF(centerX - innerRadius - offset,
                centerY - innerRadius - offset,
                centerX + innerRadius + offset,
                centerY + innerRadius + offset);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.width = w;
        this.height = h;
        // Log.d(TAG, String.format(Locale.ENGLISH, "(w,h) = (%d,%d)", this.width, this.height));
        loadDrawablesFromAdapter();
        super.onSizeChanged(w, h, oldw, oldh);
    }

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//
//        int desiredSize = (int) (innerRadius + this.adapter.getDepth() * (1 + this.layerHeight)) * 2;
//
//        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
//        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
//        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
//        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
//
//        int width;
//        int height;
//
//        // The width was specified with an exact value so let's keep it as it is
//        if (widthMode == MeasureSpec.EXACTLY) {
//            width = widthSize;
//        }
//        // The width was set to match_parent or wrap_content (with a maximum size)
//        else if (widthMode == MeasureSpec.AT_MOST) {
//            width = Math.min(desiredSize, widthSize);
//        }
//        // The width was set to wrap content (but without maximum size)
//        else {
//            width = desiredSize;
//        }
//
//        // Measure height
//        if (heightMode == MeasureSpec.EXACTLY) {
//            height = heightSize;
//        } else if (heightMode == MeasureSpec.AT_MOST) {
//            height = Math.min(desiredSize, heightSize);
//        } else {
//            height = desiredSize;
//        }
//
//        //MUST CALL THIS
//        // Log.d(TAG, String.format(Locale.ENGLISH, "onMeasure (w,h) = (%d,%d)", width, height));
//        setMeasuredDimension(width, height);
//    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int desiredHeight = getPaddingTop() + getPaddingBottom();
        int desiredWidth = getPaddingLeft() + getPaddingRight();

        float layers = adapter.getDepth() + innerRadiusWeight;
        int depth = adapter == null || adapter.getDepth() < 1 ? 0 : adapter.getDepth();

        desiredHeight += Math.round(layerHeight * layers + depth * skipLength);
        desiredWidth += Math.round(layerHeight * layers + depth * skipLength);

//        Log.d(TAG, "Desired height: " + desiredHeight);
//        Log.d(TAG, "Desired width: " + desiredWidth);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        // The width was specified with an exact value so let's keep it as it is
        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        }
        // The width was set to match_parent or wrap_content (with a maximum size)
        else if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(desiredWidth, widthSize);
        }
        // The width was set to wrap content (but without maximum size)
        else {
            width = desiredWidth;
        }

        // Measure height
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
            // Log.d(TAG, "EXACTLY");
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(desiredHeight, heightSize);
            // Log.d(TAG, "AT_MOST");
        } else {
            height = desiredHeight;
            // Log.d(TAG, "WRAP_CONTENT");
        }


        if (height > width) {
            // Remove the padding
            calculatedLayerHeight = width - getPaddingLeft() - getPaddingRight();
        } else {
            calculatedLayerHeight = height - getPaddingBottom() - getPaddingTop();
        }
        calculatedLayerHeight -= depth * skipLength;
        calculatedLayerHeight /= layers * 2;

        //MUST CALL THIS
        // Log.d(TAG, String.format(Locale.ENGLISH, "onMeasure (w,h) = (%d,%d)", width, height));
        setMeasuredDimension(width, height);
    }

    private void setRadialShadow(float offset) {
        RectF innerCircle = getInnerCircleForDepth(1);
        RectF outerCircle = getOuterCircleForDepth(1);

        shadowPath.reset();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            shadowPath.addOval((outerCircle.left + innerCircle.left + offset) / 2f,
                    (outerCircle.top + innerCircle.top + offset) / 2f,
                    (outerCircle.right + innerCircle.right - offset) / 2f,
                    (outerCircle.bottom + innerCircle.bottom - offset) / 2f,
                    Path.Direction.CCW);
            shadowPath.close();
        }

        //noinspection SuspiciousNameCombination
        shadowPaint.setStrokeWidth(calculatedLayerHeight + offset);
    }

    private void makeDrawableTreeNodes(List<TreeNode> nodes, float parentStart, float parentSweep, double parentWeight) {
        float totalWeight = 0;
        int depth = -1;
        int nonZeroNodeCounter = 0;
        for (TreeNode node : nodes) {
            totalWeight += node.getWeight();
            if (node.getWeight() > 0) {
                nonZeroNodeCounter++;
            }
            if (depth == -1) {
                depth = node.getDepth();
            } else if (depth != node.getDepth()) {
                Log.e(TAG, "The depth of the nodes are not all equal");
            }
        }
        if (parentWeight > 0) {
            if (parentWeight < totalWeight) {
                Log.w(TAG, "Given weight is smaller than total weight, dismissed");
            } else {
                totalWeight = (float) parentWeight;
            }
        }
        float realParentSweep = parentSweep;
        float nodeStart = parentStart;
        // We just want to remove skipLength between nodes
        if (nonZeroNodeCounter > 1) {
            parentSweep -= (nonZeroNodeCounter - (depth <= 1 ? 0 : 1)) * skipLength;
        }

        int nonZeroWeightIndex = 0;

        for (TreeNode node : nodes) {
            if (isRadialView()) {
                // For the radial view, I need to create the inner and outer circle
                RectF innerCircle = getInnerCircleForDepth(depth);
                RectF outerCircle = getOuterCircleForDepth(depth);

                float nodeSweep = 0f;
                if (node.getWeight() > 0) {
                    if (node.getDepth() <= 1 || nonZeroWeightIndex > 0) {
                        nodeStart += 1;  // Adding some offset to separate the drawables
                    }
                    nodeSweep = ((float) node.getWeight() / totalWeight) * parentSweep;
//                    if (depth > 1 && nonZeroWeightIndex == nonZeroNodeCounter - 1) {
//                        nodeSweep = parentStart + realParentSweep - nodeStart;
//                    }
                    nonZeroWeightIndex++;
                }
                // FIXME, dirty fix because the maximum sweep is 360 degrees
                if (nodeStart + nodeSweep > 360) {
                    nodeSweep = 360 - nodeStart;
                }
                this.drawableTreeNodes.put(node.getPath(), RadialDrawableTreeNode.getInstance(getContext(), node,
                        innerCircle, outerCircle, nodeStart, nodeSweep,
                        node.getColorResId(), node.getIconResId(), 0.75f * this.calculatedLayerHeight, this.colorIcons));
                // Log.d(TAG, String.format(Locale.ENGLISH, "%s (%d, %.1f, %.1f) as weight %.2f", node.getPath(), node.getDepth(), nodeStart, nodeSweep, node.getWeight()));
                if (node.getChildren().size() > 0) {
                    makeDrawableTreeNodes(node.getChildren(), nodeStart, nodeSweep, node.getWeight());
                }
                nodeStart += nodeSweep;
            } else {
                Log.e(TAG, "Not yet implemented");
            }

        }
    }

    @Override
    public void onDataSetChanged() {
        // Log.d(TAG, "onDataSetChanged");
        loadDrawablesFromAdapter();
    }

    @Override
    public void onWeightsChanged() {
        // TODO check this method, it does not seems to work
        // Log.d(TAG, "onWeightsChanged");
        if (this.drawableTreeNodes.isEmpty()) {
            onDataSetChanged();
        } else {
            if (doAnimate) {
                this.animatorSet.cancel();
                this.animators.clear();
            }
            if (isRadialView()) {
                // The radial view is over 360 degrees
                updateDrawableTreeNodes(adapter.getRootNodes(), 0, 360, -1);
            } else {
                // While the rectangular view uses percentages
                updateDrawableTreeNodes(adapter.getRootNodes(), 0, 100, -1);
            }
            if (doAnimate) {
                this.animatorSet.playTogether(this.animators);
                this.animatorSet.start();
            } else {
                invalidate();
            }
        }
    }

    private void updateDrawableTreeNodes(List<TreeNode> nodes, float parentStart, float parentSweep, double parentWeight) {
        float totalWeight = 0;
        int depth = -1;
        int nonZeroNodeCounter = 0;
        for (TreeNode node : nodes) {
            totalWeight += node.getWeight();
            if (node.getWeight() > 0) {
                nonZeroNodeCounter++;
            }
            if (depth == -1) {
                depth = node.getDepth();
            } else if (depth != node.getDepth()) {
                Log.e(TAG, "The depth of the nodes are not all equal");
            }
        }

        // Log.d(TAG, "Parent weight: " + parentWeight);
        // Log.d(TAG, "Total weight: " + totalWeight);
        if (parentWeight > 0) {
            if (parentWeight < totalWeight) {
                Log.w(TAG, "Given weight is smaller than total weight, dismissed");
            } else {
                totalWeight = (float) parentWeight;
            }
        }

        // Log.d(TAG, "Total weight: " + totalWeight);


        // Log.d(TAG, "Total weight: " + totalWeight);
        float realParentSweep = parentSweep;
        float nodeStart = parentStart;
        // We just want to remove skipLength between nodes
        if (nonZeroNodeCounter > 1) {
            parentSweep -= (nonZeroNodeCounter - (depth <= 1 ? 0 : 1)) * skipLength;
        }

        int nonZeroWeightIndex = 0;
        for (TreeNode node : nodes) {
            if (isRadialView()) {

                float nodeSweep = 0f;
                if (node.getWeight() > 0) {
                    if (node.getDepth() <= 1 || nonZeroWeightIndex > 0) {
                        nodeStart += 1;  // Adding some offset to separate the drawables
                    }
                    nodeSweep = ((float) node.getWeight() / totalWeight) * parentSweep;
//                    if (depth > 1 && nonZeroWeightIndex == nonZeroNodeCounter - 1) {
//                        nodeSweep = parentStart + realParentSweep - nodeStart;
//                    }

                    // FIXME, dirty fix because the maximum sweep is 360 degrees
                    if (nodeStart + nodeSweep > 360) {
                        nodeSweep = 360 - nodeStart;
                    }

                    nonZeroWeightIndex++;
                }
                if (doAnimate) {
                    PropertyValuesHolder pvhStart = PropertyValuesHolder.ofFloat("start",
                            this.drawableTreeNodes.get(node.getPath()).getStart(), nodeStart);
                    PropertyValuesHolder pvhSweep = PropertyValuesHolder.ofFloat("sweep",
                            this.drawableTreeNodes.get(node.getPath()).getSweep(), nodeSweep);
                    ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(
                            this.drawableTreeNodes.get(node.getPath()), pvhStart, pvhSweep);
                    animator.addUpdateListener(this);
                    this.animators.add(animator);
                    // Log.d(TAG, "Added a new animator for node " + node);
                } else {
                    this.drawableTreeNodes.get(node.getPath()).setStart(nodeStart);
                    this.drawableTreeNodes.get(node.getPath()).setSweep(nodeSweep);
                    // Log.d(TAG, "I updated the start and sweep for node  " + node);
                }

                if (node.getChildren().size() > 0) {
                    updateDrawableTreeNodes(node.getChildren(), nodeStart, nodeSweep, node.getWeight());
                }
                nodeStart += nodeSweep;
            } else {
                Log.e(TAG, "Not yet implemented");
            }
        }

    }

    @Override
    public void onAnimationUpdate(ValueAnimator valueAnimator) {
        // Log.d("TreeView", "onAnimationUpdate");
        invalidate();
    }

    public interface OnClickListener {
        void onNodeClicked(TreeNode node);
    }
}



