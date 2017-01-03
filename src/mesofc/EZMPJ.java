package mesofc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import mpi.MPI;
import mpi.MPIException;
import mpi.Request;
import mpi.Status;

public class EZMPJ {

    private final static Logger logger = Logger.getLogger(EZMPJ.class.getName());

    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    private static EZMessageCodec ezMessageCodec;

    private volatile static boolean stopServer = false;

    private static int busyWaitSleep = 10;
    
    private static Future<Integer> receiver;

    private static String ENV_EZMPJ_SLEEP = "EZMPJ_BUSY_SLEEP";

    private static String ENV_EZMPJ_LOG_LEVEL = "EZMPJ_LOG_LEVEL";

    private static String threadSupport = new StringBuilder()
            .append("MPI_THREAD_SINGLE(0), Only one thread will execute.\n")
            .append("MPI_THREAD_FUNNELED(1), The process may be multi-threaded, but only the main thread will make MPI calls (all MPI calls are funneled to the main thread).\n")
            .append("MPI_THREAD_SERIALIZED(2), The process may be multi-threaded, and multiple threads may make MPI calls, but only one at a time: MPI calls are not made concurrently from two distinct threads (all MPI calls are serialized),\n")
            .append("MPI_THREAD_MULTIPLE(3), Multiple threads may call MPI, with no restrictions.\n").toString();

    /**
     * 
     * @param args
     * @param messageReceiver
     * @throws MPIException
     */
    public static void initThreads(String args[]) throws MPIException {

        initLogger();

        logger.info("Initialize MPI Threads. Requesting THREAD_MULTIPLE(3)");

        int provided = MPI.InitThread(args, MPI.THREAD_MULTIPLE);

        if (provided < MPI.THREAD_SERIALIZED) {

            logger.log(Level.SEVERE, "This MPI doesn't support Threads. Requested Multiple Threads:"
                    + MPI.THREAD_MULTIPLE + ", Found:" + provided);

            logger.log(Level.SEVERE, threadSupport);

            throw new MPIException("This MPI doesn't support Threads. Requested Multiple Threads:"
                    + MPI.THREAD_MULTIPLE + " ,Found:" + provided);

        } else if (provided == MPI.THREAD_SERIALIZED) {

            if (getRank() == 0) {

                logger.log(Level.WARNING,
                        "MPI_THREAD_MULTIPLE(3) is requested **BUT** Only MPI_THREAD_SERIALIZED(2) is provided for this installation!");
                logger.log(
                        Level.WARNING,
                        "<<The process may be multi-threaded, and multiple threads may make MPI calls, but only one at a time: MPI calls are not made concurrently from two distinct threads (all MPI calls are serialized).>>");
            }
        }

        ezMessageCodec = new DefaultMessageCodec();

    }

    public static void startReceiverThread(EZMessageListener receiver) {
        startReceiver(receiver);
    }

    /**
     * 
     * @param codec
     */
    public static void setMessageCodec(EZMessageCodec codec) {
        ezMessageCodec = codec;
    }

    /**
     * 
     * @throws MPIException
     */
    public static void waitFinalizeThreads() throws MPIException {

        logger.info("process #" + getRank() + " Waiting for threads to finalize...");

        MPI.COMM_WORLD.barrier();

        try {
            receiver.get();
        } catch (Exception e) {

            logger.log(Level.SEVERE, "process #" + getRank() + " an exeception while executing receiver thread", e);

            receiver.cancel(true);

            throw new MPIException(e.getMessage());
        }

        logger.info("process #" + getRank() + " Calling MPI Finalize");
        executor.shutdown();
        MPI.Finalize();
    }

    public static void shutdownReceiverThread() {
        stopServer = true;
    }

    /**
     * 
     * @param obj
     * @param dest
     * @param tag
     * @throws MPIException
     */
    public static void send(Object obj, int dest, int tag) throws MPIException {

        byte[] buffer;
        try {
            buffer = ezMessageCodec.serialize(obj);
        } catch (IOException e) {

            logger.log(Level.SEVERE, "process #" + getRank()
                    + " Error while preparing data (serialization) to send message to process #" + dest + ", tag="
                    + tag, e);

            throw new MPIException(e);
        }

        logger.fine("process #" + getRank() + " send  to <" + dest + "> tag=" + tag);

        MPI.COMM_WORLD.send(buffer, buffer.length, MPI.BYTE, dest, tag);
    }

    public static Request iSend(Object obj, int dest, int tag) throws MPIException {

        byte[] buffer;
        try {
            buffer = ezMessageCodec.serialize(obj);
        } catch (IOException e) {

            logger.log(Level.SEVERE,
                    "process #" + getRank()
                            + " Error while preparing data (serialization) to send message from process#"
                            + MPI.COMM_WORLD.getRank() + ", to=process#" + dest + ", tag=" + tag, e);

            throw new MPIException(e);

        }

        ByteBuffer byteBuffer = MPI.newByteBuffer(buffer.length);

        byteBuffer.put(buffer);

        logger.fine("process #" + getRank() + " send  to <" + dest + "> tag=" + tag);

        return MPI.COMM_WORLD.iSend(byteBuffer, buffer.length, MPI.BYTE, dest, tag);

    }

    /**
     * 
     * @return
     * @throws MPIException
     */
    public static int getRank() throws MPIException {
        return MPI.COMM_WORLD.getRank();
    }

    /**
     * 
     * @return
     * @throws MPIException
     */
    public static int getSize() throws MPIException {
        return MPI.COMM_WORLD.getSize();
    }

    // ********************************************************************************************************************//

    /**
     * 
     * @param messageReceiver
     * @throws MPIException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private static void waitForMessage(final EZMessageListener messageReceiver) throws MPIException,
            ClassNotFoundException, IOException {

        Status status;

        long nbMessages = 0l;

        String envEZMPISleep = System.getenv(ENV_EZMPJ_SLEEP);

        if (envEZMPISleep != null && !envEZMPISleep.isEmpty()) {

            logger.config("process #" + getRank()+" Setting Receiver Busy wait timeout to:" + envEZMPISleep + " ms");
            busyWaitSleep=Integer.parseInt(envEZMPISleep);
        }

        while (!stopServer) {

            status = MPI.COMM_WORLD.iProbe(MPI.ANY_SOURCE, MPI.ANY_TAG);

            if (status == null) {
                try {
                    Thread.sleep(busyWaitSleep);
                } catch (InterruptedException e) {

                    e.printStackTrace();
                }
                continue;
            }

            nbMessages++;
            
            logger.fine("process #" + getRank() + " received from <" + status.getSource() + "> tag=" + status.getTag());
                        

            byte[] buffer = new byte[status.getCount(MPI.BYTE)];

            MPI.COMM_WORLD.recv(buffer, status.getCount(MPI.BYTE), MPI.BYTE, status.getSource(), status.getTag());

            EZStatus ezStatus = new EZStatus(ezMessageCodec.deserialize(buffer), status.getSource(), status.getTag(),
                    nbMessages);

            messageReceiver.onMessage(ezStatus);
        }

    }

    /**
     * 
     */
    private static void initLogger() {

        if (System.getProperty("java.util.logging.config.file", "").isEmpty()) {

            System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT %4$s %2$s] %5$s%6$s%n");

            logger.setLevel(Level.WARNING);

            String eZMPI_LOG_LEVEL = System.getenv(ENV_EZMPJ_LOG_LEVEL);

            if (eZMPI_LOG_LEVEL != null) {

                switch (eZMPI_LOG_LEVEL.toUpperCase()) {
                case "FINE":
                    logger.setLevel(Level.FINE);
                    ConsoleHandler handler = new ConsoleHandler();
                    handler.setLevel(Level.FINER);
                    logger.addHandler(handler);

                    break;
                case "SEVERE":
                    logger.setLevel(Level.SEVERE);
                    break;
                case "OFF":
                    logger.setLevel(Level.OFF);
                    break;

                default:
                    logger.setLevel(Level.WARNING);
                }
            }

        }

    }

    /**
     * 
     * @param messageReceiver
     */
    private static void startReceiver(final EZMessageListener messageReceiver) {

        receiver = executor.submit(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {

                waitForMessage(messageReceiver);
                logger.info("Receiver Thread is stopped");
                return 0;
            }

        });

    }

}
