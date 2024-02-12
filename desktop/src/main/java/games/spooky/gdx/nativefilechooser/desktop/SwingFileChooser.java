/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-2023 Spooky Games
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package games.spooky.gdx.nativefilechooser.desktop;

import com.badlogic.gdx.files.FileHandle;
import games.spooky.gdx.nativefilechooser.*;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Pattern;

/**
 * Implementation of a {@link NativeFileChooser} for the Desktop backend of a
 * libGDX application. This implementation uses Swing {@link JFileChooser}.
 * 
 * <p>
 * A word of warning: support for the
 * {@link NativeFileChooserConfiguration#mimeFilter} property of given
 * {@link NativeFileChooserConfiguration} is experimental and slow at best. Use
 * at your own risk.
 * 
 * @see #chooseFile(NativeFileChooserConfiguration, NativeFileChooserCallback)
 * 
 * @see NativeFileChooser
 * @see NativeFileChooserConfiguration
 * @see NativeFileChooserCallback
 * 
 * @author thorthur
 * 
 */
public class SwingFileChooser implements NativeFileChooser {

	/*
	 * (non-Javadoc)
	 * 
	 * @see NativeFileChooser#chooseFile(NativeFileChooserConfiguration,
	 * NativeFileChooserCallback)
	 */
	@Override
	public void chooseFile(final NativeFileChooserConfiguration configuration, NativeFileChooserCallback callback) {
  chooseFile(configuration, callback, null);
 }

 public void chooseFile(final NativeFileChooserConfiguration configuration, NativeFileChooserCallback callback, FilenameFilter filenameFilter) {

		NativeFileChooserUtils.checkNotNull(configuration, "configuration");
		NativeFileChooserUtils.checkNotNull(callback, "callback");

		// Create Swing JFileChooser
		JFileChooser fileChooser = new JFileChooser();
		
		String title = configuration.title;
		if (title != null)
			fileChooser.setDialogTitle(title);

  FilenameFilter filter = filenameFilter == null ? createFilenameFilter(configuration) : filenameFilter;

		if (filter != null) {
			final FilenameFilter finalFilter = filter;
			fileChooser.setFileFilter(new FileFilter() {
				@Override public String getDescription() {
					return "gdx-nativefilechooser custom filter";
				}
				
				@Override
				public boolean accept(File f) {
					return f.isDirectory() || finalFilter.accept(f.getParentFile(), f.getName());
				}
			});
			fileChooser.setAcceptAllFileFilterUsed(false);
		}

		// Set starting path if any
		if (configuration.directory != null)
			fileChooser.setCurrentDirectory(configuration.directory.file());

		// Present it to the world

		int returnState = (configuration.intent == NativeFileChooserIntent.SAVE ? fileChooser.showSaveDialog(null) : fileChooser.showOpenDialog(null));
        if (returnState == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
			FileHandle result = new FileHandle(file);
			callback.onFileChosen(result);
        } else {
			callback.onCancellation();
        }
	}

 static FilenameFilter createFilenameFilter(final NativeFileChooserConfiguration configuration) {
  FilenameFilter filter = null;

  // Add MIME type filter if any
  if (configuration.mimeFilter != null)
   filter = createMimeTypeFilter(configuration.mimeFilter);

  // Add name filter if any
  if (configuration.nameFilter != null) {
   if (filter == null) {
    filter = configuration.nameFilter;
   } else {
    // Combine filters!
    final FilenameFilter mime = filter;
    filter = new FilenameFilter() {
     @Override
					public boolean accept(File dir, String name) {
      return mime.accept(dir, name) && configuration.nameFilter.accept(dir, name);
     }
    };
   }
  }
  return filter;
 }

 private static FilenameFilter createMimeTypeFilter(final String mimeType) {
  return new FilenameFilter() {

   final Pattern mimePattern = NativeFileChooserUtils.mimePattern(mimeType);

   @Override
			public boolean accept(File dir, String name) {

    // Getting a Mime type is not warranted (and may be slow!)
    try {
     String mime = Files.probeContentType(new File(dir, name).toPath());

     if (mime != null) {
      // Try to get a match on Mime type
      // That's quite faulty I know!
      return mimePattern.matcher(mime).matches();
     }

    } catch (IOException ignored) {
    }

    // Accept by default, in case mime probing doesn't work
    return true;
   }
  };
 }
}
