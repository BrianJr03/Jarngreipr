package jr.brian.home;

interface IInputService {
    void destroy() = 16777114;
    void injectKeyEvent(int keyCode, int action) = 1;
    void injectTrigger(int axis, float value) = 2;
    void injectJoystick(float rightX, float rightY) = 3;
    void injectLeftJoystick(float leftX, float leftY) = 4;
    void injectMouseMove(float x, float y) = 5;
    void injectMouseClick(float x, float y, int button, int action) = 6;
    void injectMouseScroll(float x, float y, float vscroll) = 7;
}
