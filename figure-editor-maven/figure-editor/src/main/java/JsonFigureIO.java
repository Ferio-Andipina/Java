import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class JsonFigureIO {

    public static void saveFiguresToJson(List<ColoredPolygon> figures, String filename) throws IOException {
        JSONArray array = new JSONArray();
        for (ColoredPolygon fig : figures) {
            JSONObject obj = new JSONObject();
            obj.put("name", fig.getName());

            // Преобразуем Color в #RRGGBB
            obj.put("lineColor", String.format("#%06X", (0xFFFFFF & fig.getLineColor().getRGB())));
            obj.put("fillColor", String.format("#%06X", (0xFFFFFF & fig.getFillColor().getRGB())));

            obj.put("opacity", fig.getOpacity());

            JSONArray coords = new JSONArray();
            int[][] points = fig.getCoordinates();
            for (int[] pt : points) {
                JSONArray point = new JSONArray();
                point.put(pt[0]);
                point.put(pt[1]);
                coords.put(point);
            }
            obj.put("coordinates", coords);

            array.put(obj);
        }

        try (FileWriter fw = new FileWriter(filename)) {
            fw.write(array.toString(2));
        }
    }

    public static List<ColoredPolygon> loadFiguresFromJson(String filename) throws IOException, InvalidPolygonException {
        String content = new String(Files.readAllBytes(Paths.get(filename)));
        JSONArray array = new JSONArray(content);
        List<ColoredPolygon> figures = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            String name = obj.getString("name");
            String lineColor = obj.getString("lineColor");  // ← теперь это строка вида "#FF0000"
            String fillColor = obj.getString("fillColor");
            double opacity = obj.getDouble("opacity");

            JSONArray coordsArr = obj.getJSONArray("coordinates");
            int[][] coords = new int[coordsArr.length()][2];
            for (int j = 0; j < coordsArr.length(); j++) {
                JSONArray point = coordsArr.getJSONArray(j);
                coords[j][0] = point.getInt(0);
                coords[j][1] = point.getInt(1);
            }

            ColoredPolygon fig = new ColoredPolygon(coords, name, lineColor, fillColor, opacity);
            figures.add(fig);
        }

        return figures;
    }
}
