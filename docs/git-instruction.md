# GIT INSTRUCTION:
Our github repository is: [https://github.com/dzitkowskik/mini-dos](https://github.com/dzitkowskik/mini-dos)

1. To clone repository type:
	
	```bash
	$ git clone https://github.com/dzitkowskik/mini-dos
	```
	it will create a folder mini-dos with repository content inside

2. Before starting work always pull newest changes using
	
	```bash
	$ git pull
	```
	preferably with option -r or --rebase (it will rebase instead of merge and tree of our repository will be more stright)   
	then after you change some code you must commit your changes
	```bash
	$ git commit -a -m "somme commit message"
	```
	* if you added some files do 
		```bash
		$ git add <file path that was added> 
		```
		or
		```bash
		$ git add -A
		```
		to add many files (be careful with that since it can add to many files)   

	* you can check which files are added (stashed) for next commit or which files were changed write
		```bash
		$ git status
		```

3. After work you always have to push your changes to github repository:

	```bash
	$ git push
	```
	if you get an error message:
 	* [rejected]        master -> master (non-fast-forward) - then you must pull the changes using `$ git pull` and merge conflicts if necessary
 	* fatal: No configured push destination - then you must set push destination:
 	
 		```bash
 		$ git push -u origin master
 		```
 		or
 		
 		```bash
 		$ git branch --set-upstream-to <remote-name>
 		```
 		best solution probably is to do: `$ git config push.default simple`

4. To resolve conflicts you must remove parts that are not the newest which looks like that in files:
	
	```
	<<<<<< HEAD
	nine
	=======
	eight
	>>>>>>> branch-a
	```

	after that you must commit those changes and merge/rebase will be complete


