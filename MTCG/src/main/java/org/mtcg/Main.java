package org.mtcg;

public class Main {

    public static void main(String[] args) {
        int port = 10001;
        ResetDatabase resetDatabase = new ResetDatabase();
        resetDatabase.resetDatabase();
        Server server = new Server(port);
        server.start();
    }
}
