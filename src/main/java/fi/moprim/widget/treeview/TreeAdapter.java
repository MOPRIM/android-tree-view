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
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Created by mineraud on 03/01/17.
 */
public class TreeAdapter {

    private final ArrayList<TreeNode> rootNodes;
    private int depth;
    private ArrayList<TreeNodeChangeListener> listeners;

    public TreeAdapter() {
        this.rootNodes = new ArrayList<>();
        this.depth = 0;
        this.listeners = new ArrayList<>();
    }

    public void addTreeNodeChangeListener(TreeNodeChangeListener listener) {
        this.listeners.add(listener);
    }

    public void removeTreeNodeChangeListener(TreeNodeChangeListener listener) {
        this.listeners.remove(listener);
    }


    public void notifyDataSetChanged() {
        for (TreeNodeChangeListener listener : listeners) {
            listener.onDataSetChanged();
        }
    }

    public void notifyWeightsChanged() {
        for (TreeNodeChangeListener listener : listeners) {
            listener.onWeightsChanged();
        }
    }

    public int getDepth() {
        return depth;
    }

    public TreeNode addRootNode(String label, double weight, int colorResId, int iconResId) {
        TreeNode rootNode = TreeNode.rootNode(label, weight, colorResId, iconResId);
        if (rootNode.getDepth() > this.depth) {
            this.depth = rootNode.getDepth();
        }
        this.rootNodes.add(rootNode);
        return rootNode;
    }

    public TreeNode addRootNode(String label, int colorResId, int iconResId) {
        return addRootNode(label, 0, colorResId, iconResId);
    }

    public TreeNode addChildNode(TreeNode parent, String label, double weight,
                                 int colorResId, int iconResId) {
        TreeNode childNode = TreeNode.childNode(parent, label, weight, colorResId, iconResId);
        if (childNode.getDepth() > this.depth) {
            this.depth = childNode.getDepth();
        }
        return childNode;
    }

    public TreeNode addChildNode(TreeNode parent, String label,
                                 int colorResId, int iconResId) {
        return addChildNode(parent, label, 0, colorResId, iconResId);
    }

    public void updateWeight(double weight, String... nodePath) throws NoSuchElementException {
        List<TreeNode> possibleNodes = rootNodes;
        for (int i = 0; i < nodePath.length; i++) {
            boolean foundIt = false;
            for (TreeNode node : possibleNodes) {
                if (nodePath[i].equals(node.getLabel())) {
                    foundIt = true;
                    if (i == nodePath.length - 1) {
                        node.setWeight(weight);
                        return;
                    } else {
                        possibleNodes = node.getChildren();
                    }
                    break;
                }
            }
            if (!foundIt) {
                throw new NoSuchElementException("Could not find it: " + Arrays.toString(nodePath));
            }
        }
        throw new NoSuchElementException("Could not find it");
    }

    public void addWeight(double weight, String... nodePath) throws NoSuchElementException {
        List<TreeNode> possibleNodes = rootNodes;
        for (int i = 0; i < nodePath.length; i++) {
            boolean foundIt = false;
            for (TreeNode node : possibleNodes) {
                if (nodePath[i].equals(node.getLabel())) {
                    foundIt = true;
                    if (i == nodePath.length - 1) {
                        node.setWeight(node.getWeight() + weight);
                        return;
                    } else {
                        possibleNodes = node.getChildren();
                    }
                    break;
                }
            }
            if (!foundIt) {
                throw new NoSuchElementException("Could not find it: " + Arrays.toString(nodePath));
            }
        }
        throw new NoSuchElementException("Could not find it");
    }

    public ArrayList<TreeNode> getRootNodes() {
        return rootNodes;
    }

    private void resetWeights(List<TreeNode> nodeList) {
        for (TreeNode node : nodeList) {
            node.setWeight(0);
            resetWeights(node.getChildren());
        }
    }

    public void resetWeights() {
        resetWeights(rootNodes);
    }

    public interface TreeNodeChangeListener {
        void onDataSetChanged();
        void onWeightsChanged();
    }
}
