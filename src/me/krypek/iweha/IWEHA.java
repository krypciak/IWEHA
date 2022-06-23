package me.krypek.iweha;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import me.krypek.freeargparser.ArgType;
import me.krypek.freeargparser.ParsedData;
import me.krypek.freeargparser.ParserBuilder;

public class IWEHA {

	public static void main(final String[] args) throws IOException {
		System.out.println(Arrays.toString(args));
		//@f:off
		//me.krypek.freeargparser
		final ParsedData data = new ParserBuilder()
				.add("dp", 	"datpath", 		true,	false, 	ArgType.String, 		"Location of the .dat file")
				.add("de", 	"destination", 	true,	false, 	ArgType.String,			"Directory location where files will be copied to")
				.add("tb", 	"toBackup", 	true,	false,	ArgType.StringArray,	"Array of file paths to backup. Example: [\"/home/krypek/\"]")
				.add("e", 	"excluded", 	false,	false, 	ArgType.StringArray,	"Array of file paths to exclude from backup. Example: [\"/home/krypek/verybigfile.mp4\"]")
				.add("i",	"ignoreCache", 	false,	false, 	ArgType.None, 			"If selected, files conatining \"cache\" will not get copied and indexed. Example: \"/home/krypek/.cache\" will be skipped")
				.add("l", 	"doLog", 		false,	false, 	ArgType.None, 			"If selected, will print all copied files to console. Not recommended for big directories or slow terminals")
				.add("rl", 	"reduceLog", 	false,	false, 	ArgType.Int,			"Works only if doLog is selected. Insted of printing every copy log, print only every X times.")
				.add("rf",	"removeFiles", 	false,	false, 	ArgType.None, 			"If selected, moves files that were indexed previously and don't exist now to \"/dest"+deletedFilesDirectoryPrefix+"path/to/file/deleted\"")
				.parse(args);
		//@f:on

		final String datPath = data.getString("dp");
		final String dest = data.getString("de");
		final String[] toBackup = data.getStringArray("tb");
		final String[] excludedArray = data.getStringArrayOrDef("e", new String[] {});
		final boolean ignoreCache = data.has("i");
		final boolean doLog = data.has("l");
		final int reduceLog = data.getIntOrDef("rl", 1);
		boolean removeFiles = data.has("rf");

		final long time1 = System.nanoTime();

		final File datFile = new File(datPath);
		HashMap<String, Long> oldTimeMap;
		if(datFile.exists()) {
			System.out.println("Deserializing .dat ...");
			final IWEHA_DATA iweha_data = deserialize(datPath);
			System.out.println("Done.");
			oldTimeMap = iweha_data.oldTimeMap;
		} else {
			System.out.println(".dat file doesn't exist.");
			oldTimeMap = null;
			removeFiles = false;
		}

		final IWEHA iweha = new IWEHA(oldTimeMap, dest);
		System.out.println("Starting backup...");

		final Set<String> excluded = new HashSet<>();
		for (final String path : excludedArray)
			excluded.add(new File(path).getAbsolutePath());

		iweha.backup(toBackup, excluded, datPath, doLog, ignoreCache, reduceLog, removeFiles);

		if(!datPath.equals("")) {
			final IWEHA_DATA iweha_data = new IWEHA_DATA(iweha.newTimeMap);
			serialize(datPath, iweha_data);
		}

		final DecimalFormat df = new DecimalFormat("#,###.##");

		final double seconds = (System.nanoTime() - time1) / 1000000000d;
		final double minutes = seconds / 60;
		final double hours = minutes / 60;
		System.out.println("Backup completed in " + df.format(seconds) + "s, " + df.format(minutes) + "m, " + df.format(hours) + "h");
	}

	private static void serialize(final String path, final IWEHA_DATA iweha_data) {
		try {
			final FileOutputStream file = new FileOutputStream(path);
			final ObjectOutputStream out = new ObjectOutputStream(file);

			out.writeObject(iweha_data);
			out.close();
			file.close();
		} catch (final Exception e) {
			System.out.println("\n/\n/\n/\n");
			e.printStackTrace();
			System.out.println("Failed to serialize .dat file.");
		}
	}

	private static IWEHA_DATA deserialize(final String path) {
		try {
			final FileInputStream file = new FileInputStream(path);
			final ObjectInputStream in = new ObjectInputStream(file);

			final IWEHA_DATA iweha_data = (IWEHA_DATA) in.readObject();

			in.close();
			file.close();
			return iweha_data;
		} catch (final Exception e) {
			System.out.println("\n/\n/\n/\n");
			e.printStackTrace();

			System.out.println("Failed to deserialize .dat file.");
			throw new IllegalArgumentException(e);
		}
	}

	final static String deletedFilesDirectoryPrefix = "/IWEHA_DELETED/";

	HashMap<String, Long> oldTimeMap;
	public HashMap<String, Long> newTimeMap;
	String destPath;
	Set<String> excludedSet;

	Set<String> removedFilesList;

	boolean log;
	public CopyStack copyStack;
	public boolean indexThreadsDone;
	int reduceLog;
	boolean ignoreCache;
	boolean removeFiles;

	public IWEHA(final HashMap<String, Long> oldTimeMap, final String destPath) {
		indexThreadsDone = false;
		this.oldTimeMap = oldTimeMap;
		if(oldTimeMap != null)
			removedFilesList = oldTimeMap.keySet();
		newTimeMap = new HashMap<>(5000);
		final File destFile = new File(destPath);
		destFile.mkdirs();
		this.destPath = destFile.getAbsolutePath();
	}

	public void backup(final String[] filePaths, final Set<String> exclude, final String datPath, final boolean log, final boolean ignoreCache, final int reduceLog, final boolean removeFiles) {
		excludedSet = exclude;
		this.log = log;
		this.ignoreCache = ignoreCache;
		this.reduceLog = reduceLog;
		this.removeFiles = removeFiles;

		copyStack = new CopyStack();

		for (final String filePath : filePaths) {
			indexThreadsDone = false;
			final File f = new File(filePath);
			System.out.println("Indexing   " + f.getAbsolutePath() + " ...");

			final File[] files = f.listFiles();

			final IndexThread it1 = new IndexThread(this, files, 0);
			it1.thread.run();
			indexThreadsDone = true;
			System.out.println("Done.");
		}

		if(removeFiles)
			for (final String path1 : removedFilesList)
				copyStack.add(new CopyOperation(Path.of(destPath + path1), Path.of(destPath + deletedFilesDirectoryPrefix + path1), CopyType.Move));
		System.out.println("Copying...");
		final CopyThread ct = new CopyThread(this);
		ct.thread.run();

		System.out.println("\nAll tasks done.\n\n");

	}

}

enum CopyType {
	File, Symlink, EmptyDirectpry, Move
}

class CopyOperation {
	public final Path from;
	public final Path to;
	public final CopyType type;

	public CopyOperation(final Path from, final Path to, final CopyType type) {
		this.from = from;
		this.to = to;
		this.type = type;
	}

	@Override
	public String toString() { return "CopyOperation [from=" + from + ", to=" + to + ", type=" + type + "]"; }
}

class CopyStack {
	private final int defaultArraySize = 1000000;

	private final CopyOperation[] array;
	int arrayPointer;
	int writeArrayPointer;

	public CopyStack() {
		array = new CopyOperation[defaultArraySize];
		arrayPointer = 0;
		writeArrayPointer = 0;
	}

	public void add(final CopyOperation co) {
		if(writeArrayPointer == array.length) {
			writeArrayPointer = 0;
			if(arrayPointer == 0)
				throw new IllegalArgumentException("Ran out of array space");
		}
		array[writeArrayPointer++] = co;
	}

	public CopyOperation pop() {
		final CopyOperation co = array[arrayPointer++];
		if(arrayPointer == array.length)
			arrayPointer = 0;
		if(co == null)
			throw new IllegalArgumentException("Tried to pop an empty stack.");
		return co;
	}

	public boolean hasSomething() { return arrayPointer != writeArrayPointer; }

	@Override
	public String toString() {
		final StringBuilder str = new StringBuilder();
		for (int i = arrayPointer;; i++) {
			if(i == array.length)
				i = 0;
			if(i == writeArrayPointer)
				break;
			str.append(array[i]).append(", ");
		}
		return "CopyStack: { " + str.append(" }").toString();
	}
}

class IWEHA_DATA implements java.io.Serializable {
	private static final long serialVersionUID = 3497235229568524977L;

	public final HashMap<String, Long> oldTimeMap;

	public IWEHA_DATA(final HashMap<String, Long> oldTimeMap) { this.oldTimeMap = oldTimeMap; }

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (final Entry<String, Long> entry : oldTimeMap.entrySet())
			sb.append("   " + entry.getKey() + " -> " + entry.getValue() + "\n");
		return "IWEHA_DATA: { \n" + sb.toString() + "}";
	}
}
