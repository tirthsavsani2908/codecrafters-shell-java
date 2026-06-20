import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

public class Main {

    static File current = new File(System.getProperty("user.dir"));
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

            // Automatic reaping: check for completed background jobs and
            // print their "Done" line before showing the next prompt.
            reapJobs();

            System.out.print("$ ");

            List<String> t = parse(sc.nextLine());

            if (t.isEmpty())
                continue;


            boolean bg = false;

            if (t.get(t.size()-1).equals("&")) {
                bg = true;
                t = t.subList(0,t.size()-1);
            }



            // Pipeline support: if the command line contains one or more
            // "|" tokens, split into segments (one per command) and wire
            // each segment's stdout to the next segment's stdin via
            // ProcessBuilder.startPipeline. Only the final segment's
            // redirection (>, >>, 2>, 2>>) is honored, matching how a
            // real shell applies redirection to the last stage of a pipeline.
            List<List<String>> pipelineSegments = splitByPipe(t);

            if(pipelineSegments.size() > 1){

                List<String> lastSeg = new ArrayList<>(pipelineSegments.get(pipelineSegments.size()-1));

                String pOut=null, pErr=null;

                boolean pOutAppend=false, pErrAppend=false;

                for(int i=0;i<lastSeg.size();i++){

                    String x=lastSeg.get(i);

                    if(x.equals(">") || x.equals("1>")){
                        pOut=lastSeg.get(i+1);
                        pOutAppend=false;
                        lastSeg=new ArrayList<>(lastSeg.subList(0,i));
                        break;
                    }

                    if(x.equals(">>") || x.equals("1>>")){
                        pOut=lastSeg.get(i+1);
                        pOutAppend=true;
                        lastSeg=new ArrayList<>(lastSeg.subList(0,i));
                        break;
                    }

                    if(x.equals("2>")){
                        pErr=lastSeg.get(i+1);
                        pErrAppend=false;
                        lastSeg=new ArrayList<>(lastSeg.subList(0,i));
                        break;
                    }

                    if(x.equals("2>>")){
                        pErr=lastSeg.get(i+1);
                        pErrAppend=true;
                        lastSeg=new ArrayList<>(lastSeg.subList(0,i));
                        break;
                    }

                }

                pipelineSegments.set(pipelineSegments.size()-1, lastSeg);

                runPipeline(pipelineSegments, pOut, pOutAppend, pErr, pErrAppend, bg);

                continue;

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


                // Build the full listing in one pass: each job (running or
                // just-finished) is shown inline, in job-table order, with
                // markers computed against the current full list. Finished
                // jobs are then removed so they aren't reported again by
                // either this command or the automatic pre-prompt reaping.
                int n=jobs.size();


                StringBuilder jobsOut = new StringBuilder();

                ArrayList<Job> finished = new ArrayList<>();

                for(int i=0;i<n;i++){


                    Job j=jobs.get(i);


                    String mark =
                            (i==n-1) ? "+" :
                            (i==n-2) ? "-" :
                            " ";


                    if(isFinished(j.process)){

                        jobsOut.append(String.format(
                                "[%d]%s  Done                    %s%n",
                                j.id,
                                mark,
                                j.cmd
                        ));

                        finished.add(j);

                    }else{

                        jobsOut.append(String.format(
                                "[%d]%s  Running                 %s &%n",
                                j.id,
                                mark,
                                j.cmd
                        ));

                    }

                }


                jobs.removeAll(finished);


                if(out!=null){

                    writeToFile(out, jobsOut.toString(), outAppend);

                }else{

                    System.out.print(jobsOut);

                }


                ensureFile(err, errAppend);


            }



            else if(cmd.equals("pwd")){


                print(
                        current.getCanonicalPath(),
                        out,
                        outAppend
                );

                ensureFile(err, errAppend);


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

                ensureFile(out, outAppend);

                ensureFile(err, errAppend);


            }



            else if(cmd.equals("echo")){


                print(
                        String.join(" ",t.subList(1,t.size())),
                        out,
                        outAppend
                );

                ensureFile(err, errAppend);


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

                ensureFile(out, outAppend);

                ensureFile(err, errAppend);


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
                            nextJobId(),
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






    // Splits a token list into pipeline segments on the "|" token.
    // ["cat","file","|","wc"] -> [["cat","file"], ["wc"]]
    static List<List<String>> splitByPipe(List<String> t) {

        List<List<String>> segs = new ArrayList<>();

        List<String> cur = new ArrayList<>();

        for(String x : t){

            if(x.equals("|")){

                segs.add(cur);
                cur = new ArrayList<>();

            }else{

                cur.add(x);

            }

        }

        segs.add(cur);

        return segs;

    }



    // Runs a pipeline of external commands, connecting each command's
    // stdout to the next command's stdin via ProcessBuilder.startPipeline.
    // Redirection (out/err) is only applied to the final command, matching
    // shell semantics. If any command in the pipeline isn't found, prints
    // the usual "command not found" message and aborts the whole pipeline.
    static void runPipeline(List<List<String>> segments, String out, boolean outAppend, String err, boolean errAppend, boolean bg) throws Exception {

        List<ProcessBuilder> pbs = new ArrayList<>();

        for(List<String> seg : segments){

            String c = seg.get(0);

            String exe = find(c);

            if(exe == null){

                System.out.println(c + ": command not found");
                return;

            }

            ProcessBuilder pb = new ProcessBuilder(seg);
            pb.directory(current);

            pbs.add(pb);

        }

        ProcessBuilder last = pbs.get(pbs.size()-1);

        if(out != null){

            if(outAppend)
                last.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(out)));
            else
                last.redirectOutput(new File(out));

        }else{

            last.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        }

        for(ProcessBuilder pb : pbs){

            if(pb == last && err != null){

                if(errAppend)
                    pb.redirectError(ProcessBuilder.Redirect.appendTo(new File(err)));
                else
                    pb.redirectError(new File(err));

            }else{

                pb.redirectError(ProcessBuilder.Redirect.INHERIT);

            }

        }

        List<Process> procs = ProcessBuilder.startPipeline(pbs);

        Process lastProc = procs.get(procs.size()-1);

        if(bg){

            StringBuilder cmdStr = new StringBuilder();

            for(int i=0;i<segments.size();i++){
                if(i>0) cmdStr.append(" | ");
                cmdStr.append(String.join(" ", segments.get(i)));
            }

            Job j = new Job(nextJobId(), lastProc.pid(), cmdStr.toString(), lastProc);

            jobs.add(j);

            System.out.println("[" + j.id + "] " + j.pid);

        }else{

            for(Process p : procs)
                p.waitFor();

        }

    }



    // Checks whether a job's process has completed, giving it a brief grace
    // period to register its exit if it's in the middle of exiting. Plain
    // isAlive() can return true for a process that has just received EOF
    // (e.g. a `cat` reading a FIFO that just got closed) but hasn't been
    // reaped by the OS yet. waitFor(timeout) lets the JVM briefly block for
    // that exit to land instead of missing it due to a tight timing race.
    static boolean isFinished(Process p) {

        if(!p.isAlive())
            return true;

        try{

            return p.waitFor(50, TimeUnit.MILLISECONDS);

        }catch(InterruptedException e){

            return !p.isAlive();

        }

    }



    // Computes the job number for a newly started background job. Job
    // numbers are recycled rather than growing forever: if the table is
    // empty the next job starts at [1]; otherwise it's one more than the
    // highest job number currently in the table.
    static int nextJobId() {

        int max = 0;

        for(Job j : jobs)
            if(j.id > max)
                max = j.id;

        return max + 1;

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



    // Writes arbitrary content to a file, creating/truncating or appending as needed.
    static void writeToFile(String f, String content, boolean append) throws Exception {

        if(append)

            Files.writeString(
                    Path.of(f),
                    content,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

        else

            Files.writeString(
                    Path.of(f),
                    content
            );

    }



    // Ensures a redirect target exists (created/truncated) even when the
    // builtin command never actually wrote anything to that stream.
    // Real shells open/truncate (or create, for append mode) the redirect
    // target as part of setting up the redirection, regardless of whether
    // the command produces output on that stream.
    static void ensureFile(String f, boolean append) throws Exception {

        if(f==null)
            return;

        Path p = Path.of(f);

        if(append){

            if(!Files.exists(p))
                Files.createFile(p);

        }else{

            Files.writeString(p, "");

        }

    }



    // Shared reaping logic: checks all background jobs for completion,
    // prints a "Done" line (with marker recalculated against the current
    // job list) for each one that has exited, and removes those jobs from
    // the table. Called both automatically before every prompt and at the
    // start of the `jobs` builtin, so a completed job is reported exactly
    // once, whichever happens first.
    static void reapJobs() {

        int n = jobs.size();

        if(n == 0)
            return;

        StringBuilder doneOut = new StringBuilder();

        ArrayList<Job> finished = new ArrayList<>();

        for(int i=0;i<n;i++){

            Job j = jobs.get(i);

            if(isFinished(j.process)){

                String mark =
                        (i==n-1) ? "+" :
                        (i==n-2) ? "-" :
                        " ";

                doneOut.append(String.format(
                        "[%d]%s  Done                    %s%n",
                        j.id,
                        mark,
                        j.cmd
                ));

                finished.add(j);

            }

        }

        jobs.removeAll(finished);

        if(doneOut.length() > 0)

            System.out.print(doneOut);

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
