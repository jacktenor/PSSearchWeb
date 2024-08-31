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
import java.util.Collections;
import java.util.Comparator;
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
            .append("body { background-color: #2c3e50; color: #ecf0f1; font-family: Arial, sans-serif; }")
            .append("h1 { text-align: center; }")
            .append("table { width: 80%; border-collapse: collapse; margin: 20px auto; background-color: #020d1f; }")
            .append("th, td { padding: 12px 15px; text-align: left; border-bottom: 1px solid #020d1f; }")
            .append("th { border-radius: 12px; background-color: #010e7d; }")
            .append("tr:hover { background-color: #010e7d; color: #ffffff; }")
            .append("button { background-color: #020d1f; color: #ffffff; border-radius: 8px; margin: 10px 5px; padding: 10px 20px; font-size: 16px; cursor: pointer; border: none; border-radius: 12px; transition: background-color 0.3s ease, color 0.3s ease; }")
            .append("button:hover { background-color: #010e7d; color: #ffffff; }")
            .append("button:active { transform: scale(0.98); }")
            .append("a { color: #0071b8; text-decoration: none; }")
            .append("a:hover { color: #00f2ff; text-decoration: underline; }")
            .append("</style>")
            .append("<script>")
            .append("function confirmKill(pid) {")
            .append("  if (confirm('Are you sure you want to kill process ' + pid + '?')) {")
            .append("    window.location.href = '/kill?pid=' + pid;")
            .append("  }")
            .append("</script>")
            .append("</head><body>")
            .append("<h1 style='text-align:center;'>Running Processes</h1>")
            .append("<div style='text-align:center;'>")
            .append("<button onclick='window.location.href=\"/\"'>Refresh</button>")
            .append("<button onclick='window.location.href=\"/stop\"'>Stop Server</button>")
            .append("</div>")
            .append("<table>")
            .append("<tr><th>PID</th><th>Process Name</th><th>CPU (%)</th><th>Memory (%)</th><th>Actions</th></tr>");

        for (String[] process : processes) {
            String processName = URLEncoder.encode(process[1].trim(), StandardCharsets.UTF_8);
            String googleSearchUrl = "https://www.google.com/search?q=" + processName;
            html.append("<tr><td>").append(process[0])
                .append("</td><td><a href=\"").append(googleSearchUrl)
                .append("\" target=\"_blank\">").append(process[1])
                .append("</a></td><td>").append(process[2]).append("%")  // CPU Usage with %
                .append("</td><td>").append(process[3]).append("%")  // Memory Usage with %
                .append("</td><td>")
                .append("<a href='#' onclick='confirmKill(\"").append(process[0])
                .append("\")'>Kill</a>")
                .append("</td></tr>");
        }

       		html.append("</table>")
   				.append("<center><a href=https://beeralator.com style='color:cyan'>https://beeralator.com</a></center>")
   				.append("<br>")
       			.append("<center><a href=https://github.com/jacktenor style='color:cyan'>https://github.com/jacktenor</a></center>")
       			.append("</body></html>");

        return html.toString();
    }
    
    private static List<String[]> getRunningProcesses() {
        List<String[]> processes = new ArrayList<>();
        try {
            String line;
            // Modify the `ps` command to include PID, Command, %CPU, and %MEM
            Process p = Runtime.getRuntime().exec("ps -e -o pid=,comm=,pcpu=,pmem=");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = input.readLine()) != null) {
                line = line.trim();
                int firstSpace = line.indexOf(' ');
                String pid = line.substring(0, firstSpace).trim();

                String remaining = line.substring(firstSpace).trim();
                int lastSpace = remaining.lastIndexOf(' ');
                String mem = remaining.substring(lastSpace).trim();

                remaining = remaining.substring(0, lastSpace).trim();
                lastSpace = remaining.lastIndexOf(' ');
                String cpu = remaining.substring(lastSpace).trim();

                String command = remaining.substring(0, lastSpace).trim();

                // Store CPU and memory usage as numbers for sorting
                processes.add(new String[]{pid, command, cpu, mem});
            }
            input.close();
            
            // Sort the list by CPU usage (descending), then by memory usage (descending)
            processes.sort((p1, p2) -> {
                double cpu1 = Double.parseDouble(p1[2]);
                double cpu2 = Double.parseDouble(p2[2]);
                if (cpu1 != cpu2) {
                    return Double.compare(cpu2, cpu1); // Sort by CPU descending
                }
                double mem1 = Double.parseDouble(p1[3]);
                double mem2 = Double.parseDouble(p2[3]);
                return Double.compare(mem2, mem1); // Sort by memory descending
            });

        } catch (Exception err) {
            err.printStackTrace();
        }
        return processes;
    }
}

