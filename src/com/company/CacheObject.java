package com.company;

import java.util.Date;

/** For transporting information between cache and ProxyThread */

public class CacheObject {
    public String header;
    private String method;
    private boolean noCache;
    private boolean isPrivate;
    private boolean isPublic;
    private boolean noModify;
    private long maxAge;
    private Date date;
    private byte[] data;
    private String key;

    /**
     * Construct a new Cache Object
     */
    public CacheObject() {
        setNoCache(false);
        setPrivate(false);
        setPublic(false);
        setNoModify(false);
        setAvailableAge(100);
        setDate(new Date(0));
        setData(null);
        setKey(null);
        setMethod("404");
    }


    public boolean isCachable() {
        return (!noCache);
    }

    public void setNoCache(boolean noCache) {
        this.noCache = noCache;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void setNoModify(boolean noModify) {
        this.noModify = noModify;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public void setAvailableAge(long maxAge) {
        this.maxAge = maxAge;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public byte[] getData() {
        return data;
    }

    public void put(byte[] data) {
        if (this.data == null) {
            setData(data);
        } else {
            appendData(data);
        }
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    /* Add data to end of current data array
     */
    public void appendData(byte[] data) {
        byte[] newData = new byte[this.data.length + data.length];
        System.arraycopy(this.data, 0, newData, 0, this.data.length);
        System.arraycopy(data, 0, newData, this.data.length, data.length);
        this.data = newData;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}