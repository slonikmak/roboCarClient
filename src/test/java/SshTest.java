import com.chilkatsoft.CkSsh;

/**
 * @autor slonikmak on 19.12.2018.
 */
public class SshTest {

    static {
        try {
            System.loadLibrary("chilkat");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native code library failed to load.\n" + e);
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        CkSsh ssh = new CkSsh();

        //  Any string automatically begins a fully-functional 30-day trial.
        boolean success = ssh.UnlockComponent("Anything for 30-day trial");
        if (success != true) {
            System.out.println(ssh.lastErrorText());
            return;
        }
        String hostname;
        int port;

        //  Hostname may be an IP address or hostname:
        hostname = "192.168.10.82";
        port = 22;

        success = ssh.Connect(hostname,port);
        if (success != true) {
            System.out.println(ssh.lastErrorText());
            return;
        }

        //  Wait a max of 5 seconds when reading responses..
        ssh.put_IdleTimeoutMs(5000);

        //  Authenticate using login/password:
        success = ssh.AuthenticatePw("pi","pass");
        if (success != true) {
            System.out.println(ssh.lastErrorText());
            return;
        }

        //  Open a session channel.  (It is possible to have multiple
        //  session channels open simultaneously.)
        int channelNum;
        channelNum = ssh.OpenSessionChannel();
        if (channelNum < 0) {
            System.out.println(ssh.lastErrorText());
            return;
        }

        //  The SendReqExec method starts a command on the remote
        //  server.   The syntax of the command string depends on the
        //  default shell used on the remote server to run the command.
        //  On Windows systems it is CMD.EXE.  On UNIX/Linux
        //  systems the user's default shell is typically defined in /etc/password.

        //  Here are some examples of command lines for <b>Windows SSH servers</b>:

        //  Get a directory listing:
        String cmd1 = "dir";

        //  Do a nameserver lookup:
        String cmd2 = "nslookup chilkatsoft.com";

        //  List a specific directory.  Given that the shell is CMD.EXE, backslashes must
        //  be used:
        String cmd3 = "dir \\temp";

        //  Execute a sequence of commands.  The syntax for CMD.EXE may be found
        //  here: http://technet.microsoft.com/en-us/library/bb490880.aspx.  Notice how the commands
        //  are separated by "&&" and the entire command must be enclosed in quotes:
        String cmd4 = "\"cd \\temp&&dir\"";

        //  Here are two examples of command lines for <b>Linux/UNIX SSH servers</b>:

        //  Get a directory listing:
        String cmd5 = "ls -l /tmp";

        //  Run a series of commands (syntax may depend on your default shell):
        String cmd6 = "cd /etc; ls -la";

        //  Request a directory listing on the remote server:
        //  If your server is Windows, change the string from "ls" to "dir";
        success = ssh.SendReqExec(channelNum,"ls /dev");
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
