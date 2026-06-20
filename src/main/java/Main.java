import java.util.*;
import java.io.*;
import java.nio.file.*;

public class Main {

    static File current = new File(System.getProperty("user.dir"));

    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);

        while (true) {

            System.out.print("$ ");

            List<String> p = parse(sc.nextLine());

            if (p.isEmpty()) continue;

            String cmd = p.get(0);


            if (cmd.equals("exit")) break;


            else if (cmd.equals("echo")) {
                System.out.println(String.join(" ", p.subList(1, p.size())));
            }


            else if (cmd.equals("pwd")) {
                System.out.println(current.getCanonicalPath());
            }


            else if (cmd.equals("cd")) {

                String path = p.get(1);

                File d;

                if (path.equals("~"))
                    d = new File(System.getenv("HOME"));
                else if (path.startsWith("/"))
                    d = new File(path);
                else
                    d = new File(current, path);


                if (d.exists() && d.isDirectory())
                    current = d.getCanonicalFile();
                else
                    System.out.println("cd: " + path + ": No such file or directory");
            }


            else if (cmd.equals("type")) {

                String c = p.get(1);

                if (c.equals("echo") || c.equals("exit") ||
                    c.equals("type") || c.equals("pwd") ||
                    c.equals("cd")) {

                    System.out.println(c + " is a shell builtin");

                } else {

                    String e = find(c);

                    if (e != null)
                        System.out.println(c + " is " + e);
                    else
                        System.out.println(c + ": not found");
                }
            }


            else {

                if (find(cmd) != null) {

                    ProcessBuilder pb = new ProcessBuilder(p);
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

        List<String> r = new ArrayList<>();

        StringBuilder w = new StringBuilder();

        boolean sq = false, dq = false;


        for (int i = 0; i < s.length(); i++) {

            char c = s.charAt(i);


            if (c == '\\') {


                if (sq) {

                    w.append(c);

                }


                else if (dq) {


                    if (i + 1 < s.length() &&
                       (s.charAt(i + 1) == '"' ||
                        s.charAt(i + 1) == '\\')) {

                        w.append(s.charAt(++i));

                    } else {

                        w.append('\\');

                    }

                }


                else {

                    w.append(s.charAt(++i));

                }

            }


            else if (c == '\'' && !dq) {

                sq = !sq;

            }


            else if (c == '"' && !sq) {

                dq = !dq;

            }


            else if (c == ' ' && !sq && !dq) {


                if (w.length() > 0) {

                    r.add(w.toString());
                    w.setLength(0);

                }

            }


            else {

                w.append(c);

            }
        }


        if (w.length() > 0)
            r.add(w.toString());


        return r;
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