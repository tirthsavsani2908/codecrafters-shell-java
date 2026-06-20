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

            List<String> tokens = parse(sc.nextLine());

            if (tokens.isEmpty())
                continue;


            boolean background = false;


            if (tokens.get(tokens.size()-1).equals("&")) {
                background = true;
                tokens = tokens.subList(0, tokens.size()-1);
            }



            String stdout = null;
            String stderr = null;

            boolean appendOut = false;
            boolean appendErr = false;



            for(int i=0;i<tokens.size();i++){

                String t=tokens.get(i);


                if(t.equals(">") || t.equals("1>")){

                    stdout=tokens.get(i+1);
                    appendOut=false;

                    tokens=tokens.subList(0,i);
                    break;

                }


                if(t.equals(">>") || t.equals("1>>")){

                    stdout=tokens.get(i+1);
                    appendOut=true;

                    tokens=tokens.subList(0,i);
                    break;

                }


                if(t.equals("2>")){

                    stderr=tokens.get(i+1);
                    appendErr=false;

                    tokens=tokens.subList(0,i);
                    break;

                }


                if(t.equals("2>>")){

                    stderr=tokens.get(i+1);
                    appendErr=true;

                    tokens=tokens.subList(0,i);
                    break;

                }

            }



            String cmd=tokens.get(0);



            if(cmd.equals("exit"))
                break;



            else if(cmd.equals("jobs")){


                // print completed jobs once
                for(int i=0;i<jobs.size();i++){


                    Job j=jobs.get(i);


                    if(!j.process.isAlive()){


                        String mark =
                                i==jobs.size()-1 ? "+" :
                                i==jobs.size()-2 ? "-" :
                                " ";


                        System.out.printf(
                                "[%d]%s  Done                    %s%n",
                                j.id,
                                mark,
                                j.cmd
                        );


                        jobs.remove(i);
                        i--;

                    }

                }



                int n=jobs.size();


                for(int i=0;i<n;i++){


                    Job j=jobs.get(i);


                    String mark =
                            i==n-1 ? "+" :
                            i==n-2 ? "-" :
                            " ";


                    System.out.printf(
                            "[%d]%s  Running                 %s &%n",
                            j.id,
                            mark,
                            j.cmd
                    );

                }

            }



            else if(cmd.equals("pwd")){


                output(
                        current.getCanonicalPath(),
                        stdout,
                        appendOut
                );


            }



            else if(cmd.equals("cd")){


                String path=tokens.get(1);

                File dir;



                if(path.equals("~"))
                    dir=new File(System.getenv("HOME"));

                else if(path.startsWith("/"))
                    dir=new File(path);

                else
                    dir=new File(current,path);



                if(dir.exists() && dir.isDirectory())

                    current=dir.getCanonicalFile();

                else

                    System.out.println(
                            "cd: "+path+
                            ": No such file or directory"
                    );

            }



            else if(cmd.equals("echo")){


                output(
                        String.join(" ",
                                tokens.subList(1,tokens.size())),
                        stdout,
                        appendOut
                );


            }



            else if(cmd.equals("type")){


                String c=tokens.get(1);



                if(isBuiltin(c)){

                    System.out.println(
                            c+" is a shell builtin"
                    );

                }

                else{


                    String path=find(c);


                    if(path!=null)

                        System.out.println(
                                c+" is "+path
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



                ProcessBuilder pb =
                        new ProcessBuilder(tokens);



                pb.directory(current);



                if(stdout!=null){

                    if(appendOut)

                        pb.redirectOutput(
                                ProcessBuilder.Redirect.appendTo(
                                        new File(stdout)));

                    else

                        pb.redirectOutput(
                                new File(stdout));
                }



                else

                    pb.redirectOutput(
                            ProcessBuilder.Redirect.INHERIT);



                if(stderr!=null){

                    if(appendErr)

                        pb.redirectError(
                                ProcessBuilder.Redirect.appendTo(
                                        new File(stderr)));

                    else

                        pb.redirectError(
                                new File(stderr));

                }

                else

                    pb.redirectError(
                            ProcessBuilder.Redirect.INHERIT);



                Process p=pb.start();



                if(background){


                    Job j=new Job(
                            jobId++,
                            p.pid(),
                            String.join(" ",tokens),
                            p
                    );


                    jobs.add(j);



                    System.out.println(
                            "["+j.id+"] "+j.pid
                    );


                }

                else

                    p.waitFor();

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



        for(String p:path.split(":")){


            File f=new File(p,c);


            if(f.exists() && f.canExecute())

                return f.getPath();

        }


        return null;

    }







    static void output(String text,String file,boolean append)
            throws Exception{


        if(file==null){

            System.out.println(text);
            return;

        }



        if(append)

            Files.writeString(
                    Path.of(file),
                    text+"\n",
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

        else

            Files.writeString(
                    Path.of(file),
                    text+"\n"
            );

    }







    static List<String> parse(String s){


        ArrayList<String> list=new ArrayList<>();

        StringBuilder cur=new StringBuilder();


        boolean single=false;
        boolean doub=false;



        for(int i=0;i<s.length();i++){


            char c=s.charAt(i);



            if(c=='\\'){


                if(single){

                    cur.append(c);

                }


                else if(doub &&
                        i+1<s.length() &&
                        (s.charAt(i+1)=='"' ||
                         s.charAt(i+1)=='\\')){


                    cur.append(s.charAt(++i));

                }


                else{

                    cur.append(
                            s.charAt(++i)
                    );

                }


            }



            else if(c=='\'' && !doub)

                single=!single;



            else if(c=='"' && !single)

                doub=!doub;



            else if(Character.isWhitespace(c)
                    && !single
                    && !doub){


                if(cur.length()>0){

                    list.add(cur.toString());
                    cur.setLength(0);

                }

            }



            else

                cur.append(c);

        }



        if(cur.length()>0)

            list.add(cur.toString());


        return list;

    }

}