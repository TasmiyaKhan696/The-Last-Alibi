package com.lastalibi;

import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;

/**
 * Starts the graphical edition. The game page is bundled inside the jar (web/index.html), served from a tiny
 * local HTTP server on a free port, and opened in the default browser. Nothing leaves your machine.
 */
public final class GuiLauncher {
    private GuiLauncher() { }

    private static byte[] loadPage() {
        try (InputStream in = GuiLauncher.class.getResourceAsStream("/web/index.html")) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    public static void launch() {
        final byte[] page = loadPage();
        if (page == null) {
            System.out.println("The GUI page (web/index.html) is missing from the jar. Run with --console instead.");
            return;
        }
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.createContext("/", ex -> {
                String path = ex.getRequestURI().getPath();
                boolean ok = path.equals("/") || path.equals("/index.html");
                ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                if (ok) {
                    ex.sendResponseHeaders(200, page.length);
                    try (OutputStream os = ex.getResponseBody()) { os.write(page); }
                } else {
                    ex.sendResponseHeaders(404, -1);
                }
                ex.close();
            });
            server.start();
            String url = "http://localhost:" + server.getAddress().getPort() + "/";
            System.out.println("The Last Alibi is running at " + url);
            boolean opened = false;
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create(url));
                    opened = true;
                }
            } catch (Exception ignored) { }
            if (!opened) System.out.println("Could not open a browser automatically. Open the link above yourself.");
            System.out.println("Press Ctrl+C in this window to stop the game.");
        } catch (IOException e) {
            System.out.println("Could not start the local server: " + e.getMessage() + ". Try --console.");
        }
    }
}
