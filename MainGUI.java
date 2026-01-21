import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainGUI extends JFrame {
    private JPanel figurePanel;
    private JScrollPane figureScrollPane;
    private DrawingPanel drawingPanel;
    private List<FigureItem> currentFigures = new ArrayList<>();

    public MainGUI() {
        setTitle("Редактор фигур");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        // === Верхняя панель с кнопками ===
        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton openButton = new JButton("Открыть");
        JButton closeButton = new JButton("Закрыть");
        leftButtons.add(openButton);
        leftButtons.add(closeButton);

        JPanel centerButtons = new JPanel();

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Сохранить как PNG");
        JButton exitButton = new JButton("Выйти");
        rightButtons.add(saveButton);
        rightButtons.add(exitButton);

        topPanel.add(leftButtons, BorderLayout.WEST);
        topPanel.add(centerButtons, BorderLayout.CENTER);
        topPanel.add(rightButtons, BorderLayout.EAST);

        // === Левая панель: чекбоксы ===
        figurePanel = new JPanel();
        figurePanel.setLayout(new BoxLayout(figurePanel, BoxLayout.Y_AXIS));
        figurePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        figureScrollPane = new JScrollPane(figurePanel);
        figureScrollPane.setBorder(BorderFactory.createTitledBorder("Фигуры"));

        // === Правая панель: рисование ===
        drawingPanel = new DrawingPanel();

        // === Разделение ===
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, figureScrollPane, drawingPanel);
        centerSplit.setDividerLocation(300);

        // === Сборка ===
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(centerSplit, BorderLayout.CENTER);

        // === Обработчики кнопок ===
        openButton.addActionListener(e -> openFile());
        closeButton.addActionListener(e -> clearFigures());
        saveButton.addActionListener(e -> saveImage());
        exitButton.addActionListener(e -> System.exit(0));
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Выберите файл с фигурами");
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                List<ColoredPolygon> figures = FigureFileParser.loadFiguresFromFile(
                    fileChooser.getSelectedFile().getAbsolutePath()
                );
                loadFigures(figures);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка чтения файла:\n" + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            } catch (InvalidPolygonException ex) {
                JOptionPane.showMessageDialog(this, "Некорректные данные в файле:\n" + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadFigures(List<ColoredPolygon> figures) {
        figurePanel.removeAll();
        currentFigures.clear();

        for (ColoredPolygon fig : figures) {
            FigureItem item = new FigureItem(fig);
            currentFigures.add(item);

            JCheckBox checkBox = new JCheckBox(item.toString());
            checkBox.setSelected(true);
            checkBox.addActionListener(e -> {
                item.setVisible(checkBox.isSelected());
                drawingPanel.repaint();
            });

            checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            figurePanel.add(checkBox);
        }

        figurePanel.revalidate();
        figurePanel.repaint();
        drawingPanel.repaint();
    }

    private void clearFigures() {
        figurePanel.removeAll();
        currentFigures.clear();
        figurePanel.revalidate();
        figurePanel.repaint();
        drawingPanel.repaint();
    }

    private void saveImage() {
        if (currentFigures.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Нет фигур для сохранения!", "Предупреждение", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить изображение как...");
        fileChooser.setSelectedFile(new File("figures.png"));
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".png")) {
                file = new File(file.getPath() + ".png");
            }

            try {
                BufferedImage image = drawingPanel.createImage();
                ImageIO.write(image, "png", file);
                JOptionPane.showMessageDialog(this, "Изображение сохранено:\n" + file.getAbsolutePath(), "Успех", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка сохранения:\n" + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Внутренний класс для отрисовки с поддержкой выделения и перетаскивания
    private class DrawingPanel extends JPanel {
        private FigureItem selectedFigure = null;
        private int dragOffsetX = 0;
        private int dragOffsetY = 0;
        private java.awt.Polygon dragPreview = null;

        public DrawingPanel() {
            setBorder(BorderFactory.createTitledBorder("Рисунок"));
            setBackground(Color.WHITE);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleMousePressed(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    handleMouseReleased(e);
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    handleMouseDragged(e);
                }
            });
        }

        private void handleMousePressed(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();

            FigureItem clicked = null;
            // Поиск с конца (сверху вниз по Z-порядку)
            for (int i = currentFigures.size() - 1; i >= 0; i--) {
                FigureItem item = currentFigures.get(i);
                if (item.isVisible() && item.getFigure().contains(x, y)) {
                    clicked = item;
                    break;
                }
            }

            if (clicked != null) {
                selectedFigure = clicked;
                // Поднимаем выделенную фигуру наверх
                currentFigures.remove(selectedFigure);
                currentFigures.add(selectedFigure);

                // Вычисляем смещение курсора относительно фигуры
                java.awt.Polygon poly = selectedFigure.getFigure().getAWTPolygon();
                dragOffsetX = x - poly.getBounds().x;
                dragOffsetY = y - poly.getBounds().y;

                repaint();
            } else {
                selectedFigure = null;
                repaint();
            }
        }

        private void handleMouseDragged(MouseEvent e) {
            if (selectedFigure != null) {
                // Стираем предыдущий XOR-контур
                if (dragPreview != null) {
                    Graphics g = getGraphics();
                    if (g != null) {
                        g.setXORMode(Color.WHITE);
                        g.drawPolygon(dragPreview);
                        g.dispose();
                    }
                }

                // Текущее положение мыши
                int mouseX = e.getX();
                int mouseY = e.getY();

                // Оригинальные координаты фигуры
                java.awt.Polygon original = selectedFigure.getFigure().getAWTPolygon();
                Rectangle bounds = original.getBounds();

                // Новое положение для XOR-превью
                int newX = mouseX - dragOffsetX;
                int newY = mouseY - dragOffsetY;

                // Создаём временный полигон
                dragPreview = new java.awt.Polygon();
                int dx = newX - bounds.x;
                int dy = newY - bounds.y;
                for (int i = 0; i < original.npoints; i++) {
                    dragPreview.addPoint(original.xpoints[i] + dx, original.ypoints[i] + dy);
                }

                // Рисуем XOR-контур
                Graphics g = getGraphics();
                if (g != null) {
                    g.setXORMode(Color.BLACK); // инверсия на белом фоне
                    g.drawPolygon(dragPreview);
                    g.dispose();
                }
            }
        }

        private void handleMouseReleased(MouseEvent e) {
            if (selectedFigure != null && dragPreview != null) {
                // Стираем последний XOR-контур
                Graphics g = getGraphics();
                if (g != null) {
                    g.setXORMode(Color.WHITE);
                    g.drawPolygon(dragPreview);
                    g.dispose();
                }

                // Применяем перемещение
                java.awt.Polygon original = selectedFigure.getFigure().getAWTPolygon();
                int dx = dragPreview.getBounds().x - original.getBounds().x;
                int dy = dragPreview.getBounds().y - original.getBounds().y;

                selectedFigure.getFigure().moveBy(dx, dy);

                dragPreview = null;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            for (FigureItem item : currentFigures) {
                if (item.isVisible()) {
                    ColoredPolygon fig = item.getFigure();
                    java.awt.Polygon poly = fig.getAWTPolygon();

                    // Заливка
                    g2d.setColor(fig.getFillColor());
                    g2d.fillPolygon(poly);

                    // Контур
                    g2d.setColor(fig.getLineColor());
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawPolygon(poly);

                    // Выделение
                    if (item == selectedFigure) {
                        g2d.setColor(Color.BLUE);
                        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
                        g2d.draw(poly.getBounds());
                    }
                }
            }
        }

        public BufferedImage createImage() {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                w = 800;
                h = 600;
            }
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, w, h);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            for (FigureItem item : currentFigures) {
                if (item.isVisible()) {
                    ColoredPolygon fig = item.getFigure();
                    java.awt.Polygon poly = fig.getAWTPolygon();

                    g2d.setColor(fig.getFillColor());
                    g2d.fillPolygon(poly);

                    g2d.setColor(fig.getLineColor());
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawPolygon(poly);
                }
            }
            g2d.dispose();
            return image;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new MainGUI().setVisible(true);
        });
    }
}
