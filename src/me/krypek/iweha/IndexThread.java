package me.krypek.iweha;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class IndexThread {

	private final IWEHA iweha;

	public final Thread thread;

	public IndexThread(final IWEHA iweha, final File[] files, final int threadNumber) {
		this.iweha = iweha;
		thread = new Thread(() -> { fileArray(files); System.out.println("Index Thread " + threadNumber + " done."); });
	}

	private void fileArray(final File[] files) {
		if(files == null)
			return;
		for (final File file : files) { file(file); }

	}

	private void file(final File file) {
		if(iweha.ignoreCache) {
			String path = file.getAbsolutePath().toLowerCase();

			if(path.contains("cache") || path.contains("temp")) {
				System.out.println("Skipping temp file: " + path);
				return;
			}
		}

		if(file.isDirectory()) {
			if(iweha.excludedSet.contains(file.getAbsolutePath()))
				return;
			// is symlink
			try {
				final String path = file.getAbsolutePath();
				if(!path.equals(file.getCanonicalPath())) { index(file, true); return; }
			} catch (final IOException e) {}

			final File[] files = file.listFiles();
			if(files == null || files.length == 0)
				index(file, false);
			else
				fileArray(files);
		} else
			index(file, false);
	}

	private void index(final File file, final boolean symlink) {
		final String path = file.getAbsolutePath();

		if(iweha.excludedSet.contains(path))
			return;
		final long time1 = file.lastModified();
		Long time2 = null;
		if(iweha.oldTimeMap != null)
			time2 = iweha.oldTimeMap.get(path);
		final boolean time2null = time2 == null;

		iweha.newTimeMap.put(path, time1);

		if(file.isFile() && (time2null || time1 > time2))
			add(path, symlink);
		else if(symlink && (time2null || time1 > time2))
			addSymlink(path);
		else if(time2null)
			addDirectory(path);

		if(iweha.removeFiles)
			iweha.removedFilesList.remove(path);
	}

	private void add(final String path, final boolean symlink) {
		if(symlink)
			addSymlink(path);
		else
			addFile(path);
	}

	private void addFile(final String path) { iweha.copyStack.add(new CopyOperation(Path.of(path), Path.of(iweha.destPath + path), CopyType.File)); }

	private void addSymlink(final String path) { iweha.copyStack.add(new CopyOperation(Path.of(path), Path.of(iweha.destPath + path), CopyType.Symlink)); }

	private void addDirectory(final String path) {
		iweha.copyStack.add(new CopyOperation(Path.of(path), Path.of(iweha.destPath + path), CopyType.EmptyDirectpry));
	}
}
