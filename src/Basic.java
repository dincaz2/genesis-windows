public class Basic {
    public void snippetBegin() {  }

    public void snippetEnd() {  }

    public void test() {
        int x = 0;
        int y = 1;
        int z = 0;
        snippetBegin();
        z += x + y;
        snippetEnd();
        return;
    }
}
