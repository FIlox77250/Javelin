import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import com.sun.net.httpserver.HttpsConfigurator;


public class Javelin {
    private static Properties config = new Properties();
    private static long startTime = System.currentTimeMillis();
    private static int requestCount = 0;
    private static int errorCount = 0;

    public static void main(String[] args) throws Exception {
        // Charger config
        try (FileInputStream fis = new FileInputStream("server.conf")) {
            config.load(fis);
        }
        int port = Integer.parseInt(config.getProperty("port", "8080"));
        int sslPort = Integer.parseInt(config.getProperty("sslPort", "8443"));

        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/", Javelin::handleRequest);
        httpServer.createContext("/status", Javelin::handleStatus);
        httpServer.createContext("/api/time", Javelin::handleApiTime);
        httpServer.setExecutor(null);
        System.out.println("HTTP sur :" + port);
        httpServer.start();

        // HTTPS avec certificat auto-signé (si keystore existe)
        File ksFile = new File("certs/keystore.jks");
        if (ksFile.exists()) {
            char[] password = "changeit".toCharArray();
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(ksFile), password);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, password);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);
            SSLContext ssl = SSLContext.getInstance("TLS");
            ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(sslPort), 0);
            httpsServer.setHttpsConfigurator(new HttpsConfigurator(ssl));
            httpsServer.createContext("/", Javelin::handleRequest);
            httpsServer.setExecutor(null);
            System.out.println("HTTPS sur :" + sslPort);
            httpsServer.start();
        } else {
            System.out.println("⚠ Aucun certificat trouvé dans certs/keystore.jks (HTTPS désactivé)");
        }
    }

    private static void handleRequest(HttpExchange ex) {
        requestCount++;
        String method = ex.getRequestMethod();
        String host = ex.getRequestHeaders().getFirst("Host");
        String path = ex.getRequestURI().getPath();
        try {
            if (!method.equalsIgnoreCase("GET") && !method.equalsIgnoreCase("HEAD")) {
                serveError(ex, 405, "Method Not Allowed");
                return;
            }
            String root = getDocumentRoot(host);
            serveFile(ex, root, path);
        } catch (Exception e) {
            errorCount++;
            try {
                serveError(ex, 500, "Internal Server Error");
            } catch (IOException ignored) {}
        }
    }

    private static void serveFile(HttpExchange ex, String root, String path) throws IOException {
        if (path.equals("/")) path = "/index.html";
        File file = new File(root, path).getCanonicalFile();
        if (!file.getPath().startsWith(new File(root).getCanonicalPath())) {
            serveError(ex, 403, "Forbidden");
            return;
        }
        if (!file.exists()) {
            serveError(ex, 404, "Not Found");
            return;
        }
        if (file.isDirectory()) {
            File index = new File(file, "index.html");
            if (index.exists()) {
                file = index;
            } else {
                sendDirectoryListing(ex, file, path);
                return;
            }
        }
        byte[] data = Files.readAllBytes(file.toPath());
        String contentType = getContentType(file.getName());
        ex.getResponseHeaders().set("Content-Type", contentType);
        addCacheHeaders(ex, file);
        addDefaultHeaders(ex);
        boolean gzip = clientAcceptsGzip(ex);
        if (gzip) ex.getResponseHeaders().set("Content-Encoding", "gzip");

        if (ex.getRequestMethod().equalsIgnoreCase("HEAD")) {
            ex.sendResponseHeaders(200, -1);
        } else {
            if (gzip) {
                ex.sendResponseHeaders(200, 0);
                try (OutputStream os = new GZIPOutputStream(ex.getResponseBody())) {
                    os.write(data);
                }
            } else {
                ex.sendResponseHeaders(200, data.length);
                try (OutputStream os = ex.getResponseBody()) { os.write(data); }
            }
        }
    }

    private static void sendDirectoryListing(HttpExchange ex, File dir, String path) throws IOException {
        StringBuilder html = new StringBuilder("<html><body><h1>Index of " + path + "</h1><ul>");
        for (File f : Objects.requireNonNull(dir.listFiles())) {
            String name = f.getName() + (f.isDirectory() ? "/" : "");
            html.append("<li><a href=\"")
                    .append(path).append("/").append(name)
                    .append("\">").append(name).append("</a></li>");
        }
        html.append("</ul></body></html>");
        byte[] data = html.toString().getBytes();
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        addDefaultHeaders(ex);
        ex.sendResponseHeaders(200, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }

    private static void serveError(HttpExchange ex, int code, String message) throws IOException {
        String root = getDocumentRoot(ex.getRequestHeaders().getFirst("Host"));
        File custom = new File(root, "errors/" + code + ".html");
        byte[] data;
        if (custom.exists()) {
            data = Files.readAllBytes(custom.toPath());
        } else {
            data = ("<h1>" + code + " " + message + "</h1>").getBytes();
        }
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        addDefaultHeaders(ex);
        ex.sendResponseHeaders(code, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }

    private static void handleStatus(HttpExchange ex) throws IOException {
        long uptime = (System.currentTimeMillis() - startTime) / 1000;
        String json = "{\"uptime\":" + uptime + ",\"requests\":" + requestCount + ",\"errors\":" + errorCount + "}";
        byte[] data = json.getBytes();
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(200, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }

    private static void handleApiTime(HttpExchange ex) throws IOException {
        String json = "{\"time\":\"" + new Date() + "\"}";
        byte[] data = json.getBytes();
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(200, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }

    // --- Utils ---
    private static String getContentType(String filename) {
        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf(".")+1).toLowerCase() : "";
        switch (ext) {
            case "html": return "text/html; charset=utf-8";
            case "css": return "text/css; charset=utf-8";
            case "js": return "application/javascript; charset=utf-8";
            case "png": return "image/png";
            case "jpg": case "jpeg": return "image/jpeg";
            case "gif": return "image/gif";
            case "svg": return "image/svg+xml";
            case "json": return "application/json; charset=utf-8";
            default: return "application/octet-stream";
        }
    }

    private static boolean clientAcceptsGzip(HttpExchange ex) {
        List<String> encodings = ex.getRequestHeaders().get("Accept-Encoding");
        if (encodings == null) return false;
        for (String e : encodings) if (e.toLowerCase().contains("gzip")) return true;
        return false;
    }

    private static void addDefaultHeaders(HttpExchange ex) {
        ex.getResponseHeaders().set("Server", "Javelin/1.0");
        ex.getResponseHeaders().set("Date", new Date().toString());
    }

    private static void addCacheHeaders(HttpExchange ex, File file) {
        ex.getResponseHeaders().set("Cache-Control", "max-age=3600");
        ex.getResponseHeaders().set("Last-Modified", new Date(file.lastModified()).toString());
        ex.getResponseHeaders().set("ETag", "\""+file.lastModified()+"\"");
    }

    private static String getDocumentRoot(String host) {
        if (host != null) {
            for (String key : config.stringPropertyNames()) {
                if (key.startsWith("vhost.") && key.substring(6).equalsIgnoreCase(host)) {
                    return config.getProperty(key);
                }
            }
        }
        return config.getProperty("documentRoot", "www");
    }
}
