package com.galiglobal;

import org.junit.jupiter.api.Test;

import com.galiglobal.OutputParser;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {
    @Test void appHasAGreeting() {
        OutputParser classUnderTest = new OutputParser();
        assertNotNull("TODO", "app should have a greeting");
        //assertNotNull(null, "app should have a greeting");
    }
}
