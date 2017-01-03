package examples;

import mesofc.EZMPJ;
import mesofc.EZMessageListener;
import mesofc.EZStatus;
import mpi.MPIException;


/**
  Canonical Ring
  original version: https://github.com/open-mpi/ompi/blob/master/examples/Ring.java  
  
*/


public class Ring implements EZMessageListener {

    private int token=0;

    private int next;

    private int tag = 10;


    public static void main(String[] args) throws MPIException {

        new Ring(args);

    }

    public Ring(String[] args) throws MPIException {

        // initialize threads and the callback interface

        EZMPJ.initThreads(args);
        EZMPJ.startReceiverThread(this);

        next = (EZMPJ.getRank() + 1) % EZMPJ.getSize();

        // Rank 0 will start sending to next (1)

        if (EZMPJ.getRank() == 0) {

            token = 100;

            EZMPJ.send(token, next, tag);
        }

        // Wait for all receiver threads
        EZMPJ.waitFinalizeThreads();

    }

    @Override
    public void onMessage(EZStatus ezStatus) throws MPIException {

        System.out.println("processor#" + EZMPJ.getRank() + " Received message from: " + ezStatus.getSender() + ", tag="
                + ezStatus.getTag() + ", data=" + ezStatus.getDataObject());

        token = (int) ezStatus.getDataObject();

        if (0 == EZMPJ.getRank()) {

            token--;
            System.out.println("Process 0 decremented value: " + token);
        }

        // send to next
        EZMPJ.send(token, next, tag);

        if (token == 0) {
            
            EZMPJ.shutdownReceiverThread();
        }

    }

}
