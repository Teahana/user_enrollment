package group7.enrollmentSystem.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Component
public class ConsoleRedirector {

    private static final String LOG_DIR = "logs";
    private static final String CONSOLE_LOG_FILE = LOG_DIR + "/consoleOutput.txt";

    @PostConstruct
    public void redirectConsoleOutput() {
        try {
            Files.createDirectories(Paths.get(LOG_DIR));

            FileOutputStream fos = new FileOutputStream(CONSOLE_LOG_FILE, true);
            PrintStream fileStream = new PrintStream(fos, true);

            // Original terminal output streams
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;

            // Tee both file and original terminal
            System.setOut(new PrintStream(new TeeOutputStream(originalOut, fileStream), true));
            System.setErr(new PrintStream(new TeeOutputStream(originalErr, fileStream), true));

            System.out.println("[" + LocalDateTime.now() + "] Console output redirected to file AND terminal.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TeeOutputStream sends output to both original and file
    private static class TeeOutputStream extends OutputStream {
        private final OutputStream out1;
        private final OutputStream out2;

        public TeeOutputStream(OutputStream out1, OutputStream out2) {
            this.out1 = out1;
            this.out2 = out2;
        }

        @Override
        public void write(int b) throws IOException {
            out1.write(b);
            out2.write(b);
        }

        @Override
        public void flush() throws IOException {
            out1.flush();
            out2.flush();
        }

        @Override
        public void close() throws IOException {
            out1.close();
            out2.close();
        }
    }
}
