package com.mjwall;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// ignore, was trying to setup a sample server to reproduce the issue but I am only able to reproduce
// with crate directly
class SampleServer {

    class Handle404 implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            InputStream is = t.getRequestBody();
            Headers headers = t.getResponseHeaders();
            for(String key: headers.keySet()) {
                headers.remove(key);
            }
            String response = "";
            Map<String, List<String>> newHeaders = new HashMap<>();
            newHeaders.put("content-length", Collections.singletonList(Integer.toString(response.length())));
            headers.putAll(newHeaders); // got to use putAll because headers.add normalizes the key
            t.sendResponseHeaders(404, -1);
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    class Handle200 implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            InputStream is = t.getRequestBody();
            Headers headers = t.getResponseHeaders();
            for(String key: headers.keySet()) {
                headers.remove(key);
            }
            String response = "You got a 200, nice work";
            Map<String, List<String>> newHeaders = new HashMap<>();
            newHeaders.put("content-length", Collections.singletonList(Integer.toString(response.length())));
            newHeaders.put("accept-ranges", Collections.singletonList("bytes"));
            newHeaders.put("expires", Collections.singletonList("Thu, 31 Dec 2037 23:59:59 GMT"));
            newHeaders.put("cache-control", Collections.singletonList("max-age=313260000"));
            headers.putAll(newHeaders); // got to use putAll because headers.add normalizes the key
            t.sendResponseHeaders(404, -1);
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    HttpServer server = null;

    private int getFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    public boolean start() {
        boolean started = false;
        try {
            server = HttpServer.create(new InetSocketAddress(getFreePort()), 0);
            server.createContext("/404", new Handle404());
            server.createContext("/200", new Handle200());
            server.setExecutor(null);
            server.start();
            started = true;
        } catch (IOException e) {
            e.printStackTrace();

        }
        return started;
    }

    public boolean stop() {
        boolean stopped = false;
        if (server != null) {
            server.stop(0);
            stopped = true;
        }
        return stopped;
    }

    public String getAddress() {
        if (server != null) {
            return server.getAddress().toString();
        }
        throw new RuntimeException("Server not started");
    }

//    @Test
//    public void testBadnessFromLocalServer() {
//        SampleServer server = new SampleServer();
//        try {
//            server.start();
//            String addr = server.getAddress();
//            System.out.println("Get er done local at " + addr);
//            SomeUse bu = new SomeUse();
//            System.out.println(bu.doIt("http://" + addr + "/200"));
//            System.out.println(bu.doIt("http://" + addr + "/404"));
//            System.out.println(bu.doIt("http://" + addr + "/200"));
//            System.out.println("You done did it");
//        } finally {
//            server.stop();
//        }
//    }

}
