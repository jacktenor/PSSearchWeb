package PSSearchApp;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class PSSearchWebApp {
    private static HttpServer server;

    public static void main(String[] args) throws IOException {
        server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new ProcessListHandler());
        server.createContext("/kill", new KillProcessHandler());
        server.createContext("/stop", new StopServerHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
        System.out.println("Server is running on http://localhost:8080/");
    }

    static class ProcessListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = generateProcessListHtml();
            t.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            t.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        }
    }

    static class KillProcessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String query = t.getRequestURI().getQuery();
            if (query != null && query.startsWith("pid=")) {
                String pid = query.substring(4);
                try {
                    // Kill the process
                    Process killProcess = Runtime.getRuntime().exec("kill " + pid);
                    killProcess.waitFor();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // Redirect back to the main page
            t.getResponseHeaders().set("Location", "/");
            t.sendResponseHeaders(302, -1);
        }
    }

    static class StopServerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "<html><body><h1>Server is stopping...</h1></body></html>";
            t.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            t.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();

            // Stop the server and exit the application
            server.stop(0);
            System.exit(0);
        }
    }

    private static String generateProcessListHtml() {
        List<String[]> processes = getRunningProcesses();
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
            .append("<html><head><title>Running Processes</title>")
            .append("<style>")
            .append("table { width: 60%; border-collapse: collapse; margin: 20px auto; }")
            .append("th, td { padding: 10px; text-align: left; border-bottom: 1px solid #ddd; }")
            .append("tr:hover { background-color: #f5f5f5; }")
            .append("button { margin: 20px; padding: 10px 20px; font-size: 16px; cursor: pointer; }")
            .append("</style>")
            .append("<script>")
            .append("function confirmKill(pid) {")
            .append("  if (confirm('Are you sure you want to kill process ' + pid + '?')) {")
            .append("    window.location.href = '/kill?pid=' + pid;")
            .append("  }")
            .append("}")
            .append("</script>")
            .append("</head><body>")
            .append("<h1 style='text-align:center;'>Running Processes</h1>")
            .append("<div style='text-align:center;'>")
            .append("<button onclick='window.location.href=\"/\"'>Refresh</button>")
            .append("<button onclick='window.location.href=\"/stop\"'>Stop Server</button>")
            .append("</div>")
            .append("<table>")
            .append("<tr><th>PID</th><th>Process Name</th><th>Actions</th></tr>");

        for (String[] process : processes) {
            String processName = URLEncoder.encode(process[1].trim(), StandardCharsets.UTF_8);
            String googleSearchUrl = "https://www.google.com/search?q=" + processName;
            html.append("<tr><td>").append(process[0])
                .append("</td><td><a href=\"").append(googleSearchUrl)
                .append("\" target=\"_blank\">").append(process[1])
                .append("</a></td><td>")
                .append("<a href='#' onclick='confirmKill(\"").append(process[0])
                .append("\")'>Kill</a>")
                .append("</td></tr>");
        }

        html.append("</table>")
            .append("</body></html>");

        return html.toString();
    }

    private static List<String[]> getRunningProcesses() {
        List<String[]> processes = new ArrayList<>();
        try {
            String line;
            Process p = Runtime.getRuntime().exec("ps -e -o pid,comm");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            input.readLine(); // Skip the header line
            while ((line = input.readLine()) != null) {
                String[] processDetails = line.trim().split("\\s+", 2);
                if (processDetails.length == 2) {
                    processes.add(processDetails);
                }
            }
            input.close();
        } catch (Exception err) {
            err.printStackTrace();
        }
        return processes;
    }
}

