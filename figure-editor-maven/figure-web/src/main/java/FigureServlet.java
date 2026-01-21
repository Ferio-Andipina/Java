import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FigureServlet extends HttpServlet {

    private String getDbPath(ServletContext context) {
        String path = context.getInitParameter("db.path");
        return path != null ? path : "figures.db";
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        String dbPath = getDbPath(getServletContext());

        try {
            // Десериализация фигур из тела запроса
            List<ColoredPolygon> figures = (List<ColoredPolygon>) 
                new ObjectInputStream(req.getInputStream()).readObject();

            // Сохранение в БД (как в SqliteFigureIO, но упрощённо)
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
                // Создание таблиц
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(
                        "CREATE TABLE IF NOT EXISTS figures (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT, coordinates TEXT, line_color TEXT, fill_color TEXT, opacity REAL);"
                    );
                    stmt.execute("DELETE FROM figures");
                }

                String sql = "INSERT INTO figures (name, coordinates, line_color, fill_color, opacity) VALUES (?,?,?,?,?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    for (ColoredPolygon fig : figures) {
                        pstmt.setString(1, fig.getName());
                        pstmt.setString(2, coordsToJson(fig.getCoordinates()));
                        pstmt.setString(3, colorToHex(fig.getLineColor()));
                        pstmt.setString(4, colorToHex(fig.getFillColor()));
                        pstmt.setDouble(5, fig.getOpacity());
                        pstmt.executeUpdate();
                    }
                }
            }

            resp.getWriter().println("OK");
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Error: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/octet-stream");
        String dbPath = getDbPath(getServletContext());

        try {
            List<ColoredPolygon> figures = new ArrayList<>();
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM figures")) {

                while (rs.next()) {
                    String name = rs.getString("name");
                    String coordsJson = rs.getString("coordinates");
                    String lineColor = rs.getString("line_color");
                    String fillColor = rs.getString("fill_color");
                    double opacity = rs.getDouble("opacity");
                    int[][] coords = jsonToCoords(coordsJson);
                    figures.add(new ColoredPolygon(coords, name, lineColor, fillColor, opacity));
                }
            }

            // Сериализация в ответ
            try (ObjectOutputStream oos = new ObjectOutputStream(resp.getOutputStream())) {
                oos.writeObject(figures);
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // Вспомогательные методы (те же, что в SqliteFigureIO)
    private String coordsToJson(int[][] coords) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < coords.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("[").append(coords[i][0]).append(",").append(coords[i][1]).append("]");
        }
        sb.append("]");
        return sb.toString();
    }

    private int[][] jsonToCoords(String json) {
        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) 
            throw new IllegalArgumentException("Invalid coords");
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

    private String colorToHex(java.awt.Color c) {
        return String.format("#%06X", (0xFFFFFF & c.getRGB()));
    }
}
