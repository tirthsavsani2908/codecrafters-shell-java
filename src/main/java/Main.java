import java.util.Scanner;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");
            String input = scanner.nextLine();

            if (input.equals("exit")) {
                break;
            } 
            else if (input.startsWith("echo ")) {
                System.out.println(input.substring(5));
            } 
            else if (input.startsWith("type ")) {

                String command = input.substring(5);

                if (command.equals("echo") || command.equals("exit") || command.equals("type")) {
                    System.out.println(command + " is a shell builtin");
                } 
                else {
                    boolean found = false;

                    String path = System.getenv("PATH");

                    if (path != null) {
                        String[] directories = path.split(File.pathSeparator);

                        for (String dir : directories) {
                            Path filePath = Path.of(dir, command);

                            if (Files.exists(filePath) && Files.isExecutable(filePath)) {
                                System.out.println(command + " is " + filePath.toAbsolutePath());
                                found = true;
                                break;
                            }
                        }
                    }

                    if (!found) {
                        System.out.println(command + ": not found");
                    }
                }
            } 
            else {
                System.out.println(input + ": command not found");
            }
        }

        scanner.close();
    }
}