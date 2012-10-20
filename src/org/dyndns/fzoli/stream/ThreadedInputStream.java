package org.dyndns.fzoli.stream;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Allows to read data previously generated by ThreadedOutputStream. This class instance
 * works in single thread and retrieves data from interleaved stream, written by single 
 * thread. The raw source stream is divided on entries, depending on how many threads
 * were writing to the ThreadedOutputStream.
 * 
 * @author l0co@wp.pl
 */
public class ThreadedInputStream extends InputStream {
        
  protected DataInputStream source;
  protected byte entry;
        
  protected int bytesToRead = 0; // how many bytes can we read already from current data block?
        
  protected long pos = 0;
        
  /**
  * Creates the input stream.
  * 
  * @param source Newly instantiated, clean source input stream to raw data
  *      produced by ThreadedOutputStream.
  * @param entry Entry number. There can be many entries in source stream (0, 1, 2... etc,
  *      depending on count of writing threads).
  * @throws EOFException If there's no such entry yet (you need to manually close source then).
  */
  public ThreadedInputStream(InputStream source, byte entry) throws IOException {
    super();

    if (entry>Byte.MAX_VALUE)
      throw new IOException("Cannot serve for more than Byte.MAX_VALUE threads");
                
    this.source = new DataInputStream(source);
    this.entry = entry;
    lookupNextBlock();
  }
        
  protected void lookupNextBlock() throws IOException {
    while (true) {
      byte currentEntry = source.readByte();
                        
      if (currentEntry==entry) {
        // found next entry datablock
        bytesToRead = source.readInt();
        break;
      } else {
        // found next entry, but for different datablock (look for another)
        int blockSize = source.readInt();
        long toSkip = blockSize;
        while (toSkip>0) {
          long skip = source.skip(toSkip);
          if (skip<0)
            throw new EOFException("Cannot skip full datablock");
          toSkip -= skip;
        }                     
      }
    }
  }

  @Override
  public int read() throws IOException {
    if (bytesToRead<=0)
      try {
        lookupNextBlock();
      } catch (EOFException e) {
        return -1;
      }
                
    bytesToRead--;
    return source.read();
  }

  @Override
  public void close() throws IOException {
    source.close();
  }

}