import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.*;

public class ImageServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("image/png");

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:figures.db");

            stmt = conn.prepareStatement("SELECT raster_image FROM images WHERE id = 1");
            rs = stmt.executeQuery();

            if (rs.next()) {
                byte[] img = rs.getBytes("raster_image");
                if (img != null) {
                    try (OutputStream out = resp.getOutputStream()) {
                        out.write(img);
                    }
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Изображение отсутствует");
                }
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Нет данных");
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Ошибка БД");
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException ignored) {}
        }
    }
}
