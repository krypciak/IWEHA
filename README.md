# It Won't Ever Happen Again.
IWEHA is an synchronie-type backup tool, simmliar to rsync. What does that mean?<br>
It means it copies only new or modified files, to keep the backup up-to-date.<br>

<h2>Usage</h2>
Using <a href="https://github.com/krypciak/FreeArgParser-Java">FreeArgParser</a> as an argument parser.<br>

|Short Name | Long Name 		   	| 	 Argument Type		| Is Required	| Description		 |
|	:--- 	| :---         			|          :---: 		|    :---:     	| :---				 |
| -dp		| --datpath   			| String			  	| +				| Path to .dat file of your backup. If you don't have one yet, put "". |		
| -de		| --destination  		| String				| +				| Files will be copied into /your/path/ + /path/to/file/copied|
| -tb		| --toBackup    		| String[]			 	| +				| Either file paths or directory paths. |
| -e		| --excluded    		| String[]				| 				| File or directories that will be skipped. |
| -i		| --ignoreCache  		| none					| 				| If selected, will ignore all all files or directories that contain "cache" in their name. Recommended for big directories. |
| -l		| --doLog     			| none					| 				| If selected, will print to terminal all copied files. |
| -rl 		| --reduceLog    		| int					|				| If this and doLog is selected, will print copy logs only every X time.|
| -rf 		| --removeFiles  		| none					|				| If selected, files that were indexed in the backup don't exist anymore in the original direcotry will get moved into /dest/path/IWEHA_DELETED/path/to/file|

Example:
```
java -jar IWEHA.jar -dp /home/krypek/iweha.dat -de /mnt/sda1/backup/ -tb [/home/krypek/Documents/, /home/krypek/Downloads/] -l -rl 10 -rf
```
<br>
</h2>How does it work?</h2><br>
It checks original file mofification date and compares it with modification date stored in a HashMap.<br>
If original modification date is bigger, the file is copied and the new modification date is stored in<br>
an another HashMap that is later serialized into .dat file.<br>
This way IWEHA doesn't copy unnecessary files.<br><br>

If --removeFiles is selected, if file is indexed (in .dat file) and the original file is deleted,<br>
the file gets moved from /dest/file to /dest/IWEHA_DELETED/file.<br><br><br>

Questions? You can e-mail me at krypek@tuta.io.
