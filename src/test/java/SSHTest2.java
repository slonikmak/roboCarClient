import sample.SSHCommand;

/**
 * @autor slonikmak on 20.12.2018.
 */
public class SSHTest2 {
    public static void main(String[] args) {
        SSHCommand command = new SSHCommand();
        command.sendMsg("sudo pkill java");
    }
}
