import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqliteFigureIO {

    private static final String DB_URL = "jdbc:sqlite:figures.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC не найден", e);
        }
    }

    // Инициализация таблиц
    public static void initDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // Таблица фигур
            String sqlFigures = 
                "CREATE TABLE IF NOT EXISTS figures (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "coordinates TEXT NOT NULL, " +
                "line_color TEXT NOT NULL, " +
                "fill_color TEXT NOT  NULL, " +
                "opacity REAL NOT NULL" +
                ");";

            // Таблица изображений
            String sqlImages = 
                "CREATE TABLE IF NOT EXISTS images (" +
                "id INTEGER PRIMARY KEY, " +
                "raster_image BLOB NOT NULL, " +
                "FOREIGN KEY(id) REFERENCES figures(id)" +
                ");";

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sqlFigures);
                stmt.execute(sqlImages);
            }
        }
    }

    // Сохранить фигуры + изображение
    public static void saveToDatabase(List<ColoredPolygon> figures, BufferedImage image) throws SQLException, IOException {
        initDatabase();

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);

            // Очистить старые данные
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM images");
                stmt.execute("DELETE FROM figures");
            }

            // Сохранить фигуры
            String insertFigure = 
                "INSERT INTO figures (name, coordinates, line_color, fill_color, opacity) " +
                "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertFigure)) {
                for (ColoredPolygon fig : figures) {
                    pstmt.setString(1, fig.getName());
                    pstmt.setString(2, coordinatesToJson(fig.getCoordinates()));
                    pstmt.setString(3, String.format("#%06X", (0xFFFFFF & fig.getLineColor().getRGB())));
                    pstmt.setString(4, String.format("#%06X", (0xFFFFFF & fig.getFillColor().getRGB())));
                    pstmt.setDouble(5, fig.getOpacity());
                    pstmt.executeUpdate();
                }
            }

            // Сохранить изображение (связываем с id=1)
            String insertImage = "INSERT INTO images (id, raster_image) VALUES (1, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertImage)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                pstmt.setBytes(1, baos.toByteArray());
                pstmt.executeUpdate();
            }

            conn.commit();
        }
    }

    // Загрузить фигуры
    public static List<ColoredPolygon> loadFromDatabase() throws SQLException, InvalidPolygonException {
        initDatabase();
        List<ColoredPolygon> figures = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM figures ORDER BY id")) {

            while (rs.next()) {
                String name = rs.getString("name");
                String coordsJson = rs.getString("coordinates");
                String lineColor = rs.getString("line_color");
                String fillColor = rs.getString("fill_color");
                double opacity = rs.getDouble("opacity");

                int[][] coords = jsonToCoordinates(coordsJson);
                ColoredPolygon fig = new ColoredPolygon(coords, name, lineColor, fillColor, opacity);
                figures.add(fig);
            }
        }

        return figures;
    }

    // Загрузить изображение (опционально)
    public static BufferedImage loadImageFromDatabase() throws SQLException, IOException {
        initDatabase();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT raster_image FROM images WHERE id = 1")) {

            if (rs.next()) {
                byte[] imgBytes = rs.getBytes("raster_image");
                ByteArrayInputStream bais = new ByteArrayInputStream(imgBytes);
                return ImageIO.read(bais);
            } else {
                return null;
            }
        }
    }

    // Вспомогательные методы
    private static String coordinatesToJson(int[][] coords) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < coords.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("[").append(coords[i][0]).append(",").append(coords[i][1]).append("]");
        }
        sb.append("]");
        return sb.toString();
    }

    private static int[][] jsonToCoordinates(String json) {
        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) {
            throw new IllegalArgumentException("Некорректный JSON координат");
        }
        json = json.substring(1, json.length() - 1);
        String[] points = json.split("\\],\\[");
        int[][] coords = new int[points.length][2];
        for (int i = 0; i < points.length; i++) {
            String p = points[i].replace("[", "").replace("]", "");
            String[] xy = p.split(",");
            coords[i][0] = Integer.parseInt(xy[0].trim());
            coords[i][1] = Integer.parseInt(xy[1].trim());
        }
        return coords;
    }
}
