/**
 * Simple SFTP server implemented in Java
 * based on https://mina.apache.org/sshd-project/embedding_ssh.html 
 * @author Denis Baltor
 * 19/03/2020
 * 
 * To shutdown the server key 'q' or 'Q' followed by ENTER.
 * 
 * server command line:
 * --------------------
 * java -cp sftp/lib/sftp.jar:sftp/lib/sshd-core-1.0.0.jar:sftp/lib/slf4j-api-1.7.12.jar sftp.Server
 * @param -p=<port> (optional) Set the SFTP server's port.
 * @param -d=<dir> (optional) Set the SFTP server's home directory.
 * @param -h (optional) Show parameter options.
 * 
 */

package sftp;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.File;
import java.io.IOException;

import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuth;
import org.apache.sshd.server.auth.UserAuthNoneFactory;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.keyprovider.AbstractGeneratorHostKeyProvider;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;

public class Server {

    private static final String DEFAULT_DIRECTORY = ".";
    private static final int DEFAULT_PORT = 2222;

    private final int port;
    private final String folder;
    private final File server_key_file;
    private boolean stopped;
    private SshServer sshd;

    // Singleton double checked locking implementation
    /*private static Server instance;
    
    // disabling the default Contructor
    private Server(){}

    public static Server getInstance(){
        if (instance == null){
            synchronized (Server.class) {
                if (instance == null){
                    instance = new Server();
                }
            }
        }
        return instance;
    }*/

    private Server(int port, String folder) {
        this.port = port;
        this.folder = folder;
        this.server_key_file = new File(String.format("hostkey%d.ser", port));
        this.stopped = true;
    }

    public static ServerBuilder builder() {
        return new ServerBuilder();
    }

    public static class ServerBuilder {
        private int port;
        private String folder;

        public ServerBuilder port(int port) {
            this.port = port;
            return this;
        }
        public ServerBuilder folder(String folder){
            this.folder = folder;
            return this;
        }
        public Server build() {
            return new Server(port, folder);
        }
    }

    public boolean start() {

        AbstractGeneratorHostKeyProvider hostKeyProvider =
            new SimpleGeneratorHostKeyProvider(server_key_file.toPath());
        hostKeyProvider.setAlgorithm("RSA");

        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(port);
        sshd.setKeyPairProvider(hostKeyProvider);        

        List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<NamedFactory<UserAuth>>();
        userAuthFactories.add(new UserAuthNoneFactory());
        sshd.setUserAuthFactories(userAuthFactories);

        sshd.setCommandFactory(new ScpCommandFactory());

        List<NamedFactory<Command>> namedFactoryList = new ArrayList<NamedFactory<Command>>();
        namedFactoryList.add(new SftpSubsystemFactory());
        sshd.setSubsystemFactories(namedFactoryList);

        VirtualFileSystemFactory fileSystemFactory = new VirtualFileSystemFactory();
        fileSystemFactory.setDefaultHomeDir(folder);
        sshd.setFileSystemFactory(fileSystemFactory);

        try {
            sshd.start();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        }
        stopped = false;
        return true;
    }
    
    public boolean stop() {
        try {
            sshd.stop();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        }
        stopped = true;
        return true;
    }

    private static void showHelpMsg() {
        // Text Block - Java 13 & 14 --enable-preview
        /*System.out.println(String.format("""
        The valid parameters are:
        -d=<home directory> 'Set the SFTP server's home directory. Default value: %s'
        -p=<listening port> 'Set the SFTP server's port. Default value: %d'
        -h 'Show this message'
        """ , DEFAULT_DIRECTORY, DEFAULT_PORT));*/

        System.out.println(String.format(
            "The valid parameters are:"
            +"\n-d=<home directory> 'Set the SFTP server's home directory. Default value: %s'"
            +"\n-p=<listening port> 'Set the SFTP server's port. Default value: %d'"
            +"\n-h 'Show this message'" , DEFAULT_DIRECTORY, DEFAULT_PORT));
    }

    public static void main ( String args[] ) {
        //String argFolder = DEFAULT_DIRECTORY;
        String argFolder = System.getProperty("user.dir");
        int argPort = DEFAULT_PORT;

        // Parse command line's arguments
        final int PARAM_KEY_LENGHT = 3;
        if (args.length > 0) {
            for (String arg : args) {
              if (arg.length() >= PARAM_KEY_LENGHT && arg.substring(0, PARAM_KEY_LENGHT).equals("-d="))
                  // get the working directory
                  argFolder = arg.substring(PARAM_KEY_LENGHT);
              else if (arg.length() >= PARAM_KEY_LENGHT && arg.substring(0,PARAM_KEY_LENGHT).equals("-p="))
                  // get the listening port
                try{
                  argPort = Integer.parseInt(arg.substring(PARAM_KEY_LENGHT));
                } catch(NumberFormatException nfe){
                  System.out.println(String.format(
                    "Invalid number format entered with -p. Using default value of %s"
                    +"\n======================="
                    +"\n%s"
                    +"\n=======================", argPort, arg));
                }
              else if (arg.equals("-h")){
                showHelpMsg();
                System.exit(0);
              }
              else {
                System.out.println(String.format(
                    "Invalid parameter found!"
                    +"\n======================="
                    +"\n%s"
                    +"\n=======================", arg));
                showHelpMsg();
                System.exit(1);
              }
            }
        }

        // Start SFTP server
        Server server = Server
            .builder()
            .port(argPort)
            .folder(argFolder)
            .build();

        if( !server.start()) {
            System.exit(1);
        }
        System.out.println(String.format(
            "SFTP server started..."
            +"\nServer port: %d"
            +"\nServer home dir: %s", argPort, argFolder));

        // Wait for QUIT command to exit
        try (Scanner scanner = new Scanner(System.in)) {
            String keyedOp;
            do {
                System.out.println("Enter 'q' or 'Q' to exit...");
                keyedOp = scanner.nextLine();
                if ("q".equals(keyedOp) || "Q".equals(keyedOp)) {
                        if (!server.stop()){
                            System.exit(1);
                        }
                }
            } while (!server.stopped);
        }
    }
}
