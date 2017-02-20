package com.company;

/**
 * Created by Rob on 18/02/2017.
 */
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/** Handling HTTP response headers **/

public class ResponseHandler {

 
    private Map<String, String> mapHeader = new HashMap<>();

    public static final int CHECK_SIZE = 3;

    private String method;
    private Map<String, String> headerMap;
    private byte[] contents;
    String toString;

    public ResponseHandler(byte[] data) {
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


    public CacheObject getCacheInfo() {
        if(!checkOK()){
            return null;
        }
        CacheObject thisCache = new CacheObject();
        thisCache.setMethod(method);
        String[] settings;
        String tmp;
        boolean cacheControl = (mapHeader.containsKey("Cache-Control") || mapHeader
                .containsKey("cache-control"));
        String cacheCheck = mapHeader.get("Cache-Control");
        if(cacheCheck == null){
            cacheCheck = mapHeader.get("cache-control");
        }
        if (cacheControl) {
            if (!cacheCheck.contains("no-cache")) {
                thisCache.setNoCache(false);
                settings = cacheCheck.split(",");
                for (int i = 0; i < settings.length; i++) {
                    tmp = settings[i];
                    thisCache = checkSettings(tmp,thisCache);
                }
            } else {
                thisCache.setNoCache(true);
            }
        }
        if (mapHeader.containsKey("Date")) {
            thisCache.setDate(convertDate(mapHeader.get("Date").trim()));
        }
        thisCache.header = toString;
        return thisCache;
    }
    private boolean checkOK(){
        return(method.contains("200"));
    }

    private void putIn(String resp){
        int n = resp.indexOf(':');
        String key = resp.substring(0, n);
        String value = resp.substring(n + 1);
        headerMap.put(key, value);
    }

    private CacheObject checkSettings(String check,CacheObject object){
        if (check.contains("max-age")) {
            int index = check.indexOf("=");
            String s = check.substring(index + 1);
            object.setAvailableAge(Integer.parseInt(s));
        } else if (check.contains("private")) {
            object.setPrivate(true);
        } else if (check.contains("public")) {
            object.setPublic(true);
        } else if (check.contains("no-transform")) {
            object.setNoModify(true);
        }
        return object;
    }
    
    /** Converting from HTTP format date to Date Object*/
    
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
}
