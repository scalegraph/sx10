package polyglot.util;

import java.io.*;
import java.util.Arrays;

/**
 * Output stream for writing unicode.  Non-ASCII Unicode characters
 * are escaped.
 */
public class DiffWriter extends Writer
{
  File file;
  StringBuilder wdata = null;

  public DiffWriter(File f)
  {
    this.file = f;
    this.wdata = new StringBuilder();
  }

  public void write(int c)
  {
    if( c <= 0xFF) {
      wdata.append((char)c);
    }
    else {
      String s = String.valueOf(Integer.toHexString(c));
      wdata.append('\\');
      wdata.append('u');
      for(int i = s.length(); i < 4; i++) {
        wdata.append('0');
      }
      wdata.append(s);
    }
  }
  
  public void write(char[] cbuf, int off, int len)
  {
    for( int i = 0; i < len; i++)
    {
      write((int)cbuf[i+off]);
    }
  }

  public void write(String str, int off, int len)
  {
    write(str.toCharArray(), off, len);
  }

  public void println(String str)
  {
    String str_ = str + "\n";
    write(str_.toCharArray(), 0, str_.length());
  }

  private boolean diff() throws IOException {
	  StringBuilder rdata = new StringBuilder();
    if (file.exists()) {
        FileReader r = new FileReader(file);
        int ch;
        while((ch = r.read()) != -1) {
          rdata.append((char)ch);
        }
        r.close();
      }
      return !wdata.toString().equals(rdata.toString());
  }

  public void close() throws IOException
  {
    if (!file.exists() || diff()) {
        UnicodeWriter uw = new UnicodeWriter(new FileWriter(file));
        uw.write(wdata.toString());
        uw.flush();
        uw.close();
    }
  }

  public void flush() {}
}

