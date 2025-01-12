package cross;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Sync {
    private static final ReentrantReadWriteLock usersMapFileLock = new ReentrantReadWriteLock();
    public static final ReentrantReadWriteLock bidMapLock = new ReentrantReadWriteLock(); // non usata qui
    public static final ReentrantReadWriteLock askMapLock = new ReentrantReadWriteLock(); // non usata qui
    public static final ReentrantReadWriteLock bidMapStopLock = new ReentrantReadWriteLock(); // non usata qui
    public static final ReentrantReadWriteLock askMapStopLock = new ReentrantReadWriteLock(); // non usata qui
    public static final Object timeOutSync = new Object();
    private static final ReentrantReadWriteLock bidMapFileLock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock askMapFileLock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock bidMapStopFileLock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock askMapStopFileLock = new ReentrantReadWriteLock();

    public static void safeReadStarts (String fileName) {
        switch (fileName) {
            case Costants.USERS_MAP_FILE:
                usersMapFileLock.readLock().lock();
                break;
            case Costants.BID_MAP_FILE:
                bidMapFileLock.readLock().lock();
                break;
            case Costants.ASK_MAP_FILE:
                askMapFileLock.readLock().lock();
                break;
            case Costants.BID_MAP_STOP_FILE:
                bidMapStopFileLock.readLock().lock();
                break;
            case Costants.ASK_MAP_STOP_FILE:
                askMapStopFileLock.readLock().lock();
                break;
            default:
                throw new IllegalArgumentException("Unknown file name: " + fileName);
        }
    }

    public static void safeReadEnds (String fileName) {
        switch (fileName) {
            case Costants.USERS_MAP_FILE:
                usersMapFileLock.readLock().unlock();
                break;
            case Costants.BID_MAP_FILE:
                bidMapFileLock.readLock().unlock();
                break;
            case Costants.ASK_MAP_FILE:
                askMapFileLock.readLock().unlock();
                break;
            case Costants.BID_MAP_STOP_FILE:
                bidMapStopFileLock.readLock().unlock();
                break;
            case Costants.ASK_MAP_STOP_FILE:
                askMapStopFileLock.readLock().unlock();
                break;
            default:
                throw new IllegalArgumentException("Unknown file name: " + fileName);
        }
    }

    public static void safeWriteStarts (String fileName) {
        switch (fileName) {
            case Costants.BID_MAP_FILE:
                bidMapFileLock.writeLock().lock();
                break;
            case Costants.ASK_MAP_FILE:
                askMapFileLock.writeLock().lock();
                break;
            case Costants.USERS_MAP_FILE:
                usersMapFileLock.writeLock().lock();
                break;
            case Costants.BID_MAP_STOP_FILE:
                bidMapStopFileLock.writeLock().lock();
                break;
            case Costants.ASK_MAP_STOP_FILE:
                askMapStopFileLock.writeLock().lock();
                break;
            default:
                throw new IllegalArgumentException("Unknown file name: " + fileName);
        }
    }

    public static void safeWriteEnds (String fileName) {
        switch (fileName) {
            case Costants.BID_MAP_FILE:
                bidMapFileLock.writeLock().unlock();
                break;
            case Costants.ASK_MAP_FILE:
                askMapFileLock.writeLock().unlock();
                break;
            case Costants.USERS_MAP_FILE:
                usersMapFileLock.writeLock().unlock();
                break;
            case Costants.BID_MAP_STOP_FILE:
                bidMapStopFileLock.writeLock().unlock();
                break;
            case Costants.ASK_MAP_STOP_FILE:
                askMapStopFileLock.writeLock().unlock();
                break;
            default:
                throw new IllegalArgumentException("Unknown file name: " + fileName);
        }
    }
    
}
