package ratpack.example.java;

import ratpack.guice.Guice;
import ratpack.handling.Handler;
import ratpack.launch.LaunchConfig;

import java.util.Map;

public class HandlerFactory implements ratpack.launch.HandlerFactory {

    @Override
    public Handler create(LaunchConfig launchConfig) throws Exception {
        // A Handler that makes objects bound to Guice by modules available downstream

        return Guice.builder(launchConfig)
                .bindings(bindingsSpec -> bindingsSpec.add(new MyModule()))
                .build(chain -> chain
                        .handler("foo", context -> context.render("from the foo handler")) // Map to /foo

                        .handler("bar", context -> context.render("from the bar handler")) // Map to /bar

                        .handler("long1", context -> context.blocking(() -> {
                            Thread.sleep(10000);
                            return "long1";
                        }).then(context::render))

                        .handler("long2", context -> context.promise(f -> new Thread() {
                            public void start() {
                                try {
                                    Thread.sleep(10000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                f.success("long2");
                            }
                        }.start()).then(context::render))

                        .prefix("nested", nested -> { // Set up a nested routing block, which is delegated to `nestedHandler`
                            nested.handler(":var1/:var2?", context -> { // The path tokens are the :var1 and :var2 path components above
                                Map<String, String> pathTokens = context.getPathTokens();
                                context.render(
                                        "from the nested handler, var1: " + pathTokens.get("var1") +
                                                ", var2: " + pathTokens.get("var2")
                                );
                            });
                        })

                        .handler("injected", chain.getRegistry().get(MyHandler.class)) // Map to a dependency injected handler

                        .prefix("static", nested -> nested.assets("assets/images")) // Bind the /static app path to the src/ratpack/assets/images dir

                        .handler(context -> context.render("root handler!")));

    }
}
