import java.util.*;
import java.io.*;
import java.nio.file.*;

public class Main {

    static File cur = new File(System.getProperty("user.dir"));

    public static void main(String[] a) throws Exception {

        Scanner s = new Scanner(System.in);

        while(true){

            System.out.print("$ ");

            List<String> p = parse(s.nextLine());

            if(p.isEmpty()) continue;

            String cmd=p.get(0);

            if(cmd.equals("exit")) break;


            String out=null;

            for(int i=0;i<p.size();i++){
                if(p.get(i).equals(">") || p.get(i).equals("1>")){
                    out=p.get(i+1);
                    p=p.subList(0,i);
                    break;
                }
            }


            if(cmd.equals("echo")){
                runOut(String.join(" ",p.subList(1,p.size())),out);
            }


            else if(cmd.equals("pwd")){
                runOut(cur.getCanonicalPath(),out);
            }


            else if(cmd.equals("cd")){

                File d=p.get(1).equals("~")?
                    new File(System.getenv("HOME")):
                    new File(p.get(1).startsWith("/")?
                    p.get(1):cur+"/"+p.get(1));

                if(d.exists())
                    cur=d.getCanonicalFile();
                else
                    System.out.println("cd: "+p.get(1)+": No such file or directory");
            }


            else if(find(cmd)!=null){

                ProcessBuilder pb=new ProcessBuilder(p);
                pb.directory(cur);

                if(out!=null)
                    pb.redirectOutput(new File(out));
                else
                    pb.inheritIO();

                pb.start().waitFor();

            }
            else
                System.out.println(cmd+": command not found");
        }
    }


    static void runOut(String x,String f)throws Exception{
        if(f!=null)
            Files.writeString(Path.of(f),x+"\n");
        else
            System.out.println(x);
    }


    static List<String> parse(String s){

        List<String> r=new ArrayList<>();
        StringBuilder w=new StringBuilder();

        boolean q=false,d=false;

        for(int i=0;i<s.length();i++){

            char c=s.charAt(i);

            if(c=='\\'&&!q){
                w.append(s.charAt(++i));
            }

            else if(c=='\''&&!d) q=!q;

            else if(c=='"'&&!q) d=!d;

            else if(c==' '&&!q&&!d){
                if(w.length()>0){
                    r.add(w.toString());
                    w.setLength(0);
                }
            }

            else w.append(c);
        }

        if(w.length()>0) r.add(w.toString());

        return r;
    }


    static String find(String c){

        for(String x:System.getenv("PATH").split(":")){

            Path p=Path.of(x,c);

            if(Files.isExecutable(p))
                return p.toString();
        }

        return null;
    }
}