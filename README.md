# QueueMonitoringService
Queue monitoring service made for Student Center of Novi Sad

This system is designed for Student Center of Novi Sad for providing queue status from one of the waiting rooms online.

It consists of three parts:
- Local python script (main.py) which feth information from ticketing macihe and uploads it to the server
- PHP scripts on server, which receives uploaded data and stores them into XML
- Android app which reads XML data from the server and displays it on the screen

The goal is to minimmize time spent in the waiting room while paying the rent, or applying for the accomodation.