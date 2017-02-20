package com.company;

/**
 * Handling HTTP request headers
 */
public class RequestHandler {

    public enum METHOD {
        GET, CONNECT, ERROR
    };

    private String url;
    private String host;
    private METHOD method;
    private String protocol;
    private int headerRows;

    public RequestHandler(byte[] data) {
        String temp = new String(data);
        try {
            String[] request = temp.split("\r\n");
            headerRows = request.length;
            parse(request);
            if (method==(METHOD.CONNECT)) {
                removePort();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Remove port # if necessary */

    private void removePort() {
        host = host.substring(0, host.indexOf(':'));
    }

    /** Parse for method. Currently only accepts CONNECT and GET methods */

    private void parserMethod(String string) {
        if(string.trim().equals("CONNECT")){
            method = METHOD.CONNECT;
        }
        else if(string.trim().equals("GET")){
            method = METHOD.GET;
        }
        else{
            method = METHOD.ERROR;
        }
    }


    /** Parses request for url, host, method*/ 

    private void parse(String[] request) {
        String[] s = request[0].split(" ");
        if (s.length > 0) {
            parserMethod(s[0]);
            if (method == METHOD.CONNECT) {
                url = s[1];
                url.substring(0, url.indexOf(':'));
            } else {
                url = s[1];
            }
            protocol = s[2];
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



    public String getUrl() {
        return url;
    }

    public String getHost() {
        return host;
    }

    public METHOD getMethod() {
        return method;
    }

}
