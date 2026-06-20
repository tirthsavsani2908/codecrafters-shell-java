import java.util.*;
import java.io.*;
import java.nio.file.*;

public class Main {

    static File cur=new File(System.getProperty("user.dir"));
    static int jobNo=1;
    static ArrayList<Job> jobs=new ArrayList<>();


    static class Job{

        int id;
        long pid;
        String cmd;
        Process p;

        Job(int id,long pid,String cmd,Process p){

            this.id=id;
            this.pid=pid;
            this.cmd=cmd;
            this.p=p;

        }
    }



    public static void main(String[] args)throws Exception{


        Scanner s=new Scanner(System.in);


        while(true){


            System.out.print("$ ");


            List<String> p=parse(s.nextLine());


            if(p.isEmpty()) continue;



            boolean bg=false;


            if(p.get(p.size()-1).equals("&")){

                bg=true;
                p=p.subList(0,p.size()-1);

            }



            String out=null,err=null;

            boolean ao=false,ae=false;



            for(int i=0;i<p.size();i++){


                String x=p.get(i);



                if(x.equals(">>")||x.equals("1>>")){

                    out=p.get(i+1);
                    ao=true;
                    p=p.subList(0,i);
                    break;

                }



                if(x.equals(">")||x.equals("1>")){

                    out=p.get(i+1);
                    ao=false;
                    p=p.subList(0,i);
                    break;

                }



                if(x.equals("2>>")){

                    err=p.get(i+1);
                    ae=true;
                    p=p.subList(0,i);
                    break;

                }



                if(x.equals("2>")){

                    err=p.get(i+1);
                    ae=false;
                    p=p.subList(0,i);
                    break;

                }

            }



            String cmd=p.get(0);



            if(cmd.equals("exit"))

                break;




            else if(cmd.equals("jobs")){


                int size=jobs.size();


                for(int i=0;i<size;i++){


                    Job j=jobs.get(i);



                    if(j.p.isAlive()){


                        String mark=" ";


                        if(i==size-1)

                            mark="+";


                        else if(i==size-2)

                            mark="-";



                        System.out.printf(
                            "[%d]%s  Running                 %s &%n",
                            j.id,
                            mark,
                            j.cmd
                        );

                    }

                }


            }




            else if(cmd.equals("echo")){


                if(err!=null)

                    createErr(err,ae);



                write(
                    String.join(" ",p.subList(1,p.size())),
                    out,
                    ao
                );


            }





            else if(cmd.equals("pwd")){


                write(
                    cur.getCanonicalPath(),
                    out,
                    ao
                );


            }





            else if(cmd.equals("cd")){


                String path=p.get(1);

                File d;



                if(path.equals("~"))

                    d=new File(System.getenv("HOME"));


                else if(path.startsWith("/"))

                    d=new File(path);


                else

                    d=new File(cur,path);




                if(d.exists()&&d.isDirectory())

                    cur=d.getCanonicalFile();


                else

                    System.out.println(
                        "cd: "+path+": No such file or directory"
                    );


            }





            else if(cmd.equals("type")){


                String c=p.get(1);



                if(c.equals("echo")||
                   c.equals("exit")||
                   c.equals("type")||
                   c.equals("pwd")||
                   c.equals("cd")||
                   c.equals("jobs")){


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


                if(find(cmd)!=null){


                    ProcessBuilder pb=new ProcessBuilder(p);


                    pb.directory(cur);



                    if(out!=null){


                        if(ao)

                            pb.redirectOutput(
                                ProcessBuilder.Redirect.appendTo(
                                    new File(out)
                                )
                            );


                        else

                            pb.redirectOutput(
                                new File(out)
                            );

                    }




                    if(err!=null){


                        if(ae)

                            pb.redirectError(
                                ProcessBuilder.Redirect.appendTo(
                                    new File(err)
                                )
                            );


                        else

                            pb.redirectError(
                                new File(err)
                            );

                    }





                    if(out==null)

                        pb.redirectOutput(
                            ProcessBuilder.Redirect.INHERIT
                        );



                    if(err==null)

                        pb.redirectError(
                            ProcessBuilder.Redirect.INHERIT
                        );





                    Process pr=pb.start();




                    if(bg){



                        Job j=new Job(
                            jobNo++,
                            pr.pid(),
                            String.join(" ",p),
                            pr
                        );



                        jobs.add(j);



                        System.out.println(
                            "["+j.id+"] "+j.pid
                        );


                    }

                    else

                        pr.waitFor();



                }

                else{


                    System.out.println(
                        cmd+": command not found"
                    );

                }

            }

        }

    }






    static void createErr(String f,boolean a)throws Exception{


        if(a)

            Files.writeString(
                Path.of(f),
                "",
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );

        else

            Files.writeString(
                Path.of(f),
                ""
            );

    }







    static void write(String x,String f,boolean a)throws Exception{


        if(f==null){

            System.out.println(x);

            return;
        }



        if(a)

            Files.writeString(
                Path.of(f),
                x+"\n",
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );


        else

            Files.writeString(
                Path.of(f),
                x+"\n"
            );

    }







    static List<String> parse(String s){


        List<String> r=new ArrayList<>();

        StringBuilder w=new StringBuilder();


        boolean sq=false,dq=false;



        for(int i=0;i<s.length();i++){


            char c=s.charAt(i);



            if(c=='\\'){


                if(sq)

                    w.append(c);


                else if(dq && i+1<s.length() &&
                    (s.charAt(i+1)=='"' ||
                     s.charAt(i+1)=='\\'))


                    w.append(s.charAt(++i));


                else

                    w.append(s.charAt(++i));

            }



            else if(c=='\''&&!dq)

                sq=!sq;



            else if(c=='"'&&!sq)

                dq=!dq;



            else if(c==' '&&!sq&&!dq){


                if(w.length()>0){

                    r.add(w.toString());

                    w.setLength(0);

                }

            }


            else

                w.append(c);

        }



        if(w.length()>0)

            r.add(w.toString());



        return r;

    }







    static String find(String c){


        String path=System.getenv("PATH");


        if(path==null)

            return null;



        for(String x:path.split(":")){


            File f=new File(x,c);



            if(f.exists()&&f.canExecute())

                return f.getPath();

        }



        return null;

    }

}