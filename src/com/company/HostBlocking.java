package com.company;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileWriter;

/** Holds information on hosts blocked by proxy*/

public class TrafficFilter {

    private BufferedReader blockedHostFile;
    private List<String> blockedHostList;
    private static String pathToBlocked = ".blocked_hosts";

    private static TrafficFilter instance = new TrafficFilter();

    /** Creates instance of this class */
    
    private TrafficFilter() {
        if (blockedHostFile == null) {
            blockedHostFile = openFile(pathToBlocked);
        }

        if (blockedHostList == null) {
            blockedHostList = new ArrayList<>();
        }
        retrieveBlockedHosts();
    }




    /** Parse host file for hosts that will be blocked */
    
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
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Returns instance of TrafficFilter */

    public static TrafficFilter getInstance() {
        return instance;
    }


    /** Checks if given hostname 'host' is a blocked host */
    boolean isBlockedHost(String host) {
        for (int i = 0; i < blockedHostList.size(); i++) {
            if (blockedHostList.get(i).equalsIgnoreCase(host)) {
                return true;
            }
        }
        return false;
    }

    
    
    void blockHost(String host) {
        blockedHostList.add(host);
    }


    boolean removeBlockedHost(String host) {
        for (int i = 0; i < blockedHostList.size(); i++) {
            if (blockedHostList.get(i).equals(host)) {
                blockedHostList.remove(i);
                return true;
            }
        }
        return false;
    }


    private static BufferedReader openFile(String path) {

        File f;
        BufferedReader bufReader = null;
        FileReader reader;

        try {
            f = new File(path);
            reader = new FileReader(f);
            bufReader = new BufferedReader(reader);
        } catch (IOException e) {

        }
        return bufReader;
    }

    void writeBlockedHosts() {
        File f = new File(pathToBlocked);
        if (f.exists()){
            try {
                FileWriter fw = new FileWriter(f.getAbsoluteFile());
                BufferedWriter br = new BufferedWriter(fw);
                br.write("[HOSTS]\n");
                List<String> hlist = blockedHostList;
                for (int i = 0; i < hlist.size(); i++) {
                    System.out.print(hlist.get(i));
                    br.write(hlist.get(i) + "\n");
                }
                br.close();
            } catch (IOException e) {
            }
        }
    }
}