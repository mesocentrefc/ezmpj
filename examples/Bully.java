package examples;

import mesofc.EZMPJ;
import mesofc.EZMessageListener;
import mesofc.EZStatus;
import mpi.MPIException;

/**
 * Bully Election algorithm
 * 
Any process P can initiate an election

P sends ELECTION messages to all process with higher IDs and awaits OK messages
 - If no OK messages, P becomes coordinator and sends Coordinator messages to all processes with lower ID
 - If it receives an OK, it drops out and waits for an Coordinator message

If a process receives an ElECTION message
  - immediately sends Coordinator message if it is the process with highest ID
  - Otherwise, returns an OK and starts an election

If a process receives a COORDINATOR message, it treats sender as the coordinator
 * 
 * 
 * 
 */

public class Bully implements EZMessageListener {

    private int ELECTION = 10;

    private int OK = 20;

    private int COORDINATOR = 30;

    private int decider = 0;

    private int leader;

    private int rank;

    private int size;

    public static void main(String[] args) throws MPIException {

        new Bully(args);

    }

    public Bully(String[] args) throws MPIException {

        // initialize threads

        EZMPJ.initThreads(args);

        // Start receiver threads with callback interface
        EZMPJ.startReceiverThread(this);

        rank = EZMPJ.getRank();

        size = EZMPJ.getSize();

        // by default the actual leader is the proc with highest id
        leader = size - 1;

        // read decider from args
        if (args.length == 1) {

            decider = Integer.parseInt(args[0]);
        }
        // decider will start election
        if (decider == rank) {
            election();
        }

        // Wait for all receiver threads
        EZMPJ.waitFinalizeThreads();

        if(rank!=size-1)
        System.out.println(rank + " Leader is : " + leader);

    }

    @Override
    public void onMessage(EZStatus ezStatus) throws MPIException {

        // ignore message for the dead leader
        if (rank == leader) {

            EZMPJ.shutdownReceiverThread();

        }

        if (ezStatus.getTag() == ELECTION) {

            if (rank == size - 2) {

                leader = rank;
                
                coordination();

           EZMPJ.shutdownReceiverThread();

            } else {
                EZMPJ.send(0, ezStatus.getSender(), OK);
                election();
            }

        }

        if (ezStatus.getTag() == COORDINATOR) {

            System.out.println(rank + " received COORD from: " + ezStatus.getSender());
            leader = ezStatus.getSender();
            EZMPJ.shutdownReceiverThread();

        }

    }

    private void coordination() throws MPIException {

        for (int i = 0; i < size; i++) {

            if (rank != i)
                System.out.println(rank + " send COORD to: " + i);
            EZMPJ.send(0, i, COORDINATOR);
        }

    }

    private void election() throws MPIException {

        for (int i = rank + 1; i < size; i++) {

            System.out.println(rank + " send election to: " + i);
            EZMPJ.send(0, i, ELECTION);
        }

    }

}
