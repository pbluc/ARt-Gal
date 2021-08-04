package com.fbu.pbluc.artgal.trees;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.*;

public class LatLngKDTree {

  private static final int K = 3; // 3-d tree
  private List<Node> nodes;
  private Node tree;


  public LatLngKDTree(List<LatLng> locations) {
    nodes = new ArrayList<>(locations.size());
    for (LatLng location : locations) {
      nodes.add(new Node(location));
    }
    tree = buildTree(nodes, 0);
  }

  private static Node buildTree(List<Node> items, int depth) {
    if (items.isEmpty()) {
      return null;
    }

    Collections.sort(items, getComparator(depth % K));

    int index = items.size() / 2;
    Node root = items.get(index);

    root.left = buildTree(items.subList(0, index), depth + 1);
    root.right = buildTree(items.subList(index + 1, items.size()), depth + 1);

    return root;
  }

  public List<LatLng> findNearestMarkers(LatLng currentLocation, double withinRadius) {
    Node currentLocNode = new Node(currentLocation);
    List<LatLng> nearestMarkers = new ArrayList<>();
    int size = nodes.size();
    for (int i = 0; i < size; i++) {
      Node node = findNearestMarker(tree, new Node(currentLocation), 0);
      if (node != null) {
        if (node.calculateHaversineDistance(currentLocNode) <= withinRadius) {
          nearestMarkers.add(new LatLng(node.convertPointToLatLng().latitude, node.convertPointToLatLng().longitude));
        }
        deleteNode(tree, node.point);
      }
    }
    return nearestMarkers;
  }

  private static Node findNearestMarker(Node current, Node target, int depth) {
    int axis = depth % K;
    int direction = getComparator(axis).compare(target, current);

    Node next = (direction < 0) ? current.left : current.right;
    Node other = (direction < 0) ? current.right : current.left;
    Node nearest = (next == null) ? current : findNearestMarker(next, target, depth + 1);

    if (current.calculateEuclideanDistance(target) < nearest.calculateEuclideanDistance(target)) {
      nearest = current;
    }

    if (other != null) {
      if (current.calculateAxisDistance(target, axis) < nearest.calculateEuclideanDistance(target)) {

        Node possiblyNearer = findNearestMarker(other, target, depth + 1);
        if (possiblyNearer.calculateEuclideanDistance(target) < nearest.calculateEuclideanDistance(target)) {
          nearest = possiblyNearer;
        }
      }
    }
    return nearest;
  }

  private Node deleteNode(Node root, double[] point) {
    return deleteNode(root, point, 0);
  }

  private Node deleteNode(Node root, double[] point, int depth) {
    // If point does not exist
    if (root == null) {
      return null;
    }

    // Determine the axis we are on for current node
    int axis = depth % K;

    // If root has the point we want to delete
    if (root.point.equals(point)) {
      // If right child is not NULL
      if (root.right != null) {
        // Find minimum of root's dimension in right subtree
        Node minimum = findMinimumDimension(root.right, axis);
        // Copy this minimum to root
        copyPointOver(root.point, minimum.point);
        // Recursively delete this minimum
        root.right = deleteNode(root.right, minimum.point, depth+1);
      } else if (root.left != null) { // Follows the same on the left subtree as above
        Node minimum = findMinimumDimension(root.left, axis);
        copyPointOver(root.point, minimum.point);
        root.right = deleteNode(root.left, minimum.point, depth+1);
      } else { // Leaf node is being deleted
        return null;
      }
      return root;
    }

    // If the current node doesn't contain point, keep moving downwards
    if (point[axis] < root.point[axis]) {
      root.left = deleteNode(root.left, point, depth + 1);
    } else {
      root.right = deleteNode(root.right, point, depth + 1);
    }
    return root;
  }

  // Uses recursion to find minimum of the dimension in tree
  private Node findMinimumDimension(Node root, int dimension, int depth) {
    // Base case
    if (root == null) {
      return null;
    }

    // Current dimension is computed using current depth and total dimensions (k)
    int axis = depth % K;

    // Compare point with root with respect to current dimension
    if (axis == dimension) {
      if (root.left == null) {
        return root;
      }
      return findMinimumDimension(root.left, dimension, depth+1);
    }

    // If current dimension is different then minimum can be anywhere in this subtree
    return minNode(root,
        findMinimumDimension(root.left, dimension, depth+1),
        findMinimumDimension(root.right, dimension, depth+1), dimension);
  }

  // Wrapper helper function
  private Node findMinimumDimension(Node root, int dimension) {
    // Gives current depth as 0
    return findMinimumDimension(root, dimension, 0);
  }

  // Function using for finding minimum of three integers
  private Node minNode(Node x, Node y, Node z, int d) {
    Node result = x;
    if (y != null && y.point[d] < result.point[d]) {
      result = y;
    }
    if (z != null && z.point[d] < result.point[d]) {
      result = z;
    }
    return result;
  }

  // Copies one point over to another
  private void copyPointOver(double[] p1, double[] p2) {
    for (int i = 0; i < K; i++) {
      p1[i] = p2[i];
    }
  }

  private static Comparator<Node> getComparator(int i) {
    return NodeComparator.values()[i];
  }

  private enum NodeComparator implements Comparator<Node> {
    x {
      @Override
      public int compare(Node o1, Node o2) {
        return Double.compare(o1.point[0], o2.point[0]);
      }
    },
    y {
      @Override
      public int compare(Node o1, Node o2) {
        return Double.compare(o1.point[1], o2.point[1]);
      }
    },
    z {
      @Override
      public int compare(Node o1, Node o2) {
        return Double.compare(o1.point[2], o2.point[2]);
      }
    }
  }

  class Node {
    Node left;
    Node right;
    final double[] point = new double[K];

    public Node(double latitude, double longitude) {
      point[0] = (Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(longitude)));
      point[1] = (Math.cos(Math.toRadians(latitude)) * Math.sin(Math.toRadians(longitude)));
      point[2] = (Math.sin(Math.toRadians(latitude)));
    }

    public Node(LatLng location) {
      this(location.latitude, location.longitude);
    }

    public LatLng convertPointToLatLng() {
      double latitude = Math.toDegrees(Math.asin(point[2]));
      double longitude = Math.toDegrees(Math.atan2(point[1], point[0]));
      return new LatLng(latitude, longitude);
    }

    public double calculateEuclideanDistance(Node other) {
      double x = this.point[0] - other.point[0];
      double y = this.point[1] - other.point[1];
      double z = this.point[2] - other.point[2];

      return (x * x) + (y * y) + (z * z);
    }

    public double calculateAxisDistance(Node other, int axis) {
      double d = this.point[axis] - other.point[axis];
      return d * d;
    }

    public double calculateHaversineDistance(Node other) {
      LatLng thisLoc = this.convertPointToLatLng();
      LatLng otherLoc = other.convertPointToLatLng();
      // Distance between latitudes and longitudes
      double dLat = Math.toRadians(otherLoc.latitude - thisLoc.latitude);
      double dLon = Math.toRadians(otherLoc.longitude - thisLoc.longitude);

      // Apply the Haversine distance formulae
      double a = Math.pow(Math.sin(dLat / 2), 2) +
          Math.pow(Math.sin(dLon / 2), 2) *
              Math.cos(Math.toRadians(thisLoc.latitude)) *
              Math.cos(Math.toRadians(otherLoc.latitude));
      double rad = 6371;
      double c = 2 * Math.asin(Math.sqrt(a));
      return (rad * c) / 1.609344;
    }
  }
}

