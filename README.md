IntelliHack
===========

Plugin for IntelliJ.

Loads and executes a class from within the IntelliJ process, instead of spawning a new process.
The advantage is that plugin code can be tested without spawning a new IntelliJ process.

Example:

```java
package example;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;

import java.util.concurrent.CountDownLatch;

public class TryMe implements Runnable, AutoCloseable {

    private final ConsoleView consoleView;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    public TryMe(ConsoleView consoleView, Project project) {
        this.consoleView = consoleView;
    }

    @Override
    public void run() {
        consoleView.print("Hello in the console view\n", ConsoleViewContentType.NORMAL_OUTPUT);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        consoleView.print("Countdownlatch awaiting finished\n", ConsoleViewContentType.NORMAL_OUTPUT);
    }

    // Will be called when the Stop button is pressed.
    @Override
    public void close() throws Exception {
        countDownLatch.countDown();
    }
}
```

Implementing Runnable is mandatory. Implementing AutoCloseable is optional. Constructor arguments
are also optional.

To test this example, copy the file to an IntelliJ plugin project, add an IntelliHack runtime configuration,
and set the runnable class to example.TryMe.
