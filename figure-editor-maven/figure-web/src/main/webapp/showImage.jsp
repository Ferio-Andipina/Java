import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

public class WebServer {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        Server server = new Server(port);

        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setResourceBase("src/main/webapp"); // или "webapp" при упаковке

        webapp.setResourceBase("webapp"); // при запуске из корня проекта

        server.setHandler(webapp);

        System.out.println("Запуск Jetty на http://localhost:" + port);
        server.start();
        server.join();
    }
}
