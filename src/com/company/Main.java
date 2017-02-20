package com.company;

import java.io.*;
import java.net.ServerSocket;

public class Main extends Thread{

    public Main(){
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;

        int port = 443;	//default
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
        }

       try {
            serverSocket = new ServerSocket(port);
            System.out.println("Started on port " + port);
        } catch (IOException e) {
            //System.err.println("Could not listen on port: " + args[0]);
            System.exit(-1);
        }

        while (listening) {
            new ProxyThread(serverSocket.accept()).start();
        }
        serverSocket.close();
    }
}
