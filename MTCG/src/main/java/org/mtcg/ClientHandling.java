package org.mtcg;

import java.io.*;
import java.net.Socket;

public class ClientHandling {

    public static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {

            // Erste Zeile der Anfrage (Request Line) auslesen
            String requestLine = in.readLine();
            System.out.println("Anfrage: " + requestLine);

            if (requestLine != null) {
                String[] requestLineParts = requestLine.split(" ");
                if (requestLineParts.length < 2) {
                    sendResponse(out, 400, "{\"message\":\"Bad Request\"}");
                    return;
                }

                String method = requestLineParts[0];
                String url = requestLineParts[1];
                StringBuilder body = new StringBuilder();
                String line;
                String authorizationHeader = null;

                // Kopfzeilen auslesen
                while (!(line = in.readLine()).isEmpty()) {
                    if (line.startsWith("Authorization:")) {
                        authorizationHeader = line.substring("Authorization:".length()).trim();
                    }
                }

                // Body lesen, falls vorhanden
                while (in.ready()) {
                    body.append((char) in.read());
                }

                // Auf die entsprechende Klasse basierend auf der HTTP-Methode zugreifen
                switch (method) {
                    case "GET":
                        GETRequestHandling.handleGetRequest(url, authorizationHeader, out);
                        break;
                    case "POST":
                        POSTRequestHandling.handlePostRequest(url, body.toString(), authorizationHeader, out);
                        break;
                    case "PUT":
                        PUTRequestHandling.handlePutRequest(url, body.toString(), authorizationHeader, out);
                        break;
                    default:
                        sendResponse(out, 405, "{\"message\":\"Method Not Allowed\"}");
                        break;
                }
            } else {
                sendResponse(out, 400, "{\"message\":\"Bad Request\"}");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static void sendResponse(OutputStream out, int statusCode, String body) throws IOException {
        String statusLine = "HTTP/1.1 " + statusCode + "\r\n";
        String httpResponse = statusLine +
                "Content-Type: application/json\r\n" +
                "Content-Length: " + body.getBytes("UTF-8").length + "\r\n" +
                "\r\n" +
                body;
        out.write(httpResponse.getBytes("UTF-8"));
    }
}
