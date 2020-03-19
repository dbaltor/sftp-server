/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package sftp;

import org.junit.Test;
import static org.junit.Assert.*;

public class ServerTest {
    @Test public void testServerMethod() {
        Server serverUnderTest = new Server
            .Builder()
            .setPort(2222)
            .setFolder(".")
            .build();
        assertTrue("testServerMethod should return 'true'", serverUnderTest.testServerMethod());
    }
}
