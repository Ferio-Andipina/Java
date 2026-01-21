import java.util.Arrays;

public class Polygon implements Shape, Comparable<Polygon> {
    protected final double[] sides;
    protected final double area;

    public Polygon(double[] sides) throws InvalidPolygonException {
        validateSides(sides);
        this.sides = sides.clone();
        this.area = computeAreaFromSides(sides);
    }

    public Polygon(int[][] coordinates) throws InvalidPolygonException {
        if (coordinates == null || coordinates.length < 3) {
            throw new InvalidPolygonException("Многоугольник должен иметь минимум 3 вершины.");
        }

        int n = coordinates.length;
        double[] computedSides = new double[n];
        for (int i = 0; i < n; i++) {
            int[] p1 = coordinates[i];
            int[] p2 = coordinates[(i + 1) % n];
            double dx = p2[0] - p1[0];
            double dy = p2[1] - p1[1];
            computedSides[i] = Math.sqrt(dx * dx + dy * dy);
        }

        validateSides(computedSides);
        this.sides = computedSides;
        this.area = computeAreaFromCoordinates(coordinates);
    }

    private void validateSides(double[] sides) throws InvalidPolygonException {
        if (sides == null || sides.length < 3) {
            throw new InvalidPolygonException("Требуется минимум 3 стороны.");
        }
        for (int i = 0; i < sides.length; i++) {
            if (sides[i] <= 0) {
                throw new InvalidPolygonException("Сторона #" + (i + 1) + " <= 0.");
            }
        }
        double perim = Arrays.stream(sides).sum();
        for (int i = 0; i < sides.length; i++) {
            if (sides[i] >= perim - sides[i]) {
                throw new InvalidPolygonException("Сторона #" + (i + 1) + " нарушает неравенство многоугольника.");
            }
        }
    }

    private double computeAreaFromSides(double[] sides) {
        if (sides.length == 3) {
            double a = sides[0], b = sides[1], c = sides[2];
            double s = (a + b + c) / 2.0;
            return Math.sqrt(s * (s - a) * (s - b) * (s - c));
        }
        return -1; // неизвестно
    }

    private double computeAreaFromCoordinates(int[][] coords) {
        int n = coords.length;
        double area = 0.0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            area += coords[i][0] * coords[j][1];
            area -= coords[j][0] * coords[i][1];
        }
        area = Math.abs(area) / 2.0;
        return area;
    }

    @Override
    public double getPerimeter() {
        return Arrays.stream(sides).sum();
    }

    @Override
    public double getArea() {
        return area;
    }

    public double[] getSides() {
        return sides.clone();
    }

    public int getSideCount() {
        return sides.length;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Polygon{");
        sb.append("sides=").append(Arrays.toString(sides));
        sb.append(", perimeter=").append(String.format("%.2f", getPerimeter()));
        if (area >= 0) {
            sb.append(", area=").append(String.format("%.2f", area));
        } else {
            sb.append(", area=н/д");
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public int compareTo(Polygon other) {
        double a1 = this.area < 0 ? Double.MAX_VALUE : this.area;
        double a2 = other.area < 0 ? Double.MAX_VALUE : other.area;
        return Double.compare(a1, a2);
    }
}
