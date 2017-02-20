package com.company;

/**
 * Created by Rob on 18/02/2017.
 */
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
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.text.DefaultCaret;

/**
 *  Management console (user interface) for proxy)
 *  Allows user to block and unblock hosts
 *  Also allows viewing of cache contents
 *
 */
public class Interface {

    private JFrame frmWebProxy;
    private JTextField infoField;
    private static JTextArea txtInfoArea;
    private static JScrollPane txtInfoScrollPane;
    static Main proxyMainThread;
    private static Lock l;
    public static PrintStream originalSystemOut;

    public static void main(String[] args) {
        l = new ReentrantLock();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e1) {
            e1.printStackTrace();
        }
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    Interface window = new Interface();
                    window.frmWebProxy.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        proxyMainThread = new Main();
        String[] arguments = {};
        try{
            proxyMainThread.main(arguments);
        }
        catch (IOException e) {
            //System.err.println("Could not listen on port: " + args[0]);
            System.exit(-1);
        }

    }

    public Interface() {
        initialize();
        setUpSystemOut();
    }

    private void setUpSystemOut() {
        originalSystemOut = System.out;
        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int arg0) throws IOException {
                consolePrint((char) arg0 + "", false);
            }
        }));
    }

    private void initialize() {
        frmWebProxy = new JFrame();
        frmWebProxy.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.setOut(originalSystemOut);
                frmWebProxy.dispose();
                TrafficFilter.getInstance().writeBlockedHosts(); /** writes blocked hosts to file*/
            }
        });
        frmWebProxy.setTitle("Proxy Management Console");
        frmWebProxy.setBounds(100, 100, 750, 450);
        frmWebProxy.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frmWebProxy.setResizable(false);

        JMenuBar menuBar = new JMenuBar();
        frmWebProxy.setJMenuBar(menuBar);


        JButton mntmOpenFile = new JButton("Show Cache");
        mntmOpenFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CacheMgr manager = CacheMgr.getInstance();
                CacheObject[] cachedObjects = manager.getCache();
                consolePrint(
                        "***********CACHE***CONTENTS**********",
                        true);
                for (int i = 0; i < cachedObjects.length; i++) {
                    consolePrint("|" + cachedObjects[i].getDate().toString() + "|" + cachedObjects[i].getMaxAge()
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
        menuBar.add(mntmOpenFile);


        frmWebProxy.getContentPane().setLayout(null);

        JButton btnBlockHost = new JButton("Block Host");
        btnBlockHost.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String hostToBlock = infoField.getText();
                if (!hostToBlock.isEmpty()) {
                    infoField.setText("");
                        TrafficFilter.getInstance().addBlockedHost(hostToBlock);
                        txtInfoArea.append("Blocked Host: " + hostToBlock + "\n");
                } else {
                    txtInfoArea.append("You need to enter a host.\n");
                }
            }
        });
        btnBlockHost.setBounds(15, 15, 350, 37);
        frmWebProxy.getContentPane().add(btnBlockHost);



        JButton btnUnblockHost = new JButton("Unblock Host");
        btnUnblockHost.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String hostToUnblock = infoField.getText();
                infoField.setText("");
                TrafficFilter.getInstance().removeBlockedHost(hostToUnblock);
                txtInfoArea.append("Unblocked Host: " + hostToUnblock + "\n");
            }
        });
        btnUnblockHost.setBounds(380, 15, 350, 37);
        frmWebProxy.getContentPane().add(btnUnblockHost);




        txtInfoArea = new JTextArea(10, 60);
        txtInfoArea.setLineWrap(true);
        txtInfoScrollPane = new JScrollPane(txtInfoArea);
        txtInfoScrollPane.setBounds(15, 120, 720, 225);
        txtInfoScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        frmWebProxy.getContentPane().add(txtInfoScrollPane);
        DefaultCaret caret = (DefaultCaret) txtInfoArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        infoField = new JTextField();
        infoField.setBounds(15, 70, 720, 40);
        frmWebProxy.getContentPane().add(infoField);
        infoField.setColumns(10);

    }

    public static void consolePrint(String s, boolean addNewLine) {
        l.lock();
        try {
            if (addNewLine) {
                s += "\n";
            }
            txtInfoArea.append(s);
        } finally {
            l.unlock();
        }
    }
}
