### Introduction:  
 
The application is made using Scala 2.12, using mainly akka components (akka-actors)

### Running the application:  
- Navigate to `.../follower-maze/FMS`  
- Run `sbt run`  
- The server starts listenning on the specified ports `9009` and `9090` for clients. 
- After receiving and processing all the messages, the Server terminates the clients and the user actors, and is ready to accept new connections.

### Testing:   
- Simply running `sbt test` will run all the designated unit tests

###Diagrams:
![Arch Diagram](arch_diagram.png)

### Details:  
- TCP Layer establishes the connection and creates and registers handlers once the eventStream and user clients are connected and identified.
- Handlers Layer consists of eventBatch handler, which will handle each batch of events, and will approperiately send the approperiate messages to the user actors to process. And user Actors which are mainly responsible for each user, keeping the state of the user (followers, followings..etc) and handles each message designated to the user accordingly.

### Improvements: 
- The task implementation did not address stress testing, or fault tolerance from client side. However this can be something to consider in the future.
- Improved server actor providing back pressure, and buffering against the event stream client.
- Improved dead-letter handling. Since the current implementation assumes user actors by the message, it does not check whether or not these users exist.

