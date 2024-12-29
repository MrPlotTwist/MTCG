package org.mtcg;

public class Main {

    public static void main(String[] args) {
        int port = 10001;
        Server server = new Server(port);
        server.start();
    }
}
