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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mineraud on 04/01/17.
 */
public class TreeNode {

    private TreeNode parent;
    private List<TreeNode> children;
    private String label;
    private int depth;
    private double weight;
    private int colorResId;
    private int iconResId;

    private TreeNode(TreeNode parent, List<TreeNode> children, String label, int depth, double weight, int colorResId, int iconResId) {
        this.parent = parent;
        this.children = children;
        this.label = label;
        this.depth = depth;
        this.weight = weight;
        this.colorResId = colorResId;
        this.iconResId = iconResId;
    }

    public static TreeNode rootNode(String label, double weight, int colorResId, int iconResId) {
        return new TreeNode(null, new ArrayList<TreeNode>(),
                label, 1, weight, colorResId, iconResId);
    }

    public static TreeNode childNode(TreeNode parent,
                                     String label, double weight, int colorResId, int iconResId) {
        TreeNode childNode = new TreeNode(parent, new ArrayList<TreeNode>(),
                label, parent.depth + 1, weight, colorResId, iconResId);
        parent.children.add(childNode);
        return childNode;
    }

    public TreeNode getParent() {
        return parent;
    }

    public List<TreeNode> getChildren() {
        return children;
    }

    public String getLabel() {
        return label;
    }

    public int getDepth() {
        return depth;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public int getColorResId() {
        return colorResId;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getPath() {
        if (getParent() == null) {
            return getLabel();
        } else {
            return getParent().getPath() + "/" + getLabel();
        }
    }

    @Override
    public String toString() {
        return "TreeNode{" +
                "label='" + label + '\'' +
                ", depth=" + depth +
                ", weight=" + weight +
                ", colorResId=" + colorResId +
                ", iconResId=" + iconResId +
                '}';
    }
}
