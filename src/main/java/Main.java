import java.util.*;
import java.io.*;
import java.nio.file.*;

public class Main {

    static File current = new File(System.getProperty("user.dir"));

    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");

            String input = sc.nextLine();
            List<String> parts = parse(input);

            if (parts.isEmpty()) continue;

            String cmd = parts.get(0);


            if (cmd.equals("exit")) break;


            else if (cmd.equals("echo")) {
                System.out.println(String.join(" ", parts.subList(1, parts.size())));
            }


            else if (cmd.equals("pwd")) {
                System.out.println(current.getCanonicalPath());
            }


            else if (cmd.equals("cd")) {

                String path = parts.get(1);
                File dir;

                if (path.equals("~"))
                    dir = new File(System.getenv("HOME"));
                else
                    dir = new File(current, path);

                if (dir.exists() && dir.isDirectory())
                    current = dir.getCanonicalFile();
                else
                    System.out.println("cd: " + path + ": No such file or directory");
            }


            else {

                String exe = find(cmd);

                if (exe != null) {

                    ProcessBuilder pb = new ProcessBuilder(parts);
                    pb.directory(current);
                    pb.inheritIO();
                    pb.start().waitFor();

                } else {
                    System.out.println(cmd + ": command not found");
                }
            }
        }
    }


    static List<String> parse(String s) {

        List<String> res = new ArrayList<>();
        StringBuilder cur = new StringBuilder();

        boolean quote = false;

        for (char c : s.toCharArray()) {

            if (c == '\'') {
                quote = !quote;
            }

            else if (c == ' ' && !quote) {

                if (cur.length() > 0) {
                    res.add(cur.toString());
                    cur.setLength(0);
                }

            }

            else {
                cur.append(c);
            }
        }

        if (cur.length() > 0)
            res.add(cur.toString());

        return res;
    }


    static String find(String cmd) {

        String path = System.getenv("PATH");

        for (String d : path.split(File.pathSeparator)) {

            Path p = Path.of(d, cmd);

            if (Files.exists(p) && Files.isExecutable(p))
                return p.toString();
        }

        return null;
    }
}