package jr.brian.home;

interface IInputService {
    void destroy() = 16777114;
    void injectKeyEvent(int keyCode, int action) = 1;
    void injectTrigger(int axis, float value) = 2;
}
