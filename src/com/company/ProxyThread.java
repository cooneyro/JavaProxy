package com.company;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProxyThread extends Thread {
    private Socket socket = null;
    private static final int HTTP_PORT = 80;
    private static final int HTTPS_PORT = 443;


    private Socket proxySocket;

    private OutputStream requestOut;
    private InputStream requestIn;

    private OutputStream responseOut;
    private InputStream responseIn;
    private CacheObject thisObject;


    private CacheMgr cacheManager = CacheMgr.getInstance();
    private TrafficFilter trafficFilter = TrafficFilter.getInstance();

    private byte[] refusedData;

    public ProxyThread(Socket socket) {
        super("ProxyThread");
        this.socket = socket;
        if (refusedData == null) {
            Path path = Paths.get("blocked_host.html");
            try {
                refusedData = Files.readAllBytes(path);
            } catch (IOException e) {
            }
        }
    }

    public void run() {
        //get input from user
        //send request to server
        //get response from server
        //send response to user
        try {


            boolean putInCache;
            byte[] fromHost;
            byte[] fromUser;

            RequestHandler requestHeader;
            ResponseHandler responseHeader;

            requestIn = socket.getInputStream();
            requestOut = socket.getOutputStream();

            /**
	        * Get data from the user
	        */

            fromUser = dataIn();
            if (fromUser == null) {
                return;
            }


            requestHeader = new RequestHandler(fromUser);
            if (filterHost(requestHeader.getHost())){
                return;
            }

            /** Checks if site is cached, if so, load from cache */

            if (cacheManager.isCached(requestHeader.getUrl())) {
                byte[] data = cacheManager.getData(requestHeader.getUrl()).getData();
                returnResponse(data);
                ShowCacheHit(requestHeader.getUrl(), data.length);
            } else {
                /*if(requestHeader.getMethod()== RequestHandler.METHOD.CONNECT){
                    proxySocket = new Socket(requestHeader.getHost(), HTTPS_PORT);
                }else{

                }*/
                proxySocket = new Socket(requestHeader.getHost(), HTTP_PORT);
                Interface.consolePrint("Connecting to\t" + requestHeader.getHost(),true);
                
                responseIn = proxySocket.getInputStream();
                responseOut = proxySocket.getOutputStream();
                
                sendReq(fromUser);
                fromHost = getResponse();
                responseHeader = new ResponseHandler(fromHost);
                thisObject = responseHeader.getCacheInfo();
                if (thisObject!=null && thisObject.isCachable()) {
                    thisObject.put(fromHost);
                    putInCache = true;
                } else {
                    putInCache = false;
                }
                /*if(requestHeader.getMethod()== RequestHandler.METHOD.CONNECT){
                    String ConnectResponse = "HTTP/1.1 200 Connection Established\r\n" +
                            "Proxy-agent:CS3031 Server/1.0\r\n" +
                            "\r\n";
                    byte[] response = ConnectResponse.getBytes();
                    returnResponse(response);
                }else{*/
                    returnResponse(fromHost);
                //}


                while (socket.isConnected() && proxySocket.isConnected()) {


		            /* Data from host for user*/

                    if (responseIn.available() != 0) {
                        fromHost = getResponse();
                        returnResponse(fromHost);
                        if (putInCache) {
                            thisObject.put(fromHost);
                        }
                    }

		            /* Data from user for host */

                    if (requestIn.available() != 0) {

                        fromUser = dataIn();
                        sendReq(fromUser);
                    }

                    if (requestIn.available() == 0
                            && requestIn.available() == 0) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (requestIn.available() == 0
                                && responseIn.available() == 0) {
                            break;
                        }
                    }
                }
                writeCache(thisObject, requestHeader.getUrl());


            }

            closeConnection();
            closeDataStreams();

            } catch(IOException e){
                e.printStackTrace();
            }


    }


    private byte[] dataIn() throws IOException {
        byte[] byteBuffer;
        byte[] byteTmp = new byte[getMaxBufferSize()];
        int size = requestIn.read(byteTmp, 0, byteTmp.length);
        if (size <= 0) {
            return null;
        }
        byteBuffer = new byte[size];
        System.arraycopy(byteTmp, 0, byteBuffer, 0, size);
        return byteBuffer;
    }

    private void sendReq(byte[] byteBuffer) throws IOException {
        responseOut.write(byteBuffer, 0, byteBuffer.length);
    }

    private byte[] getResponse() throws IOException {
        byte[] result;
        byte[] byteTmp = new byte[getMaxBufferSize()];
        int size = responseIn.read(byteTmp, 0, byteTmp.length);
        if (size <= 0) {
            return null;
        }
        result = new byte[size];
        System.arraycopy(byteTmp, 0, result, 0, size);
        return result;
    }

    private void returnResponse(byte[] byteBuffer) throws IOException {
        requestOut.write(byteBuffer, 0, byteBuffer.length);
        requestOut.flush();
    }

    private void closeDataStreams() throws IOException {
        try {
            requestIn.close();
            requestOut.close();
            responseIn.close();
            responseOut.close();
        } catch (NullPointerException e) {
            System.out.println("Null pointer exception");
        }
    }

    private void closeConnection() {
        try {
            socket.close();
            proxySocket.close();
        } catch (IOException e) {
            System.out.println("IO Exception");
        }
    }



    /** If user attempts to access blocked host
     *  show blocked_host html page and shut down thread
     */
    private void doActionIfBlocked() {
        byte[] data = getRefused();
        try {
            returnResponse(data);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        closeConnection();
        try {
            closeDataStreams();
        } catch (IOException e) {
        }
    }


    /** Check if host is blocked */

    private boolean filterHost(String host) {

        boolean blocked;
        try {
            boolean hostIsBlocked = trafficFilter.isBlockedHost(host);
            if (hostIsBlocked) {
                blocked = true;
            } else {
                blocked = false;
            }
        } catch (Exception e) {
            System.out.print("Error");
            blocked = false;
        }
        if (blocked) {
            Interface.consolePrint("BLOCKED\t" + host,true);
            doActionIfBlocked();
            return true;
        }
        return false;
    }

    /** Show cache hit on user interface */

    private void ShowCacheHit(String url, int length) {
        if (url.length() > 40) {
            url = url.substring(0, 40) + "...";
        }
        Interface.consolePrint("**********FOUND\t" + url + " IN CACHE, SIZE=" + length + " bytes************************",true);
    }

    /** Write data to cache */

    private void writeCache(CacheObject thisObject, String url) {
        if (thisObject != null && thisObject.isCachable()) {
            thisObject.setKey(url);
            if (!thisObject.isPrivate()) {
                CacheMgr.getInstance().cacheIn(url, thisObject);
            }
        }
    }

    private byte[] getRefused() {
        return refusedData;
    }

    private int getMaxBufferSize() { return 65536;}


}
