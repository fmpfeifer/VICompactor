/*
Copyright 2014-2014
Fabio Melo Pfeifer

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
*/
package org.pfeifer.blockreader.datastructure;

/**
 *
 * @author Fabio Melo Pfeifer <fmpfeifer@gmail.com>
 * @param <T>
 */
public class SelfBalancingBST<T> {

    private TreeNode<T> root = null;

    public SelfBalancingBST() {
    }

    public boolean isEmpty() {
        return root == null;
    }

    public void put(long key, T value) {
        root = put(key, value, root);
    }

    private int height(TreeNode t) {
        return t == null ? -1 : t.height;
    }

    private TreeNode<T> put(long key, T value, TreeNode<T> t) {
        if (t == null) {
            t = new TreeNode(key, value);
        } else if (key < t.key) {
            t.left = put(key, value, t.left);
            if (height(t.left) - height(t.right) == 2) {
                if (key < t.left.key) {
                    t = rotateWithLeftChild(t);
                } else {
                    t = doubleWithLeftChild(t);
                }
            }
        } else if (key > t.key) {
            t.right = put(key, value, t.right);
            if (height(t.right) - height(t.left) == 2) {
                if (key > t.right.key) {
                    t = rotateWithRightChild(t);
                } else {
                    t = doubleWithRightChild(t);
                }
            }
        }
        t.height = Math.max(height(t.left), height(t.right)) + 1;
        return t;
    }

    private TreeNode<T> rotateWithLeftChild(TreeNode<T> k2) {
        TreeNode<T> k1 = k2.left;
        k2.left = k1.right;
        k1.right = k2;
        k2.height = Math.max(height(k2.left), height(k2.right)) + 1;
        k1.height = Math.max(height(k1.left), k2.height) + 1;
        return k1;
    }

    private TreeNode<T> rotateWithRightChild(TreeNode<T> k1) {
        TreeNode<T> k2 = k1.right;
        k1.right = k2.left;
        k2.left = k1;
        k1.height = Math.max(height(k1.left), height(k1.right)) + 1;
        k2.height = Math.max(height(k2.right), k1.height) + 1;
        return k2;
    }

    private TreeNode<T> doubleWithLeftChild(TreeNode<T> k3) {
        k3.left = rotateWithRightChild(k3.left);
        return rotateWithLeftChild(k3);
    }

    private TreeNode<T> doubleWithRightChild(TreeNode<T> k1) {
        k1.right = rotateWithLeftChild(k1.right);
        return rotateWithRightChild(k1);
    }

    public int size() {
        return size(root);
    }

    private int size(TreeNode<T> r) {
        if (r == null) {
            return 0;
        } else {
            int s = 1;
            s += size(r.left);
            s += size(r.right);
            return s;
        }
    }

    public T searchLessOrEqual(long key) {
        return searchLessOrEqual(root, key);
    }

    private T searchLessOrEqual(TreeNode<T> r, long key) {
        T resp = null;
        while ((r != null) && resp == null) {
            if (key < r.key) {
                return searchLessOrEqual(r.left, key);
            } else if (key > r.key) {
                if (r.right == null) {
                    return r.value;
                } else {
                    resp = searchLessOrEqual(r.right, key);
                    if (resp == null) {
                        return r.value;
                    }
                    return resp;
                }
            } else {
                return r.value;
            }
        }
        return resp;
    }

    private static class TreeNode<T> {
        TreeNode<T> left;
        TreeNode<T> right;
        long key;
        int height;
        T value;

        public TreeNode(long key, T value) {
            left = null;
            right = null;
            this.key = key;
            height = 0;
            this.value = value;
        }
    }
}
