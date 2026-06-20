import java.util.Scanner;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {

    static File currentDirectory = new File(System.getProperty("user.dir"));

    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);

        while (true) {

            System.out.print("$ ");

            String input = scanner.nextLine();

            if (input.equals("exit")) {
                break;
            }

            String[] parts = input.split(" ");
            String command = parts[0];


            if (command.equals("echo")) {

                System.out.println(input.substring(5));

            }


            else if (command.equals("pwd")) {

                System.out.println(currentDirectory.getCanonicalPath());

            }


            else if (command.equals("cd")) {

                if (parts.length < 2) {
                    continue;
                }

                String target = parts[1];

                File newDirectory;


                // Handle ~
                if (target.equals("~")) {

                    String home = System.getenv("HOME");

                    newDirectory = new File(home);

                }

                // Absolute path
                else if (target.startsWith("/")) {

                    newDirectory = new File(target);

                }

                // Relative path
                else {

                    newDirectory = new File(currentDirectory, target);

                }


                if (newDirectory.exists() && newDirectory.isDirectory()) {

                    currentDirectory = newDirectory.getCanonicalFile();

                } 
                else {

                    System.out.println(
                        "cd: " + target + ": No such file or directory"
                    );

                }

            }


            else if (command.equals("type")) {

                if (parts.length < 2) {
                    continue;
                }

                String cmd = parts[1];

                if (cmd.equals("echo") ||
                    cmd.equals("exit") ||
                    cmd.equals("type") ||
                    cmd.equals("pwd") ||
                    cmd.equals("cd")) {

                    System.out.println(cmd + " is a shell builtin");

                } 
                else {

                    String executablePath = findExecutable(cmd);

                    if (executablePath != null) {

                        System.out.println(cmd + " is " + executablePath);

                    } 
                    else {

                        System.out.println(cmd + ": not found");

                    }
                }
            }


            else {

                String executablePath = findExecutable(command);


                if (executablePath != null) {

                    List<String> processCommand = new ArrayList<>();

                    processCommand.add(command);


                    for (int i = 1; i < parts.length; i++) {

                        processCommand.add(parts[i]);

                    }


                    ProcessBuilder pb = new ProcessBuilder(processCommand);

                    pb.directory(currentDirectory);

                    pb.inheritIO();


                    Process process = pb.start();

                    process.waitFor();


                } 
                else {

                    System.out.println(command + ": command not found");

                }

            }
        }

        scanner.close();
    }


    static String findExecutable(String command) {

        String pathEnv = System.getenv("PATH");


        if (pathEnv == null) {
            return null;
        }


        String[] directories = pathEnv.split(File.pathSeparator);


        for (String dir : directories) {

            Path filePath = Path.of(dir, command);


            if (Files.exists(filePath) && Files.isExecutable(filePath)) {

                return filePath.toAbsolutePath().toString();

            }
        }


        return null;
    }
}