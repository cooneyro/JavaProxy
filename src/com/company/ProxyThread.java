package com.company;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
* ProxyThread handles sockets used for communication,
 * data streams between client and host
 * and HTTP requests and responses as necessary.
 */

public class ProxyThread extends Thread {

    private static final int HTTP_PORT = 80;
    private static final int START = 0;
    private static final int SLEEP_TIME = 120;

    private Socket socket = null; //used for communication between client and proxy
    private Socket proxySocket; // communication between proxy and web host


    private OutputStream requestOut; //proxy to client
    private OutputStream responseOut; //proxy to host
    private InputStream responseIn; //host to proxy
    private InputStream requestIn; //client to proxy


    private CacheMgr cacheManager = CacheMgr.getInstance();
    private HostBlocking hostBlocker = HostBlocking.getInstance();

    private byte[] refusedData;

    /**
     * ProxyThread constructor taking socket as param from Main
     */
    ProxyThread(Socket socket) {
        super("ProxyThread");
        this.socket = socket;
        if (refusedData == null) {
            Path path = Paths.get("blocked_host.html");
            try {
                refusedData = Files.readAllBytes(path);
            } catch (IOException e) {
                System.out.println("Error showing blocked host file");
            }
        }
    }

    /**
     * Handles opening input and output streams of data, HTTP requests and responses
     */
    public void run() {
        try {
            boolean putInCache;
            byte[] fromHost; // for transferring data sent by host to client
            byte[] fromClient; // for transferring data sent by client to host

            /* for handling requests and responses */

            RequestHandler requestHeader;
            ResponseHandler responseHeader;

            requestIn = socket.getInputStream();
            requestOut = socket.getOutputStream();

            /* Get data from client. If no data received, close thread */

            fromClient = dataIn();
            if (fromClient == null) {
                return;
            }

            /* Check request for blocked host*/

            requestHeader = new RequestHandler(fromClient);
            if (checkForBlockedHost(requestHeader.getHost())) {
                return;
            }

            /* Checks if site is cached, if so, load from cache */

            if (cacheManager.isCached(requestHeader.getUrl())) {
                byte[] data = cacheManager.getData(requestHeader.getUrl()).getData();
                returnResponse(data);
                ShowCacheHit(requestHeader.getUrl());
            } else {

                /* Site isn't cached, connect to host and send request */

                proxySocket = new Socket(requestHeader.getHost(), HTTP_PORT);
                Interface.consolePrint("Connecting to " + requestHeader.getHost(), true);

                responseIn = proxySocket.getInputStream();
                responseOut = proxySocket.getOutputStream();

                sendReq(fromClient);
                fromHost = getResponse();
                responseHeader = new ResponseHandler(fromHost);
                CacheObject thisObject = responseHeader.cachingProtocol();

                /* Check if data can be cached */

                if (thisObject != null && thisObject.canBeCached()) {
                    thisObject.put(fromHost);
                    putInCache = true;
                } else {
                    putInCache = false;
                }

                /* Transfer response data to client from host */

                returnResponse(fromHost);


                while (socket.isConnected() && proxySocket.isConnected()) {


		            /* Data to be transferred to client from host */

                    if (checkResponseIn()) {
                        fromHost = getResponse();
                        returnResponse(fromHost);
                        if (putInCache) {
                            thisObject.put(fromHost);
                        }
                    }

		            /* Data to be transferred to host from client */

                    if (checkRequestIn()) {
                        fromClient = dataIn();
                        sendReq(fromClient);
                    }

                    /* Puts thread to sleep for 120ms to allow for possible delay in sending*/
                    if (!checkReqRespIn()) {
                        try {
                            Thread.sleep(SLEEP_TIME);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (!checkReqRespIn()){
                            break;
                        }
                    }
                }
                /* Write to cache if necessary/possible */
                writeCache(thisObject, requestHeader.getUrl());

            }
            /* Close all open socket connections and data streams*/
            closeAllConnections();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * Get data sent by client to host
     */
    private byte[] dataIn() throws IOException {
        byte[] inputData;
        byte[] byteTmp = new byte[getMaxBufferSize()];
        int size = requestIn.read(byteTmp, START, byteTmp.length);
        if (size <= 0) {
            return null;
        }
        inputData = new byte[size];
        System.arraycopy(byteTmp, START, inputData, START, size);
        return inputData;
    }

    private void sendReq(byte[] outputData) throws IOException {
        responseOut.write(outputData, START, outputData.length);
    }

    /**
     * Retrieve response from host
     */
    private byte[] getResponse() throws IOException {
        byte[] result;
        byte[] byteTmp = new byte[getMaxBufferSize()];
        int size = responseIn.read(byteTmp, START, byteTmp.length);
        if (size <= 0) {
            return null;
        }
        result = new byte[size];
        System.arraycopy(byteTmp, START, result, START, size);
        return result;
    }

    /**
     * Transfer response from host to client
     */
    private void returnResponse(byte[] responseData) throws IOException {
        requestOut.write(responseData, START, responseData.length);
        requestOut.flush();
    }


    /**
     * If user attempts to access blocked host
     * show blocked_host html page and shut down thread
     */
    private void ifBlocked() {
        byte[] data = retrieveBlockPage();
        try {
            returnResponse(data);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        try{
            closeAllConnections();
        }catch(IOException e){
        }
    }

    /**
     * Close all open connections to sockets and datastreams
     */
    private void closeAllConnections() throws IOException {
        try {
            socket.close();
            proxySocket.close();
        } catch (NullPointerException e) {
        }
        try {
            requestIn.close();
            requestOut.close();
            responseIn.close();
            responseOut.close();
        } catch (NullPointerException e) {
        }
    }


    /**
     * Check if host is blocked
     */
    private boolean checkForBlockedHost(String host) {

        boolean blocked;
        try {
            boolean hostIsBlocked = hostBlocker.isBlockedHost(host);
            blocked = hostIsBlocked;
        } catch (Exception e) {
            System.out.print("Error");
            blocked = false;
        }
        if (blocked) {
            Interface.consolePrint("You are currently blocked from accessing " + host, true);
            ifBlocked();
            return true;
        }
        return false;
    }

    /**
     * Show cache hit on user interface
     */
    private void ShowCacheHit(String url) {
        if (url.length() > 40) {
            url = url.substring(0, 40) + "...";
        }
        Interface.consolePrint("********************FOUND " + url + " IN CACHE*****************************", true);
    }

    /**
     * Write data to cache if it can be cached and is not public
     */
    private void writeCache(CacheObject thisObject, String url) {
        if (thisObject != null && thisObject.canBeCached()) {
            thisObject.setKey(url);
            if (!thisObject.isPrivate()) {
                CacheMgr.getInstance().cacheIn(url, thisObject);
            }
        }
    }

    /**
     *  Return blocked_host page
     */
    private byte[] retrieveBlockPage() {
        return refusedData;
    }


    /**
     *  Return max buffer size for data streams
     */
    private int getMaxBufferSize() {
        return 65536;
    }

    /**
     *  Check if either client or host want to send data
     */
    private boolean checkReqRespIn() throws IOException{
        return (checkRequestIn() || checkResponseIn());
    }

    /**
     *  Check if client wants to send data
     */
    private boolean checkResponseIn() throws IOException{
        return responseIn.available()!=0;
    }

    /**
     *  Check if host wants to send data
     */
    private boolean checkRequestIn() throws IOException{
        return requestIn.available()!=0;
    }


}
