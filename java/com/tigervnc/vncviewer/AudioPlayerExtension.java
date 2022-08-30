package com.tigervnc.vncviewer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class AudioPlayerExtension {
    public static void runAudioPlayerThread(String host, int port) {
        Thread tmp = new Thread(new Runnable() {
            public void run() {
                try {
                    audioPlayer(host, port);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        });
        tmp.start();
    }

    public static void audioPlayer(String host, int port) throws Exception {
        Socket s = new Socket(host, port);
        if (!s.isConnected())
            throw new IOException("failed to connect");
        InputStream input = s.getInputStream(); // adb forward lets us connect but then blocks forever when reading if the socket is closed
        int tries = 0;
        while (input.available() < 1 && tries++ < 50) {
            Thread.sleep(10);
        }
        if (input.available() < 1)
            throw new IOException("no data");

        AudioFormat af = new AudioFormat(48000, 16, 2, true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(af, 4096);
        line.start();
        byte[] buffer;
        while (s.isConnected() && !s.isClosed() && !s.isInputShutdown()) {
            tries = 0;
            while (input.available() < 1 && tries++ < 50) {
                Thread.sleep(10);
            }
            if (input.available() < 1)
                break;
            buffer = input.readNBytes(64);
            line.write(buffer, 0, buffer.length);
        }
        line.drain();
        line.stop();
        line.close();
    }
}
