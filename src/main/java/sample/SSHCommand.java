package sample;

import com.chilkatsoft.CkSsh;

/**
 * @autor slonikmak on 20.12.2018.
 */
public class SSHCommand {
    static {
        try {
            System.loadLibrary("chilkat");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native code library failed to load.\n" + e);
            System.exit(1);
        }
    }

    CkSsh ssh;


    /*hostname = "192.168.10.82";
    port = 22;*/
    String hostname = "192.168.10.82";
    int port = 22;

    int channelNum;

    public SSHCommand(){
        ssh = new CkSsh();

        boolean success = ssh.UnlockComponent("Anything for 30-day trial");
        if (success != true) {
            System.out.println(ssh.lastErrorText());
            return;
        }

        success = ssh.Connect(hostname,port);
        if (success != true) {
            System.out.println(ssh.lastErrorText());
            return;
        }

        success = ssh.AuthenticatePw("pi","pass");
        if (success != true) {
            System.out.println(ssh.lastErrorText());
            return;
        }

        ssh.put_IdleTimeoutMs(5000);

        //  Authenticate using login/password:


        //  Open a session channel.  (It is possible to have multiple
        //  session channels open simultaneously.)

        channelNum = ssh.OpenSessionChannel();
        if (channelNum < 0) {
            System.out.println(ssh.lastErrorText());
            return;
        }
    }

    public void sendMsg(String msg) {


        //  Request a directory listing on the remote server:
        //  If your server is Windows, change the string from "ls" to "dir";
        boolean success = ssh.SendReqExec(channelNum,msg);

        if (success != true) {
            System.out.println(ssh.lastErrorText());
            return;
        }

        //  Call ChannelReceiveToClose to read
        //  output until the server's corresponding "channel close" is received.
        success = ssh.ChannelReceiveToClose(channelNum);
        if (success != true) {
            System.out.println(ssh.lastErrorText());
            return;
        }

        //  Let's pickup the accumulated output of the command:
        String cmdOutput = ssh.getReceivedText(channelNum,"ansi");
        if (ssh.get_LastMethodSuccess() != true) {
            System.out.println(ssh.lastErrorText());
            return;
        }

        //  Display the remote shell's command output:
        System.out.println(cmdOutput);

        //  Disconnect
        ssh.Disconnect();
    }
}
