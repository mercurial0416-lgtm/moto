import javax.tools.*;
import com.sun.source.util.JavacTask;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/** Parse all Android Java sources without requiring an installed Android SDK. */
public final class JavaSyntaxCheck {
 public static void main(String[] args)throws Exception {
  List<File> sources;
  if(args.length>0){
   sources=Arrays.stream(args).map(File::new).collect(Collectors.toList());
  }else{
   Path root=Paths.get("app/src/main/java");
   try(Stream<Path> paths=Files.walk(root)){
    sources=paths.filter(p->p.toString().endsWith(".java")).map(Path::toFile).sorted().collect(Collectors.toList());
   }
  }
  if(sources.isEmpty())throw new IllegalStateException("No Android Java source files found");
  JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
  if(compiler==null)throw new IllegalStateException("A JDK (not JRE) is required");
  DiagnosticCollector<JavaFileObject> diagnostics=new DiagnosticCollector<>();
  try(StandardJavaFileManager manager=compiler.getStandardFileManager(diagnostics,Locale.ROOT,null)){
   Iterable<? extends JavaFileObject> units=manager.getJavaFileObjectsFromFiles(sources);
   JavacTask task=(JavacTask)compiler.getTask(null,manager,diagnostics,List.of("-proc:none","-source","17"),null,units);
   task.parse(); // Syntax only: this does not resolve Android APIs or prove an APK will compile.
  }
  long errors=diagnostics.getDiagnostics().stream().filter(d->d.getKind()==Diagnostic.Kind.ERROR).count();
  for(Diagnostic<? extends JavaFileObject> d:diagnostics.getDiagnostics())if(d.getKind()==Diagnostic.Kind.ERROR)
   System.err.println(d.getSource()+":"+d.getLineNumber()+": "+d.getMessage(Locale.ROOT));
  if(errors!=0)throw new IllegalStateException("Java syntax errors: "+errors);
  System.out.println("PASS: parsed "+sources.size()+" Android Java source files (syntax only, no SDK or type resolution)");
 }
}
