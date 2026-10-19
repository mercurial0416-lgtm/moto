import javax.tools.*;
import com.sun.source.util.*;
import java.util.*;
import java.nio.file.*;
import java.io.*;
public final class ParseAllJava {
 public static void main(String[] args)throws Exception{
  JavaCompiler c=ToolProvider.getSystemJavaCompiler();
  List<File> files=new ArrayList<>();
  try(var p=Files.walk(Path.of("app/src/main/java"))){p.filter(x->x.toString().endsWith(".java")).forEach(x->files.add(x.toFile()));}
  DiagnosticCollector<JavaFileObject> diag=new DiagnosticCollector<>();
  try(StandardJavaFileManager manager=c.getStandardFileManager(diag,null,null)){
   Iterable<? extends JavaFileObject> units=manager.getJavaFileObjectsFromFiles(files);
   JavacTask task=(JavacTask)c.getTask(null,manager,diag,List.of("-proc:none"),null,units);
   task.parse();
   if(diag.getDiagnostics().stream().anyMatch(d->d.getKind()==Diagnostic.Kind.ERROR)){
    diag.getDiagnostics().forEach(System.err::println);
    System.exit(1);
   }
   System.out.println("PASS: parsed "+files.size()+" Java source files (syntax only)");
  }
 }
}
