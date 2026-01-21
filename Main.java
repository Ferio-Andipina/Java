import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("=== Режим 1: Интерактивный ввод одного многоугольника ===");
        createAndValidateSinglePolygon();

        System.out.println("\n=== Режим 2: Демонстрация коллекции и сортировок ===");
        demonstratePolygonsCollection();

        System.out.println("\n=== Режим 3: Загрузка фигур из файла ===");
        demonstrateFileLoading();
    }

    // ===  интерактивный ввод одного многоугольника ===
    private static void createAndValidateSinglePolygon() {
        Polygon polygon = null;
        while (polygon == null) {
            try {
                System.out.print("Введите количество сторон (>=3): ");
                int n = readPositiveInt();
                if (n < 3) {
                    System.out.println("Ошибка: минимум 3 стороны.");
                    continue;
                }
                double[] sides = new double[n];
                for (int i = 0; i < n; i++) {
                    System.out.print("Сторона " + (i + 1) + ": ");
                    sides[i] = readPositiveDouble();
                }
                polygon = createPolygon(sides);
                System.out.println(" Многоугольник создан: " + polygon);
            } catch (InvalidPolygonException e) {
                System.out.println(" Ошибка: " + e.getMessage());
                System.out.println("Повторите ввод.\n");
            }
        }
    }

    // === коллекция и сортировки ===
    private static void demonstratePolygonsCollection() {
        List<Polygon> polygons = new ArrayList<>();

        double[][] testData = {
            {3, 4, 5},
            {5, 5, 5},
            {2, 2, 3, 4},
            {6, 8, 10},
            {1, 1, 1}
        };

        System.out.println("Создание многоугольников из тестовых данных...");
        for (double[] sides : testData) {
            try {
                Polygon p = createPolygon(sides);
                polygons.add(p);
                System.out.println(" Добавлен: " + p);
            } catch (InvalidPolygonException e) {
                System.out.println(" Пропущен: " + Arrays.toString(sides) + " — " + e.getMessage());
            }
        }

        System.out.println("\n--- Исходный список ---");
        polygons.forEach(System.out::println);

        List<Polygon> byArea = new ArrayList<>(polygons);
        Collections.sort(byArea);
        System.out.println("\n--- Отсортировано по площади (Comparable) ---");
        byArea.forEach(System.out::println);

        List<Polygon> byPerimeterThenArea = new ArrayList<>(polygons);
        byPerimeterThenArea.sort(
            Comparator.comparingDouble(Polygon::getPerimeter)
                      .thenComparing(p -> p.getArea() < 0 ? Double.MAX_VALUE : p.getArea())
        );
        System.out.println("\n--- Отсортировано по периметру, затем по площади (Comparator) ---");
        byPerimeterThenArea.forEach(System.out::println);
    }

    // === загрузка из файла ===
    private static void demonstrateFileLoading() {
        String filename = "figures.txt";
        createSampleFile(filename);

        try {
            List<ColoredPolygon> figures = FigureFileParser.loadFiguresFromFile(filename);
            System.out.println(" Загружено " + figures.size() + " фигур:");
            figures.forEach(System.out::println);
        } catch (IOException e) {
            System.err.println("Ошибка чтения файла: " + e.getMessage());
        } catch (InvalidPolygonException e) {
            System.err.println("Ошибка в данных файла: " + e.getMessage());
        }
    }

    private static void createSampleFile(String filename) {
        try (FileWriter fw = new FileWriter(filename)) {
            fw.write("--- FIGURE ---\n");
            fw.write("class: ColoredPolygon\n");
            fw.write("name: Красный треугольник\n");
            fw.write("coordinates: (0,0) (4,0) (2,3)\n");
            fw.write("lineColor: #FF0000\n");
            fw.write("fillColor: #FFAAAA\n");
            fw.write("opacity: 0.7\n");
            fw.write("\n");
            fw.write("--- FIGURE ---\n");
            fw.write("class: ColoredPolygon\n");
            fw.write("name: Синий квадрат\n");
            fw.write("coordinates: (1,1) (5,1) (5,5) (1,5)\n");
            fw.write("lineColor: #0000FF\n");
            fw.write("fillColor: #AAAAFF\n");
            fw.write("opacity: 0.9\n");
        } catch (IOException e) {
            System.err.println("Не удалось создать пример файла: " + e.getMessage());
        }
    }


    public static Polygon createPolygon(double[] sides) throws InvalidPolygonException {
        return new Polygon(sides);
    }

    private static int readPositiveInt() {
        while (true) {
            try {
                int value = scanner.nextInt();
                if (value <= 0) {
                    System.out.print("Должно быть > 0. Повторите: ");
                    continue;
                }
                return value;
            } catch (InputMismatchException e) {
                System.out.print("Целое число! Повторите: ");
                scanner.next();
            }
        }
    }

    private static double readPositiveDouble() {
        while (true) {
            try {
                double value = scanner.nextDouble();
                if (value <= 0) {
                    System.out.print("Должно быть > 0. Повторите: ");
                    continue;
                }
                return value;
            } catch (InputMismatchException e) {
                System.out.print("Число! Повторите: ");
                scanner.next();
            }
        }
    }
}
