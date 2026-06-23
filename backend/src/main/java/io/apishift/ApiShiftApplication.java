package io.apishift;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class ApiShiftApplication {
    public static void main(String... args) {
        Quarkus.run(args);
    }
}
