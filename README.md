# mini-dos [![Stories in Ready](https://badge.waffle.io/dzitkowskik/mini-dos.svg?label=ready&title=Ready)](http://waffle.io/dzitkowskik/mini-dos) [![Build Status](https://travis-ci.org/dzitkowskik/mini-dos.svg?branch=master)](https://travis-ci.org/dzitkowskik/mini-dos) [![GitHub license](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](https://raw.githubusercontent.com/dzitkowskik/mini-dos/master/LICENSE)

## PROJECT CLOSED - WE WON'T CONTRIBUTE TO IT ANYMORE

### Project for Distributed Operating Systems at Warsaw University of Technology (Mathematics and Computer Science department) Winter 2015

Our goal is to create distributed database focusing on fault tolerance, ease of deployment and scalability. All the data will be replicated in several nodes, improving the performance and protecting the availability of applications. The stack of technologies that we will use will reduce the cost of deployment. Furthermore, it will enable database scale up instantly and database scale out transparently.

## TIMETABLE (New version - updated 14-12-2015)

1. **2015.11.24** 
  - nodes registering in master
  - working insert of single row from many clients (no failure detection, no replication etc.)

2. **2015.12.04** 
  - insert data with replication with some replication factor
  - failure of node detection with replicating data it had
  - client can send requests to master

3. **2015.12.08** (1st part of I checkpoint)
  - inserts with replication (not fault tolerant replication) working
  - creating tables on all nodes
  - basic tests + one working test on CI (travis)
  - gathering statuses of nodes by master + node failure detection by master

4. **2015.12.15** (2nd part of I checkpoint)
  - be able to perform selects on our system, for example SELECT c1, c2, c3 FROM table
  - 2 phase commit working when inserting data to nodes
  - first integration tests + integration with CI (travis)
  - some unittests (environment for testing ready)   
   
5. **2015.12.22** (last sumup before christmas)
  - we will be able to do update, delete and select queries on our data (possible not full failure detection and bugs)
  - we will have nearly working first version (step 1) of our project
  - we will start working on multiple masters or backup master

6. **2015.01.05**
  - we will have „backup master” or some parts of "multiple masters" implemented
  - master synchronized with main master that can be switched in case of main master failure 
  - backup plan is to end on that if we won’t have time

7. **2016.01.12** 
  - beta version of multiple masters or backup master 
  - can be buggy and we can have some troubles with consistency but is should be working
  - many integration and component as well as some unittest present and integrated with CI
  - tests proving fault tolerance (automated and manual)

8. **2016.01.19** (II checkpoint)
  - we will fix as many bugs as possible
  - ready scenarios of testing and presentation of our system ready
  - all documentation finished

9. **2016.01.26** (last chance)
  - in case of some troubles, bugs will be fixed or more things will be implemented
  - all things done

## TIMETABLE (Old version - updated 14-12-2015)

2015.11.24 - we will have nodes registering in master and working insert of single row from many clients (no failure detection, no replication etc.)

**2015.12.04** - we will be able to insert data with replication with some replication factor + failure of node detection with replicating data it had

2015.12.12 - we will be able to do update, delete and select queries on our data (possible not full failure detection and bugs)

2015.12.15 - we will have working and bug free first version (step 1) of our project we will start working on multiple masters (step 2)

2015.12.22 - we will have „backup master” - master synchronized with main master that can be switched in case of main master failure (backup plan is to end on that if we won’t have time)

2016.01.12 - beta version of multiple masters (can be buggy and we can have some troubles with consistency but is should be working)

2016.01.19 - we will fix as many bugs as possible 

### [Documentation](https://github.com/dzitkowskik/mini-dos/wiki)

## Throughput
[![Throughput Graph](https://graphs.waffle.io/dzitkowskik/mini-dos/throughput.svg)](https://waffle.io/dzitkowskik/mini-dos/metrics) 
