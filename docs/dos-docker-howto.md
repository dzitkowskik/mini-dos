1. Ubuntu 14.04 on VB installation:

   ```bash
   $ sudo apt-get install build-essential module-assistant
   $ sudo m-a prepare
   ```
   
   Now click "Devices > Insert guest additions CD image" in the virtualbox window.
   Install guest additions:)
	
   ```bash
   $ sudo apt-get install vim
   $ sudo reboot
   ```
2. Docker installation: (instructions on: http://docs.docker.com/engine/installation/ubuntulinux/)

   ```bash
   $ sudo apt-key adv --keyserver hkp://pgp.mit.edu:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D
   $ sudo vim /etc/apt/sources.list.d/docker.list
   ```
   
   press "i" to insert text
   write: deb https://apt.dockerproject.org/repo ubuntu-trusty main
   press esc and then type ":wq" [enter]

   ```bash
   $ sudo apt-get update
   $ sudo apt-get purge lxc-docker*
   $ sudo apt-cache policy docker-engine
   ```
   ```bash
   $ sudo apt-get install docker-engine
   $ sudo service docker start
   $ sudo docker run hello-world
   ```

   you should see Hello from Docker.

   ```bash
   $ sudo usermod -aG docker <username>
   $ sudo reboot
   ```
3. Get default ubuntu:14.04 container

	$ docker pull ubuntu:trusty
	$ docker images

	you should see sth like:

	REPOSITORY          TAG                 IMAGE ID            CREATED             VIRTUAL SIZE
	ubuntu              trusty              1d073211c498        12 days ago         187.9 MB
	hello-world         latest              0a6ba66e537a        3 weeks ago         960 B

	to run the container type:

	$ docker run -t -i ubuntu:trusty bash

4. in docker install JAVA

	$ sudo apt-get update
	$ sudo apt-get install default-jre
	$ sudo apt-get install default-jdk
	$ sudo vi /etc/environment
	add line: JAVA_HOME="/usr/lib/jvm/java-7-openjdk-amd64"
	save

	// Install additional things
	sudo apt-get install curl wget ...

5. go to another console and commit docker image

	$ docker commit -a "Karol Dzitkowski <k.dzitkowski@gmail.com>" -m "Initial docker image for DOS project on MINI (Warsaw University of Technology" <image id> <repository name>

	where <repository name> is for example mini-dos/server or mini-dos/node

	to inspect commits write:
	$ docker images -a --no-trunc | head -n4 | grep -v "IMAGE ID" | awk '{ print $3 }' | xargs docker inspect

6. Mount folder and run java application

	a) create folder for app on your fs
		$ mkdir ~/mini-dos/server
	b) write simple HelloWorld.java file with one class:
		public class HelloWorld {
		    public static void main(String[] args) {
		        System.out.println("Hello World!");
		    }
		}
	c) docker run -it -v ~/mini-dos/server:/server mini-dos/server sh -c 'cd server;javac HelloWorld.java;java -cp . HelloWorld'

7. Logging ELK

	a) ElasticSearch
		$ sudo mkdir -p /data/elasticsearch
		$ sudo docker run -d --name elasticsearch -p 9200:9200 -v /data/elasticsearch:/usr/share/elasticsearch/data elasticsearch -Des.network.host=0.0.0.0
	b) LogStash
		Next we must create config file:
		$ cd conf
		$ sudo touch syslog.conf
		$ sudo chmod 764 syslog.conf
		$ sudo gedit syslog.conf

		paste inside this content and save:

		input {
		  syslog {
		    type => syslog
		    port => 25826
		  }
		}
		 
		filter {
		  if "docker/" in [program] {
		    mutate {
		      add_field => {
		        "container_id" => "%{program}"
		      }
		    }
		    mutate {
		      gsub => [
		        "container_id", "docker/", ""
		      ]
		    }
		    mutate {
		      update => [
		        "program", "docker"
		      ]
		    }
		  }
		}
		 
		output {
		  stdout {
		    codec => rubydebug
		  }
		  elasticsearch {
		    hosts => db
		  }
		}

		After that we run logstash docker:
		$ sudo docker run -d --name logstash --expose 25826 -p 25826:25826 -p 25826:25826/udp -v $PWD/conf:/conf --link elasticsearch:db logstash logstash -f /conf/syslog.conf

		Next we set up config for rsyslog:
		$ sudo echo "*.* @@<system ip>:25826" /etc/rsyslog.d/10-logstash.conf
		$ sudo service rsyslog restart

	c) Kibana
		$ sudo docker run -d --name kibana -p 5601:5601 --link elasticsearch:elasticsearch kibana
		
		For sample kibana config we can use:
		$ sudo docker run --rm -v $PWD/conf:/data vfarcic/elastic-dump --input=/data/es-kibana.json --output=http://<system ip>:9200/.kibana --type=data

		kibana will be available at: http://localhost:5601


		TO LOG THINGS FROM CONTAINER:

		add --log-driver syslog when running container

		$ docker run -it --log-driver syslog -v ~/Dokumenty/mini-dos/server:/server mini-dos/server sh -c 'cd server;javac HelloWorld.java;java -cp . HelloWorld'

		Sample HelloWorld.java file:

		import java.io.*;
		public class HelloWorld {

		    public static void main(String[] args) {
		        System.out.println("Method 1");

		        PrintWriter writer = new PrintWriter(System.out);
		        writer.println("Method 2");
		        writer.flush();
		        writer.close();
		    }
		}


TO STOP AND REMOVE STOPPED CONTAINERS

$ docker stop kibana logstash elasticsearch
$ docker rm kibana logstash elasticsearch
