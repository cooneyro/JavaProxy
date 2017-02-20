package com.company;

/**
 * Handling HTTP request headers
 */
public class RequestHandler {

    public enum METHOD {
        GET, CONNECT, ERROR
    }

    ;

    private String url;
    private String host;
    private METHOD method;
    private int headerRows;

    /**
     * RequestHandler constructor where data is request sent by client
     */

    RequestHandler(byte[] data) {
        String temp = new String(data);
        try {
            String[] request = temp.split("\r\n");
            headerRows = request.length;
            parse(request);
            if (method == (METHOD.CONNECT)) {
                removePort();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses host from port # if necessary
     */
    private void removePort() {
        host = host.substring(0, host.indexOf(':'));
    }

    /**
     * Parse for method. Currently only accepts CONNECT and GET methods
     */
    private void parserMethod(String string) {
        if (string.trim().equals("CONNECT")) {
            method = METHOD.CONNECT;
        } else if (string.trim().equals("GET")) {
            method = METHOD.GET;
        } else {
            method = METHOD.ERROR;
        }
    }


    /**
     * Parses request for url, host, method
     */
    private void parse(String[] request) {
        String[] s = request[0].split(" ");
        if (s.length > 0) {
            parserMethod(s[0]);
            url = s[1];
        } else {
            System.out.println("Couldn't parse first line");
        }

        for (int i = 1; i < headerRows; i++) {
            if (request[i].contains("Host:")) {
                host = request[i].substring(6).trim();
                if (host.contains("\n")) {
                    host = host.substring(0, (host.indexOf('\n') - 1));
                }
            }
        }
    }

    /**
     *  Returns url associated with request
     */
    public String getUrl() {
        return url;
    }

    /**
     *  Returns host associated with request
     */
    String getHost() {
        return host;
    }

    /**
     *  Returns method associated with request
     */
    public METHOD getMethod() {
        return method;
    }

}
