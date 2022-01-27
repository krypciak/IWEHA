# It Won't Ever Happen Again.
IWEHA is an synchronie-type backup tool. What does that mean?<br>
It means it copies only new or modified files, to keep the backup up-to-date.<br>

<h2>Usage</h2>
Using <a href="https://github.com/krypciak/FreeArgParser-Java">FreeArgParser</a> as an argument parser.<br>

|Short Name | Long Name 			| 	 Argument Type		| Is Required	|
|	:--- 	| :---         			|          :---: 		|    :---:     	|	
| -dp		| --datpath   			| String			  	| +				|		
| -de		| --destination  		| String				| +				|
| -tb		| --toBackup    		| String[]			 	| +				|
| -e		| --excluded    		| String[]				| 				|
| -i		| --ignoreCache  		| none					| 				|
| -l		| --doLog     			| none					| 				|
| -rl 		| --reduceLog    		| int					|				|
| -rf 		| --removeFiles  		| none					|				|




I'ts preety quick since it stores last modified dates in a hashmap, insted of checking
