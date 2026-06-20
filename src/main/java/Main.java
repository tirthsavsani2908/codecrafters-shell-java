import java.util.Scanner;
import java.io.File;

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

                // Check builtins
                if (command.equals("echo") || command.equals("exit") || command.equals("type")) {
                    System.out.println(command + " is a shell builtin");
                } 
                else {
                    boolean found = false;

                    String path = System.getenv("PATH");

                    if (path != null) {
                        String[] directories = path.split(File.pathSeparator);

                        for (String dir : directories) {
                            File file = new File(dir, command);

                            if (file.exists() && file.isExecutable()) {
                                System.out.println(command + " is " + file.getAbsolutePath());
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