|	TIME    |	MESSAGE LOG                                                                                                                     |
|-----------|:----------------------------------------------------------------------------------------------------------------------------------|
|15-10-2015	|	We started using Slack for team communication (https://slack.com/) - created team mini-dos										|
|15-10-2015	|	Created github repository for the project: https://github.com/dzitkowskik/mini-dos												|
|15-10-2015	|	Created waffle for issue tracker and task manager: https://waffle.io/dzitkowskik/mini-dos										|
|15-10-2015	|	Created travis for continous integration: https://travis-ci.org/dzitkowskik/mini-dos											|
|15-10-2015	|	Integration of waffle and travis with github repository																			|
|15-10-2015	|	Created slack for group chatting (our way of communication and meetings): https://mini-dos.slack.com 							|
|21-10-2015	|	Session with Ozan to find out which kind of architecture we would like to implement												|
|21-10-2015	|	Gathering information with Ozan about distributed systems and how to implement them												|
|21-10-2015	|	We try to answer following questions:																							|
|		   	| 	   1. How to parse an SQL query?																								|
|		   	|	   2. How does the node know what data he needs to answer query?																|
|		   	|	   3. How node can find out where all parts of data he needs are? In which nodes?												|
|		   	|	   4. How we will implement message passing?																					|
|		   	|	   	- it is question about communication mechanism?																				|
|		   	|	   5. How will we replicate the data?																							|
|		   	|	   	- probably full replication is not a good idea but it is the simplest														|
|		   	|	   6. What should be done and how when one node crashes?																		|
|		   	|	   7. What proxy will we use to distribute requests to nodes?																	|
|		   	|	   8. How we will monitor the state of our system?																				|
|28-10-2015	|	I decided to use ELK stack for logging in our distributed system (ElasticSearch + LogStash + Kibana) (https://www.elastic.co/)	|
|28-10-2015	|	I decided we will use Docker for easy deployment of our application and easier testing (https://www.docker.com/)				|
|28-10-2015	|	Add integration between slack and git repository (we see commits as messages in slack communicator)								|
|30-10-2015	|	Meeting with all group members in order to try answer above questions															|
|30-10-2015	|	We decided to use SQLite as a base database for all nodes																		|
|30-10-2015	|	We decided to use architecture with central server - scheduler and nodes registered in it - tasks								|
|30-10-2015	|	We choosed roles for group members:																								|
|			|	   a) Karol Dzitkowski - Project Manager + (Presentation + Repository Manager)													|
|			|	   b) Dawid Pachowski - testing 																								|
|			|	   c) Ozan Oz - Documentation + Presentation																					|
|			|	   d) David Miguel - Database engineer																							|
|30-10-2015	|	Add folowing roles:																												|
|			|	   a) Scheduler programmer (who is responsible for master node) - David Miguel													|
|			|	   b) Task programmes (who is responsible for slave nodes) - Ozan Oz 															|
|			|	   c) Integration provider (who is responsible for proper integration - comminication between nodes) - Dawid Pachowski			|
|30-10-2015	|	We divided project into two phases:																								|
|			|	   a) Working distributed database with single master node and many slaves with replication factor, 							|
|			|	      and with data register (where each row in db is located) in one table in master 											|
|			|	   b) Try to multiply master nodes to remove single point of failure - so many masters and many nodes. We use proxy to 			|
|			|	      split traffic to different master nodes.																					|
|05-11-2015	|	I created instruction how to use docker and elk stack for logging with samle java app in docker container to log sth 			|

