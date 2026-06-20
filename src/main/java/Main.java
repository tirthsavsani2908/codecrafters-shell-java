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


    // -------------------------------------------------------------------------
    // Pipeline execution — supports both external commands and built-ins.
    //
    // Strategy:
    //   Each "slot" in the pipeline is either an external process (handled by
    //   ProcessBuilder) or a built-in (handled in a Java thread). We create an
    //   array of PipedOutputStream/PipedInputStream pairs to connect adjacent
    //   slots, then launch each slot concurrently so no pipe buffer fills up
    //   and deadlocks while we wait for a stage to finish.
    // -------------------------------------------------------------------------

    static void runPipeline(List<List<String>> segments,
                            String out, boolean outAppend,
                            String err, boolean errAppend,
                            boolean bg) throws Exception {

        int n = segments.size();

        // Decide the final stdout destination once.
        OutputStream finalOut;
        if (out != null) {
            finalOut = outAppend
                    ? new FileOutputStream(out, true)
                    : new FileOutputStream(out, false);
        } else {
            finalOut = System.out;
        }

        // Build inter-stage pipes: pipe[i] connects stage i's stdout to stage i+1's stdin.
        // pipe[i][0] = reader end (given to stage i+1 as stdin)
        // pipe[i][1] = writer end (given to stage i as stdout)
        PipedInputStream[]  pipeIn  = new PipedInputStream [n-1];
        PipedOutputStream[] pipeOut = new PipedOutputStream[n-1];
        for (int i = 0; i < n-1; i++) {
            pipeOut[i] = new PipedOutputStream();
            pipeIn[i]  = new PipedInputStream(pipeOut[i], 65536);
        }

        // We'll collect all threads/processes so we can wait for them.
        List<Thread>  threads   = new ArrayList<>();
        List<Process> processes = new ArrayList<>();

        for (int i = 0; i < n; i++) {

            List<String> seg  = segments.get(i);
            String       cmd0 = seg.get(0);

            // Determine this stage's stdin/stdout streams.
            final InputStream  stageIn  = (i == 0)   ? System.in      : pipeIn[i-1];
            final OutputStream stageOut = (i == n-1) ? finalOut        : pipeOut[i];
            final boolean      isLast   = (i == n-1);

            if (isBuiltin(cmd0)) {

                // ---- Built-in: run in a dedicated thread --------------------
                final List<String> fseg = seg;

                Thread th = new Thread(() -> {
                    try {
                        runBuiltinInPipeline(fseg, stageIn, stageOut, err, errAppend, isLast);
                    } catch (Exception e) {
                        // ignore
                    } finally {
                        // Close our write-end so downstream sees EOF — but never
                        // close System.out (finalOut may point to it).
                        boolean isSystemOut = (stageOut == System.out);
                        if (!isSystemOut) {
                            try { stageOut.close(); } catch (Exception ignore) {}
                        } else {
                            try { System.out.flush(); } catch (Exception ignore) {}
                        }
                        // If we consumed a pipe's read-end, close it too.
                        if (stageIn != System.in) {
                            try { stageIn.close(); } catch (Exception ignore) {}
                        }
                    }
                });

                th.setDaemon(false);
                th.start();
                threads.add(th);

            } else {

                // ---- External command: ProcessBuilder ----------------------
                String exe = find(cmd0);

                if (exe == null) {
                    System.err.println(cmd0 + ": command not found");
                    // Close all pipes to avoid deadlock and bail out.
                    for (int k = 0; k < n-1; k++) {
                        try { pipeOut[k].close(); } catch (Exception ignore) {}
                        try { pipeIn[k].close();  } catch (Exception ignore) {}
                    }
                    if (finalOut != System.out) finalOut.close();
                    return;
                }

                ProcessBuilder pb = new ProcessBuilder(seg);
                pb.directory(current);

                // stdin
                if (i == 0) {
                    pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
                } else {
                    // We'll pump manually via a thread (pipe stream → process stdin).
                    pb.redirectInput(ProcessBuilder.Redirect.PIPE);
                }

                // stdout
                if (isLast) {
                    if (out != null) {
                        if (outAppend)
                            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(out)));
                        else
                            pb.redirectOutput(new File(out));
                    } else {
                        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    }
                } else {
                    pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
                }

                // stderr
                if (isLast && err != null) {
                    if (errAppend)
                        pb.redirectError(ProcessBuilder.Redirect.appendTo(new File(err)));
                    else
                        pb.redirectError(new File(err));
                } else {
                    pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                }

                Process proc = pb.start();
                processes.add(proc);

                // If this process reads from a pipe-stream, pump it in a thread.
                if (i > 0) {
                    final InputStream  src  = stageIn;          // pipeIn[i-1]
                    final OutputStream dst  = proc.getOutputStream();
                    Thread pump = new Thread(() -> {
                        try { src.transferTo(dst); }
                        catch (Exception ignore) {}
                        finally {
                            try { dst.close(); } catch (Exception ignore2) {}
                            try { src.close(); } catch (Exception ignore2) {}
                        }
                    });
                    pump.setDaemon(true);
                    pump.start();
                    threads.add(pump);
                }

                // If this process writes to a pipe-stream, pump its stdout out.
                if (!isLast) {
                    final InputStream  src  = proc.getInputStream();
                    final OutputStream dst  = stageOut;   // pipeOut[i]
                    Thread pump = new Thread(() -> {
                        try { src.transferTo(dst); }
                        catch (Exception ignore) {}
                        finally {
                            try { dst.close(); } catch (Exception ignore2) {}
                            try { src.close(); } catch (Exception ignore2) {}
                        }
                    });
                    pump.setDaemon(true);
                    pump.start();
                    threads.add(pump);
                }
            }
        }

        // Wait for everything.
        if (bg) {
            // For background pipelines, track the last process if there is one.
            if (!processes.isEmpty()) {
                Process lastProc = processes.get(processes.size()-1);
                StringBuilder cmdStr = new StringBuilder();
                for (int i = 0; i < segments.size(); i++) {
                    if (i > 0) cmdStr.append(" | ");
                    cmdStr.append(String.join(" ", segments.get(i)));
                }
                Job j = new Job(nextJobId(), lastProc.pid(), cmdStr.toString(), lastProc);
                jobs.add(j);
                System.out.println("[" + j.id + "] " + j.pid);
            }
        } else {
            for (Process p  : processes) p.waitFor();
            for (Thread  th : threads)   th.join();
            System.out.flush();
            if (finalOut != System.out) finalOut.close();
        }
    }


    // -------------------------------------------------------------------------
    // Execute a single built-in command inside a pipeline.
    // stdin  comes from `in`  (previous stage's pipe or System.in)
    // stdout goes   to  `out` (next stage's pipe or final destination)
    // -------------------------------------------------------------------------
    static void runBuiltinInPipeline(List<String> seg,
                                     InputStream in,
                                     OutputStream out,
                                     String errFile, boolean errAppend,
                                     boolean isLast) throws Exception {

        String cmd = seg.get(0);
        // Avoid wrapping System.out in a new PrintStream (closing the wrapper
        // can close System.out and kill all subsequent output).
        PrintStream ps = (out == System.out)
                ? System.out
                : new PrintStream(out, /*autoFlush=*/true);

        switch (cmd) {

            case "echo": {
                ps.println(String.join(" ", seg.subList(1, seg.size())));
                break;
            }

            case "pwd": {
                ps.println(current.getCanonicalPath());
                break;
            }

            case "type": {
                String c = seg.get(1);
                if (isBuiltin(c)) {
                    ps.println(c + " is a shell builtin");
                } else {
                    String f = find(c);
                    if (f != null)
                        ps.println(c + " is " + f);
                    else
                        ps.println(c + ": not found");
                }
                break;
            }

            case "cd": {
                // cd in a pipeline is unusual but handle gracefully.
                String p = seg.get(1);
                File d;
                if (p.equals("~"))
                    d = new File(System.getenv("HOME"));
                else if (p.startsWith("/"))
                    d = new File(p);
                else
                    d = new File(current, p);
                if (d.exists() && d.isDirectory())
                    current = d.getCanonicalFile();
                else
                    System.err.println("cd: " + p + ": No such file or directory");
                break;
            }

            case "jobs": {
                int n = jobs.size();
                ArrayList<Job> finished = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    Job j = jobs.get(i);
                    String mark = (i==n-1) ? "+" : (i==n-2) ? "-" : " ";
                    if (isFinished(j.process)) {
                        ps.printf("[%d]%s  Done                    %s%n", j.id, mark, j.cmd);
                        finished.add(j);
                    } else {
                        ps.printf("[%d]%s  Running                 %s &%n", j.id, mark, j.cmd);
                    }
                }
                jobs.removeAll(finished);
                break;
            }

            // "exit" inside a pipeline is ignored (can't exit mid-pipeline sensibly).
            default:
                break;
        }

        ps.flush();

        // Ensure error redirect target exists if this is the last stage.
        if (isLast) ensureFile(errFile, errAppend);
    }


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


    static boolean isFinished(Process p) {
        if(!p.isAlive()) return true;
        try {
            return p.waitFor(50, TimeUnit.MILLISECONDS);
        } catch(InterruptedException e) {
            return !p.isAlive();
        }
    }


    static int nextJobId() {
        int max = 0;
        for(Job j : jobs) if(j.id > max) max = j.id;
        return max + 1;
    }


    static boolean isBuiltin(String s){
        return s.equals("echo") ||
               s.equals("exit") ||
               s.equals("type") ||
               s.equals("pwd")  ||
               s.equals("cd")   ||
               s.equals("jobs");
    }


    static String find(String c){
        String path = System.getenv("PATH");
        if(path == null) return null;
        for(String x : path.split(":")){
            File f = new File(x, c);
            if(f.exists() && f.canExecute()) return f.getPath();
        }
        return null;
    }


    static void print(String s, String f, boolean append) throws Exception {
        if(f == null){ System.out.println(s); return; }
        if(append)
            Files.writeString(Path.of(f), s+"\n",
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        else
            Files.writeString(Path.of(f), s+"\n");
    }


    static void writeToFile(String f, String content, boolean append) throws Exception {
        if(append)
            Files.writeString(Path.of(f), content,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        else
            Files.writeString(Path.of(f), content);
    }


    static void ensureFile(String f, boolean append) throws Exception {
        if(f == null) return;
        Path p = Path.of(f);
        if(append){
            if(!Files.exists(p)) Files.createFile(p);
        }else{
            Files.writeString(p, "");
        }
    }


    static void reapJobs() {
        int n = jobs.size();
        if(n == 0) return;
        StringBuilder doneOut = new StringBuilder();
        ArrayList<Job> finished = new ArrayList<>();
        for(int i = 0; i < n; i++){
            Job j = jobs.get(i);
            if(isFinished(j.process)){
                String mark = (i==n-1) ? "+" : (i==n-2) ? "-" : " ";
                doneOut.append(String.format("[%d]%s  Done                    %s%n",
                        j.id, mark, j.cmd));
                finished.add(j);
            }
        }
        jobs.removeAll(finished);
        if(doneOut.length() > 0) System.out.print(doneOut);
    }


    static List<String> parse(String s){
        ArrayList<String> r = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        boolean sq = false, dq = false;

        for(int i = 0; i < s.length(); i++){
            char c = s.charAt(i);

            if(c == '\\'){
                if(sq){
                    b.append(c);
                } else if(dq && i+1 < s.length()
                        && (s.charAt(i+1) == '"' || s.charAt(i+1) == '\\')){
                    b.append(s.charAt(++i));
                } else {
                    b.append(s.charAt(++i));
                }
            } else if(c == '\'' && !dq){
                sq = !sq;
            } else if(c == '"' && !sq){
                dq = !dq;
            } else if(Character.isWhitespace(c) && !sq && !dq){
                if(b.length() > 0){ r.add(b.toString()); b.setLength(0); }
            } else {
                b.append(c);
            }
        }

        if(b.length() > 0) r.add(b.toString());
        return r;
    }
}
