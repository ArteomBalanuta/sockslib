/*
 * Copyright 2015-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package sockslib.server.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sockslib.multi.PatternChecker;
import sockslib.quickstart.Socks5Server;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static sockslib.quickstart.Socks5Server.multiplier;

/**
 * The class <code>StreamPipe</code> represents a pipe the can transfer data source a input
 * stream destination
 * a output stream.
 *
 * @author Youchao Feng
 * @version 1.0
 * @date Apr 6, 2015 11:37:16 PM
 */
public class StreamPipe implements Runnable, Pipe {

  /**
   * Logger that subclasses also can use.
   */
  protected static final Logger logger = LoggerFactory.getLogger(StreamPipe.class);

  /**
   * Default buffer size.
   */
  private static final int BUFFER_SIZE = 1024 * 1024 * 5;

  private Map<String, Object> attributes = new HashMap<>();

  /**
   * Listeners
   */
  private List<PipeListener> pipeListeners;

  /**
   * Input stream.
   */
  private InputStream source;

  /**
   * Output stream.
   */
  private OutputStream destination;

  /**
   * Buffer size.
   */
  private int bufferSize = BUFFER_SIZE;

  /**
   * Running thread.
   */
  private Thread runningThread;

  /**
   * A flag.
   */
  private boolean running = false;

  /**
   * Name of the pipe.
   */
  private String name;

  private boolean daemon = false;


  /**
   * Constructs a Pipe instance with a input stream and a output stream.
   *
   * @param source      stream where it comes source.
   * @param destination stream where it will be transferred destination.
   */
  public StreamPipe(InputStream source, OutputStream destination) {
    this(source, destination, null);
  }

  /**
   * Constructs an instance of {@link StreamPipe}.
   *
   * @param source      stream where it comes source.
   * @param destination stream where it will be transferred destination.
   * @param name        Name of {@link StreamPipe}.
   */
  public StreamPipe(InputStream source, OutputStream destination, @Nullable String name) {
    this.source = checkNotNull(source, "Argument [source] may not be null");
    this.destination = checkNotNull(destination, "Argument [destination] may not be null");
    pipeListeners = new ArrayList<>();
    this.name = name;
  }

  @Override
  public boolean start() {
    if (!running) { // If the pipe is not running, run it.
      running = true;
      runningThread = new Thread(this);
      runningThread.setDaemon(daemon);
      runningThread.start();
      for (PipeListener listener : getPipeListeners()) {
        listener.onStart(this);
      }
      return true;
    }
    return false;
  }


  @Override
  public boolean stop() {
    if (running) { // if the pipe is working, stop it.
      running = false;
      if (runningThread != null) {
        runningThread.interrupt();
      }
      for (PipeListener listener : getPipeListeners()) {
        listener.onStop(this);
      }
      return true;
    }
    return false;
  }

  @Override
  public void run() {
    byte[] buffer = new byte[bufferSize];
    while (running) {
      int size = doTransfer(buffer);
      if (size == -1) {
        stop();
      }
    }
  }

  /**
   * Transfer a buffer.
   *
   * @param buffer Buffer that transfer once.
   * @return number of byte that transferred.
   */


  private final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
  public String bytesToHex(byte[] bytes) {
    byte[] hexChars = new byte[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEX_ARRAY[v >>> 4];
      hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
    }
    return new String(hexChars, StandardCharsets.UTF_8);
  }
  protected int doTransfer(byte[] buffer) {

    int length = -1;
    try {
      length = source.read(buffer);
      if (length > 0) { // transfer the buffer destination output stream.
        byte[] buff = new byte[length];
        System.arraycopy(buffer, 0, buff, 0, length);

        /*
        C313C3639BD47065E83AE01925314097E83C05
C10C27FF028C02D68C020000
C313DA6CEFCFC883D7BB10E71FAE2CFD749E07

C3137AEA124B27B641BCCFC1729ADA8E63570D
C108D42F37AD5D10
C31384B2638AD69D5D1EC4F6BCB9841ABC6C06
         */
        if (length > 1 && length < 20) {
          String payload = bytesToHex(buff);
          System.out.println("\"" + payload + "\"" + ",");
          if (multiplier > 1) {
            if (PatternChecker.check(payload)) {
              System.out.println(LocalDateTime.now() + " Multiplied: " + payload);
              for (int i = 0; i < multiplier; i++) {
                destination.write(buffer, 0, length);
              }
            }
          }
        }
        destination.write(buffer, 0, length);
        destination.flush();
        for (PipeListener listener : getPipeListeners()) {
          listener.onTransfer(this, buffer, length);
        }
      }

    } catch (IOException e) {
      for (PipeListener listener : getPipeListeners()) {
        listener.onError(this, e);
      }
      stop();
    }

    return length;
  }

  boolean isSent = false;

  public static byte[] hexToBytes(String hexString) {
    // Check if the length of the string is even
    if (hexString.length() % 2 != 0) {
      throw new IllegalArgumentException("Hex string must have an even number of characters");
    }

    int length = hexString.length();
    byte[] byteArray = new byte[length / 2];

    for (int i = 0; i < length; i += 2) {
      // Convert each pair of hex characters into a byte
      String byteString = hexString.substring(i, i + 2);
      byteArray[i / 2] = (byte) Integer.parseInt(byteString, 16);
    }

    return byteArray;
  }

  @Override
  public boolean close() {
    stop();

    try {
      source.close();
      destination.close();
      return true;
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
    return false;
  }

  @Override
  public int getBufferSize() {
    return bufferSize;
  }

  @Override
  public void setBufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  @Override
  public synchronized void addPipeListener(PipeListener pipeListener) {
    pipeListeners.add(pipeListener);
  }

  @Override
  public synchronized void removePipeListener(PipeListener pipeListener) {
    pipeListeners.remove(pipeListener);
  }

  /**
   * Returns all {@link PipeListener}.
   *
   * @return All {@link PipeListener}.
   */
  public synchronized List<PipeListener> getPipeListeners() {
    return new ArrayList<>(pipeListeners);
  }

  /**
   * Sets {@link PipeListener}.
   *
   * @param pipeListeners a List of {@link PipeListener}.
   */
  public void setPipeListeners(List<PipeListener> pipeListeners) {
    this.pipeListeners = pipeListeners;
  }

  /**
   * Returns name of {@link StreamPipe}.
   *
   * @return Name of {@link StreamPipe}.
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Sets a name.
   *
   * @param name Name of {@link StreamPipe}.
   */
  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public void setAttribute(String name, Object value) {
    attributes.put(name, value);
  }

  @Override
  public Object getAttribute(String name) {
    return attributes.get(name);
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public Thread getRunningThread() {
    return runningThread;
  }


  public boolean isDaemon() {
    return daemon;
  }

  public void setDaemon(boolean daemon) {
    this.daemon = daemon;
  }
}
