package com.company;

import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * CacheMgr manages all CacheObjects
 * CacheObjects are stored in hashtable
 * URL is the hash key and the value is the CacheObject
 * Cache is not persistent
 */
public class CacheMgr {
    // Used by locking system
    private static final boolean fairLock = true;
    private static final long maxWait = 50;
    private static final TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    private static CacheMgr instance = new CacheMgr();

    // Hashtable used to store CacheObjects
    private static Hashtable<String, CacheObject> cache = new Hashtable<>();
    private static ReentrantReadWriteLock cacheLock;

    private CacheMgr() {
        // Check to see if a lock already exists, create it if not
        if (cacheLock == null) {
            cacheLock = new ReentrantReadWriteLock(fairLock);
        }

    }

    /**
     * Returns instance of this class
     */
    public static CacheMgr getInstance() {
        return instance;
    }

    /**
     * Checks for cached URL
     */
    synchronized boolean isCached(String url) {
        boolean result = false;
        try {
            /* maxWait prevents deadlock when trying to read from cache
             * while ensuring data integrity */

            if (cacheLock.readLock().tryLock(maxWait, timeUnit)) {
                result = cache.containsKey(url);

                /* Get copy of result if cached */

                if (result) {
                    CacheObject r = cache.get(url);
                    cacheLock.readLock().unlock();

			     /* Check that cached object has not expired */

                    Calendar c = Calendar.getInstance();
                    Date d = r.getDate();
                    c.setTime(d);
                    c.add(Calendar.SECOND, (int) r.getExpiryAge());

                        /* If cached object has expired, remove it and return false
                        * ie this object is not (should not be) in cache*/
                    if (c.after(d)) {
                        result = false;
                        cacheLock.writeLock().lock();
                        cache.remove(url);
                        cacheLock.writeLock().unlock();
                    }
                } else {
                    cacheLock.readLock().unlock();
                }
            }
        } catch (InterruptedException e) {
            result = false;
        }
        return result;
    }

    /**
     * Returns CacheObject associated with url passed to function
     **/
    synchronized CacheObject getData(String url) {
        CacheObject result = null;
        if (isCached(url)) {
            try {
                if (cacheLock.readLock().tryLock(maxWait, timeUnit)) {
                    result = cache.get(url);
                    cacheLock.readLock().unlock();
                }
            } catch (InterruptedException e) {
            }
        }
        return result;
    }

    /**
     * Add items to cache, returning true if data could be cached
     * or false if not cached
     **/
    synchronized boolean cacheIn(String url, CacheObject data) {
        if (data == null) {
            return false;
        }
        boolean result = false;
        try {
            /* maxWait prevents deadlock when attempting to cache */
            if (cacheLock.writeLock().tryLock(maxWait, timeUnit)) {
                cache.put(url, data);
                result = true;
                cacheLock.writeLock().unlock();
            }
        } catch (InterruptedException e) {
            try {
                cacheLock.writeLock().unlock();
            } catch (Exception f) {
            }
        }
        return result;
    }

    /**
     * Return cache in its entirety in array of CacheObjects
     * Uses readLock to ensure integrity of data returned
     */
    CacheObject[] getCache() {
        CacheObject[] result = null;
        try {
            if (cacheLock.readLock().tryLock()) {
                Set<String> keys = cache.keySet();  // gets keys of all objects in cache
                String[] k = new String[keys.size()]; // and returns cache object corresponding to each
                result = new CacheObject[keys.size()];
                keys.toArray(k);
                for (int i = 0; i < k.length; i++) {
                    result[i] = cache.get(k[i]);
                }
            }
        } finally {
            cacheLock.readLock().unlock();
        }
        return result;
    }

}