package examples.masterworker;

import mesofc.EZMPJ;
import mpi.MPIException;


/**
 *  Master Worker example with dynamic load balancing
 *  
 *  1- Master generate Task
 *  2- Send Task to Workers
 *  3- Worker execute Task and return Result
 *  4- A new work is sent to the last finished worker
 *  5- Repeat until no work to do
 * 
 *
 */

public class Main {

    public static int COMPUTE_TAG = 100;
    public static int RESULT_TAG = 200;
    public static int EXIT_TAG = 200;
    public static int MASTER = 0;

    public static void main(String[] args) throws MPIException {

        EZMPJ.initThreads(args);

        Master master = new Master();
        Worker worker = new Worker();

        if (EZMPJ.getRank() == 0) {

            EZMPJ.startReceiverThread(master);

            master.main(args);

        } else {

            EZMPJ.startReceiverThread(worker);

            worker.main(args);
        }

        EZMPJ.waitFinalizeThreads();
    }

}
