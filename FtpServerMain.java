package com.example;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FtpServerMain {
    public static void main(String[] args) throws Exception {
        // Server factory
        FtpServerFactory serverFactory = new FtpServerFactory();

        // Listener factory on port 2121
        ListenerFactory listenerFactory = new ListenerFactory();
        listenerFactory.setPort(2121);
        serverFactory.addListener("default", listenerFactory.createListener());

        // User setup
        String ftpHome = System.getenv().getOrDefault("FTP_STORAGE_DIR", "ftp_home");
        File homeDir = new File(ftpHome);
        if (!homeDir.exists()) homeDir.mkdirs();

        BaseUser user = new BaseUser();
        user.setName("testuser");
        user.setPassword("testpass");
        user.setHomeDirectory(homeDir.getAbsolutePath());
        user.setAuthorities(Collections.singletonList(new WritePermission()));
        serverFactory.getUserManager().save(user);

        // Register custom Ftplet
        Map<String, Ftplet> ftpletMap = new HashMap<>();
        ftpletMap.put("approvalFtplet", new ApprovalFtplet());
        serverFactory.setFtplets(ftpletMap);

        // Start server
        FtpServer server = serverFactory.createServer();
        server.start();

        System.out.println("FTP Server started on ftp://localhost:2121");
        System.out.println("User: testuser | Password: testpass");
    }

    public static class ApprovalFtplet extends DefaultFtplet {
        @Override
        public FtpletResult onDownloadStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
            String filename = request.getArgument();
            File home = new File(session.getUser().getHomeDirectory());
            File approvalFlag = new File(home, "approved.txt");

            if (!approvalFlag.exists()) {
                System.out.println("Denied download. Approval file not found.");
                return FtpletResult.DISCONNECT;
            }

            try (Scanner sc = new Scanner(approvalFlag)) {
                String status = sc.hasNextLine() ? sc.nextLine().trim() : "NO";
                if (!status.equalsIgnoreCase("YES")) {
                    System.out.println("Download denied: approved.txt doesn't contain 'YES'.");
                    return FtpletResult.DISCONNECT;
                }
            }

            System.out.println("Download approved for: " + filename);
            return FtpletResult.DEFAULT;
        }

        @Override
        public FtpletResult onUploadStart(FtpSession session, FtpRequest request) throws FtpException, IOException {
            File home = new File(session.getUser().getHomeDirectory());
            long freeSpace = home.getFreeSpace();

            if (freeSpace < 10 * 1024 * 1024) { // 10MB
                System.out.println("Upload denied: insufficient space.");
                return FtpletResult.DISCONNECT;
            }

            return FtpletResult.DEFAULT;
        }
    }
}
