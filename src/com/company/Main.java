package com.company;

import java.io.*;
import java.net.ServerSocket;

/**
 * Main class for listening on proxy port
 * Opens a new socket for each connection which is passed
 * to ProxyThread for handling
 */
public class Main extends Thread {

    public Main() {
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null; //ServerSocket for listening to specified port
        boolean listening = true;

        int port = 4000;    //default port

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Started on port " + port);
        } catch (IOException e) {
            //System.err.println("Could not listen on port: " + args[0]);
            System.exit(-1);
        }

        while (listening) {
            //creating new thread for each request
            new ProxyThread(serverSocket.accept()).start();
        }
        serverSocket.close();
    }
}
