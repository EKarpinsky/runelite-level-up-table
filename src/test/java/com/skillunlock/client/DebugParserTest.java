package com.skillunlock.client;

import org.junit.Test;

public class DebugParserTest {
    @Test
    public void testParameterRegex() {
        // Sample content that would be in the parts array after splitting by |
        String[] testParts = {
            "\nfreeplay1 =\n* Wield bronze weapons",
            "\nmembers1 =\n* Wield black weapons",
            "freeplay5 =\n* Wield steel weapons",
            " members10 =\n* Wield black weapons"
        };
        
        String paramRegex = "(?s)^\\s*(freeplay|members)(\\d+|all)\\s*=.*";
        
        for (String part : testParts) {
            boolean matches = part.matches(paramRegex);
            System.out.println("Part: [" + part.replace("\n", "\\n") + "]");
            System.out.println("Matches: " + matches);
            System.out.println("---");
        }
    }
}