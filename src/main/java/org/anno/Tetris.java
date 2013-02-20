package org.anno;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

/**
 * @author anno.langen@gmail.com (Anno Langen)
 */
public class Tetris {

  enum Color {

    RED, BLUE, YELLOW
  }

  static class Piece {

    static final Piece[] ALL = {
        new Piece(Color.BLUE, new int[][]{{0, 0, 0}, {1, 0, 0}, {2, 0, 0}, {0, 1, 0}, {0, 2, 0}, {2, 0, 1}}),
        new Piece(Color.BLUE, new int[][]{{0, 0, 0}, {1, 0, 0}, {2, 0, 0}, {0, 1, 0}, {0, 2, 0}, {1, 0, 1}}),
        new Piece(Color.BLUE, new int[][]{{0, 0, 0}, {1, 0, 0}, {2, 0, 0}, {2, 1, 0}, {3, 1, 0}}),
        new Piece(Color.BLUE, new int[][]{{1, 0, 0}, {2, 0, 0}, {2, 0, 1}, {1, 1, 0}, {0, 1, 0}}),
        new Piece(Color.RED, new int[][]{{0, 0, 0}, {1, 0, 0}, {0, 0, 1}, {1, 1, 0}, {2, 1, 0}, {1, 2, 0}}),
        new Piece(Color.RED, new int[][]{{0, 0, 0}, {1, 0, 0}, {2, 0, 0}, {0, 1, 0}, {0, 2, 0}}),
        new Piece(Color.RED, new int[][]{{0, 0, 0}, {1, 0, 0}, {2, 0, 0}, {3, 0, 0}, {1, 1, 0}}),
        new Piece(Color.RED, new int[][]{{0, 0, 0}, {1, 0, 0}, {0, 1, 0}, {1, 1, 0}, {0, 0, 1}}),
        new Piece(Color.YELLOW, new int[][]{{0, 0, 0}, {1, 0, 0}, {2, 0, 0}, {0, 0, 1}, {0, 1, 1}}),
        new Piece(Color.YELLOW, new int[][]{{0, 0, 0}, {1, 0, 0}, {2, 0, 0}, {1, 1, 0}, {1, 1, 1}}),
        new Piece(Color.YELLOW, new int[][]{{0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {1, 1, 1}, {2, 1, 1}}),
        new Piece(Color.YELLOW, new int[][]{{0, 0, 1}, {1, 0, 1}, {2, 0, 1}, {1, 0, 2}, {1, 1, 1}, {1, 1, 0}})};

    final Color color;
    final long initialPositions;

    Piece(Color color, int[][] cubes) {
      long bits = 0;
      for (int[] cube : cubes) {
        bits += (1L << indexFromVector(cube));
      }
      this.color = color;
      initialPositions = bits;
    }

    class Placement {

      final long positions;
      public Placement(long positions) {
        this.positions = positions;
      }

      @Override
      public String toString() {
        StringBuilder buf = new StringBuilder();
        for (int y = 4; --y >= 0;) {
          appendFourRowLine(buf, y);
        }
        return buf.toString();
      }

      private void appendFourRowLine(StringBuilder buf, int y) {
        int start = y << 2;
        buf.append('\n');
        appendRow(buf, start);
        buf.append(' ');
        appendRow(buf, start + 16);
        buf.append(' ');
        appendRow(buf, start + 32);
        buf.append(' ');
        appendRow(buf, start + 48);
      }

      private void appendRow(StringBuilder buf, int start) {
        buf.append((positions & (1L << start++)) != 0 ? 'X' : '_');
        buf.append((positions & (1L << start++)) != 0 ? 'X' : '_');
        buf.append((positions & (1L << start++)) != 0 ? 'X' : '_');
        buf.append((positions & (1L << start)) != 0 ? 'X' : '_');
      }

      public Piece getPiece() {
        return Piece.this;
      }
    }

    ArrayList<Placement> allShifts() {
      int[] max = getBoundingBox(initialPositions);
      ArrayList<Placement> result = new ArrayList<Placement>();
      long xbits = initialPositions;
      for (int xShift = 4 - max[0]; --xShift >= 0;) {
        long ybits = xbits;
        for (int y = 4 - max[1]; --y >= 0;) {
          long zbits = ybits;
          for (int z = 4 - max[2]; --z >= 0;) {
            result.add(new Placement(zbits));
            zbits = Transform.Z.apply(zbits);
          }
          ybits = Transform.Y.apply(ybits);
        }
        xbits = Transform.X.apply(xbits);
      }
      return result;
    }

    ArrayList<Placement> allPlacements() {
      ArrayList<Placement> result = new ArrayList<Placement>();
      for (Placement placement : allShifts()) {
        addAllRotations(placement.positions, result);
      }
      return result;
    }

    private Collection<Placement> addAllRotations(long positions, Collection<Placement> result) {
      for (Transform transform : Transform.ROTATIONS) {
        result.add(newPlacement(transform.apply(positions)));
      }
      return result;
    }

    private Placement newPlacement(long positions) {
      return new Placement(positions);
    }

    private static int[] getBoundingBox(long positions) {
      int[] max = new int[3];
      for (int i = 64; --i >= 0; ) {
        if ((positions & (1L << i)) != 0) {
          int[] v = asVector(i);
          for (int d = 3; --d >= 0; ) {
            if (v[d] > max[d]) max[d] = v[d];
          }
        }
      }
      return max;
    }
  }

  static class Transform {

    final int[] map;
    static final Transform X = new Transform(shift(0));
    static final Transform Y = new Transform(shift(1));
    static final Transform Z = new Transform(shift(2));
    static final Transform[] ROTATIONS = getAllRotations();

    public Transform(int[] map) {
      this.map = map;
    }

    public Transform(int[][] matrix) {
      map = new int[64];
      for (int i = 64; --i >= 0;) {
        map[i] = indexFromCenteredVector(matrixApply(matrix, asCenteredVector(i)));
      }
    }

    public long apply(long placement) {
      long result = 0;
      for (int i = 64; --i >= 0;) {
        if ((placement & (1L << i)) != 0) {
          result += (1L << map[i]);
        }
      }
      return result;
    }
  }

  static class Rotation extends Transform {

    final int[][] matrix;

    public Rotation(int[][] matrix) {
      super(matrix);
      this.matrix = new int[3][];
      for (int i = 3; --i >=0;) {
        this.matrix[i] = new int[3];
        System.arraycopy(matrix[i], 0, this.matrix[i], 0, 3);
      }
    }
  }

  static int[] matrixApply(int[][] m, int[] v) {
    return new int[]{dotProduct(m[0], v), dotProduct(m[1], v), dotProduct(m[2], v)};
  }

  static int dotProduct(int[] u, int[] v) {
    return u[0] * v[0] + u[1] * v[1] + u[2] * v[2];
  }

  static int[] asVector(int index) {
    int x = index & 3;
    int y = (index & 15) >> 2;
    int z = (index >> 4) & 3;
    return new int[]{x, y, z};
  }

  static int[] asCenteredVector(int index) {
    int[] v = asVector(index);
    return new int[]{2 * v[0] - 3, 2 * v[1] - 3, 2 * v[2] - 3};
  }

  static int indexFromVector(int... v) {
    return v[0] + (v[1] << 2) + (v[2] << 4);
  }

  static int indexFromCenteredVector(int[] v) {
    return ((v[0] + 3) >> 1) + ((v[1] + 3) << 1) + ((v[2] + 3) << 3);
  }

  /**
   * Returns vector that is perpendicular to the given two vectors with the direction determined by the right-hand rule.
   * See <a href='http://en.wikipedia.org/wiki/Cross_product'>Cross product</a> for more info.
   */
  static int[] crossProduct(int[] u, int[] v) {
    return new int[]{u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0]};
  }

  private static Transform[] getAllRotations() {
    int[][] matrix = new int[3][];

    ArrayList<Transform> list = new ArrayList<Transform>(24);
    for (int i = 3; --i >= 0;) {
      addAllRotations(matrix, i, 1, list);
      addAllRotations(matrix, i, -1, list);
    }
    return list.toArray(new Transform[list.size()]);
  }

  private static void addAllRotations(int[][] matrix, int x, int sx, List<Transform> transforms) {
    matrix[0] = new int[3];
    matrix[0][x] = sx;
    for (int y = 3; --y >= 0;) {
      if (x != y) {
        addAllYZRotations(matrix, y, 1, transforms);
        addAllYZRotations(matrix, y, -1, transforms);
      }
    }
  }

  private static void addAllYZRotations(int[][] matrix, int y, int sy, List<Transform> transforms) {
    matrix[1] = new int[3];
    matrix[1][y] = sy;
    matrix[2] = crossProduct(matrix[0], matrix[1]);
    transforms.add(new Rotation(matrix));
  }

  static int[] shift(int index) {
    int[] result = new int[64];
    for (int i = 64; --i >= 0;) {
      int[] v = asVector(i);
      v[index] += 1;
      result[i] = indexFromVector(v) % 64;
    }
    return result;
  }

  static class Solution {
    final List<Piece.Placement> placements = new ArrayList<Piece.Placement>();

    public Solution(List<Piece.Placement> placements) {
      this.placements.addAll(placements);
    }

    int[] getPieces() {
      int[] pieces = new int[64];
      for (int i = 0; i < placements.size(); i++) {
        Piece.Placement placement = placements.get(i);
        long bits = placement.positions;
        for (int j = 64; --j >= 0;) {
          if ((bits & (1L << j)) != 0) {
            pieces[j] = i;
          }
        }
      }
      return pieces;
    }

    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder();
      int[] pieces = getPieces();
      for (int y = 4; --y >=0;) {
        buf.append('\n');
        for (int z = 0; z < 4; z++) {
          for (int x = 0; x < 4; x++) {
            buf.append(pieces[indexFromVector(x,y,z)]);
          }
          buf.append(' ');
        }
      }
      return buf.toString();
    }
  }

  public static void main(String[] args) {
    List<List<Piece.Placement>> placementsList = new ArrayList<List<Piece.Placement>>();
    placementsList.add(Piece.ALL[0].allShifts());
    for (int i = 1; i < Piece.ALL.length; i++) {
      placementsList.add(Piece.ALL[i].allPlacements());
    }
    List<Solution> solutions = addAll(placementsList, new ArrayList<Piece.Placement>(), 0L, new ArrayList<Solution>());
    System.out.println(solutions);
  }

  private static List<Solution> addAll(List<List<Piece.Placement>> placementsList, List<Piece.Placement> solution,
      long bits, List<Solution> solutions) {
    int n = solution.size();
    if (n == placementsList.size()) {
      solutions.add(new Solution(solution));
    } else {
      for (Piece.Placement placement : placementsList.get(n)) {
        if ((placement.positions & bits) == 0) {
          solution.add(placement);
          addAll(placementsList, solution, bits | placement.positions, solutions);
          solution.remove(n);
        }
      }
    }
    return solutions;
  }
}
