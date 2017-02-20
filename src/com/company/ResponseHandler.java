package com.company;

/**
 * Created by Rob on 18/02/2017.
 */

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Handling HTTP response headers
 **/
public class ResponseHandler {


    private static final int CHECK_SIZE = 3;
    private static final String CC_LW = "cache-control";
    private static final String CC_UP = "Cache-Control";

    private String method;
    private Map<String, String> headerMap;
    private byte[] contents;
    String toString;

    /**
     * ResponseHandler constructor where data is the response received from host
     */
    ResponseHandler(byte[] data) {
        if (data != null) {
            headerMap = new HashMap<>();
            toString = new String(data);
            String[] response = toString.split("\r\n");
            method = response[0].split(" ")[1];
            for (int i = 1; i < response.length; i++) {
                if (!response[i].isEmpty()) {
                    String[] keyValue = response[i].split(":");
                    if (keyValue.length == 2) {
                        headerMap.put(keyValue[0].trim(), keyValue[1]);
                    } else {
                        try {
                            putIn(response[i]);
                        } catch (StringIndexOutOfBoundsException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    break;
                }
            }
            for (int i = 0; i < data.length; i++) {
                try {
                    if (data[i] == '\n' && data[i + 1] == '\r' && data[i + 2] == '\n') {
                        contents = new byte[data.length - (i + CHECK_SIZE)];
                        System.arraycopy(data, (i + CHECK_SIZE), contents, 0, data.length - (i + CHECK_SIZE));
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                }
            }
        }
    }

    /**
     * Check if response can be cached, create cache object if necessary
     */
    CacheObject cachingProtocol() {
        /* If response was not OK, don't cache */
        if (!checkOK()) {
            return null;
        }
        CacheObject thisCache = new CacheObject();
        String[] settings;
        String tmp;
        boolean cacheControl = checkForCaching();
        if (cacheControl) {
            String cacheCheck = getCacheCheck(); //check if data can be cached
            if (cacheCheck.contains("no-cache")) {
                thisCache.setNoCache(true);
            } else {
                thisCache.setNoCache(false);
                settings = cacheCheck.split(",");  //parse for settings as necessary
                for (int i = 0; i < settings.length; i++) {
                    tmp = settings[i];
                    thisCache = checkSettings(tmp, thisCache);
                }
            }
        }
        if (headerMap.containsKey("Date")) {
            thisCache.setDate(convertDate(headerMap.get("Date").trim()));
        }
        thisCache.setMethod(method);
        thisCache.header = toString;
        return thisCache;
    }

    /**
     * Check the reponse returned was OK
     */
    private boolean checkOK() {
        return (method.contains("200"));
    }

    /**
     * Enter key value pair in hashmap
     */
    private void putIn(String resp) {
        int n = resp.indexOf(':');
        String key = resp.substring(0, n);
        String value = resp.substring(n + 1);
        headerMap.put(key, value);
    }


    /**
     * Converting from HTTP format date to Date Object
     */
    private Date convertDate(String date) {
        Date d = null;
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        try {
            d = format.parse(date);
        } catch (ParseException e) {
            System.out.println("Error while parsing");
        }
        return d;
    }

    /**
     * Check for cache settings and set attributes in object accordingly
     */
    private CacheObject checkSettings(String check, CacheObject object) {
        if (check.contains("max-age")) {
            int index = check.indexOf("=");
            String s = check.substring(index + 1);
            object.setExpiryAge(Integer.parseInt(s));
        } else if (check.contains("private")) {
            object.setPrivate(true);
        } else if (check.contains("public")) {
            object.setPublic(true);
        }
        return object;
    }

    /**
     * Check if response can be cached
     */
    private boolean checkForCaching() {
        return (headerMap.containsKey(CC_UP) || headerMap.containsKey(CC_LW));
    }

    /**
     * Return correctly capitalized version of cache protocol
     */
    private String getCacheCheck() {
        String cacheCheck = headerMap.get(CC_UP);
        if (cacheCheck == null) {
            cacheCheck = headerMap.get(CC_LW);
        }

        return cacheCheck;
    }
}
