package com.company;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileWriter;

/**
 * Holds information on hosts blocked by proxy
 */
public class HostBlocking {

    private BufferedReader blockedHostFile;
    private List<String> blockedHostList;
    private static String pathToBlocked = ".blocked_hosts"; //path to file containing blocked hosts

    private static HostBlocking instance = new HostBlocking();

    /**
     * Creates instance of this class
     */
    private HostBlocking() {
        if (blockedHostFile == null) {
            blockedHostFile = openFile(pathToBlocked);
        }

        if (blockedHostList == null) {
            blockedHostList = new ArrayList<>();
        }
        retrieveBlockedHosts();
    }


    /**
     * Parse host file for hosts that will be blocked
     */
    private void retrieveBlockedHosts() {
        String hostString;
        try {
            while ((hostString = blockedHostFile.readLine()) != null) {
                if (!hostString.startsWith("#") && !hostString.isEmpty()) {
                    if (!hostString.equals("[HOSTS]")) {
                        blockedHostList.add(hostString.trim());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns instance of HostBlocking
     */
    public static HostBlocking getInstance() {
        return instance;
    }


    /**
     * Checks if given hostname 'host' is a blocked host
     */
    boolean isBlockedHost(String host) {
        for (int i = 0; i < blockedHostList.size(); i++) {
            if (blockedHostList.get(i).equalsIgnoreCase(host)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Block host (URL) passed to function
     */
    void blockHost(String host) {
        blockedHostList.add(host);
    }

    /**
     * Unblock host (URL) passed to function
     */
    boolean unblockHost(String host) {
        for (int i = 0; i < blockedHostList.size(); i++) {
            if (blockedHostList.get(i).equals(host)) {
                blockedHostList.remove(i);
                return true;
            }
        }
        return false;
    }


    /**
     * Used for reading from blocked host file
     */
    private static BufferedReader openFile(String path) {

        File f;
        BufferedReader myReader = null;
        FileReader reader;

        try {
            f = new File(path);
            reader = new FileReader(f);
            myReader = new BufferedReader(reader);
        } catch (IOException e) {

        }
        return myReader;
    }

    /**
     * Write to blocked host file when proxy is exited ensuring blocked host data is persistent
     */
    void writeBlockedHosts() {
        File f = new File(pathToBlocked);
        if (f.exists()) {
            try {
                FileWriter fw = new FileWriter(f.getAbsoluteFile());
                BufferedWriter br = new BufferedWriter(fw);
                br.write("[HOSTS]\n");
                List<String> hlist = blockedHostList;
                if (hlist != null) {
                    for (int i = 0; i < hlist.size(); i++) {
                        br.write(hlist.get(i) + "\n");
                    }
                }
                br.close();
            } catch (IOException e) {
            }
        }
    }
}