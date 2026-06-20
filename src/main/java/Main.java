import java.util.*;
import java.io.*;
import java.nio.file.*;

public class Main {

    static File current = new File(System.getProperty("user.dir"));

    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);

        while (true) {

            System.out.print("$ ");

            List<String> parts = parse(sc.nextLine());

            if (parts.isEmpty()) continue;

            String cmd = parts.get(0);


            if (cmd.equals("exit")) {
                break;
            }


            else if (cmd.equals("echo")) {

                System.out.println(
                    String.join(" ", parts.subList(1, parts.size()))
                );

            }


            else if (cmd.equals("pwd")) {

                System.out.println(current.getCanonicalPath());

            }


            else if (cmd.equals("cd")) {

                if (parts.size() < 2) continue;

                String path = parts.get(1);

                File dir;


                if (path.equals("~")) {

                    dir = new File(System.getenv("HOME"));

                } 
                else if (path.startsWith("/")) {

                    dir = new File(path);

                } 
                else {

                    dir = new File(current, path);

                }


                if (dir.exists() && dir.isDirectory()) {

                    current = dir.getCanonicalFile();

                } 
                else {

                    System.out.println(
                        "cd: " + path + ": No such file or directory"
                    );

                }
            }


            else if (cmd.equals("type")) {

                String c = parts.get(1);


                if (c.equals("echo") ||
                    c.equals("exit") ||
                    c.equals("type") ||
                    c.equals("pwd") ||
                    c.equals("cd")) {


                    System.out.println(c + " is a shell builtin");


                } else {


                    String exe = find(c);


                    if (exe != null)

                        System.out.println(c + " is " + exe);

                    else

                        System.out.println(c + ": not found");
                }
            }


            else {

                if (find(cmd) != null) {


                    ProcessBuilder pb = new ProcessBuilder(parts);

                    pb.directory(current);

                    pb.inheritIO();

                    pb.start().waitFor();


                } else {

                    System.out.println(cmd + ": command not found");

                }
            }
        }

        sc.close();
    }



    static List<String> parse(String s) {


        List<String> result = new ArrayList<>();

        StringBuilder word = new StringBuilder();


        boolean single = false;

        boolean dbl = false;



        for (int i = 0; i < s.length(); i++) {


            char c = s.charAt(i);



            // Backslash outside quotes
            if (c == '\\' && !single && !dbl) {


                if (i + 1 < s.length()) {

                    word.append(s.charAt(i + 1));

                    i++;

                }

            }



            else if (c == '\'' && !dbl) {


                single = !single;


            }



            else if (c == '"' && !single) {


                dbl = !dbl;


            }



            else if (c == ' ' && !single && !dbl) {


                if (word.length() > 0) {

                    result.add(word.toString());

                    word.setLength(0);

                }


            }



            else {


                word.append(c);


            }

        }



        if (word.length() > 0)

            result.add(word.toString());



        return result;

    }




    static String find(String cmd) {


        String path = System.getenv("PATH");


        if (path == null)

            return null;



        for (String d : path.split(File.pathSeparator)) {


            Path p = Path.of(d, cmd);



            if (Files.exists(p) && Files.isExecutable(p))

                return p.toString();

        }


        return null;

    }
}