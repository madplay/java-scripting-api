package graalvm;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;

public class GraalVMTester {

    private void simple() {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("graal.js");

        System.out.println("engine name: " + engine.getFactory().getEngineName());

        try {
            engine.eval("print( Math.min(2, 3) )");
        } catch (ScriptException e) {
            System.err.println(e);
        }
    }

    private void simplePolyglot() {
        try (Context context = Context.create("js")) {
            context.eval("js", "print( Math.min(2, 3) )");
        } catch (Exception e) {
            System.err.println();
        }
    }

    private void callFunction() {
        try (Context context = Context.create("js")) {
            // 스크립트 파일을 읽어와서 실행시킨다.
            context.eval(Source.newBuilder("js",
                    ClassLoader.getSystemResource("sample_script.js")).build());

            // 컨텍스트의 바인딩 객체에서 "accumulator" 함수를 가져온다.
            Value accumulatorFunc = context.getBindings("js").getMember("accumulator");

            // 함수를 파라미터 1, 2을 넘겨 실행시키고 결과는 int에 매핑시킨다.
            int result = accumulatorFunc.execute(1, 2).asInt();
            System.out.println("result: " + result);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private void accessObject() {
        try (Context context = Context.create("js")) {
            context.eval(Source.newBuilder("js",
                    ClassLoader.getSystemResource("sample_script.js")).build());

            // 컨텍스트의 바인딩 객체에서 "makeContract" 함수를 가져온다.
            Value makeContractFunc = context.getBindings("js").getMember("makeContract");

            // 함수를 파라미터와 함께 실행시키고 결과를 `Value` 객체에 매핑한다.
            Value obj = makeContractFunc.execute("madplay", "010-1234-1234");

            // 반환값의 key-value 구조를 스트림을 이용해 모두 출력한다.
            obj.getMemberKeys().stream()
                    .forEach(key -> System.out.printf("%s: %s\n", key, obj.getMember(key)));
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public static void main(String[] args) {
        GraalVMTester tester = new GraalVMTester();
        tester.simple();
        tester.simplePolyglot();
        tester.callFunction();
        tester.accessObject();
    }
}
