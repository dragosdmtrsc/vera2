package org.change.v2.stategraph;

public class Gauss {

    public static double[][] gaussPartialPivot(double[][] matrix, int[] permutation) {

        int m = matrix.length;
        int n = matrix[0].length;
        for (int i = 0; i < n; i++)
            permutation[i] = i;
        for (int k = 0; k < Math.min(m, n); k++) { //for k = 1:n
            int max = -1; //used to track pivot point
            if ((k + 1) < Math.min(m, n)) { //zero indexed, so it checks if there is another row
                max = findLargestPivot(matrix, k); //choose ip(k) such that |A(ip,k)|= max{|A(i,k)|: i >= k}
            }
            if (max == -1)
                return matrix;
            if (matrix[max][k] == 0)
                return matrix;
            double[] tmp = matrix[max];
            matrix[max] = matrix[k];
            matrix[k] = tmp;
            int initPmax = permutation[max];
            int initPk = permutation[k];
            permutation[initPk] = initPmax;
            permutation[initPmax] = initPk;

            for (int i = k + 1; i < m; i++) {
                double f = matrix[i][k] / matrix[k][k];
                for (int j = k + 1; j < n; j++) {
                    matrix[i][j] = matrix[i][j] - matrix[k][j] * f;
                }
                matrix[i][k] = 0;
            }
        }
        return matrix;
    }

    public static double[][] gaussPartialPivot(double[][] matrix) {
        int m = matrix.length;
        int n = matrix[0].length;
        for (int k = 0; k < Math.min(m, n); k++) { //for k = 1:n
            int max = -1; //used to track pivot point
            if ((k + 1) < Math.min(m, n)) { //zero indexed, so it checks if there is another row
                max = findLargestPivot(matrix, k); //choose ip(k) such that |A(ip,k)|= max{|A(i,k)|: i >= k}
            }
            if (max == -1)
                return matrix;
            if (matrix[max][k] == 0)
                return matrix;
            double[] tmp = matrix[max];
            matrix[max] = matrix[k];
            matrix[k] = tmp;

            for (int i = k + 1; i < m; i++) {
                double f = matrix[i][k] / matrix[k][k];
                for (int j = k + 1; j < n; j++) {
                    matrix[i][j] = matrix[i][j] - matrix[k][j] * f;
                }
                matrix[i][k] = 0;
            }
        }
        return matrix;
    }

    private static int findLargestPivot(double[][] matrix, int col) {
        int maxRow = col;
        double maxValue = 0;
        for (int row = col; row < matrix.length; row++) {
            if (Math.abs(matrix[row][col]) > maxValue && Math.abs(matrix[row][col]) > 0) {
                maxRow = row;
                maxValue = Math.abs(matrix[row][col]);
            }
        }
        return maxRow;
    }
}
