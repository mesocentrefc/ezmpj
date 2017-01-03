package examples;

import mesofc.EZMPJ;
import mesofc.EZMessageListener;
import mesofc.EZStatus;
import mpi.MPI;
import mpi.MPIException;

/**
   1) proc 0 bcast a string to all procs, using standard MPI bcast method
   2) procs do reverse and send back the string to proc 0 using EZPJ send 

  Notice that we can use MPI bcast and others MPI global communication functions since they don't use TAG 
  
*/

public class Reverse implements EZMessageListener {

    private char[] message;

    private int tag = 10;

    private int[] length = new int[1];

    public static void main(String[] args) throws MPIException {

        new Reverse(args);

    }

    public Reverse(String[] args) throws MPIException {

        // initialize threads and the callback interface

        EZMPJ.initThreads(args);
        EZMPJ.startReceiverThread(this);

        // proc 0 generate String
        if (MPI.COMM_WORLD.getRank() == 0) {
            message = "Hello World From MPI Java".toCharArray();

            length[0] = message.length;

        }

        // We need to bcast the size of String
        MPI.COMM_WORLD.bcast(length, 1, MPI.INT, 0);

        // Other processors will allocated String buffer
        if (MPI.COMM_WORLD.getRank() != 0) {
            message = new char[length[0]];
        }

        // Bcas the String buffer
        MPI.COMM_WORLD.bcast(message, message.length, MPI.CHAR, 0);

        // Do reverse and send back to 0
        if (MPI.COMM_WORLD.getRank() != 0) {

            String reverse = new StringBuilder(new String(message)).reverse().toString();
            EZMPJ.send(reverse, 0, tag);
        }

        // We MUST tell others receivers than 0 to stop
        if (MPI.COMM_WORLD.getRank() != 0) {
            EZMPJ.shutdownReceiverThread();
        }

        // Wait for all receiver threads
        EZMPJ.waitFinalizeThreads();

    }

    @Override
    public void onMessage(EZStatus ezStatus) throws MPIException {

        // only proc 0 will receive here
        System.out.println("processor#" + EZMPJ.getRank() + " Received message from: " + ezStatus.getSender() + ", tag="
                + ezStatus.getTag() + ", data=" + ezStatus.getDataObject());

        // We expect (size-1) messages, we exit receiving
        if (ezStatus.getNbMessages() == EZMPJ.getSize() - 1)
            EZMPJ.shutdownReceiverThread();
    }

}
