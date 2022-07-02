package me.krypek.iweha;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class CopyThread {
	private final IWEHA iweha;

	private void log(final String str) {
		if(iweha.log && operationCount++ % iweha.reduceLog == 0)
			System.out.println(str);
	}

	private void log(final String str, final int reduceLog) {
		if(iweha.log && operationCount++ % reduceLog == 0)
			System.out.println(str);
	}

	int operationCount = 0;

	public CopyThread(final IWEHA iweha) {
		this.iweha = iweha;
		thread = new Thread(() -> {
			while (!iweha.indexThreadsDone || iweha.copyStack.hasSomething()) while (iweha.copyStack.hasSomething()) {
				final CopyOperation co = iweha.copyStack.pop();
				if(co.type == CopyType.File)
					copy(co.from, co.to);
				else if(co.type == CopyType.Symlink)
					copySymlink(co.from, co.to);
				else if(co.type == CopyType.EmptyDirectpry) {
					co.to.toFile().getParentFile().mkdirs();
					log("+ directory " + co.to);
				} else
					move(co.from, co.to);
			}
			System.out.println("Copying done.");
		});
	}

	public final Thread thread;

	private void copy(final Path from, final Path to) {
		log(from + " -> " + to);

		try {
			to.toFile().getParentFile().mkdirs();
			Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private void copySymlink(final Path from, final Path to) {
		log("(symlink) " + from + " -> " + to);

		try {
			to.toFile().getParentFile().mkdirs();
			Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private void move(final Path from, final Path to) {
		log(from + " >-> " + to, 1);
		
		try {
			to.toFile().getParentFile().mkdirs();
			Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

}
