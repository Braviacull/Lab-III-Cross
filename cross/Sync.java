package cross;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Sync {
    private static final ReentrantReadWriteLock usersMapFileLock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock usersMapTempFileLock = new ReentrantReadWriteLock();
    public static final ReentrantReadWriteLock bidMapLock = new ReentrantReadWriteLock(); // non usata qui
    public static final ReentrantReadWriteLock askMapLock = new ReentrantReadWriteLock(); // non usata qui
    public static final ReentrantReadWriteLock bidMapStopLock = new ReentrantReadWriteLock(); // non usata qui
    public static final ReentrantReadWriteLock askMapStopLock = new ReentrantReadWriteLock(); // non usata qui
    private static final ReentrantReadWriteLock bidMapFileLock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock askMapFileLock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock bidMapTempFileLock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock askMapTempFileLock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock bidMapStopFileLock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock askMapStopFileLock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock bidMapStopTempFileLock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock askMapStopTempFileLock = new ReentrantReadWriteLock();

    public static void safeReadStarts (String fileName) {
        switch (fileName) {
            case Costants.USERS_MAP_FILE:
                usersMapFileLock.readLock().lock();
                break;
            case Costants.USERS_MAP_TEMP_FILE:
                usersMapTempFileLock.readLock().lock();
                break;
            case Costants.BID_MAP_FILE:
                bidMapFileLock.readLock().lock();
                break;
            case Costants.ASK_MAP_FILE:
                askMapFileLock.readLock().lock();
                break;
            case Costants.BID_MAP_TEMP_FILE:
                bidMapTempFileLock.readLock().lock();
                break;
            case Costants.ASK_MAP_TEMP_FILE:
                askMapTempFileLock.readLock().lock();
                break;
            case Costants.BID_MAP_STOP_FILE:
                bidMapStopFileLock.readLock().lock();
                break;
            case Costants.ASK_MAP_STOP_FILE:
                askMapStopFileLock.readLock().lock();
                break;
            case Costants.BID_MAP_TEMP_STOP_FILE:
                bidMapStopTempFileLock.readLock().lock();
                break;
            case Costants.ASK_MAP_TEMP_STOP_FILE:
                askMapStopTempFileLock.readLock().lock();
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
            case Costants.USERS_MAP_TEMP_FILE:
                usersMapTempFileLock.readLock().unlock();
                break;
            case Costants.BID_MAP_FILE:
                bidMapFileLock.readLock().unlock();
                break;
            case Costants.ASK_MAP_FILE:
                askMapFileLock.readLock().unlock();
                break;
            case Costants.BID_MAP_TEMP_FILE:
                bidMapTempFileLock.readLock().unlock();
                break;
            case Costants.ASK_MAP_TEMP_FILE:
                askMapTempFileLock.readLock().unlock();
                break;
            case Costants.BID_MAP_STOP_FILE:
                bidMapStopFileLock.readLock().unlock();
                break;
            case Costants.ASK_MAP_STOP_FILE:
                askMapStopFileLock.readLock().unlock();
                break;
            case Costants.BID_MAP_TEMP_STOP_FILE:
                bidMapStopTempFileLock.readLock().unlock();
                break;
            case Costants.ASK_MAP_TEMP_STOP_FILE:
                askMapStopTempFileLock.readLock().unlock();
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
            case Costants.BID_MAP_TEMP_FILE:
                bidMapTempFileLock.writeLock().lock();
                break;
            case Costants.ASK_MAP_TEMP_FILE:
                askMapTempFileLock.writeLock().lock();
                break;
            case Costants.USERS_MAP_FILE:
                usersMapFileLock.writeLock().lock();
                break;
            case Costants.USERS_MAP_TEMP_FILE:
                usersMapTempFileLock.writeLock().lock();
                break;
            case Costants.BID_MAP_STOP_FILE:
                bidMapStopFileLock.writeLock().lock();
                break;
            case Costants.ASK_MAP_STOP_FILE:
                askMapStopFileLock.writeLock().lock();
                break;
            case Costants.BID_MAP_TEMP_STOP_FILE:
                bidMapStopTempFileLock.writeLock().lock();
                break;
            case Costants.ASK_MAP_TEMP_STOP_FILE:
                askMapStopTempFileLock.writeLock().lock();
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
            case Costants.BID_MAP_TEMP_FILE:
                bidMapTempFileLock.writeLock().unlock();
                break;
            case Costants.ASK_MAP_TEMP_FILE:
                askMapTempFileLock.writeLock().unlock();
                break;
            case Costants.USERS_MAP_FILE:
                usersMapFileLock.writeLock().unlock();
                break;
            case Costants.USERS_MAP_TEMP_FILE:
                usersMapTempFileLock.writeLock().unlock();
                break;
            case Costants.BID_MAP_STOP_FILE:
                bidMapStopFileLock.writeLock().unlock();
                break;
            case Costants.ASK_MAP_STOP_FILE:
                askMapStopFileLock.writeLock().unlock();
                break;
            case Costants.BID_MAP_TEMP_STOP_FILE:
                bidMapStopTempFileLock.writeLock().unlock();
                break;
            case Costants.ASK_MAP_TEMP_STOP_FILE:
                askMapStopTempFileLock.writeLock().unlock();
                break;
            default:
                throw new IllegalArgumentException("Unknown file name: " + fileName);
        }
    }
    
}
