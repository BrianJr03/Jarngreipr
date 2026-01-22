package jr.brian.home;

interface IShellService {
    int forceStop(String packageName);
    int removeFromRecents(String packageName);
    int forceStopAndRemoveFromRecents(String packageName);
    void destroy();
}
