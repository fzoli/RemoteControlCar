package org.dyndns.fzoli.stream;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Wraps target output stream and allows to write to it from multiple threads, handling
 * underlying data. The data in target output stream is interleaved. It can be then
 * read then by ThreadedInputStream.
 * 
 * The stream uses internal buffering and synchronization. The default buffer size is 512KB.
 * This means, than for each thread there'll be allocated 512KB buffer, when thread starts
 * to write into the stream. The buffer is flushed into the target output stream periodically.
 * 
 * Maximum number of threads: Byte.MAX_VALUE
 * 
 * The lifecycle is following:
 * <ol>
 * <li>Create new stream in a main thread
 * <li>For each worker thread:
 *   <ul>
 *   <li>use the stream in general way
 *   <li>on the end of processing you need to call close(); the stream will not be really closed,
 *       but will be flushed and thread buffers will be removed
 *   </ul>
 * <li>Call close() in main thread to close stream permanently (as well as the target stream)
 * </ol> 
 * 
 * @author l0co@wp.pl
 */
public class ThreadedOutputStream extends OutputStream {
        
  protected DataOutputStream target;
  protected int bufSize = 512*1024; // default buffer size = 512 KB
  protected volatile byte threadsCount = 0;
  protected Thread creatorThread;
        
  /** Internal thread data holder and buffer **/
  protected class ThreadStreamHolder {
    byte index = 0;
    int size = 0;
    byte[] buffer = new byte[bufSize];
                
    public ThreadStreamHolder(byte index) {
      super();
      this.index = index;
    }
                
    /** Flush data to the target stream **/
    public void flush() throws IOException {
      if (size>0) {
        synchronized (target) {
          target.writeByte(index); // write thread index
          target.writeInt(size); // write block size
          target.write(buffer, 0, size); // write data
          size = 0;
        }
      }
    }
                
    public void write(int b) throws IOException {
      buffer[size++] = (byte) b;
      if (size>=bufSize)
        flush();
      }
    }
        
    protected ThreadLocal<ThreadStreamHolder> threads = 
     new ThreadLocal<ThreadedOutputStream.ThreadStreamHolder>(); 
        
  /**
  * Creates stream using default buffer size (512 KB).
  * @param target Target output stream where data will be really written.
  */
  public ThreadedOutputStream(OutputStream target) {
    super();
    this.target = new DataOutputStream(target);
    creatorThread = Thread.currentThread();
  }

  /**
  * Creates stream using custom buffer size value.
  * @param target Target output stream where data will be really written.
  * @param bufSize Buffer size in bytes.
  */
  public ThreadedOutputStream(OutputStream target, int bufSize) {
    this(target);
    this.bufSize = bufSize;
  }

  @Override
  public void write(int b) throws IOException {
    ThreadStreamHolder sh = threads.get();
    if (sh==null) {
      synchronized (this) { // to avoid more threads with the same threads count
        if (threadsCount==Byte.MAX_VALUE)
          throw new IOException("Cannot serve for more than Byte.MAX_VALUE threads");
        sh = new ThreadStreamHolder(threadsCount++); // passing threadsCount and ++ is not atomic !
        threads.set(sh);
      }
    }
                
    sh.write(b);
  }

  @Override
  public void flush() throws IOException {
    super.flush();
                
    // flush the buffers on the end
    ThreadStreamHolder sh = threads.get();
    if (sh!=null)
      sh.flush();
  }

  @Override
  public void close() throws IOException {
    flush();
                
    threads.remove();
                
    // in main thread, close stream
    if (Thread.currentThread().equals(creatorThread))
      target.close();
  }

}