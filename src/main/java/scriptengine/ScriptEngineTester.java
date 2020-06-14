package scriptengine;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author madplay
 */
public class ScriptEngineTester {

    /**
     * 스크립트 엔진 팩토리의 정보를 출력한다.
     */
    private void printAllScriptEngines() {
        ScriptEngineManager manager = new ScriptEngineManager();
        List<ScriptEngineFactory> engineFactories = manager.getEngineFactories();

        for (ScriptEngineFactory factory : engineFactories) {
            System.out.println("engine name: " + factory.getEngineName());
            System.out.println("engine version: " + factory.getEngineVersion());

            String extensions = factory.getExtensions().stream()
                    .collect(Collectors.joining(", "));
            System.out.println("extensions: " + extensions);

            System.out.println("language name: " + factory.getLanguageName());
            System.out.println("language version: " + factory.getLanguageVersion());

            String mimeTypes = factory.getMimeTypes().stream()
                    .collect(Collectors.joining(", "));
            System.out.println("mimeTypes: " + mimeTypes);

            String shortNames = factory.getNames().stream()
                    .collect(Collectors.joining(", "));
            System.out.println("shortNames: " + shortNames);

            String[] params = {
                    ScriptEngine.NAME, ScriptEngine.ENGINE,
                    ScriptEngine.ENGINE_VERSION, ScriptEngine.LANGUAGE,
                    ScriptEngine.LANGUAGE_VERSION
            };

            for (String param : params) {
                System.out.printf("parameter '%s': %s\n", param, factory.getParameter(param));
            }
            System.out.println("---------------");
        }

    }

    /**
     * 문자열에 담긴 자바스크립트 코드를 실행한다.
     */
    private void executeInlineScript() {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");

        try {
            Object result = engine.eval("Math.min(2, 3)");

            if (result instanceof Integer) {
                System.out.println(result);
            }
        } catch (ScriptException e) {
            System.err.println(e);
        }
    }

    /**
     * 바인딩을 이용하여 상태를 저장하거나 읽는다.
     */
    private void executeScriptWithVariable() {
        ScriptEngineManager manager = new ScriptEngineManager();

        ScriptEngine engine = manager.getEngineByName("JavaScript");

        try {

            engine.put("myName", "madplay");
            engine.eval("var yourName = ''; if (myName === 'madplay') yourName = 'kimtaeng'");
            System.out.println("Your name: " + engine.get("yourName"));

        } catch (ScriptException e) {
            System.err.println(e);
        }
    }

    /**
     * 외부 js파일을 이용한다.
     */
    private void executeScriptUsingExternalFile() {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");

        try {
            engine.eval(Files.newBufferedReader(Paths.get(
                    ClassLoader.getSystemResource("sample_script.js").toURI())));

            // 현재 스크립트 엔진이 Invocable을 구현하고 있는지?
            if (!(engine instanceof Invocable)) {
                System.out.println("Invocable 인터페이스 사용 불가");
                return;
            }

            // 자바스크립트의 함수를 실행하게 해주는 Invocable`
            Invocable inv = (Invocable) engine;

            // `makeContract` 자바스크립트 함수를 호출하고, 결과 반환
            Object object = inv.invokeFunction("makeContract", "madplay", "010-1234-1234");

            if (object instanceof ScriptObjectMirror) {
                // 스크립트 실행 결과는 key/value 구조인 자바스크립트 Object 이므로 Map에 매핑된다.
                // 이를 stream + foreach로 출력한다.
                ScriptObjectMirror scriptObject = (ScriptObjectMirror) object;
                scriptObject.keySet().stream()
                        .forEach(key -> {
                            String value = String.valueOf(scriptObject.getOrDefault(key, "Not Found"));
                            System.out.printf("%s: %s\n", key, value);
                        });
            }

            System.out.println("----------");

            // `invokeFunction`을 통해 가져온 결과의 멤버인 `print` 함수를 호출한다.
            Object name = inv.invokeMethod(object, "print");

            System.out.println("----------");

            // Javascript의 Number 타입 연산은 Java의 Double 타입에 매핑된다.
            Object result = inv.invokeFunction("accumulator", 1, 2);

            if (result instanceof Double) {
                System.out.println("accumulator: " + result);
            }

        } catch (Exception e) {
            System.err.println(e);
        }
    }

    /**
     * 바인딩 스코프 변경
     */
    private void executeScriptChangeBindingScope() {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");

        try {
            engine.put("myName", "madplay");
            engine.eval("var yourName = ''; " +
                    "if (myName === 'madplay') yourName = 'kimtaeng';" +
                    "else yourName = 'madplay';");
            System.out.println("Your name: " + engine.get("yourName"));
            System.out.println("----------");

            // 현재 엔진의 바인딩 객체를 가져온다.
            Bindings oldBindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);

            // 새로운 바인딩 객체를 생성하고 새로운 상태를 저장한다.
            Bindings newBindings = engine.createBindings();
            newBindings.put("myName", "kimtaeng");

            // 새로운 바인딩 객체를 현재 바인딩으로 설정한다.
            engine.setBindings(newBindings, ScriptContext.ENGINE_SCOPE);

            engine.eval("var yourName = ''; " +
                    "if (myName === 'madplay') yourName = 'kimtaeng';" +
                    "else yourName = 'madplay';");
            System.out.println("Your name: " + engine.get("yourName"));
        } catch (ScriptException e) {
            System.err.println(e);
        }
    }

    /**
     * 바인딩 객체를 직접 지정한다.
     */
    private void executeScriptWithoutChangeBindingScope() {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");

        // 새로운 바인딩 객체를 생성하고 새로운 상태를 저장한다.
        Bindings newBindings = engine.createBindings();
        newBindings.put("myName", "madplay");

        try {
            // `eval` 메서드를 호출할 때 파라미터로 바인딩 객체를 넣어준다.
            engine.eval("var yourName = ''; " +
                    "if (myName === 'madplay') yourName = 'kimtaeng';" +
                    "else yourName = 'madplay';", newBindings);

            // 새로운 바인딩 객체에서 값을 가져온다.
            System.out.println("Your name: " + newBindings.get("yourName"));

            // 엔진에 설정된 기본 바인딩 객체에서 가져온다.
            System.out.println("Your name(engine): " + engine.get("yourName"));
        } catch (ScriptException e) {
            System.err.println(e);
        }
    }

    /**
     * 스크립트 엔진의 출력을 변경한다.
     */
    private void executeScriptWithChangeWriter() {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");
        ScriptContext context = engine.getContext();

        // try-with-resources 사용
        try (StringWriter writer = new StringWriter()) {

            // wrtier를 지정하는 코드의 주석 여부에 따라 결과가 다르다.
            context.setWriter(writer);

            engine.eval("print ('hello! madplay :) ');");
            StringBuffer buffer = writer.getBuffer();

            System.out.println("StringBuffer: " + buffer.toString());
        } catch (ScriptException | IOException e) {
            System.err.println(e);
        }
    }

    /**
     * 더 빠르게 실행시키기
     */
    public void executeScriptMoreFaster() {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");
        final int MAX_LOOP_COUNT = 100_000;

        try {
            // 스크립트 파일 읽기
            String script = Files.readAllLines(Paths.get(
                    ClassLoader.getSystemResource("sample_script.js").toURI())
            ).stream().collect(Collectors.joining("\n"));

            // 기존 방식으로 실행
            long start = System.nanoTime();
            for (int i = 0; i < MAX_LOOP_COUNT; i++) {
                engine.eval(script);
            }
            long end = System.nanoTime();
            System.out.printf("script: %d ms\n", TimeUnit.MILLISECONDS.convert(
                    end - start, TimeUnit.NANOSECONDS));


            // `Compilable` 인터페이스 참조가 가능한지 먼저 확인이 필요하다.
            if (!(engine instanceof Compilable)) {
                System.err.println("Compilable 인터페이스 사용 불가");
            }
            Compilable compilable = (Compilable) engine;
            CompiledScript compiledScript = compilable.compile(script);

            // `CompiledScript` 사용
            start = System.nanoTime();
            for (int i = 0; i < MAX_LOOP_COUNT; i++) {
                compiledScript.eval();
            }
            end = System.nanoTime();
            System.out.printf("compiled script: %d ms\n", TimeUnit.MILLISECONDS.convert(
                    end - start, TimeUnit.NANOSECONDS));


        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public static void main(String[] args) {
        ScriptEngineTester executor = new ScriptEngineTester();
        executor.printAllScriptEngines();
        executor.executeInlineScript();
        executor.executeScriptWithVariable();
        executor.executeScriptUsingExternalFile();
        executor.executeScriptChangeBindingScope();
        executor.executeScriptWithoutChangeBindingScope();
        executor.executeScriptWithChangeWriter();
        executor.executeScriptMoreFaster();
    }
}
