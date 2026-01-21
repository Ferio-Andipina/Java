public class FigureItem {
    private final ColoredPolygon figure;
    private boolean visible;

    public FigureItem(ColoredPolygon figure) {
        this.figure = figure;
        this.visible = true; // по умолчанию показывать
    }

    public ColoredPolygon getFigure() {
        return figure;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public String toString() {
        return figure.getName() + " (P=" + String.format("%.2f", figure.getPerimeter()) + ")";
    }
}
