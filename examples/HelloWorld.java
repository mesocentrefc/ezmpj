package examples;

import mesofc.EZMPJ;
import mesofc.EZMessageListener;
import mesofc.EZStatus;
import mpi.MPIException;

/**
 * proc0 sends a message to others processors with EZMPJ.send method using
 * object serialization. Other procs will receive the message in the listener
 * function.
 * 
 * Important : we can't use send MPI.COMM_WORLD.send and onMessage() together
 */

public class HelloWorld implements EZMessageListener {

    private String message;

    private int tag = 10;

    public static void main(String[] args) throws MPIException {

        new HelloWorld(args);

    }

    public HelloWorld(String[] args) throws MPIException {

        // initialize threads

        EZMPJ.initThreads(args);

        // Start receiver threads with callback interface
        EZMPJ.startReceiverThread(this);

        // One can use MPI.COMM_WORLD.getRank()
        if (EZMPJ.getRank() == 0) {

            message = new String("Hello World !");

            // send to all other process

            for (int i = 1; i < EZMPJ.getSize(); i++) {

                EZMPJ.send(message, i, tag);
            }

            // Stop receiving threads in proc 0 since it will not receive msg
            EZMPJ.shutdownReceiverThread();
        }

        // Wait for all receiver threads
        EZMPJ.waitFinalizeThreads();

    }

    @Override
    public void onMessage(EZStatus ezStatus) throws MPIException {

        System.out.println("processor#" + EZMPJ.getRank() + " Received message from: " + ezStatus.getSender()
                + ", tag=" + ezStatus.getTag() + ", data=" + ezStatus.getDataObject());

        // stop receiving
        EZMPJ.shutdownReceiverThread();

    }

}
