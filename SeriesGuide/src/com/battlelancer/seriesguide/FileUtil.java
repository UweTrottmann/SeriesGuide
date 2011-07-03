package com.battlelancer.seriesguide;

import com.battlelancer.seriesguide.beta.R;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public final class FileUtil {

   private FileUtil() {
   }

   public static void copyFile(File src, File dst) throws IOException {
      FileChannel inChannel = new FileInputStream(src).getChannel();
      FileChannel outChannel = new FileOutputStream(dst).getChannel();
      try {
         inChannel.transferTo(0, inChannel.size(), outChannel);
      } finally {
         if (inChannel != null) {
            inChannel.close();
         }
         if (outChannel != null) {
            outChannel.close();
         }
      }
   }
}

