package com.company;

import java.util.Date;

/**
 * For transporting information between Cache and ProxyThread
 */

public class CacheObject {
    public String header;
    private String method;
    private boolean privateCache = false; // Represents if cache is private
    private boolean publicCache = false;  // or public
    private boolean noCache = false;      // true if data is not cachable
    private long expiryAge = 100;         // maximum age of cache object before it expires
    private Date date = new Date(0);// date brought into cache
    private byte[] data = null;
    private String key = null;

    /**
     * Construct a new Cache Object
     */
    CacheObject() {
        setMethod("404");
    }

    /**
     * For putting data into cache object
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * Check if data can be cached
     */
    boolean canBeCached() {
        return (!noCache);
    }

    /**
     * Return expiry age of object
     */
    long getExpiryAge() {
        return expiryAge;
    }

    /**
     * Return date object was cached
     */
    public Date getDate() {
        return date;
    }

    /**
     * Return data in cached object
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Get key (URL in String format) corresponding to this cache object
     */
    String getKey() {
        return key;
    }

    /**
     * Signal that this data cannot be cached
     */
    void setNoCache(boolean noCache) {
        this.noCache = noCache;
    }

    /**
     * Check if cache is set as private
     */
    public boolean isPrivate() {
        return privateCache;
    }


    /**
     * Set expiry age of cache object
     */
    public void setExpiryAge(long expiryAge) {
        this.expiryAge = (long) expiryAge;
    }

    /**
     * Set date that data was cached
     */
    public void setDate(Date date) {
        this.date = date;
    }

    /**
     * Set whether cache data is private
     */
    public void setPrivate(boolean result) {
        this.privateCache = result;
    }

    /**
     * Set whether cache data is public
     */
    public void setPublic(boolean isPublic) {
        this.publicCache = isPublic;
    }

    /**
     * Pass data into this cache object
     */
    void put(byte[] data) {
        if (this.data == null) {
            setData(data);
        } else {
            appendData(data);
        }
    }

    /**
     * Add data to data currently existing in this cache object
     */
    private void appendData(byte[] data) {
        byte[] newData = new byte[this.data.length + data.length];
        System.arraycopy(this.data, 0, newData, 0, this.data.length);
        System.arraycopy(data, 0, newData, this.data.length, data.length);
        this.data = newData;
    }

    /**
     * Set this object's key (URL in string format)
     */
    void setKey(String key) {
        this.key = key;
    }

    /**
     * Set this object's method
     */
    public void setMethod(String method) {
        this.method = method;
    }
}