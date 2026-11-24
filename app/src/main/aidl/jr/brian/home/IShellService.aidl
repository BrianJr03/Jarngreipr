package jr.brian.home;

interface IShellService {
    int forceStop(String packageName);
    int removeFromRecents(String packageName);
    int forceStopAndRemoveFromRecents(String packageName);
    int putSetting(String namespace, String key, String value);
    void destroy();
}
