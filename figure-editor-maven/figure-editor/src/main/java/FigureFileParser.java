import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FigureFileParser {

    public static List<ColoredPolygon> loadFiguresFromFile(String filename) throws IOException, InvalidPolygonException {
        List<ColoredPolygon> figures = new ArrayList<>();
        List<String> lines = readAllLines(filename);

        List<String> currentBlock = new ArrayList<>();
        boolean inBlock = false;

        for (String line : lines) {
            line = line.trim();
            if (line.equals("--- FIGURE ---")) {
                if (inBlock && !currentBlock.isEmpty()) {
                    ColoredPolygon poly = parseFigureBlock(currentBlock);
                    if (poly != null) figures.add(poly);
                }
                currentBlock.clear();
                inBlock = true;
            } else if (inBlock) {
                if (!line.isEmpty()) {
                    currentBlock.add(line);
                }
            }
        }

        if (inBlock && !currentBlock.isEmpty()) {
            ColoredPolygon poly = parseFigureBlock(currentBlock);
            if (poly != null) figures.add(poly);
        }

        return figures;
    }

    private static List<String> readAllLines(String filename) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static ColoredPolygon parseFigureBlock(List<String> block) throws InvalidPolygonException {
        Map<String, String> props = new HashMap<>();
        for (String line : block) {
            int colon = line.indexOf(':');
            if (colon > 0) {
                String key = line.substring(0, colon).trim();
                String value = line.substring(colon + 1).trim();
                props.put(key, value);
            }
        }

        // Проверка обязательных полей
        if (!"ColoredPolygon".equals(props.get("class"))) {
            System.err.println("Пропущена фигура: поддерживается только ColoredPolygon");
            return null;
        }

        String name = props.getOrDefault("name", "Безымянный");
        String lineColor = props.get("lineColor");
        String fillColor = props.get("fillColor");
        String opacityStr = props.get("opacity");
        String coordsStr = props.get("coordinates");

        if (coordsStr == null) {
            throw new InvalidPolygonException("Отсутствуют координаты");
        }

        // Парсинг координат: (x1,y1) (x2,y2) ...
        List<int[]> coordList = new ArrayList<>();
        Pattern p = Pattern.compile("\\((\\d+),\\s*(\\d+)\\)");
        Matcher m = p.matcher(coordsStr);
        while (m.find()) {
            int x = Integer.parseInt(m.group(1));
            int y = Integer.parseInt(m.group(2));
            coordList.add(new int[]{x, y});
        }

        if (coordList.size() < 3) {
            throw new InvalidPolygonException("Недостаточно вершин: " + coordList.size());
        }

        int[][] coordinates = coordList.toArray(new int[0][]);

        double opacity = 1.0;
        if (opacityStr != null) {
            try {
                opacity = Double.parseDouble(opacityStr.trim());
            } catch (NumberFormatException e) {
                throw new InvalidPolygonException("Некорректная прозрачность: " + opacityStr);
            }
        }

        return new ColoredPolygon(coordinates, name, lineColor, fillColor, opacity);
    }
}
