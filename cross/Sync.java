package cross;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Sync {
    public static final ReentrantReadWriteLock bidMapLock = new ReentrantReadWriteLock();
    public static final ReentrantReadWriteLock askMapLock = new ReentrantReadWriteLock();
    public static final ReentrantReadWriteLock markerPriceLock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock bidMapFileLock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock askMapFileLock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock bidMapTempFileLock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock askMapTempFileLock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock usersMapFileLock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock usersMapTempFileLock = new ReentrantReadWriteLock();

    public static void safeReadStarts (String fileName) {
        switch (fileName) {
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
            case Costants.USERS_MAP_FILE:
                usersMapFileLock.readLock().lock();
                break;
            case Costants.USERS_MAP_TEMP_FILE:
                usersMapTempFileLock.readLock().lock();
                break;
            default:
                throw new IllegalArgumentException("Unknown file name: " + fileName);
        }
    }

    public static void safeReadEnds (String fileName) {
        switch (fileName) {
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
            case Costants.USERS_MAP_FILE:
                usersMapFileLock.readLock().unlock();
                break;
            case Costants.USERS_MAP_TEMP_FILE:
                usersMapTempFileLock.readLock().unlock();
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
            default:
                throw new IllegalArgumentException("Unknown file name: " + fileName);
        }
    }
    
}
