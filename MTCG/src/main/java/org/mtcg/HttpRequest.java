package org.mtcg;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    private String method;
    private String url;
    private Map<String, String> headers = new HashMap<>();
    private String body;

    public HttpRequest(BufferedReader in) throws IOException {
        parseRequest(in);
    }

    private void parseRequest(BufferedReader in) throws IOException {
        // Parse Request Line
        String requestLine = in.readLine();
        if (requestLine != null && !requestLine.isEmpty()) {
            String[] parts = requestLine.split(" ");
            this.method = parts[0];
            this.url = parts[1];
        }

        // Parse Headers
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            String[] headerParts = line.split(": ", 2);
            if (headerParts.length == 2) {
                headers.put(headerParts[0], headerParts[1]);
            }
        }

        // Parse Body
        if ("POST".equalsIgnoreCase(method)) {
            StringBuilder bodyBuilder = new StringBuilder();
            while (in.ready()) {
                bodyBuilder.append((char) in.read());
            }
            this.body = bodyBuilder.toString().trim();
        }
    }

    // Getter-Methoden
    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }
}
