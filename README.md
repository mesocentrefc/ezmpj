# EZMPJ
A set of helper classes to enable Asynchronous Message Receiving (Event Driven) with Open MPI Java Binding.

The goal is to provide a simple interface on top of MPI (Java binding) to program applications using events for receiving messages. For example:
* Master worker paradigm
* Message driven architecture
* Asynchronous communication


## How it works?
Each MPI process:
* creates and spawns a Java thread for receiving messages using MPI functions **Iprobe()** and **Recv()**
* Adds a listener to receive asynchronous messages using **onMessage()** method
* Uses a new Method to send Serialized objects **send(object, dest, tag)**
* Uses Java Serialization (or custom serialization APIs) for Data exchange.

**Notice:**

1. EZMPJ finalize depends on your application, use  **waitFinalizeThreads()** and **shutdownReceiverThread()** to properly stop your application. _waitFinalizeThreads()_ is used to synchronize the master thread with receiver thread (wait the end of the receiver thread). _shutdownReceiverThread()_ is used to force the receiver threads to stop (call this method when you're sure that there is no more message to consum).  
2. On can still use global communication functions since they not rely on tag
3. Open MPI uses aggressive busy wait to receive messages. This keep one processor pegged at 100% for each process that is waiting.  We are using iProbe() in a loop with a sleep call before calling a blocking receive. The default value of sleep is 10ms, this value can be changed  by the environment variable: **EZMPJ_BUSY_SLEEP**



## Example

```java

// initialize threads
EZMPJ.initThreads(args);
        
// Start receiver threads with listener interface
EZMPJ.startReceiverThread(this);

// We could use MPI.COMM_WORLD.getRank()
if (EZMPJ.getRank() == 0) {

String message = new String("Hello World !");
// send to  process 1
EZMPJ.send(message, 1, tag);
 
//Stop receiving threads in proc 0 since it will not receive msg
EZMPJ.shutdownReceiverThread();
}

// Wait for all receiver threads
EZMPJ.waitFinalizeThreads();

//receiver listener
/*
 This method is called when a message is received
*/
@Override
public void onMessage(EZStatus ezStatus) throws MPIException {

System.out.println("processor#" + EZMPJ.getRank() + " Received message from: " + ezStatus.getSender()
                + ", tag=" + ezStatus.getTag() + ", data=" + ezStatus.getDataObject());

//stop receiving
EZMPJ.shutdownReceiverThread();
 
}

```

### Compile your application

First we need to generate a jar file:

- make sure that **mpi.jar** is in your CLASSPATH
- generate **ezmpj.jar** with **ant**

To compile your application:

```bash
mpijavac -cp .:/path/to/ezmpj.jar  examples/HelloWorld.java
```

To run your application:
```bash
mpirun -np 4 java  -cp .:/path/to/ezmpj.jar  examples.HelloWorld
```

## Limitations
* Combine **onMessage()** with standard MPI sending methods (Send, Isend,...) will cause a conflict in receiving messages. Use only _**EZMPJ.Send()**_ method to send objects.
* Standard Java serialization is slow. On can use 3rd party APIs like Java Fast Serialization (https://ruedigermoeller.github.io/fast-serialization/). You need to write a class that implements **EZMessageCodecInterface** and than call **setMessageCodec()** method.   

## Requirements
* Open MPI with Java Interface (https://www.open-mpi.org/faq/?category=java)
* Open MPI with Multi-threading support (at least **MPI_THREAD_SERIALIZED** is provided). Consult the [MPI_Init_thread()](https://www.open-mpi.org/doc/v2.0/man3/MPI_Init_thread.3.php) man page for details.
