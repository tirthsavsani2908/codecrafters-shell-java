import java.util.Scanner;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");
            String input = scanner.nextLine();

            String[] parts = input.split(" ");
            String command = parts[0];

            if (command.equals("exit")) {
                break;
            } 
            else if (command.equals("echo")) {
                System.out.println(input.substring(5));
            } 
            else if (command.equals("type")) {

                String cmd = parts[1];

                if (cmd.equals("echo") || cmd.equals("exit") || cmd.equals("type")) {
                    System.out.println(cmd + " is a shell builtin");
                } 
                else {
                    String path = findExecutable(cmd);

                    if (path != null) {
                        System.out.println(cmd + " is " + path);
                    } else {
                        System.out.println(cmd + ": not found");
                    }
                }
            } 
            else {

                String executable = findExecutable(command);

                if (executable != null) {

                    ArrayList<String> commandArgs = new ArrayList<>();

                    commandArgs.add(executable);

                    for (int i = 1; i < parts.length; i++) {
                        commandArgs.add(parts[i]);
                    }

                    ProcessBuilder pb = new ProcessBuilder(commandArgs);
                    pb.inheritIO();

                    Process process = pb.start();
                    process.waitFor();

                } else {
                    System.out.println(command + ": command not found");
                }
            }
        }

        scanner.close();
    }


    static String findExecutable(String command) {

        String path = System.getenv("PATH");

        if (path != null) {

            String[] directories = path.split(File.pathSeparator);

            for (String dir : directories) {

                Path file = Path.of(dir, command);

                if (Files.exists(file) && Files.isExecutable(file)) {
                    return file.toAbsolutePath().toString();
                }
            }
        }

        return null;
    }
}