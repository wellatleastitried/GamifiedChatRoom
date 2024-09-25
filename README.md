# GamifiedChatRoom
This project will consist of Conway's Game of Life being hosted on a server for clients to connect to and run. The server will handle the rendering and will send out the states to the client machines. This project is going to be done in Java, Python, Go, and Ruby.
This project started with me wanting to make a Game of Life simulator and ended with me making a fully fledged chat room with the ability to play games both solo or with other people in the chat room.

## Requirements
- Java 17+
- Maven

## Build
To build this project, clone the repo and build the server using Maven:
```
git clone https://github.com/GamifiedChatRoom.git
```
Next, change the port number to match the machine you will run the server on. The file to change this information in is located at Server/src/main/java/com/walit/lifeServer/ServerDriver.java and the line to change is marked with a comment. After you have changed the port in the server application, you will need to change the IP and port within the client program. To do this go to Client/src/main/java/com/walit/lifeClient/ClientDriver.java and the line to change is marked with a comment. Once the IP address and port number are correct in the client program, you are ready to build.

To do this, navigate to GamifiedChatRoom/Server and build the server using Maven:
```
mvn clean install
```
Next, build the client using Maven:
```
cd ../Client
mvn clean install
```
After this is done, the server can be run on your local machine using the JAR file found in GamifiedChatRoom/Server/target. The client program (Also a JAR located in the target directory of GamifiedChatRoom/Client) can be run on any device that is able to access the machine you ran the server JAR on. 

## Usage
To start an instance of the server run:
```
java -jar
```
