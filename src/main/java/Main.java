import java.util.*;
import java.io.*;
import java.nio.file.*;

public class Main {

    static File current = new File(System.getProperty("user.dir"));
    static int jobId = 1;
    static ArrayList<Job> jobs = new ArrayList<>();


    static class Job {
        int id;
        long pid;
        String cmd;
        Process process;

        Job(int id, long pid, String cmd, Process process) {
            this.id = id;
            this.pid = pid;
            this.cmd = cmd;
            this.process = process;
        }
    }


    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);


        while (true) {

            System.out.print("$ ");

            List<String> t = parse(sc.nextLine());

            if (t.isEmpty())
                continue;


            boolean bg = false;

            if (t.get(t.size()-1).equals("&")) {
                bg = true;
                t = t.subList(0,t.size()-1);
            }



            String out=null, err=null;

            boolean outAppend=false, errAppend=false;



            for(int i=0;i<t.size();i++){

                String x=t.get(i);


                if(x.equals(">") || x.equals("1>")){

                    out=t.get(i+1);
                    outAppend=false;
                    t=t.subList(0,i);
                    break;

                }


                if(x.equals(">>") || x.equals("1>>")){

                    out=t.get(i+1);
                    outAppend=true;
                    t=t.subList(0,i);
                    break;

                }


                if(x.equals("2>")){

                    err=t.get(i+1);
                    errAppend=false;
                    t=t.subList(0,i);
                    break;

                }


                if(x.equals("2>>")){

                    err=t.get(i+1);
                    errAppend=true;
                    t=t.subList(0,i);
                    break;

                }

            }



            String cmd=t.get(0);



            if(cmd.equals("exit"))
                break;



            else if(cmd.equals("jobs")){


                int n=jobs.size();


                // print all jobs in job number order
                for(int i=0;i<n;i++){


                    Job j=jobs.get(i);


                    String mark =
                            (i==n-1) ? "+" :
                            (i==n-2) ? "-" :
                            " ";



                    if(!j.process.isAlive()){


                        System.out.printf(
                                "[%d]%s  Done                    %s%n",
                                j.id,
                                mark,
                                j.cmd
                        );


                    }else{


                        System.out.printf(
                                "[%d]%s  Running                 %s &%n",
                                j.id,
                                mark,
                                j.cmd
                        );

                    }

                }



                // remove finished jobs
                jobs.removeIf(j -> !j.process.isAlive());


            }



            else if(cmd.equals("pwd")){


                print(
                        current.getCanonicalPath(),
                        out,
                        outAppend
                );


            }



            else if(cmd.equals("cd")){


                String p=t.get(1);

                File d;


                if(p.equals("~"))

                    d=new File(System.getenv("HOME"));

                else if(p.startsWith("/"))

                    d=new File(p);

                else

                    d=new File(current,p);



                if(d.exists() && d.isDirectory())

                    current=d.getCanonicalFile();

                else

                    System.out.println(
                            "cd: "+p+": No such file or directory"
                    );


            }



            else if(cmd.equals("echo")){


                print(
                        String.join(" ",t.subList(1,t.size())),
                        out,
                        outAppend
                );


            }



            else if(cmd.equals("type")){


                String c=t.get(1);


                if(isBuiltin(c)){

                    System.out.println(
                            c+" is a shell builtin"
                    );

                }else{


                    String f=find(c);


                    if(f!=null)

                        System.out.println(
                                c+" is "+f
                        );

                    else

                        System.out.println(
                                c+": not found"
                        );

                }


            }



            else{


                String exe=find(cmd);


                if(exe==null){

                    System.out.println(
                            cmd+": command not found"
                    );

                    continue;
                }



                ProcessBuilder pb=new ProcessBuilder(t);

                pb.directory(current);



                if(out!=null){

                    if(outAppend)

                        pb.redirectOutput(
                                ProcessBuilder.Redirect.appendTo(
                                        new File(out)));

                    else

                        pb.redirectOutput(
                                new File(out));

                }else{

                    pb.redirectOutput(
                            ProcessBuilder.Redirect.INHERIT);

                }




                if(err!=null){

                    if(errAppend)

                        pb.redirectError(
                                ProcessBuilder.Redirect.appendTo(
                                        new File(err)));

                    else

                        pb.redirectError(
                                new File(err));

                }else{

                    pb.redirectError(
                            ProcessBuilder.Redirect.INHERIT);

                }




                Process p=pb.start();



                if(bg){


                    Job j=new Job(
                            jobId++,
                            p.pid(),
                            String.join(" ",t),
                            p
                    );


                    jobs.add(j);


                    System.out.println(
                            "["+j.id+"] "+j.pid
                    );


                }else{


                    p.waitFor();


                }

            }


        }

    }






    static boolean isBuiltin(String s){

        return s.equals("echo") ||
               s.equals("exit") ||
               s.equals("type") ||
               s.equals("pwd") ||
               s.equals("cd") ||
               s.equals("jobs");

    }







    static String find(String c){

        String path=System.getenv("PATH");


        if(path==null)
            return null;



        for(String x:path.split(":")){


            File f=new File(x,c);


            if(f.exists() && f.canExecute())

                return f.getPath();

        }


        return null;

    }







    static void print(String s,String f,boolean append)throws Exception{


        if(f==null){

            System.out.println(s);
            return;

        }



        if(append)

            Files.writeString(
                    Path.of(f),
                    s+"\n",
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

        else

            Files.writeString(
                    Path.of(f),
                    s+"\n"
            );


    }







    static List<String> parse(String s){


        ArrayList<String> r=new ArrayList<>();

        StringBuilder b=new StringBuilder();

        boolean sq=false,dq=false;



        for(int i=0;i<s.length();i++){


            char c=s.charAt(i);



            if(c=='\\'){


                if(sq){

                    b.append(c);

                }

                else if(dq && i+1<s.length()
                        && (s.charAt(i+1)=='"' ||
                            s.charAt(i+1)=='\\')){


                    b.append(s.charAt(++i));

                }

                else{

                    b.append(s.charAt(++i));

                }


            }



            else if(c=='\'' && !dq)

                sq=!sq;



            else if(c=='"' && !sq)

                dq=!dq;



            else if(Character.isWhitespace(c)
                    && !sq && !dq){


                if(b.length()>0){

                    r.add(b.toString());

                    b.setLength(0);

                }


            }



            else

                b.append(c);

        }



        if(b.length()>0)

            r.add(b.toString());



        return r;

    }

}