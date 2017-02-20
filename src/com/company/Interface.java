package com.company;


import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.text.DefaultCaret;

/**
 *  Management console (user interface) for proxy)
 *  Allows user to block and unblock hosts
 *  Also allows viewing of cache contents
 */
public class Interface {

    private JFrame frame;
    private JTextField textField;
    private static JTextArea textArea;
    private static Lock lock;
    private static PrintStream originalSystemOut;

    public static void main(String[] args) {
        lock = new ReentrantLock();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        /* Make management console visible */

        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    Interface window = new Interface();
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        /* Set up main port listener */

        Main proxyMainThread = new Main();
        String[] arguments = {};
        try{
            proxyMainThread.main(arguments);
        }
        catch (IOException e) {
            //System.err.println("Could not listen on port: " + args[0]);
            System.exit(-1);
        }

    }

    /* Constructor */

    public Interface() {
        initialize();
    }

    /* Sets up user interface for proxy management */

    private void initialize() {
        frame = new JFrame();
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.setOut(originalSystemOut);
                frame.dispose();
                HostBlocking.getInstance().writeBlockedHosts(); /* writes blocked hosts to file */
            }
        });

        /* Defining bounds for user interface */

        frame.setTitle("Proxy Management Console");
        frame.setBounds(100, 100, 750, 450);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        /* Setting menu and text areas */
        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);
        textArea = new JTextArea(10, 60);
        textArea.setLineWrap(true);
        JScrollPane txtInfoScrollPane = new JScrollPane(textArea);
        txtInfoScrollPane.setBounds(15, 120, 720, 225);
        txtInfoScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        frame.getContentPane().add(txtInfoScrollPane);
        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        textField = new JTextField();
        textField.setBounds(15, 70, 720, 40);
        frame.getContentPane().add(textField);
        textField.setColumns(10);

        /* Creating button to show cache contents when clicked */

        JButton showCache = new JButton("Show Cache");
        showCache.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CacheMgr manager = CacheMgr.getInstance();
                CacheObject[] cachedObjects = manager.getCache();
                consolePrint(
                        "***********CACHE***CONTENTS**********",
                        true);
                for (int i = 0; i < cachedObjects.length; i++) {
                    consolePrint("|" + cachedObjects[i].getDate().toString() + "|" + cachedObjects[i].getExpiryAge()
                            + "|" + cachedObjects[i].getKey(), true);
                    consolePrint(
                            "*****************************************************************************",
                            true);
                }
                consolePrint(
                        "*****************END*****************",
                        true);
            }
        });
        menuBar.add(showCache);
        frame.getContentPane().setLayout(null);

        /* Creating button to allow user to block hostname typed in text area */

        JButton btnBlockHost = new JButton("Block Host");
        btnBlockHost.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String hostToBlock = textField.getText();
                if (!hostToBlock.isEmpty()) {
                    textField.setText("");
                        HostBlocking.getInstance().blockHost(hostToBlock);
                        textArea.append("Blocked Host: " + hostToBlock + "\n");
                } else {
                    textArea.append("You need to enter a host.\n");
                }
            }
        });
        btnBlockHost.setBounds(15, 15, 350, 37);
        frame.getContentPane().add(btnBlockHost);

        /* Creating button to unblock hostname typed in text area */

        JButton btnUnblockHost = new JButton("Unblock Host");
        btnUnblockHost.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String hostToUnblock = textField.getText();
                textField.setText("");
                HostBlocking.getInstance().unblockHost(hostToUnblock);
                textArea.append("Unblocked Host: " + hostToUnblock + "\n");
            }
        });
        btnUnblockHost.setBounds(380, 15, 350, 37);
        frame.getContentPane().add(btnUnblockHost);



    }

    /** Prints to management console, with newline if (newL) */
    
    static void consolePrint(String s, boolean newL) {
        lock.lock();
        try {
            if (newL) {
                s += "\n";
            }
            textArea.append(s);
        } finally {
            lock.unlock();
        }
    }
}
