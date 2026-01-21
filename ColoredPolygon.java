import java.awt.*;

public class ColoredPolygon extends Polygon {
    private final String name;
    private final String lineColor;
    private final String fillColor;
    private final double opacity;
    private int[][] coordinates;

    public ColoredPolygon(int[][] coordinates, String name, String lineColor, String fillColor, double opacity)
            throws InvalidPolygonException {
        super(coordinates); // валидация
        this.coordinates = deepCopy(coordinates);
        this.name = (name == null || name.trim().isEmpty()) ? "Безымянный" : name.trim();
        this.lineColor = validateColor(lineColor, "lineColor");
        this.fillColor = validateColor(fillColor, "fillColor");
        if (opacity < 0.0 || opacity > 1.0) {
            throw new IllegalArgumentException("Прозрачность должна быть в диапазоне [0.0, 1.0]");
        }
        this.opacity = opacity;
    }

    private int[][] deepCopy(int[][] src) {
        int[][] copy = new int[src.length][];
        for (int i = 0; i < src.length; i++) {
            copy[i] = src[i].clone();
        }
        return copy;
    }

    private String validateColor(String color, String fieldName) {
        if (color == null || !color.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("Некорректный формат цвета в " + fieldName + ": " + color + " (ожидается #RRGGBB)");
        }
        return color;
    }

    // Проверка попадания точки в многоугольник
    public boolean contains(int x, int y) {
        java.awt.Polygon p = getAWTPolygon();
        return p.contains(x, y);
    }

    // Получить AWT-полигон (для отрисовки и contains)
    public java.awt.Polygon getAWTPolygon() {
        int n = coordinates.length;
        int[] xPoints = new int[n];
        int[] yPoints = new int[n];
        for (int i = 0; i < n; i++) {
            xPoints[i] = coordinates[i][0];
            yPoints[i] = coordinates[i][1];
        }
        return new java.awt.Polygon(xPoints, yPoints, n);
    }

    // Переместить фигуру на (dx, dy)
    public void moveBy(int dx, int dy) {
        for (int[] point : coordinates) {
            point[0] += dx;
            point[1] += dy;
        }
    }

    // Геттеры
    public Color getLineColor() { return Color.decode(lineColor); }
    public Color getFillColor() { return Color.decode(fillColor); }
    public double getOpacity() { return opacity; }
    public String getName() { return name; }
    public int[][] getCoordinates() { return deepCopy(coordinates); }
}
