package com.company;

/**
 * Created by Rob on 18/02/2017.
 */
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;



/** CacheMgr manages all CacheObjects
 *  CacheObjects are stored in hashtable
 *  URL is the hash key and the value is the CacheObject
 *  Cache is not persistent
 */
public class CacheMgr {
    // Used by locking system
    private static final boolean fairLock = true;
    private static final long maxWait = 50;
    private static final TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    // This is the ONLY instance of this class in the whole program.
    private static CacheMgr instance = new CacheMgr();

    private static Hashtable<String, CacheObject> cache = new Hashtable<>();
    private static ReentrantReadWriteLock cacheLock;
    private Calendar c = Calendar.getInstance();
    private CacheMgr() {
        // Check to see if a lock already exists, create it if not
        if (cacheLock == null) {
            cacheLock = new ReentrantReadWriteLock(fairLock);
        }

    }

    /** Returns instance of this class*/

    public static CacheMgr getInstance() {
        return instance;
    }

    /** Checks for cached URL */

    public synchronized boolean isCached(String url) {
        boolean result = false;
        try {
            try {
		/*
		 * Get copy of result if cached
		 */
                if (cacheLock.readLock().tryLock(maxWait, timeUnit)) {
                    result = cache.containsKey(url);
                    if (result) {
                        CacheObject r = cache.get(url);
                        cacheLock.readLock().unlock();
			    /*
			     * Check that cached object has not expired
			     */
                        Date d = r.getDate();
                        c.setTime(d);
                        c.add(Calendar.SECOND, (int)r.getMaxAge());
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
            } finally {
                cacheLock.readLock();
            }
        } catch (InterruptedException e) {
            result = false;
        }
        return result;
    }

    /** Returns data stored inside cached object**/

    public synchronized CacheObject getData(String url) {
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

    /** Add items to cache**/

    public synchronized boolean cacheIn(String url, CacheObject data) {
        if(data==null){
            return false;
        }
        boolean result = false;
        try {
            if (cacheLock.writeLock().tryLock(maxWait, timeUnit)) {
                cache.put(url, data);
                result = true;
                cacheLock.writeLock().unlock();
            }
        } catch (InterruptedException e) {
            try {
                cacheLock.writeLock().unlock();
            } catch (IllegalMonitorStateException f) {
            }
        }
        return result;
    }

    /** Return cache in its entirety */

    public CacheObject[] getCache() {
        CacheObject[] result = null;
        try {
            if (cacheLock.readLock().tryLock()) {
                Set<String> keys = cache.keySet();
                String[] k = new String[keys.size()];
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