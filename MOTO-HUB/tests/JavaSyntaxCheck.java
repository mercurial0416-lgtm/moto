import java.util.*;
import java.io.*;
import javax.tools.*;
import com.sun.source.util.JavacTask;
import com.sun.source.tree.CompilationUnitTree;
public class JavaSyntaxCheck {
 public static void main(String[] args)throws Exception {
  JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
  DiagnosticCollector<JavaFileObject> diags=new DiagnosticCollector<>();
  StandardJavaFileManager fm=compiler.getStandardFileManager(diags,null,null);
  List<String> files=new ArrayList<>();for(String file:args)files.add(file);
  Iterable<? extends JavaFileObject> units=fm.getJavaFileObjectsFromStrings(files);
  JavacTask task=(JavacTask)compiler.getTask(null,fm,diags,List.of("--source","17","-proc:none"),null,units);
  int parsed=0;for(CompilationUnitTree unit:task.parse())parsed++;
  int errors=0;for(Diagnostic<? extends JavaFileObject>d:diags.getDiagnostics())if(d.getKind()==Diagnostic.Kind.ERROR){errors++;System.err.println(d);}
  if(errors>0)throw new AssertionError(errors+" Java syntax errors");
  System.out.println("PASS: " +parsed+" Java files parse with Java 17 syntax");
 }
}
