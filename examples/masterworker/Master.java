package examples.masterworker;

import java.util.Random;

import mesofc.EZMPJ;
import mesofc.EZMessageListener;
import mesofc.EZStatus;
import mpi.MPIException;

public class Master implements EZMessageListener {

    private int TaskPoolSize = 80;

    public void main(String[] args) throws MPIException {

        //Get Task pool size from args
        if (args.length == 1) {
            TaskPoolSize = Integer.parseInt(args[0]);
        }

        //Send the Tasks to workers
        for (int i = 1; i < EZMPJ.getSize(); i++) {

            Task<Integer> task = getWork();
            EZMPJ.send(task, i, Main.COMPUTE_TAG);
            TaskPoolSize = TaskPoolSize - 1;
        }

    }

    @Override
    public void onMessage(EZStatus ezStatus) throws MPIException {

        System.out.println("Master#" + EZMPJ.getRank() + " Received work from worker# " + ezStatus.getSender()
                + ", tag=" + ezStatus.getTag() + ", data=" + ezStatus.getDataObject() + ", chunk left = "
                + TaskPoolSize);

        //Give new Task to worker
        Task<Integer> task = getWork();

        if (task != null) {
            EZMPJ.send(task, ezStatus.getSender(), Main.COMPUTE_TAG);

        } else {

            finsihWorkers();
            EZMPJ.shutdownReceiverThread();
        }

    }

    private void finsihWorkers() throws MPIException {

        for (int i = 1; i < EZMPJ.getSize(); i++) {

            EZMPJ.send(0, i, Main.EXIT_TAG);
        }

    }

    private Task<Integer> getWork() {

        if (TaskPoolSize <= 0) {
            return null;
        }

        TaskPoolSize = TaskPoolSize - 1;

        Random rand = new Random();

        int min = 10;
        int max = 1000;

        int randomNum = rand.nextInt((max - min) + 1) + min;

        SleepTask sleepTask = new SleepTask(randomNum);
        return sleepTask;
    }

}
